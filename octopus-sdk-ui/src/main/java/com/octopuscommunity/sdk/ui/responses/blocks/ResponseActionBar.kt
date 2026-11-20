package com.octopuscommunity.sdk.ui.responses.blocks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.domain.model.OctopusItem
import com.octopuscommunity.sdk.domain.model.OctopusItem.Reaction
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.components.OctopusActionButton
import com.octopuscommunity.sdk.ui.components.OctopusLikeButton
import com.octopuscommunity.sdk.ui.components.OctopusReactions
import com.octopuscommunity.sdk.ui.responses.ResponseKind

/**
 * Action bar below the response card: like · optional reply button (Comment only) · reactions
 * summary. Iso iOS `ResponseActionBarView`.
 *
 * The like icon is picked from [kind] — comments and replies have distinct "not-reacted" glyphs
 * in the theme, iso iOS's `likeAssetForKind`. Reply button is suppressed for replies (a reply
 * cannot itself be replied to) and also when [displayReplyButton] is `false` — internal ticket hides it
 * when the comment does not allow new replies (`canCreateChildren = false`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ResponseActionBar(
    kind: ResponseKind,
    contentId: String,
    displayReplyButton: Boolean,
    userReaction: OctopusItem.Reaction?,
    reactions: List<OctopusItem.ReactionCount>,
    onReactionClick: (Reaction.Kind) -> Unit,
    onReplyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val likeIcon =
        when (kind) {
            ResponseKind.Comment ->
                OctopusTheme.icons.content.comment
                    .likeNotSelected()
            ResponseKind.Reply ->
                OctopusTheme.icons.content.reply
                    .likeNotSelected()
        }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 44.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Like (44dp tap target — iso iOS `ReactionToggleView` `frame(minWidth: 44, minHeight: 44)`,
        // applied internally via OctopusLikeButton's wrapped action button).
        OctopusLikeButton(
            reaction = userReaction?.kind,
            icon = likeIcon,
            onReactionClick = onReactionClick,
        )
        if (kind == ResponseKind.Comment && displayReplyButton) {
            OctopusActionButton(
                text = stringResource(R.string.content_aggregated_info_reply),
                icon =
                    OctopusTheme.icons.content.comment
                        .openReplyCreation(),
                onClick = onReplyClick,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        if (reactions.isNotEmpty()) {
            OctopusReactions(
                contentId = contentId,
                reactions = reactions,
                layoutDirection = LayoutDirection.Rtl,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun ResponseActionBarCommentPreview() {
    OctopusTheme {
        ResponseActionBar(
            contentId = "preview",
            kind = ResponseKind.Comment,
            displayReplyButton = true,
            userReaction = null,
            reactions = listOf(OctopusItem.ReactionCount(Reaction.Kind.Heart, 3)),
            onReactionClick = {},
            onReplyClick = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun ResponseActionBarReplyPreview() {
    OctopusTheme {
        ResponseActionBar(
            contentId = "preview",
            kind = ResponseKind.Reply,
            displayReplyButton = true,
            userReaction = Reaction(Reaction.Kind.Joy),
            reactions = emptyList(),
            onReactionClick = {},
            onReplyClick = {},
        )
    }
}
