package com.octopuscommunity.sdk.ui.posts.blocks

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.TooltipState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.domain.model.OctopusItem
import com.octopuscommunity.sdk.domain.model.OctopusItem.Reaction
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.components.OctopusAnimatedCounter
import com.octopuscommunity.sdk.ui.components.OctopusEmojisTooltip
import com.octopuscommunity.sdk.ui.components.ReactionEmojiStack
import com.octopuscommunity.sdk.ui.components.ReactionImage
import com.octopuscommunity.sdk.ui.components.getAccessibilityLabel
import com.octopuscommunity.sdk.ui.components.reactionChangeAnimation
import com.octopuscommunity.sdk.ui.components.reactions.OctopusReactionsListSheet
import com.octopuscommunity.sdk.ui.medium
import kotlinx.coroutines.launch

/**
 * Post footer on one line (internal ticket), under every post card (feed, group, profile, post detail).
 *
 * Left: the like button (empty heart, or the member's reaction, and the total of reactions), then
 * the comment button (bubble and number of comments, hidden when comments are closed). Right: the
 * three most used reactions, drawn like the reactions summary, opening who reacted. A count of 0 shows the icon alone.
 *
 * Tap on the like button sends [PostFooterState.tapReaction] to [onReactionClick] (adds a heart or
 * removes the member's reaction); a long press opens the reactions picker anchored on it.
 *
 * Every button is at least 48 × 48 dp. The row keeps 8dp side padding so the icons, inset by
 * each button's own 8dp padding, sit on the 16dp gutter of the card.
 *
 * @param reactions the post's reaction counters, passed to the who-reacted sheet.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun PostFooter(
    contentId: String,
    state: PostFooterState,
    reactions: List<OctopusItem.ReactionCount>,
    onReactionClick: (Reaction.Kind) -> Unit,
    onCommentClick: () -> Unit,
    modifier: Modifier = Modifier,
    tooltipState: TooltipState = rememberTooltipState(isPersistent = true),
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PostFooterLikeButton(
            state = state,
            onReactionClick = onReactionClick,
            tooltipState = tooltipState,
        )
        if (state.showCommentButton) {
            PostFooterCommentButton(
                commentCount = state.commentCount,
                onClick = onCommentClick,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        if (state.topReactions.isNotEmpty()) {
            PostFooterReactionStack(
                contentId = contentId,
                topReactions = state.topReactions,
                reactionCount = state.reactionCount,
                reactions = reactions,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun PostFooterLikeButton(
    state: PostFooterState,
    onReactionClick: (Reaction.Kind) -> Unit,
    tooltipState: TooltipState,
) {
    val coroutineScope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val userReaction = state.userReaction
    val reactedLabel =
        stringResource(
            if (userReaction != null) {
                R.string.accessibility_common_selected
            } else {
                R.string.accessibility_common_not_selected
            },
        )

    OctopusEmojisTooltip(
        state = tooltipState,
        reactions = Reaction.Kind.ALL,
        alignment = Alignment.CenterHorizontally,
        onReactionClick = onReactionClick,
    ) {
        PostFooterButton(
            modifier =
                Modifier
                    .combinedClickable(
                        role = Role.Button,
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onReactionClick(state.tapReaction)
                        },
                        onLongClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            coroutineScope.launch { tooltipState.show() }
                        },
                    )
                    // The clickable already merges the glyph and count labels into this button: a
                    // second merging boundary here would nest an empty node under it.
                    .semantics { stateDescription = reactedLabel },
        ) {
            // "Like, 192 reactions": the glyph carries the reaction name, the count its raw
            // plural — both merged into the button, the abbreviated text stays out of TalkBack.
            val glyphModifier = Modifier.size(24.dp).then(reactionChangeAnimation(userReaction))
            val glyphLabel = (userReaction ?: Reaction.Kind.Heart).getAccessibilityLabel()
            if (userReaction == null) {
                Icon(
                    modifier = glyphModifier,
                    painter =
                        OctopusTheme.icons.content.post
                            .likeNotSelected(),
                    contentDescription = glyphLabel,
                    tint = OctopusTheme.colorScheme.gray700,
                )
            } else {
                ReactionImage(
                    kind = userReaction,
                    modifier = glyphModifier,
                    contentDescription = glyphLabel,
                )
            }
            if (state.reactionCount > 0) {
                PostFooterCount(
                    count = state.reactionCount,
                    a11yLabel =
                        pluralStringResource(
                            R.plurals.accessibility_reaction_count_count,
                            state.reactionCount,
                            state.reactionCount,
                        ),
                    color =
                        if (userReaction != null) {
                            OctopusTheme.colorScheme.gray900
                        } else {
                            OctopusTheme.colorScheme.gray700
                        },
                )
            }
        }
    }
}

@Composable
private fun PostFooterCommentButton(
    commentCount: Int,
    onClick: () -> Unit,
) {
    // "15 comments", or "Comment" when there is none yet.
    val a11yLabel =
        if (commentCount > 0) {
            pluralStringResource(R.plurals.accessibility_comment_count_count, commentCount, commentCount)
        } else {
            stringResource(R.string.content_aggregated_info_comment)
        }
    val hapticFeedback = LocalHapticFeedback.current
    PostFooterButton(
        modifier =
            Modifier
                .clickable(role = Role.Button) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }.clearAndSetSemantics {
                    role = Role.Button
                    contentDescription = a11yLabel
                    onClick {
                        onClick()
                        true
                    }
                },
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            painter =
                OctopusTheme.icons.content.post
                    .openCommentCreation(),
            contentDescription = null,
            tint = OctopusTheme.colorScheme.gray700,
        )
        if (commentCount > 0) {
            PostFooterCount(
                count = commentCount,
                a11yLabel = null,
                color = OctopusTheme.colorScheme.gray700,
            )
        }
    }
}

@Composable
private fun PostFooterReactionStack(
    contentId: String,
    topReactions: List<Reaction.Kind>,
    reactionCount: Int,
    reactions: List<OctopusItem.ReactionCount>,
) {
    var showReactionsSheet by remember { mutableStateOf(false) }
    val a11yLabel =
        pluralStringResource(R.plurals.accessibility_reaction_count_count, reactionCount, reactionCount)
    val seeAllLabel = stringResource(R.string.accessibility_reaction_summary_see_all_formal)
    PostFooterButton(
        modifier =
            Modifier
                .clickable(role = Role.Button) { showReactionsSheet = true }
                .clearAndSetSemantics {
                    role = Role.Button
                    contentDescription = a11yLabel
                    onClick(label = seeAllLabel) {
                        showReactionsSheet = true
                        true
                    }
                },
        // Right-aligned inside the 48dp minimum, so a single emoji still ends on the gutter.
        horizontalArrangement = Arrangement.End,
    ) {
        // Same emojis as the reactions summary under comments, only placed on the right.
        ReactionEmojiStack(kinds = topReactions)
    }

    if (showReactionsSheet) {
        OctopusReactionsListSheet(
            contentId = contentId,
            reactions = reactions,
            onDismissRequest = { showReactionsSheet = false },
        )
    }
}

/** A footer button: 48 × 48 dp minimum, 8dp padding around its content, 6dp between icon and count. */
@Composable
private fun PostFooterButton(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(6.dp),
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier =
            modifier
                .heightIn(min = 48.dp)
                .widthIn(min = 48.dp)
                .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = horizontalArrangement,
        content = content,
    )
}

