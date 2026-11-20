package com.octopuscommunity.sdk.ui.events

import com.octopuscommunity.sdk.domain.model.Gamification.Action.COMMENT
import com.octopuscommunity.sdk.domain.model.Gamification.Action.DAILY_SESSION
import com.octopuscommunity.sdk.domain.model.Gamification.Action.POST
import com.octopuscommunity.sdk.domain.model.Gamification.Action.POST_COMMENTED
import com.octopuscommunity.sdk.domain.model.Gamification.Action.PROFILE_COMPLETED
import com.octopuscommunity.sdk.domain.model.Gamification.Action.REACTION
import com.octopuscommunity.sdk.domain.model.Gamification.Action.REPLY
import com.octopuscommunity.sdk.domain.model.Gamification.Action.VOTE
import com.octopuscommunity.sdk.domain.model.Gamification.Toast
import com.octopuscommunity.sdk.domain.model.OctopusEvent.CommentDeleted
import com.octopuscommunity.sdk.domain.model.OctopusEvent.GamificationPointsGained
import com.octopuscommunity.sdk.domain.model.OctopusEvent.PostCreated
import com.octopuscommunity.sdk.domain.model.OctopusEvent.PostDeleted
import com.octopuscommunity.sdk.domain.model.OctopusEvent.ReplyDeleted
import com.octopuscommunity.sdk.domain.model.SDKEvent.Disconnected
import com.octopuscommunity.sdk.domain.repository.EventRepository
import com.octopuscommunity.sdk.domain.repository.SessionRepository
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.UiText
import com.octopuscommunity.sdk.ui.common.UiText.StringResource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge

class OctopusSnackbarEvents(
    private val eventRepository: EventRepository,
    private val sessionRepository: SessionRepository,
) {
    sealed class Event(
        val clickable: Boolean = false,
    ) {
        data class Info(
            override val message: UiText,
        ) : Event()

        data class Success(
            override val message: UiText,
        ) : Event()

        /**
         * A load that failed over content already on screen.
         *
         * @property onRetry re-runs the fetch that failed, offered as the snackbar's action when the
         * screen has something to retry (Screen states spec). Null leaves the snackbar plain.
         */
        data class Error(
            override val message: UiText,
            val onRetry: (() -> Unit)? = null,
            /**
             * A lost connection names a device state the member can recognise, so the design gives it
             * its own glyph. Anything else stays plain.
             */
            val isNoNetwork: Boolean = false,
        ) : Event()

        data class Gamification(
            val toast: Toast,
        ) : Event(clickable = true) {
            override val message =
                StringResource(
                    when (toast.action) {
                        REACTION -> R.string.gamification_toast_reaction_formal
                        COMMENT, REPLY -> R.string.gamification_toast_comment_formal
                        POST -> R.string.gamification_toast_post_formal
                        VOTE -> R.string.gamification_toast_vote_formal
                        POST_COMMENTED -> R.string.gamification_toast_post_commented_formal
                        PROFILE_COMPLETED -> R.string.gamification_toast_profile_completed_formal
                        DAILY_SESSION -> R.string.gamification_toast_daily_session_formal
                    },
                    listOf(toast.points.shortText),
                )
        }

        abstract val message: UiText
    }

    private val screenEvents = MutableSharedFlow<Event>()

    fun getEvents(): Flow<Event> =
        merge(
            screenEvents.asSharedFlow(),
            // Map domain deletion events to snackbar events
            eventRepository.getEventsFlow().mapNotNull { event ->
                when (event) {
                    is PostDeleted -> Event.Info(StringResource(R.string.post_delete_done))
                    is PostCreated -> Event.Success(StringResource(R.string.post_toast_create))
                    is CommentDeleted -> Event.Info(StringResource(R.string.comment_delete_done))
                    is ReplyDeleted -> Event.Info(StringResource(R.string.reply_delete_done))
                    is Disconnected -> Event.Info(StringResource(R.string.settings_logout_done_title_formal))
                    is GamificationPointsGained -> {
                        // Only emit if not already displayed
                        if (!sessionRepository.hasDisplayedGamification(event.toast.action)) {
                            Event.Gamification(event.toast).also {
                                sessionRepository.setGamificationDisplayed(event.toast.action)
                            }
                        } else {
                            null
                        }
                    }

                    else -> null // Return null instead of Unit
                }
            },
        )

    suspend fun emitEvent(event: Event) {
        screenEvents.emit(event)
    }
}
