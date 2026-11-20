package com.octopuscommunity.sdk.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.domain.model.OctopusItem.Reaction.Kind
import com.octopuscommunity.sdk.domain.model.OctopusItem.ReactionCount
import com.octopuscommunity.sdk.test.mock.MockReactions
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.components.reactions.OctopusReactionsListSheet
import com.octopuscommunity.sdk.ui.utils.emojiOverlapBorder

@Composable
internal fun OctopusReactions(
    contentId: String,
    reactions: List<ReactionCount>,
    modifier: Modifier = Modifier,
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
) {
    var showReactionsSheet by remember { mutableStateOf(false) }
    val reactionCount = reactions.sumOf { it.count }

    val contentDescription =
        pluralStringResource(
            R.plurals.accessibility_reaction_count_count,
            reactionCount,
            reactionCount,
        )
    val accessibilityLabel = stringResource(R.string.accessibility_reaction_summary_see_all_formal)
    // 2dp gap between emoji stack and count, 44dp tap target via defaultMinSize. iOS adds an explicit `padding(.vertical, 6)` on top
    // of `frame(minHeight: 44)` — at content sizes < 32dp it has no effect (the frame already
    // dominates), and at content sizes ≥ 32dp the SwiftUI clamp differs from Compose's, so we
    // skip the redundant padding.
    Row(
        modifier =
            modifier
                .animateContentSize()
                .semantics {
                    this.contentDescription = contentDescription
                    this.onClick(
                        label = accessibilityLabel,
                        action = {
                            showReactionsSheet = true
                            true
                        },
                    )
                }.clickable(role = Role.Button) { showReactionsSheet = true }
                .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (layoutDirection == LayoutDirection.Rtl) {
            OctopusAnimatedCounter(
                count = reactionCount,
                color = OctopusTheme.colorScheme.gray700,
                style = OctopusTheme.typography.caption1,
            )
        }
        ReactionEmojiStack(kinds = reactions.take(3).map { it.reactionKind })
        if (layoutDirection == LayoutDirection.Ltr) {
            OctopusAnimatedCounter(
                count = reactionCount,
                color = OctopusTheme.colorScheme.gray700,
                style = OctopusTheme.typography.caption1,
            )
        }
    }

    if (showReactionsSheet) {
        OctopusReactionsListSheet(
            contentId = contentId,
            reactions = reactions,
            onDismissRequest = {
                showReactionsSheet = false
            },
        )
    }
}

/**
 * The emojis of a reactions summary: 20dp each, overlapping by 8dp. Each emoji has an outline of
 * the background colour that follows its shape (like Figma). Shared by [OctopusReactions] and the
 * post footer so both draw the same summary.
 */
@Composable
internal fun ReactionEmojiStack(
    kinds: List<Kind>,
    modifier: Modifier = Modifier,
) {
    val borderColor = OctopusTheme.colorScheme.background
    Row(
        modifier = modifier.animateContentSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy((-8).dp),
    ) {
        kinds.forEach {
            ReactionImage(
                kind = it,
                modifier =
                    Modifier
                        .size(20.dp)
                        .emojiOverlapBorder(borderColor),
            )
        }
    }
}

@Composable
internal fun Kind.getAccessibilityLabel() =
    stringResource(
        when (this) {
            Kind.Heart -> R.string.accessibility_reaction_like
            Kind.Joy -> R.string.accessibility_reaction_joy
            Kind.MouthOpen -> R.string.accessibility_reaction_surprise
            Kind.Clap -> R.string.accessibility_reaction_congrats
            Kind.Cry -> R.string.accessibility_reaction_cry
            Kind.Rage -> R.string.accessibility_reaction_rage
            else -> R.string.accessibility_reaction_unknown
        },
    )

/**
 * Returns the user-visible label for a reaction kind (e.g. "Like", "Haha", "Bravo").
 * Used on the interaction button, where no reaction yet shows the [Kind.Heart] label.
 */
@Composable
internal fun Kind.getLabel() =
    stringResource(
        when (this) {
            Kind.Heart -> R.string.content_aggregated_info_reaction_heart
            Kind.Joy -> R.string.content_aggregated_info_reaction_joy
            Kind.MouthOpen -> R.string.content_aggregated_info_reaction_mouth_open
            Kind.Clap -> R.string.content_aggregated_info_reaction_clap
            Kind.Cry -> R.string.content_aggregated_info_reaction_cry
            Kind.Rage -> R.string.content_aggregated_info_reaction_rage
            else -> R.string.content_aggregated_info_reaction_heart
        },
    )

@Preview(showBackground = true)
@Composable
private fun OctopusReactionsPreview() {
    OctopusTheme {
        OctopusReactions(
            contentId = "preview",
            reactions = MockReactions.all,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OctopusReactionsRtlPreview() {
    OctopusTheme {
        OctopusReactions(
            contentId = "preview",
            reactions = MockReactions.all,
            layoutDirection = LayoutDirection.Rtl,
        )
    }
}
