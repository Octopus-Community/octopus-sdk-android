package com.octopuscommunity.sdk.ui.report.common

import androidx.compose.runtime.Immutable
import com.octopuscommunity.sdk.domain.model.Moderation
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.UiText
import com.octopuscommunity.sdk.ui.common.errorText
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal abstract class ReportViewModel(
    commonParameters: CommonParameters,
    reasons: List<Moderation.ReportReason>,
) : OctopusViewModel(commonParameters) {
    @Immutable
    internal data class UiState(
        val reasons: List<Moderation.ReportReason>,
        val selectedReasons: List<Moderation.ReportReason> = listOf(),
        val isSending: Boolean = false,
        val error: UiText? = null,
        // Block offered by the "Report sent" alert (internal ticket). The report is already sent at that
        // point: neither declining nor a failed block touches it. Outcome dialogs live on the
        // navigator so they survive sheet disposal; [Event] only tells an open sheet to close.
        val isBlocking: Boolean = false,
    ) {
        val canSend = selectedReasons.isNotEmpty() && !isSending
    }

    internal sealed interface Event {
        // One-shot signal that the report was sent successfully. Modeled as an event (not a
        // persistent flag in UiState) because the host ViewModelStore keeps this VM alive across
        // sheet dismissals for the same target — a persistent flag would replay the success
        // dialog on every subsequent open. iOS sidesteps this with @StateObject (new VM per
        // presentation); we keep one VM and fire a transient event instead.
        //
        // MutableSharedFlow with replay=0 (instead of a buffered Channel) ensures the event is
        // dropped if no subscriber is active at emit time — protects against the race where the
        // sheet is dismissed mid-flight: the in-flight `sendReport` completes after the composable
        // is gone, the event has no subscriber, and is discarded rather than buffered for the
        // next reopen.
        data object Sent : Event

        /**
         * The block succeeded: the sheet closes. The "This user is blocked" confirmation is already
         * up at screen level (navigator dialog), because the blocked author's card, and on post
         * details the screen itself, leave composition right away and would take the sheet with
         * them.
         */
        data object Blocked : Event

        /** The block failed: [error] is shown at screen level and the sheet closes. */
        data class BlockFailed(
            val error: UiText,
        ) : Event
    }

    private val _state =
        MutableStateFlow(
            UiState(reasons = reasons),
        )
    val state = _state.asStateFlow()

    private val _events =
        MutableSharedFlow<Event>(
            replay = 0,
            extraBufferCapacity = 1,
        )
    val events: Flow<Event> = _events.asSharedFlow()

    fun toggleSelectedReason(reason: Moderation.ReportReason) {
        _state.update {
            it.copy(
                selectedReasons =
                    if (it.selectedReasons.contains(reason)) {
                        it.selectedReasons - reason
                    } else {
                        it.selectedReasons + reason
                    },
            )
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    /**
     * Blocks [userId] from the "Report sent" alert. No intermediate "Block this user" confirmation
     * (the alert already asked) and no further report: this only calls the existing block. On
     * success the confirmation goes through the screen-level dialog, as the menu's own Block does,
     * and [onBlocked] runs when it is dismissed. Failures use the same dialog path so the error
     * survives a sheet dismissed while the block is running, without calling [onBlocked].
     */
    fun blockUser(
        userId: String,
        onBlocked: () -> Unit = {},
    ) {
        // Raised synchronously, before the connection / config reads, so a second Block tap (or Not
        // now, disabled while blocking) cannot slip in; lowered once the check settles, including
        // its exits where the block never runs.
        if (_state.value.isBlocking) return
        _state.update { it.copy(isBlocking = true) }
        launchCheckAuthenticated(action = UserAction.BlockUser) {
            when (val result = connectionRepository.blockProfile(userId)) {
                is OctopusResult.Success -> {
                    showAlertDialog(
                        text = UiText.StringResource(R.string.block_profile_done_title),
                        onDismiss = onBlocked,
                    )
                    _events.tryEmit(Event.Blocked)
                }

                is OctopusResult.Failure -> {
                    val error = result.errorText
                    showAlertDialog(text = error)
                    _events.tryEmit(Event.BlockFailed(error))
                }
            }
        }.invokeOnCompletion { _state.update { it.copy(isBlocking = false) } }
    }

    fun send() {
        val reasons = _state.value.selectedReasons.takeIf { it.isNotEmpty() } ?: return
        checkAuthenticated(action = UserAction.Moderation) {
            _state.update { it.copy(isSending = true, error = null) }
            val result = sendReport(reasons = reasons)
            when (result) {
                is OctopusResult.Success -> {
                    _state.update {
                        it.copy(
                            isSending = false,
                            selectedReasons = emptyList(),
                        )
                    }
                    _events.tryEmit(Event.Sent)
                }

                is OctopusResult.Failure ->
                    _state.update {
                        it.copy(isSending = false, error = result.errorText)
                    }
            }
        }
    }

    abstract suspend fun sendReport(reasons: List<Moderation.ReportReason>): OctopusResult<Unit, Nothing>
}