/**
 * Count next to a footer icon: up to 999 in full, then abbreviated with the locale's decimal
 * separator (`1,2K`, `1.2K`). TalkBack reads [a11yLabel] (the raw count) instead, or nothing when it is `null`.
 */
@Composable
private fun PostFooterCount(
    count: Int,
    a11yLabel: String?,
    color: Color,
) {
    OctopusAnimatedCounter(
        modifier =
            Modifier.clearAndSetSemantics {
                a11yLabel?.let { contentDescription = it }
            },
        count = count,
        color = color,
        style = OctopusTheme.typography.caption1.medium,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun PostFooterPreview() {
    OctopusTheme {
        val reactions =
            listOf(
                OctopusItem.ReactionCount(Reaction.Kind.Heart, 120),
                OctopusItem.ReactionCount(Reaction.Kind.Joy, 50),
                OctopusItem.ReactionCount(Reaction.Kind.MouthOpen, 22),
            )
        PostFooter(
            contentId = "preview",
            state =
                postFooterState(
                    reactions = reactions,
                    userReaction = null,
                    commentCount = 15,
                    canCreateChildren = true,
                ),
            reactions = reactions,
            onReactionClick = {},
            onCommentClick = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun PostFooterEmptyPreview() {
    OctopusTheme {
        PostFooter(
            contentId = "preview",
            state =
                postFooterState(
                    reactions = emptyList(),
                    userReaction = null,
                    commentCount = 0,
                    canCreateChildren = true,
                ),
            reactions = emptyList(),
            onReactionClick = {},
            onCommentClick = {},
        )
    }
}
