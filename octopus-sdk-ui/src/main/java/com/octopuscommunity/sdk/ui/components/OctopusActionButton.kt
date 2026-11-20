package com.octopuscommunity.sdk.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.domain.model.OctopusItem.Reaction
import com.octopuscommunity.sdk.domain.model.OctopusItem.Reaction.Kind.Heart
import com.octopuscommunity.sdk.domain.model.OctopusItem.Reaction.Kind.Joy
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.medium
import kotlinx.coroutines.delay

@Composable
fun OctopusActionButton(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    icon: Any? = null,
    image: Painter? = null,
    emoji: String? = null,
    reactionKind: Reaction.Kind? = null,
    count: Int? = null,
    text: String? = null,
    textStyle: TextStyle = OctopusTheme.typography.caption1.medium,
    color: Color = OctopusTheme.colorScheme.gray700,
    selectedColor: Color = OctopusTheme.colorScheme.gray900,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    contentDescription: String? = text
) {
    val hapticFeedback = LocalHapticFeedback.current

    // Reaction change animation — iso iOS: scale 1.4x + alpha 0.9 with spring on every reactionKind change.
    // Fires on tap-to-react, unreact, and emoji picker selection (any path that updates reactionKind).
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
        label = "OctopusActionButton-scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (animate) 0.9f else 1f,
        animationSpec = animationSpec,
        label = "OctopusActionButton-alpha"
    )
    val contentAnimation = Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
        this.alpha = alpha
    }

    Row(
    modifier = modifier
        .then(
            if (onLongClick != null) {
                Modifier.combinedClickable(
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick?.invoke()
                    },
                    onLongClick = onLongClick,
                    role = Role.ValuePicker
                )
            } else if (onClick != null) {
                Modifier.clickable(role = Role.Button) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                }
            } else Modifier
        )
        // 44dp min tap target — `heightIn(min = 44.dp)` rather than `defaultMinSize(min = 44)`
        // because `defaultMinSize` is layered such that the clickable's hit bounds report the
        // *intrinsic* row height (~24dp) instead of the constrained 44dp; `heightIn` forces a
        // hard min on the row layout itself so the click bounds match the 44dp target.
        // Iso iOS `frame(minWidth: 44, minHeight: 44).contentShape(Rectangle())`.
        .heightIn(min = 44.dp)
        .widthIn(min = 44.dp)
        .minimumInteractiveComponentSize()
        .semantics(mergeDescendants = contentDescription != null) {
            role = Role.Button
            contentDescription?.let { this.contentDescription = it }
        },
    horizontalArrangement = horizontalArrangement,
    verticalAlignment = Alignment.CenterVertically
) {
    if (icon != null) {
        when (icon) {
            is ImageVector -> Icon(
                modifier = Modifier.size(24.dp).then(contentAnimation),
                imageVector = icon,
                contentDescription = text,
                tint = color
            )

            is Painter -> Icon(
                modifier = Modifier.size(24.dp).then(contentAnimation),
                painter = icon,
                contentDescription = text,
                tint = color
            )
        }
    } else if (image != null) {
        Image(
            modifier = Modifier.size(24.dp).then(contentAnimation),
            painter = image,
            contentDescription = text
        )
    } else if (reactionKind != null) {
        ReactionImage(
            kind = reactionKind,
            modifier = Modifier.size(24.dp).then(contentAnimation),
            contentDescription = text
        )
    } else if (emoji != null) {
        OctopusEmoji(
            modifier = Modifier.size(24.dp).then(contentAnimation),
            unicode = emoji
        )
    }

    // Gap between icon/emoji and text/count
    if ((icon != null || image != null || reactionKind != null || emoji != null) &&
        (count != null || text != null)
    ) {
        Spacer(modifier = Modifier.width(4.dp))
    }

    if (count != null) {
        OctopusAnimatedCounter(
            count = count,
            color = when (selected) {
                true -> selectedColor
                false -> color
            },
            style = textStyle
        )
    } else if (text != null) {
        Text(
            text = text,
            color = when (selected) {
                true -> selectedColor
                false -> color
            },
            style = textStyle
        )
    }
}
}

@Preview(showBackground = true)
@Composable
private fun OctopusActionButtonPreview() {
    OctopusTheme {
        OctopusActionButton(
            selected = false,
            image = OctopusTheme.icons.content.post.likeNotSelected(),
            text = "Action",
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OctopusActionButtonHeartPreview() {
    OctopusTheme {
        OctopusActionButton(
            selected = true,
            reactionKind = Heart,
            text = "Like",
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OctopusActionButtonJoyPreview() {
    OctopusTheme {
        OctopusActionButton(
            selected = true,
            reactionKind = Joy,
            text = "Haha",
            onClick = {}
        )
    }
}