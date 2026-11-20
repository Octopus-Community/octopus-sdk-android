package com.octopuscommunity.sdk.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.domain.model.OctopusItem.Reaction
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OctopusEmojisTooltip(
    reactions: Set<Reaction.Kind>,
    onReactionClick: (Reaction.Kind) -> Unit,
    modifier: Modifier = Modifier,
    state: TooltipState = rememberTooltipState(isPersistent = true),
    alignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    content: @Composable () -> Unit,
) {
    val hapticFeedback = LocalHapticFeedback.current

    TooltipBox(
        modifier = modifier,
        state = state,
        positionProvider =
            OctopusTooltipDefaults.rememberPositionProvider(
                alignment = alignment,
                verticalOffset = 12.dp,
            ),
        enableUserInput = false,
        tooltip = {
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = OctopusTheme.colorScheme.background,
                border = BorderStroke(1.dp, OctopusTheme.colorScheme.gray300),
                shadowElevation = 4.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    reactions.forEach { reaction ->
                        val reactionContentDescription =
                            stringResource(
                                R.string.accessibility_reaction_button_reaction,
                                reaction.getAccessibilityLabel(),
                            )
                        OctopusIconButton(
                            modifier =
                                Modifier
                                    .size(48.dp)
                                    .semantics {
                                        this.contentDescription = reactionContentDescription
                                    },
                            content = {
                                ReactionImage(
                                    kind = reaction,
                                    modifier = Modifier.size(44.dp),
                                )
                            },
                            onClick = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                state.dismiss()
                                onReactionClick(reaction)
                            },
                        )
                    }
                }
            }
        },
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun OctopusReactionCountTooltipPreview() {
    OctopusTheme {
        OctopusEmojisTooltip(
            reactions = Reaction.Kind.ALL,
            onReactionClick = {},
        ) {}
    }
}
