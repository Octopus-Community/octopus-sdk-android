package com.octopuscommunity.sdk.domain.usecase

import com.octopuscommunity.sdk.domain.model.Moderation
import com.octopuscommunity.sdk.domain.model.OctopusEvent.ProfileReported
import com.octopuscommunity.sdk.domain.network.onSuccess
import com.octopuscommunity.sdk.domain.repository.EventRepository
import com.octopuscommunity.sdk.domain.repository.ModerationRepository

/** Reports a user profile to moderation, and emits [ProfileReported] on success. */
class ReportProfileUseCase(
    private val moderationRepository: ModerationRepository,
    private val eventRepository: EventRepository,
) {
    /** Reports the profile [profileId] for [reasons], with an optional free-text [customReason]. */
    suspend operator fun invoke(
        profileId: String,
        reasons: List<Moderation.ReportReason>,
        customReason: String? = null,
    ) = moderationRepository
        .reportUser(
            userId = profileId,
            reasons = reasons,
            customReason = customReason,
        ).onSuccess {
            eventRepository.emitEvent(
                ProfileReported(
                    profileId = profileId,
                    reasons = reasons,
                ),
            )
        }
}
