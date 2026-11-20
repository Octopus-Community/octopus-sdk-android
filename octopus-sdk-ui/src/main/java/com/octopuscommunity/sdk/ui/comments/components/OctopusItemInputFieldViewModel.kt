package com.octopuscommunity.sdk.ui.comments.components

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.domain.model.CommunityConfig.ConsentTexts
import com.octopuscommunity.sdk.domain.model.CommunityConfig.TermsAcceptanceMode
import com.octopuscommunity.sdk.domain.model.OctopusItem
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.ui.OctopusDestination.ValidateNickname
import com.octopuscommunity.sdk.ui.common.UiText
import com.octopuscommunity.sdk.ui.common.errorText
import com.octopuscommunity.sdk.ui.common.validation.PictureValidationError
import com.octopuscommunity.sdk.ui.common.validation.TextValidationError
import com.octopuscommunity.sdk.ui.common.validation.findError
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusViewModel
import com.octopuscommunity.sdk.ui.consent.shouldPresentConsentSheet
import com.octopuscommunity.sdk.ui.profile.current.nickname.NicknameChoiceContribution
import com.octopuscommunity.sdk.ui.profile.current.nickname.NicknameChoiceHold
import com.octopuscommunity.sdk.ui.profile.current.nickname.NicknameChoiceSession
import com.octopuscommunity.sdk.ui.profile.current.nickname.shouldPresentNicknameChoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class OctopusItemInputFieldViewModel<Item : OctopusItem, DraftItem : OctopusItem.Draft> internal constructor(
    commonParameters: CommonParameters,
    private val userAction: UserAction,
    private val draftItem: DraftItem,
    private val sendItem: suspend (DraftItem) -> OctopusResult<*, *>,
    private val nicknameChoiceSession: NicknameChoiceSession,
) : OctopusViewModel(commonParameters) {
    @Immutable
    data class UiState<U : OctopusItem.Draft>(
        val item: U,
        val hasAcceptedCgu: Boolean = false,
        val isSending: Boolean = false,
        val textError: TextValidationError? = null,
        val mediaError: PictureValidationError? = null,
        val serverErrors: UiText? = null,
        // Community config gating for the add-picture affordance. Default `true` keeps today's
        // behaviour until the config resolves (and for unseeded communities). Resolved per surface:
        // comment uses `contentOptions.comment`, reply uses `contentOptions.reply`.
        val enablePictures: Boolean = true,
        // Explicit terms-acceptance. `IMPLICIT` (default until the config resolves) keeps today's
        // behaviour; the explicit modes present the consent sheet at the first contribution.
        val termsAcceptanceMode: TermsAcceptanceMode = TermsAcceptanceMode.IMPLICIT,
        val showConsentSheet: Boolean = false,
        // The community's own consent wordings (null = the SDK's), resolved against
        // [termsAcceptanceMode] by the input field and the consent sheet.
        val consentTexts: ConsentTexts? = null,
        // Whether the nickname choice comes right after the consent sheet: its button then reads
        // "Accept", the contribution only going out once the nickname is confirmed.
        val consentSheetLeadsToNicknameChoice: Boolean = false,
    ) {
        val isEditable = !isSending
        val hasErrors = textError != null || mediaError != null || serverErrors != null
        val canSend = isEditable && textError == null && mediaError == null && item.hasContent

        // Only the implicit mode shows the inline legal disclaimer. The explicit modes use the
        // consent sheet instead, so the inline banner must be suppressed there (otherwise it would
        // sit under the sheet — matches iOS `CreateCommentView`/`CreateReplyView` `!isExplicit`).
        val showCguText = !termsAcceptanceMode.isExplicit && !hasAcceptedCgu && canSend
    }

    // Protected (not private) so the comment/reply subclasses update it from their
    // updateText / updateImage overrides; the ktlint backing-property rule assumes private.
    @Suppress("ktlint:standard:backing-property-naming")
    protected val _state = MutableStateFlow(UiState(item = draftItem))
    val state = _state.asStateFlow()

    // Set once the user has explicitly accepted via the consent sheet in this session, so the gate
    // is not re-triggered between the sheet's confirm and the profile's `hasAcceptedCgu` flip.
    private var consentGivenThisSession = false

    // A contribution held back by the nickname choice, for the member it was held for (see
    // NicknameChoiceHold).
    private var nicknameChoiceHold: NicknameChoiceHold? = null

    abstract fun updateText(text: String)

    abstract fun updateImage(
        context: Context,
        localUri: Uri?,
    )

    init {
        viewModelScope.launch {
            currentUserProfile.collectLatest { profile ->
                _state.update {
                    it.copy(hasAcceptedCgu = profile?.hasAcceptedCgu != false)
                }
                val hold = nicknameChoiceHold ?: return@collectLatest
                when (hold.resolution(profile)) {
                    NicknameChoiceHold.Resolution.SEND -> {
                        nicknameChoiceHold = null
                        send()
                    }

                    NicknameChoiceHold.Resolution.DROP -> {
                        nicknameChoiceHold = null
                        nicknameChoiceSession.finish()
                    }

                    NicknameChoiceHold.Resolution.KEEP_WAITING -> Unit
                }
            }
        }
        viewModelScope.launch {
            // Closed without a choice: the contribution stays in the field and is never sent on its
            // own later.
            nicknameChoiceSession.closedWithoutChoice.collect { nicknameChoiceHold = null }
        }
        viewModelScope.launch {
            communityConfig.collectLatest { config ->
                config ?: return@collectLatest
                val contentOptions = config.contentOptions
                val enablePictures =
                    when (userAction) {
                        UserAction.Comment -> contentOptions.comment.enablePictures
                        UserAction.Reply -> contentOptions.reply.enablePictures
                        else -> true
                    }
                _state.update {
                    it.copy(
                        enablePictures = enablePictures,
                        termsAcceptanceMode = config.termsAcceptanceMode,
                        consentTexts = config.consentTexts,
                    )
                }
            }
        }
    }

    fun send() =
        checkAuthenticated(userAction) { profile ->
            val state = _state.value.takeIf { it.canSend } ?: return@checkAuthenticated

            // First-contribution gates, in this order: consent → nickname choice → publish.
            // Explicit terms-acceptance gate: on the user's first contribution, present the consent
            // sheet BEFORE anything else, the nickname choice included; the send resumes from
            // [acceptConsentAndSend].
            if (shouldPresentConsentSheet(
                    mode = state.termsAcceptanceMode,
                    hasAcceptedCgu = profile.hasAcceptedCgu,
                    consentGivenThisSession = consentGivenThisSession,
                )
            ) {
                // The nickname choice gate below, evaluated ahead: it labels the sheet's button.
                val nicknameChoiceFollows =
                    shouldPresentNicknameChoice(
                        action = userAction,
                        profile = profile,
                        config = configRepository.getCommunityConfig(),
                        appManagedFields = appManagedFields,
                    )
                _state.update {
                    it.copy(
                        showConsentSheet = true,
                        consentSheetLeadsToNicknameChoice = nicknameChoiceFollows,
                    )
                }
                return@checkAuthenticated
            }

            // Nickname choice gate: a member whose nickname is not confirmed yet chooses it before the
            // contribution goes. It is held for this member and sent again once their nickname is
            // confirmed (see the profile collector in init).
            if (shouldPresentNicknameChoice(
                    action = userAction,
                    profile = profile,
                    config = configRepository.getCommunityConfig(),
                    appManagedFields = appManagedFields,
                )
            ) {
                // Already holding one (a second tap during the check or the transition): the screen
                // is open or opening, so do not open it again. The hold is cleared on send, drop and
                // close, so this never sticks.
                if (nicknameChoiceHold != null) return@checkAuthenticated
                nicknameChoiceHold = NicknameChoiceHold(profileId = profile.userId)
                nicknameChoiceSession.open(NicknameChoiceContribution.of(userAction))
                navigateTo(ValidateNickname) { launchSingleTop = true }
                return@checkAuthenticated
            }

            _state.update { it.copy(isSending = true, serverErrors = null) }

            if (!profile.hasAcceptedCgu) {
                when (val result = acceptCgu(profile)) {
                    is OctopusResult.Success -> {}
                    is OctopusResult.Failure -> {
                        _state.update { state -> state.copy(isSending = false) }
                        showError(result)
                        return@checkAuthenticated
                    }
                }
            }

            when (val result = sendItem(state.item)) {
                is OctopusResult.Success -> {
                    _state.update {
                        it.copy(
                            isSending = false,
                            item = draftItem,
                        )
                    }
                }

                is OctopusResult.Failure.InvalidArguments -> {
                    _state.update {
                        it.copy(
                            isSending = false,
                            serverErrors = result.errorText,
                        )
                    }
                    result.findError<OctopusItem.RateLimitExceedError>()?.message?.let { error ->
                        showAlertDialog(error)
                    }
                }

                is OctopusResult.Failure -> {
                    _state.update { it.copy(isSending = false) }
                    showError(result)
                }
            }
        }

    /**
     * Called when the user confirms the explicit-consent sheet. Records that consent was given this
     * session, dismisses the sheet, and resumes [send] — which now passes the consent gate, goes through
     * the nickname choice when the nickname is not confirmed yet, and publishes (the publish path still
     * calls [acceptCgu] since the profile has not accepted yet).
     */
    fun acceptConsentAndSend() {
        consentGivenThisSession = true
        _state.update { it.copy(showConsentSheet = false) }
        send()
    }

    /** Dismisses the consent sheet without publishing (swipe-down / tap outside). */
    fun dismissConsentSheet() {
        _state.update { it.copy(showConsentSheet = false) }
    }
}
