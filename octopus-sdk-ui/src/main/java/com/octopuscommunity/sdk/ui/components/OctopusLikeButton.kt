package com.octopuscommunity.sdk.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TooltipState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import com.octopuscommunity.sdk.domain.model.OctopusItem.Reaction
import com.octopuscommunity.sdk.domain.model.OctopusItem.Reaction.Kind.Heart
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OctopusLikeButton(
    reaction: Reaction.Kind?,
    onReactionClick: (Reaction.Kind) -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter = OctopusTheme.icons.content.like(),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Center,
    tooltipState: TooltipState = rememberTooltipState(isPersistent = true),
    interactive: Boolean = true,
) {
    val coroutineScope = rememberCoroutineScope()
    val reactionsTooltipState = tooltipState
    val hapticFeedback = LocalHapticFeedback.current

    OctopusEmojisTooltip(
        modifier = modifier,
        state = reactionsTooltipState,
        reactions = Reaction.Kind.ALL,
        alignment = Alignment.CenterHorizontally,
        onReactionClick = onReactionClick,
    ) {
        val selectedReaction = reaction ?: Heart
        val contentDescription =
            stringResource(
                R.string.accessibility_reaction_button_reaction,
                selectedReaction.getAccessibilityLabel(),
            )
        val stateDescription =
            stringResource(
                if (reaction != null) {
                    R.string.accessibility_common_selected
                } else {
                    R.string.accessibility_common_not_selected
                },
            )

        OctopusActionButton(
            modifier =
                Modifier
                    .semantics(mergeDescendants = true) {
                        this.contentDescription = contentDescription
                        this.stateDescription = stateDescription
                    },
            // No reaction yet: the Heart label, as iOS `(reaction ?? .heart).labelKey`.
            text = selectedReaction.getLabel(),
            icon = if (reaction == null) icon else null,
            reactionKind = reaction,
            selected = reaction != null,
            horizontalArrangement = horizontalArrangement,
            onClick =
                if (interactive) {
                    { onReactionClick(reaction ?: Heart) }
                } else {
                    null
                },
            onLongClick =
                if (interactive) {
                    {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        coroutineScope.launch {
                            reactionsTooltipState.show()
                        }
                    }
                } else {
                    null
                },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun OctopusLikeButtonPreview() {
    OctopusTheme {
        OctopusLikeButton(
            reaction = null,
            onReactionClick = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun OctopusLikeButtonHeartPreview() {
    OctopusTheme {
        OctopusLikeButton(
            reaction = Heart,
            onReactionClick = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun OctopusLikeButtonJoyPreview() {
    OctopusTheme {
        OctopusLikeButton(
            reaction = Reaction.Kind.Joy,
            onReactionClick = {},
        )
    }
}
