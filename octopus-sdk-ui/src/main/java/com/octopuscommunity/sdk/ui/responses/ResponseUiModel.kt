package com.octopuscommunity.sdk.ui.responses

import androidx.compose.runtime.Immutable
import com.octopuscommunity.sdk.domain.model.Author
import com.octopuscommunity.sdk.domain.model.Media
import com.octopuscommunity.sdk.domain.model.Moderation
import com.octopuscommunity.sdk.domain.model.OctopusItem
import com.octopuscommunity.sdk.domain.model.TranslatableText
import java.util.Date

/**
 * UI-layer model for a response (comment or reply). Consumed by the shared `ResponseCard`
 * composable in both list (summary) and detail contexts.
 *
 * Equivalent to iOS `ResponseViewData`.
 */
@Immutable
data class ResponseUiModel(
    val id: String,
    val kind: ResponseKind,
    val author: Author?,
    val creationDate: Date,
    val content: ResponseContent,
    val reactions: List<OctopusItem.ReactionCount>,
    val userReaction: OctopusItem.Reaction?,
    val replyCount: Int,
    val canBeDeleted: Boolean,
    val canBeReported: Boolean,
    val canCreateChildren: Boolean,
    val hasTranslation: Boolean,
)

sealed interface ResponseContent {
    @Immutable
    data class Published(
        val text: TranslatableText,
        val image: Media.Image?,
    ) : ResponseContent

    @Immutable
    data class Moderated(val reasons: List<Moderation.Reason>) : ResponseContent
}
