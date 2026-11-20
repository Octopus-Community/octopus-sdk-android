package com.octopuscommunity.sdk.domain.usecase

import com.octopuscommunity.sdk.domain.model.OctopusEvent
import com.octopuscommunity.sdk.domain.model.OctopusItem
import com.octopuscommunity.sdk.domain.repository.EventRepository
import com.octopuscommunity.sdk.domain.repository.TrackerRepository
import com.octopuscommunity.sdk.domain.repository.TranslationRepository

/**
 * Switches an item between its translated and original text.
 *
 * Tracks the translation button click and emits [OctopusEvent.TranslationButtonClicked].
 */
class ToggleTranslationUseCase(
    private val translationRepository: TranslationRepository,
    private val trackerRepository: TrackerRepository,
    private val eventRepository: EventRepository,
) {
    /** Outcome of [ToggleTranslationUseCase]. */
    data class Result(
        /** Whether the item is now displayed translated. */
        val translated: Boolean,
    )

    /** Toggles the translation of [item]. */
    suspend operator fun invoke(item: OctopusItem) =
        Result(
            translated = translationRepository.toggleTranslation(item.id),
        ).also {
            trackerRepository.trackTranslationButtonClicked(translationDisplayed = it.translated)
            eventRepository.emitEvent(
                OctopusEvent.TranslationButtonClicked(
                    item = item,
                    viewTranslated = it.translated,
                ),
            )
        }
}
