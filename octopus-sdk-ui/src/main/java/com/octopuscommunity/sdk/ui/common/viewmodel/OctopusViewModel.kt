package com.octopuscommunity.sdk.ui.common.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavOptionsBuilder
import com.octopuscommunity.sdk.domain.DrivenLoginTracker
import com.octopuscommunity.sdk.domain.event.LifecycleEvents
import com.octopuscommunity.sdk.domain.model.CommunityClosure
import com.octopuscommunity.sdk.domain.model.CurrentUserProfile
import com.octopuscommunity.sdk.domain.model.MinimalProfile
import com.octopuscommunity.sdk.domain.model.OctopusDrivenLoginAction
import com.octopuscommunity.sdk.domain.model.OctopusItem
import com.octopuscommunity.sdk.domain.model.Post
import com.octopuscommunity.sdk.domain.model.ProfileField
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.network.OctopusResult.Failure.InvalidArguments
import com.octopuscommunity.sdk.domain.network.mapSuccess
import com.octopuscommunity.sdk.domain.network.onFailure
import com.octopuscommunity.sdk.domain.repository.CommunityClosureRepository
import com.octopuscommunity.sdk.domain.repository.CommunityConfigRepository
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository.ConnectionState.Connected
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository.ConnectionState.NotConnected
import com.octopuscommunity.sdk.domain.repository.EventRepository
import com.octopuscommunity.sdk.domain.repository.ExternalLinkRepository
import com.octopuscommunity.sdk.domain.repository.ProfileUpdateError
import com.octopuscommunity.sdk.domain.repository.TrackerRepository
import com.octopuscommunity.sdk.domain.repository.getUserBannedMessage
import com.octopuscommunity.sdk.ui.OctopusDestination
import com.octopuscommunity.sdk.ui.OctopusDestination.CreatePost
import com.octopuscommunity.sdk.ui.OctopusDestination.CurrentUserProfileSummary
import com.octopuscommunity.sdk.ui.OctopusDestination.ProfileSummary
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.access.banMessageText
import com.octopuscommunity.sdk.ui.common.OctopusDialogState
import com.octopuscommunity.sdk.ui.common.OctopusNavigationEvent
import com.octopuscommunity.sdk.ui.common.OctopusNavigationEvent.NavigateTo
import com.octopuscommunity.sdk.ui.common.OctopusNavigationEvent.NavigateToClientObject
import com.octopuscommunity.sdk.ui.common.OctopusNavigationEvent.NavigateToClientProfileEdit
import com.octopuscommunity.sdk.ui.common.OctopusNavigationEvent.NavigateToProfile
import com.octopuscommunity.sdk.ui.common.OctopusNavigationEvent.NavigateToUrl
import com.octopuscommunity.sdk.ui.common.OctopusNavigationEvent.PopBackStackOrNavigateTo
import com.octopuscommunity.sdk.ui.common.OctopusNavigationEvent.PopBackStackTo
import com.octopuscommunity.sdk.ui.common.OctopusNavigator
import com.octopuscommunity.sdk.ui.common.UiText
import com.octopuscommunity.sdk.ui.common.UiText.StringResource
import com.octopuscommunity.sdk.ui.common.errorText
import com.octopuscommunity.sdk.ui.events.OctopusSnackbarEvents
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.coroutines.EmptyCoroutineContext

