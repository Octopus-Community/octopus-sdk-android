package com.octopuscommunity.sdk.ui.access

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.domain.model.CommunityAccessDenial
import com.octopuscommunity.sdk.domain.model.OctopusEvent
import com.octopuscommunity.sdk.domain.model.UserConfig
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.ClientUserError
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository
import com.octopuscommunity.sdk.domain.repository.recoverFromDeletedAccount
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.UiText
import com.octopuscommunity.sdk.ui.common.errorText
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CommunityAccessDeniedViewModel(
    commonParameters: CommonParameters,
) : OctopusViewModel(commonParameters) {
    @Immutable
    data class UiState(
        @StringRes val title: Int = R.string.community_access_denied_unavailable_title,
        val subtitle: Subtitle? = null,
        val button: Button? = null,
        val isButtonLoading: Boolean = false,
        /** The policy shown by the "Know more" sheet; the sheet is open while this is set. */
        val policy: Policy? = null,
    ) {
        @Immutable
        sealed interface Subtitle {
            /** The message of the server, shown as it is. */
            @Immutable
            data class ServerMessage(
                val text: String,
            ) : Subtitle

            /** What the sentences about a ban are built from. */
            @Immutable
            data class Banned(
                val nickname: String?,
                val policyName: String,
                val contactEmail: String?,
            ) : Subtitle
        }

        /**
         * The text of the screen as it was before the title, subtitle and button: the message of the server, the
         * sentences of a ban, or the title when there is no message.
         */
        @Deprecated(
            "The message alone no longer describes the screen: read title, subtitle and button, or " +
                "OctopusSDK.communityAccessDeniedReason. Will be removed in the next major version.",
            level = DeprecationLevel.WARNING,
        )
        val accessDeniedMessage: UiText
            get() =
                when (val subtitle = subtitle) {
                    is Subtitle.ServerMessage -> UiText.DynamicString(subtitle.text)
                    is Subtitle.Banned -> UiText.JoinedResource(subtitle.paragraphs(), separator = "\n")
                    null -> UiText.StringResource(title)
                }

        enum class Button { KnowMore, AccessCommunity }

        @Immutable
        data class Policy(
            val name: String,
            val description: String,
        )
    }

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private var denial: CommunityAccessDenial? = null

    /** Whether the screen event of the current display was emitted. */
    private var displayed = false

    init {
        viewModelScope.launch {
            connectionRepository.getUserConfigFlow().collectLatest { userConfig ->
                denial = userConfig?.accessDenial
                // The access came back: the next denial is another display of the screen.
                if (userConfig?.canAccessCommunity == true) displayed = false
                _state.update { current ->
                    userConfig.toAccessDeniedUiState().copy(
                        isButtonLoading = current.isButtonLoading,
                        // A sheet left open must not show the policy of a ban that is no longer the reason.
                        policy = current.policy?.let { denial.policyOrNull() },
                    )
                }
            }
        }
    }

    fun onResume() {
        viewModelScope.launch {
            connectionRepository.fetchCommunityAccess()
        }
    }

    /** Tells the host app the screen is displayed, once per display. */
    fun onDisplayed() {
        if (displayed) return
        displayed = true
        viewModelScope.launch { eventRepository.emitEvent(OctopusEvent.ScreenDisplayed.CommunityAccessDenied) }
    }

    fun onButtonClick() {
        when (state.value.button) {
            UiState.Button.KnowMore -> _state.update { it.copy(policy = denial.policyOrNull()) }
            UiState.Button.AccessCommunity -> recoverFromDeletedAccount()
            null -> Unit
        }
    }

    fun onPolicyDismiss() {
        _state.update { it.copy(policy = null) }
    }

    private fun recoverFromDeletedAccount() {
        // One return at a time: a second tap would give up the session the first one is bringing.
        if (state.value.isButtonLoading) return
        _state.update { it.copy(isButtonLoading = true) }
        viewModelScope.launch {
            try {
                when (val result = connectionRepository.recoverFromDeletedAccount()) {
                    is OctopusResult.Success -> Unit
                    // A typed refusal of the server, a ban above all, is said in full.
                    is OctopusResult.Failure.InvalidArguments ->
                        showAlertDialog(
                            result.errors
                                .filterIsInstance<ClientUserError.UserBanned>()
                                .firstOrNull()
                                ?.let { connectionRepository.banMessageText(it.errorMessage) }
                                ?: result.errorText,
                        )
                    is OctopusResult.Failure -> showError(result)
                }
            } finally {
                _state.update { it.copy(isButtonLoading = false) }
            }
        }
    }
}

