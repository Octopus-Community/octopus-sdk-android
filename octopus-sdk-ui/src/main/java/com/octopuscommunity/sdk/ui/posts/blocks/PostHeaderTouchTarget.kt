package com.octopuscommunity.sdk.ui.posts.blocks

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset

/** Keep one clickable Text node; compensate invisible touch padding without moving its pixels. */
@Composable
internal fun PostHeaderTouchTarget(
    onClick: (() -> Unit)?,
    aboveGroup: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable (Modifier) -> Unit,
) {
    if (!enabled || onClick == null) {
        content(modifier.then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier))
        return
    }
    val configuration = LocalViewConfiguration.current
    val boundedConfiguration =
        remember(configuration) {
            object : ViewConfiguration by configuration {
                override val minimumTouchTargetSize = DpSize.Zero
            }
        }
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val halfGap =
        with(LocalDensity.current) {
            val gap = 2.dp.roundToPx()
            (if (aboveGroup) gap / 2 else gap - gap / 2).toDp()
        }
    val top = if (aboveGroup) 16.dp else halfGap
    val bottom = if (aboveGroup) halfGap else 4.dp
    CompositionLocalProvider(LocalViewConfiguration provides boundedConfiguration) {
        if (!aboveGroup) {
            // Pointer-only expansion lives on the header. Keep this button's accessible bounds
            // on the painted text, so increasing its hit area cannot reorder TalkBack nodes.
            content(
                modifier
                    .clickable(
                        interactionSource = source,
                        indication = null,
                        role = Role.Button,
                        onClick = onClick,
                    ).indication(source, LocalIndication.current),
            )
            return@CompositionLocalProvider
        }
        content(
            modifier
                .layout { measurable, constraints ->
                    val topPx = top.roundToPx()
                    val bottomPx = bottom.roundToPx()
                    val target = measurable.measure(constraints.offset(vertical = topPx + bottomPx))
                    layout(target.width, target.height - topPx - bottomPx) {
                        target.placeRelative(0, -topPx)
                    }
                }.clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
                .padding(top = top, bottom = bottom)
                .indication(source, LocalIndication.current),
        )
    }
}