open class OctopusViewModel internal constructor(
    commonParameters: CommonParameters,
) : ViewModel(commonParameters.coroutineScope()) {
    class CommonParameters(
        val communityConfigRepository: CommunityConfigRepository,
        val connectionRepository: ConnectionRepository,
        val trackerRepository: TrackerRepository,
        val drivenLoginTracker: DrivenLoginTracker,
        val linksRepository: ExternalLinkRepository,
        val lifecycleEvents: LifecycleEvents,
        val navigator: OctopusNavigator,
        val eventRepository: EventRepository,
        val snackbarEvents: OctopusSnackbarEvents,
        val closureRepository: CommunityClosureRepository? = null,
    ) {
        internal var sdkScope: CoroutineScope? = null
        internal var containerScope: CoroutineScope? = null

        internal fun coroutineScope(): CoroutineScope = octopusViewModelScope(sdkScope)
    }

    sealed class UserAction(
        val isStrong: Boolean,
        val needNicknameValidation: Boolean,
    ) {
        data object Post : UserAction(isStrong = true, needNicknameValidation = true)

        data object Comment : UserAction(isStrong = true, needNicknameValidation = true)

        data object Reply : UserAction(isStrong = true, needNicknameValidation = true)

        data object Reaction : UserAction(isStrong = false, needNicknameValidation = false)

        data object Vote : UserAction(isStrong = false, needNicknameValidation = false)

        data object Moderation : UserAction(isStrong = false, needNicknameValidation = false)

        data object BlockUser : UserAction(isStrong = false, needNicknameValidation = false)

        data object ViewOwnProfile : UserAction(isStrong = true, needNicknameValidation = false)
    }

    val configRepository = commonParameters.communityConfigRepository
    val connectionRepository = commonParameters.connectionRepository
    val trackerRepository = commonParameters.trackerRepository
    val drivenLoginTracker = commonParameters.drivenLoginTracker
    val linksRepository = commonParameters.linksRepository
    val lifecycleEvents = commonParameters.lifecycleEvents
    val navigator = commonParameters.navigator
    val eventRepository = commonParameters.eventRepository
    val snackbarEvents = commonParameters.snackbarEvents

    val alertState = navigator.dialogState

    val communityConfig =
        configRepository
            .getCommunityConfigFlow()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null,
            )

    // Stamp the host identity on connected, non-guest profiles.
    val currentUserProfile =
        combine(
            connectionRepository.getProfileFlow(),
            connectionRepository.getClientUserFlow(),
        ) { profile, clientUser ->
            if (profile != null && !profile.isGuest && clientUser != null) {
                profile.copy(clientUserId = clientUser.userId)
            } else {
                profile
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    /**
     * Emits once the session that follows a member's logout is connected. The logout deleted the
     * content cached for that member, and what is on screen has to load again instead of reading
     * the deletion as content deleted on the server.
     *
     * Internal on purpose: mirrors iOS `InternalSdkEvent.userContentCleared` without adding a
     * public [com.octopuscommunity.sdk.domain.model.OctopusEvent]. A guest who becomes a member
     * keeps the content, as does a refreshed token (same user id).
     */
    internal val userContentCleared: Flow<Unit> = currentUserProfile.memberLeft()

    /**
     * Emits each time a guest becomes a member (`connectUser` from a guest session). The content
     * is kept, but what was fetched for the session, such as the root feeds, is the guest's.
     */
    internal val guestBecameMember: Flow<Unit> = currentUserProfile.guestBecameMember()

    /**
     * Emits once the community reopened and a session is there. The closure deleted the cached
     * content, so a screen that outlived it (kept in the back stack, or under the access-denied
     * screen) shows nothing until it loads again.
     */
    internal val communityReopened: Flow<Unit> =
        (commonParameters.closureRepository?.closureFlow ?: emptyFlow())
            .reopenings()
            .onEach { currentUserProfile.filterNotNull().first() }

    /**
     * Emits when what is on screen was deleted from the cache and has to load again: after a
     * member's logout ([userContentCleared]) or once the community reopened ([communityReopened]).
     */
    internal val displayedContentCleared: Flow<Unit> = merge(userContentCleared, communityReopened)

    val appManagedFields = connectionRepository.appManagedFields
    val hasAppManagedFields = appManagedFields.isNotEmpty()

    val showCommunityAccessDenied =
        connectionRepository
            .getUserConfigFlow()
            .map { it?.canAccessCommunity == false }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false,
            )

    val isBlockingContent = showCommunityAccessDenied
    val forcedBack get() = isBlockingContent.value

    fun showError(failure: OctopusResult.Failure<*>) {
        when (failure) {
            is OctopusResult.Failure.UserNotAuthenticated -> {
                failure.reason?.let { reason ->
                    showAlertDialog(text = UiText.DynamicString(reason))
                } ?: showErrorSnackbar(failure.errorText)
            }

            else -> showErrorSnackbar(failure.errorText)
        }
    }

    /**
     * @param onRetry re-runs the fetch that failed. A load failure over visible content offers it as
     * the snackbar's action; failures with nothing to retry leave it out.
     */
    fun showErrorSnackbar(
        message: UiText,
        isNoNetwork: Boolean = false,
        onRetry: (() -> Unit)? = null,
    ) = viewModelScope.launch {
        this@OctopusViewModel.snackbarEvents.emitEvent(
            OctopusSnackbarEvents.Event.Error(
                message = message,
                onRetry = onRetry,
                isNoNetwork = isNoNetwork,
            ),
        )
    }

    fun showAlertDialog(
        text: UiText,
        title: UiText? = null,
        onDismiss: () -> Unit = {},
    ) = navigator.setDialogState(
        OctopusDialogState.Alert(
            text = text,
            title = title,
            onDismiss = {
                navigator.setDialogState(null)
                onDismiss()
            },
        ),
    )

    fun showConfirmDialog(
        text: UiText,
        title: UiText? = null,
        confirmButtonText: UiText = StringResource(R.string.common_ok),
        dismissButtonText: UiText = StringResource(R.string.common_cancel),
        onConfirm: () -> Unit = {},
        onDismiss: () -> Unit = {},
    ) = navigator.setDialogState(
        OctopusDialogState.Confirm(
            title = title,
            text = text,
            confirmButtonText = confirmButtonText,
            dismissButtonText = dismissButtonText,
            onConfirm = {
                navigator.setDialogState(null)
                onConfirm()
            },
            onDismiss = {
                navigator.setDialogState(null)
                onDismiss()
            },
        ),
    )

    fun showLoadingDialog() {
        navigator.setDialogState(
            OctopusDialogState.Loading(
                onDismiss = {
                    hideDialog()
                },
            ),
        )
    }

    fun hideDialog() {
        navigator.setDialogState(null)
    }

    fun navigateTo(
        destination: OctopusDestination,
        navOptions: (NavOptionsBuilder.() -> Unit) = {},
    ) {
        sendNavigationEvent(NavigateTo(destination, navOptions))
    }

    fun navigateUp() = sendNavigationEvent(OctopusNavigationEvent.NavigateUp)

    fun popBackStackTo(
        destination: OctopusDestination,
        inclusive: Boolean = false,
    ) = sendNavigationEvent(
        PopBackStackTo(
            destination = destination,
            inclusive = inclusive,
        ),
    )

    fun popBackStackOrNavigateTo(destination: OctopusDestination) =
        sendNavigationEvent(
            PopBackStackOrNavigateTo(destination = destination),
        )

    fun navigateToProfileEdit(fieldToEdit: ProfileField? = null) {
        viewModelScope.launch {
            val currentUserProfile = currentUserProfile.first() ?: return@launch

            if (appManagedFields.isNotEmpty() && !currentUserProfile.isGuest) {
                if (!appManagedFields.containsAll(ProfileField.ALL)) {
                    // Mixed
                    if (fieldToEdit != null) {
                        if (fieldToEdit in appManagedFields) {
                            navigateToClientProfileEdit(fieldToEdit = fieldToEdit)
                        } else {
                            navigateToOctopusProfileEdit(fieldToEdit = fieldToEdit)
                        }
                    } else {
                        navigateToOctopusProfileEdit(fieldToEdit = fieldToEdit)
                    }
                } else {
                    // EditInApp
                    navigateToClientProfileEdit(fieldToEdit = fieldToEdit)
                }
            } else {
                // EditInOctopus
                navigateToOctopusProfileEdit(fieldToEdit = fieldToEdit)
            }
        }
    }

    fun navigateToOctopusProfileEdit(fieldToEdit: ProfileField? = null) {
        navigateTo(OctopusDestination.CurrentUserProfileEdit(focusedField = fieldToEdit))
    }

    fun navigateToClientProfileEdit(fieldToEdit: ProfileField?) {
        if (fieldToEdit != null) {
            showConfirmDialog(
                text = StringResource(R.string.profile_edit_client_app_alert_title_formal),
                confirmButtonText = StringResource(R.string.common_ok),
                dismissButtonText = StringResource(R.string.common_cancel),
                onConfirm = {
                    sendNavigationEvent(NavigateToClientProfileEdit(fieldToEdit = fieldToEdit))
                },
            )
        } else {
            sendNavigationEvent(NavigateToClientProfileEdit(fieldToEdit = fieldToEdit))
        }
    }

    fun navigateToProfile(item: OctopusItem) = item.author?.let { navigateToProfile(it) }

    /**
     * Single funnel for every profile tap inside the community (post/comment/reply headers,
     * notifications), including a tap on the connected user's own profile. Emits
     * [OctopusNavigationEvent.NavigateToProfile] carrying every input the dispatcher
     * ([OctopusScreenContent]) needs to resolve the target (see `resolveProfileTapTarget`): the
     * tapped member's [MinimalProfile.clientUserId], whether the community exposes client ids, the
     * native profile destination, and the Octopus activity destination used for members without a
     * client id (guest / BO / admin).
     */
    fun navigateToProfile(profile: MinimalProfile) {
        val userId = profile.userId
        viewModelScope.launch {
            // Read exposeClientUserId from the repository (a local Room read — NOT a network call)
            // rather than communityConfig.value: that StateFlow is WhileSubscribed, so on a screen
            // whose composables don't collect it (e.g. the Activity screen) it stays cold and .value
            // is null → the tap would wrongly fall back to the native profile even when Unified
            // Profile is active. getCommunityConfig() reads the cached config, so no server call.
            val exposeClientUserId = configRepository.getCommunityConfig()?.exposeClientUserId ?: false
            navigator.sendNavigationEvent(
                NavigateToProfile(
                    userId = userId,
                    clientUserId = profile.clientUserId,
                    exposeClientUserId = exposeClientUserId,
                    destination =
                        profileDestination(
                            userId = userId,
                            // currentUserProfile is a WhileSubscribed StateFlow too, but unlike
                            // communityConfig it is collected by every profile-bearing screen
                            // (Activity, posts, profile summary), so .value is warm here; it only
                            // feeds the native-fallback destination anyway.
                            currentUserId = currentUserProfile.value?.userId,
                        ),
                    activityDestination = OctopusDestination.Activity(userId = userId),
                ),
            )
        }
    }

    /**
     * Opens the create-post screen. The auth gate is deliberately deferred to
     * [CreatePostViewModel.sendPost], which routes the host's auth flow when a guest user taps
     * "Post" — matching iOS `CreatePostViewModel.send()` (internal change), where
     * `ensureConnected(action: .post)` gates publish, not navigation. The user can compose a
     * draft before being asked to authenticate.
     */
    fun navigateToCreatePost(
        type: Post.Draft.Type = Post.Draft.Type.TEXT,
        groupId: String? = null,
    ) = navigateTo(CreatePost(type = type, groupId = groupId))

    fun navigateToClientLogin() = sendNavigationEvent(OctopusNavigationEvent.NavigateToClientLogin)

    fun navigateToClientObject(post: Post) =
        post.clientObject?.id?.let {
            viewModelScope.launch { trackerRepository.trackClientObjectOpened() }
            sendNavigationEvent(NavigateToClientObject(id = it))
        }

    fun navigateToCustomAction(post: Post) =
        post.customAction?.targetUrl?.let {
            viewModelScope.launch { trackerRepository.trackCtaButtonClicked(post.id) }
            navigateToUrl(url = it)
        }

    fun navigateToTermsOfUse() = navigateToUrl(url = linksRepository.termsOfUseUrl)

    fun navigateToPrivacyPolicy() = navigateToUrl(url = linksRepository.privacyPolicyUrl)

    fun navigateToCommunityGuidelines() = navigateToUrl(url = linksRepository.communityGuidelinesUrl)

    fun navigateToUrl(url: String) = sendNavigationEvent(NavigateToUrl(url = url))

    /**
     * Ensures the user is connected (converting a guest / prompting login as needed) then runs
     * [block] with the resolved profile.
     *
     * [block] runs once the user is connected, the community config is loaded and any forced login
     * is done; it does not run for a banned member's strong action. A first post, comment or reply is
     * gated by the send block itself, in this order: it presents the explicit terms-acceptance consent
     * sheet when needed, then the nickname choice (`shouldPresentNicknameChoice`) when the member's
     * nickname is not confirmed yet, and only then publishes.
     */
    fun checkAuthenticated(
        action: UserAction,
        block: suspend (profile: CurrentUserProfile) -> Unit,
    ) {
        launchCheckAuthenticated(action, block = block)
    }

    /**
     * [checkAuthenticated], returning its [Job] so a caller can tell when the check has settled,
     * including the exits where [block] never runs (login prompt, failed connection or config).
     * [exceptionHandler] lets completion-aware callers handle coroutine exceptions, and [onConfigFailure]
     * distinguishes a failed configuration fetch from an authentication refusal.
     */
    internal fun launchCheckAuthenticated(
        action: UserAction,
        exceptionHandler: CoroutineExceptionHandler? = null,
        onConfigFailure: () -> Unit = {},
        block: suspend (profile: CurrentUserProfile) -> Unit,
    ): Job =
        viewModelScope.launch(exceptionHandler ?: EmptyCoroutineContext) {
            when (val connectionState = connectionRepository.getConnectionState()) {
                is NotConnected -> {
                    showLoadingDialog()
                    val result =
                        try {
                            connectionRepository.checkConnection()
                        } finally {
                            hideDialog()
                        }
                    result
                        .onFailure { failure ->
                            when (failure) {
                                is InvalidArguments -> showAlertDialog(failure.errorText)
                                else -> showError(failure)
                            }
                        }
                }

                is Connected -> {
                    // Keep browsing on the existing guest; only strong actions consult the
                    // retained SSO refusal, without refreshing a profile or exchanging a token.
                    val banMessage = if (action.isStrong) connectionRepository.getUserBannedMessage() else null
                    if (banMessage != null) {
                        showAlertDialog(connectionRepository.banMessageText(banMessage))
                        return@launch
                    }
                    val profile = connectionState.profile

                    var communityConfig = configRepository.getCommunityConfig()
                    if (communityConfig == null) {
                        showLoadingDialog()
                        val result =
                            try {
                                configRepository.fetchCommunityConfig()
                            } finally {
                                hideDialog()
                            }
                        when (result) {
                            is OctopusResult.Success -> {
                                communityConfig = result.data
                            }

                            is OctopusResult.Failure -> {
                                showError(result)
                                onConfigFailure()
                                return@launch
                            }
                        }
                    }
                    val forceLoginOnStrongActions = communityConfig.forceLoginOnStrongActions
                    if (action.isStrong && profile.isGuest && forceLoginOnStrongActions) {
                        // Record that Octopus presented the login screen for this action, so the
                        // `octopus_driven_login` event can be attributed if the user then logs in
                        // (StartCollectingDrivenLoginUseCase). Mirrors iOS ConnectedActionChecker.
                        drivenLoginTracker.notifyLoginRequested(action.toDrivenLoginAction())
                        navigateToClientLogin()
                    } else {
                        block(profile)
                    }
                }
            }
        }

    private fun UserAction.toDrivenLoginAction(): OctopusDrivenLoginAction =
        when (this) {
            UserAction.Post -> OctopusDrivenLoginAction.POST
            UserAction.Comment -> OctopusDrivenLoginAction.COMMENT
            UserAction.Reply -> OctopusDrivenLoginAction.REPLY
            UserAction.Reaction -> OctopusDrivenLoginAction.REACTION
            UserAction.Vote -> OctopusDrivenLoginAction.VOTE
            UserAction.Moderation -> OctopusDrivenLoginAction.MODERATION
            UserAction.BlockUser -> OctopusDrivenLoginAction.BLOCK_USER
            UserAction.ViewOwnProfile -> OctopusDrivenLoginAction.VIEW_OWN_PROFILE
        }

    suspend fun acceptCgu(profile: CurrentUserProfile): OctopusResult<CurrentUserProfile, ProfileUpdateError> =
        connectionRepository
            .updateProfile(
                newProfile =
                    profile.copy(
                        hasAcceptedCgu = true,
                    ),
                previousProfile = profile,
            ).mapSuccess { it.newProfile }

    fun sendEmail(
        context: Context,
        email: String,
    ) {
        runCatching {
            context.startActivity(
                // The address is text someone typed: trimmed, and escaped rather than pasted into the link as it is.
                Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", email.trim(), null)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        }
    }

    private fun sendNavigationEvent(event: OctopusNavigationEvent) {
        viewModelScope.launch {
            navigator.sendNavigationEvent(event)
        }
    }
}

/**
 * Resolves the SDK's native profile destination for a profile tap: the connected user's own
 * profile graph when [userId] is the current user, otherwise the public profile of [userId].
 * Pure so the self/other routing stays unit-testable independently of the ViewModel wiring.
 */
internal fun profileDestination(
    userId: String,
    currentUserId: String?,
): OctopusDestination =
    when (userId) {
        currentUserId -> CurrentUserProfileSummary.Posts
        else -> ProfileSummary(userId = userId)
    }

/** The three destinations of a profile tap (Unified Profile, internal ticket). */
internal enum class ProfileTapTarget {
    /** Hand the tap to the host's `onNavigateToProfile` with the tapped member's client user id. */
    HOST_CLIENT_PROFILE,

    /** Open the Octopus posts-only activity when an active member has no client id. */
    OCTOPUS_ACTIVITY,

    /** Open the SDK's native profile when Unified Profile is inactive. */
    NATIVE_PROFILE,
}

/**
 * Whether Unified Profile is active: BOTH the backend flag [exposeClientUserId] and the host's
 * `onNavigateToProfile` callback [onNavigateToProfileWired] are required. A host without the
 * callback keeps native profiles even when the backend flag changes. Matches released v1.14.2.
 *
 * @param exposeClientUserId whether the backend community config exposes client user ids.
 * @param onNavigateToProfileWired whether the host wired `onNavigateToProfile`.
 */
internal fun unifiedProfileActive(
    exposeClientUserId: Boolean,
    onNavigateToProfileWired: Boolean,
): Boolean = exposeClientUserId && onNavigateToProfileWired

/**
 * Resolves how a profile tap is handled:
 * - inactive (flag off or callback absent) → native profile;
 * - active, member without a client id (guest / BO / admin) → Octopus posts-only activity;
 * - active, member with a client id → host profile via `onNavigateToProfile`.
 *
 * @param unifiedActive whether Unified Profile is active (see [unifiedProfileActive]).
 * @param clientUserId the tapped member's client user id, or `null`.
 */
internal fun resolveProfileTapTarget(
    unifiedActive: Boolean,
    clientUserId: String?,
): ProfileTapTarget =
    when {
        !unifiedActive -> ProfileTapTarget.NATIVE_PROFILE
        clientUserId == null -> ProfileTapTarget.OCTOPUS_ACTIVITY
        else -> ProfileTapTarget.HOST_CLIENT_PROFILE
    }

/**
 * Emits each time the connected user changes away from a member (logout, or another member
 * connecting): the moments the SDK purges the content cached for that member. The first profile
 * seen only sets the baseline, so a screen that starts collecting never reloads for nothing.
 */
internal fun Flow<CurrentUserProfile?>.memberLeft(): Flow<Unit> =
    filterNotNull()
        .distinctUntilChangedBy { it.userId }
        .runningFold(null as Pair<CurrentUserProfile?, CurrentUserProfile>?) { change, profile ->
            change?.second to profile
        }.mapNotNull { it?.first }
        .filter { !it.isGuest }
        .map { }

/**
 * Emits each time the community reopens. The first state seen only sets the baseline, so a
 * community open at launch does not emit.
 */
internal fun Flow<CommunityClosure?>.reopenings(): Flow<Unit> =
    map { it != null }
        .distinctUntilChanged()
        .drop(1)
        .filter { closed -> !closed }
        .map { }

/**
 * Emits each time a guest becomes a member. Sessions are told apart by user id and guest status,
 * so a guest who becomes a member under the same user id is seen too, and the same profile emitted
 * again (an edited nickname, a profile fetched again) is not. The first profile seen only sets the
 * baseline.
 */
internal fun Flow<CurrentUserProfile?>.guestBecameMember(): Flow<Unit> =
    filterNotNull()
        .distinctUntilChangedBy { it.userId to it.isGuest }
        .runningFold(null as Pair<CurrentUserProfile?, CurrentUserProfile>?) { change, profile ->
            change?.second to profile
        }.filter { change -> change != null && change.first?.isGuest == true && !change.second.isGuest }
        .map { }
