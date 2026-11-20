package com.octopuscommunity.sdk.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.octopuscommunity.sdk.domain.model.OctopusItem.Reaction
import kotlinx.coroutines.delay

/**
 * Reaction change animation — iso iOS: scale 1.4x + alpha 0.9 with spring on every [reactionKind]
 * change (tap-to-react, unreact, picker selection), never on the first composition. Apply the
 * returned modifier to the reaction glyph. Same animation as [OctopusActionButton]'s.
 */
@Composable
internal fun reactionChangeAnimation(reactionKind: Reaction.Kind?): Modifier {
    var animate by remember { mutableStateOf(false) }
    var isFirstComposition by remember { mutableStateOf(true) }
    LaunchedEffect(reactionKind) {
        if (isFirstComposition) {
            isFirstComposition = false
        } else {
            animate = true
            delay(200)
            animate = false
        }
    }
    val animationSpec = spring<Float>(dampingRatio = 0.3f, stiffness = Spring.StiffnessMediumLow)
    val scale by animateFloatAsState(
        targetValue = if (animate) 1.4f else 1f,
        animationSpec = animationSpec,
        label = "ReactionChangeAnimation-scale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (animate) 0.9f else 1f,
        animationSpec = animationSpec,
        label = "ReactionChangeAnimation-alpha",
    )
    return Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
        this.alpha = alpha
    }
}
