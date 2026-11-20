package com.octopuscommunity.sdk.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.UiText
import com.octopuscommunity.sdk.ui.medium

/**
 * Size of the icon at the default font size, used both as sp (it grows with the font, like the
 * label) and as dp (its floor): one value, so the two cannot drift apart.
 */
private const val TRANSLATION_TOGGLE_ICON_SIZE = 20f

/** Smallest icon size: the icon grows with the font but never shrinks below its default size. */
internal val TranslationToggleIconMinSize = TRANSLATION_TOGGLE_ICON_SIZE.dp

/** Space above and below the icon inside the row: 3dp, so the row is 26dp at the default font size. */
private val TranslationToggleIconInset = 3.dp

/**
 * Height the toggle takes in the layout flow at the default font size, and its minimum at any font
 * size: the icon plus its insets.
 */
internal val TranslationToggleRowHeight = TranslationToggleIconMinSize + TranslationToggleIconInset * 2

/**
 * Height of the toggle's touch area at the default font size: the 48dp Android minimum, which it
 * never goes under since the row never goes under [TranslationToggleRowHeight].
 */
internal val TranslationToggleTouchHeight = 48.dp

/**
 * How far the touch area reaches above and below the row, at every font size: the touch area is
 * always the row plus 22dp. Only the touch area spills onto the neighbours' margins; the row
 * alone takes space in the layout, so the content keeps its rhythm.
 */
internal val TranslationToggleTouchOverflow = (TranslationToggleTouchHeight - TranslationToggleRowHeight) / 2

/**
 * Icon size for the current font scale: 20sp, so the icon grows with the label at large font
 * sizes, as on iOS where it scales with the caption text style. It never shrinks below 20dp, so at
 * a small font scale the row stays 26dp and the touch area 48dp.
 */
@Composable
internal fun translationToggleIconSize(): Dp =
    with(LocalDensity.current) { TRANSLATION_TOGGLE_ICON_SIZE.sp.toDp() }.coerceAtLeast(TranslationToggleIconMinSize)

/**
 * Label of the "See original" / "See translation" toggle: the original language is named when
 * it is known, so a member reading a translation knows what they would switch to.
 */
internal fun translationToggleLabel(
    translated: Boolean,
    originalLanguage: String,
): UiText.StringResource =
    when {
        translated && originalLanguage.isNotEmpty() ->
            UiText.StringResource(R.string.common_view_original_language, listOf(originalLanguage))
        translated -> UiText.StringResource(R.string.common_view_original)
        else -> UiText.StringResource(R.string.common_view_translation)
    }

/**
 * "See original (en)" / "See translation" toggle of automatically translated content, shown
 * right under the content header: a language icon and a one-line label.
 *
 * The row is the icon plus 3dp above and below — [TranslationToggleRowHeight] at the default font
 * size or smaller, more when the icon grows with the font — and only the row takes space in the
 * layout flow. Its touch area is 22dp taller ([TranslationToggleTouchHeight] at the default font
 * size): the clickable row is measured with 11dp more above and below, then reported 22dp shorter
 * and placed 11dp higher, so it overlaps the neighbouring blocks' margins without pushing them.
 * [zIndex] puts it above its siblings, so a tap in the overlapping strip reaches the toggle rather
 * than the header or the text. The clickable is a
 * child layout node of the reported box: Compose hit-tests a child in its own bounds, not its
 * parent's, which is what makes the strips outside the reported row tappable.
 *
 * Horizontally, [startPadding] stays outside the touch area, while the icon, the label and
 * [endPadding] are inside it.
 *
 * The label never wraps: when it does not fit (a large font scale, a long translation), it is
 * shortened in the middle so the language at its end stays readable. TalkBack reads it as a
 * button; the icon is decorative.
 * [touchTargetModifier] observes the expanded clickable row, including its vertical overflow.
 */
@Composable
internal fun TranslationToggle(
    translated: Boolean,
    originalLanguage: String,
    onClick: () -> Unit,
    startPadding: Dp,
    endPadding: Dp,
    modifier: Modifier = Modifier,
    touchTargetModifier: Modifier = Modifier,
) {
    val label = translationToggleLabel(translated = translated, originalLanguage = originalLanguage).asString()
    val color = OctopusTheme.colorScheme.gray500
    val iconSize = translationToggleIconSize()
    Box(
        modifier =
            modifier
                .zIndex(1f)
                .padding(start = startPadding)
                .layout { measurable, constraints ->
                    val overflow = TranslationToggleTouchOverflow.roundToPx()
                    val placeable = measurable.measure(constraints.offset(vertical = 2 * overflow))
                    layout(placeable.width, placeable.height - 2 * overflow) {
                        placeable.place(0, -overflow)
                    }
                },
    ) {
        Row(
            modifier =
                touchTargetModifier
                    .clickable(role = Role.Button, onClick = onClick)
                    .padding(vertical = TranslationToggleTouchOverflow)
                    .heightIn(min = iconSize + TranslationToggleIconInset * 2)
                    .padding(end = endPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(iconSize),
                painter = OctopusTheme.icons.content.translation(),
                contentDescription = null,
                tint = color,
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                modifier = Modifier.weight(1f, fill = false),
                text = label,
                color = color,
                style = OctopusTheme.typography.caption1.medium,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TranslationTogglePreview() {
    OctopusTheme {
        Column {
            TranslationToggle(
                translated = true,
                originalLanguage = "en",
                onClick = {},
                startPadding = 13.dp,
                endPadding = 16.dp,
            )
            TranslationToggle(
                translated = false,
                originalLanguage = "en",
                onClick = {},
                startPadding = 13.dp,
                endPadding = 16.dp,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 160, fontScale = 2f)
@Composable
private fun TranslationToggleLongLabelPreview() {
    OctopusTheme {
        TranslationToggle(
            translated = true,
            originalLanguage = "en",
            onClick = {},
            startPadding = 13.dp,
            endPadding = 16.dp,
        )
    }
}
