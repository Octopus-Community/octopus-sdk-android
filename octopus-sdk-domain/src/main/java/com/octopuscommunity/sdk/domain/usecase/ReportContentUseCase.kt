package com.octopuscommunity.sdk.domain.usecase

import com.octopuscommunity.sdk.domain.model.Moderation
import com.octopuscommunity.sdk.domain.model.OctopusEvent.ContentReported
import com.octopuscommunity.sdk.domain.model.OctopusItem
import com.octopuscommunity.sdk.domain.network.onSuccess
import com.octopuscommunity.sdk.domain.repository.EventRepository
import com.octopuscommunity.sdk.domain.repository.ModerationRepository

/** Reports a post, comment or reply to moderation, and emits [ContentReported] on success. */
class ReportContentUseCase(
    private val moderationRepository: ModerationRepository,
    private val eventRepository: EventRepository,
) {
    /**
     * Reports the content [contentId] of kind [contentKind] for [reasons], with an optional free-text
     * [customReason].
     */
    suspend operator fun invoke(
        contentId: String,
        contentKind: OctopusItem.ContentKind,
        reasons: List<Moderation.ReportReason>,
        customReason: String? = null,
    ) = moderationRepository
        .reportContent(
            contentId = contentId,
            reasons = reasons,
            customReason = customReason,
        ).onSuccess {
            eventRepository.emitEvent(
                ContentReported(
                    contentId = contentId,
                    contentKind = contentKind,
                    reasons = reasons,
                ),
            )
        }
}
