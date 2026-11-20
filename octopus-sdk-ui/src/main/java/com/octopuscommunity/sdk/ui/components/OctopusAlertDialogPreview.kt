package com.octopuscommunity.sdk.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp

/**
 * Layoutlib does not capture Dialog's separate window. In inspection mode only, render the real
 * alert slots inline with Material 3's content sizing, padding and typography. Runtime continues
 * to use Material's AlertDialog unchanged. These previews cover content, not window behavior.
 * Mirrors Material3 1.4.0 (Compose BOM 2025.10.00 from gradle/libs.versions.toml), including
 * confirming actions above dismissive actions on wrap; the forced-wrap screenshot pins that case.
 */
@Composable
internal fun OctopusPreviewableAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    containerColor: Color = AlertDialogDefaults.containerColor,
) {
    if (!LocalInspectionMode.current) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = confirmButton,
            dismissButton = dismissButton,
            title = title,
            text = text,
            containerColor = containerColor,
        )
        return
    }

    Surface(
        modifier = Modifier.sizeIn(minWidth = 280.dp, maxWidth = 560.dp),
        shape = AlertDialogDefaults.shape,
        color = containerColor,
        tonalElevation = AlertDialogDefaults.TonalElevation,
    ) {
        Column(Modifier.padding(24.dp)) {
            title?.let {
                CompositionLocalProvider(LocalContentColor provides AlertDialogDefaults.titleContentColor) {
                    ProvideTextStyle(MaterialTheme.typography.headlineSmall) {
                        Box(Modifier.padding(bottom = 16.dp).align(Alignment.Start)) { it() }
                    }
                }
            }
            text?.let {
                CompositionLocalProvider(LocalContentColor provides AlertDialogDefaults.textContentColor) {
                    ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
                        Box(Modifier.weight(1f, fill = false).padding(bottom = 24.dp).align(Alignment.Start)) { it() }
                    }
                }
            }
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
                ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                    // Material puts confirming actions above dismissive actions when they wrap.
                    Layout(
                        modifier = Modifier.align(Alignment.End),
                        content = {
                            dismissButton?.invoke()
                            confirmButton()
                        },
                    ) { measurables, constraints ->
                        val buttons = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
                        val gap = if (buttons.size == 2) 8.dp.roundToPx() else 0
                        val rowWidth = buttons.sumOf { it.width } + gap
                        if (rowWidth <= constraints.maxWidth) {
                            val width = constraints.constrainWidth(rowWidth)
                            layout(width, constraints.constrainHeight(buttons.maxOf { it.height })) {
                                var x = width - rowWidth
                                buttons.forEach {
                                    it.placeRelative(x, 0)
                                    x += it.width + gap
                                }
                            }
                        } else {
                            val width = constraints.constrainWidth(buttons.maxOf { it.width })
                            val verticalGap = 12.dp.roundToPx()
                            layout(width, constraints.constrainHeight(buttons.sumOf { it.height } + verticalGap)) {
                                var y = 0
                                buttons.asReversed().forEach {
                                    it.placeRelative(width - it.width, y)
                                    y += it.height + verticalGap
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