private fun CommunityAccessDenial?.policyOrNull() =
    (this as? CommunityAccessDenial.AccountBanned)
        ?.takeIf { it.policyDescription.isNotEmpty() }
        ?.let { CommunityAccessDeniedViewModel.UiState.Policy(name = it.policyName, description = it.policyDescription) }

/**
 * The screen for this access, without what the member did on it (loading button, open sheet). A denial without
 * reason, and an access not known yet, show the screen of an unknown reason.
 */
internal fun UserConfig?.toAccessDeniedUiState(): CommunityAccessDeniedViewModel.UiState {
    // Without a message of the server the title stands alone.
    val message =
        this
            ?.accessDeniedMessage
            ?.ifEmpty { null }
            ?.let { CommunityAccessDeniedViewModel.UiState.Subtitle.ServerMessage(it) }
    return when (val denial = this?.accessDenial) {
        CommunityAccessDenial.NotInCohort ->
            CommunityAccessDeniedViewModel.UiState(R.string.community_access_denied_cohort_title, message)

        CommunityAccessDenial.CommunityTemporarilyDisabled ->
            CommunityAccessDeniedViewModel.UiState(R.string.community_access_denied_temporarily_disabled_title, message)

        CommunityAccessDenial.CommunityClosed ->
            CommunityAccessDeniedViewModel.UiState(R.string.community_access_denied_closed_title, message)

        is CommunityAccessDenial.AccountBanned ->
            CommunityAccessDeniedViewModel.UiState(
                title = R.string.community_access_denied_banned_title_formal,
                subtitle =
                    CommunityAccessDeniedViewModel.UiState.Subtitle.Banned(
                        nickname = denial.nickname,
                        policyName = denial.policyName,
                        contactEmail = denial.contactEmail,
                    ),
                button =
                    CommunityAccessDeniedViewModel.UiState.Button.KnowMore
                        .takeIf { denial.policyOrNull() != null },
            )

        // The default message of the server promises a way back in: the button is always there.
        CommunityAccessDenial.AccountDeleted ->
            CommunityAccessDeniedViewModel.UiState(
                title = R.string.community_access_denied_deleted_title_formal,
                subtitle = message,
                button = CommunityAccessDeniedViewModel.UiState.Button.AccessCommunity,
            )

        CommunityAccessDenial.MemberExcluded,
        CommunityAccessDenial.Unknown,
        null,
        -> CommunityAccessDeniedViewModel.UiState(R.string.community_access_denied_unavailable_title, message)
    }
}

/** The sentences about a ban: why, then how to appeal when the community gives an address. */
internal fun CommunityAccessDeniedViewModel.UiState.Subtitle.Banned.paragraphs(): List<UiText> =
    listOfNotNull(
        if (nickname != null) {
            UiText.StringResource(R.string.community_access_denied_banned_text_formal, listOf(nickname, policyName))
        } else {
            UiText.StringResource(R.string.community_access_denied_banned_text_without_nickname_formal, listOf(policyName))
        },
        contactEmail?.let { UiText.StringResource(R.string.community_access_denied_banned_appeal_formal, listOf(it)) },
    )

/**
 * The text of a ban alert for [message], the ban message the connection gave. That message is the server's or,
 * without one, a sentence composed in English for the host app: the SDK says that one with its own, localized
 * sentence.
 */
internal suspend fun ConnectionRepository.banMessageText(message: String): UiText {
    val access = getUserConfig()
    val ban = access?.accessDenial as? CommunityAccessDenial.AccountBanned
    if (ban == null || !access.accessDeniedMessage.isNullOrEmpty()) return UiText.DynamicString(message)
    return CommunityAccessDeniedViewModel.UiState.Subtitle
        .Banned(nickname = ban.nickname, policyName = ban.policyName, contactEmail = null)
        .paragraphs()
        .first()
}
