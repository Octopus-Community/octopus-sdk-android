package com.octopuscommunity.sdk.sample.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.octopuscommunity.sdk.sample.ui.theme.LocalSampleDarkTheme
import com.octopuscommunity.sdk.sample.ui.theme.SampleBranding
import com.octopuscommunity.sdk.sample.ui.theme.SampleColors
import com.octopuscommunity.sdk.sample.ui.theme.SampleMotion
import com.octopuscommunity.sdk.sample.ui.theme.SampleRadius
import com.octopuscommunity.sdk.sample.ui.theme.SampleSize
import com.octopuscommunity.sdk.sample.ui.theme.SampleSpacing
import com.octopuscommunity.sdk.sample.ui.theme.SampleTextStyles
import com.octopuscommunity.sdk.sample.ui.theme.SampleTheme
import com.octopuscommunity.sdk.sample.ui.theme.sampleCardEdge
import com.octopuscommunity.sdk.sample.ui.theme.sampleFocusRing
import com.octopuscommunity.sdk.sample.ui.theme.sampleNestedEdge
import com.octopuscommunity.sdk.sample.ui.theme.sampleTopBarColor
import com.octopuscommunity.sdk.sample.ui.theme.sampleTopBarScrolledColor

// The sample's shared UI kit, drawn from the Octopus sample design system (the frozen component
// set: app bar, platform marker, buttons, cards, rows, list group, section header, status badge,
// banner, state view, snackbar, segmented control, result compare). Every colour, size and radius
// comes from `SampleTokens.kt`; a screen that hand-rolls its own button is how the samples drifted
// apart the last time. Nothing in this file belongs to the SDK.

/** Tinted ground of a sample-layer block (`accent-tint`). */
val sampleTintedBackground: Color
    @Composable @ReadOnlyComposable
    get() = SampleTheme.colors.accentTint

/**
 * Edge of a framed block (`border`): the 1px half of a level-1 card's edge. An unfilled control
 * whose outline is its only boundary (outlined pill, chip, switch track) takes `outline` instead.
 */
val sampleBorderColor: Color
    @Composable @ReadOnlyComposable
    get() = SampleTheme.colors.border

/** The accent as ink (`accent-text`): links, text buttons, selected marks. */
val sampleAccentColor: Color
    @Composable @ReadOnlyComposable
    get() = SampleTheme.colors.accentText

/** The accent as a control fill (`accent`): the on-track of a switch. A fill, never a label. */
val sampleControlAccent: Color
    @Composable @ReadOnlyComposable
    get() = SampleTheme.colors.accent

/** Ink on the primary button (`button-text`). */
val sampleOnAccentColor: Color
    @Composable @ReadOnlyComposable
    get() = SampleTheme.colors.buttonText

/** Danger ink (`danger`). */
val sampleDangerColor: Color
    @Composable @ReadOnlyComposable
    get() = SampleTheme.colors.danger

/** Warning ink (`warning-text`): the guest and degraded states. */
val sampleAmberColor: Color
    @Composable @ReadOnlyComposable
    get() = SampleTheme.colors.warningText

/** Success ink (`success-text`): a passed call, a wired callback, a connected user. */
val sampleSuccessColor: Color
    @Composable @ReadOnlyComposable
    get() = SampleTheme.colors.successText

/**
 * The DS Toggle on Material's switch: off is a 2px `control-off` outline on `surface-2` with a
 * `control-off` thumb; on is an `accent` track with a white thumb.
 */
@Composable
fun sampleSwitchColors(): SwitchColors {
    val c = SampleTheme.colors
    return SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = c.accent,
        checkedBorderColor = c.accent,
        checkedIconColor = c.accent,
        uncheckedThumbColor = c.controlOff,
        uncheckedTrackColor = c.surface2,
        uncheckedBorderColor = c.controlOff,
        uncheckedIconColor = c.surface2,
    )
}

/** Same DS Toggle: every switch in the sample is one control, not two families. */
@Composable
fun sampleDefaultSwitchColors(): SwitchColors = sampleSwitchColors()

/** DS Radio: a `control-off` ring, filled in `accent-text` when chosen. */
@Composable
fun sampleRadioColors() =
    RadioButtonDefaults.colors(
        selectedColor = SampleTheme.colors.accentText,
        unselectedColor = SampleTheme.colors.controlOff,
    )

// --- App bar ------------------------------------------------------------------------------------

/**
 * The DS PlatformMarker: which of the five samples this screen belongs to.
 *
 * A neutral pill (`surface`, 1px `outline`, label in `text`, 12/16 semibold, 28 high); only the
 * glyph takes the platform colour. [compact] is the app-bar size: the glyph alone in a 28 circle,
 * the full name kept in the accessible label. The marker never becomes a button.
 *
 * [SampleBranding.PLATFORM_ATTRIBUTION] is shown once on the About screen.
 */
@Composable
fun PlatformBadge(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val c = SampleTheme.colors
    Row(
        modifier =
            modifier
                .height(SampleSize.badgeHeight)
                .then(if (compact) Modifier.width(SampleSize.badgeHeight) else Modifier)
                .clip(SampleRadius.pillShape)
                .background(c.surface)
                .border(1.dp, c.outline, SampleRadius.pillShape)
                .then(if (compact) Modifier else Modifier.padding(start = 8.dp, end = 10.dp))
                .semantics(mergeDescendants = true) {
                    contentDescription = SampleBranding.PLATFORM_LABEL
                },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        Icon(
            imageVector = Icons.Default.Android,
            contentDescription = null,
            modifier = Modifier.size(SampleSize.iconXs),
            tint = c.platformAndroid,
        )
        if (!compact) {
            Text(
                text = SampleBranding.PLATFORM_LABEL,
                style = MaterialTheme.typography.labelMedium,
                color = c.text,
            )
        }
    }
}

/** The three DS AppBar sizes on Android (M3 Expressive): 64, 112 and 152dp. */
enum class SampleTopAppBarSize { Small, Medium, Large }

/**
 * The DS AppBar, Android flavour. [SampleTopAppBarSize.Small] is the 64dp row with a 22/28 title;
 * [SampleTopAppBarSize.Medium] (112dp, 28/36) and [SampleTopAppBarSize.Large] (152dp, 36/44) put the
 * title on a second line that collapses into the row as content scrolls. Every size sits on `bg` and
 * tints to `surface-2` once content scrolls under it, with the compact PlatformMarker at the
 * trailing edge.
 *
 * The expanded title wraps, as the DS asks, up to [MAX_EXPANDED_TITLE_LINES] lines: the M3 bar has
 * a fixed expanded height, so the title is measured at the bar's width first and the expanded
 * height grows by one line height per extra line (a long scenario title such as "Open the community
 * full screen" was cut on a phone). The collapsed row keeps the single ellipsized line of M3.
 *
 * [showPlatformBadge] exists for the bars that are not a top-level sample screen (a pushed
 * sub-screen inside the same tab): the marker is an identity, not a decoration for every level.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    size: SampleTopAppBarSize = SampleTopAppBarSize.Small,
    showPlatformBadge: Boolean = true,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val c = SampleTheme.colors
    val colors =
        TopAppBarDefaults.topAppBarColors(
            containerColor = sampleTopBarColor,
            scrolledContainerColor = sampleTopBarScrolledColor,
            titleContentColor = c.text,
            navigationIconContentColor = c.text,
            actionIconContentColor = c.text,
        )
    val titleContent: @Composable () -> Unit = {
        // The two-row bars compose this lambda twice, in the collapsed row (titleLarge) and in the
        // expanded one (the larger DS size provided below): only the expanded row may wrap.
        val isExpandedRow = LocalTextStyle.current.fontSize.value >= EXPANDED_TITLE_MIN_FONT_SP
        Text(
            text = title,
            maxLines = if (isExpandedRow) MAX_EXPANDED_TITLE_LINES else 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
    val actionsContent: @Composable RowScope.() -> Unit = {
        actions()
        if (showPlatformBadge) {
            PlatformBadge(
                compact = true,
                modifier = Modifier.padding(start = 4.dp, end = 12.dp),
            )
        }
    }
    when (size) {
        SampleTopAppBarSize.Small ->
            TopAppBar(
                modifier = modifier,
                title = {
                    ProvideTextStyle(MaterialTheme.typography.titleLarge) { titleContent() }
                },
                navigationIcon = navigationIcon,
                actions = actionsContent,
                scrollBehavior = scrollBehavior,
                colors = colors,
            )

        SampleTopAppBarSize.Medium, SampleTopAppBarSize.Large -> {
            // TwoRowsTopAppBar reads titleLarge for the collapsed row and headlineSmall (medium) or
            // headlineMedium (large) for the expanded one; the sample's type scale maps both to
            // title-1 (26/34), so the DS expanded sizes are provided for the bar alone.
            val typography = MaterialTheme.typography
            val mediumTitle = typography.headlineSmall.copy(fontSize = 28.sp, lineHeight = 36.sp)
            val largeTitle = typography.headlineMedium.copy(fontSize = 36.sp, lineHeight = 44.sp)
            val expandedStyle = if (size == SampleTopAppBarSize.Medium) mediumTitle else largeTitle
            val measurer = rememberTextMeasurer()
            val density = LocalDensity.current
            val layoutDirection = LocalLayoutDirection.current
            // The bar pads its rows by its own horizontal window insets (a display cutout in
            // landscape), so the title gets that much less width than the bar.
            val barInsets = TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Horizontal).asPaddingValues()
            val barInsetWidth =
                barInsets.calculateLeftPadding(layoutDirection) + barInsets.calculateRightPadding(layoutDirection)
            BoxWithConstraints {
                val titleWidthPx =
                    with(density) { (maxWidth - barInsetWidth - EXPANDED_TITLE_HORIZONTAL_INSET).roundToPx() }
                        .coerceAtLeast(1)
                val lines =
                    remember(title, titleWidthPx, expandedStyle) {
                        measurer
                            .measure(
                                text = title,
                                style = expandedStyle,
                                maxLines = MAX_EXPANDED_TITLE_LINES,
                                constraints = Constraints(maxWidth = titleWidthPx),
                            ).lineCount
                    }
                // The bar sits outside the tab NavHost and follows the back stack as soon as a
                // push or a pop starts: animated, a title that wraps to another line count grows
                // or shrinks the bar with the transition instead of resizing the NavHost (and
                // jumping the outgoing screen) on its first frame. The first composition starts
                // at its target, so a screen entered from cold does not animate its bar.
                val extraHeight by animateDpAsState(
                    targetValue = with(density) { expandedStyle.lineHeight.toDp() } * (lines - 1),
                    animationSpec = SampleMotion.defaultSpatial(),
                    label = "expandedTitleExtraHeight",
                )
                MaterialTheme(
                    typography = typography.copy(headlineSmall = mediumTitle, headlineMedium = largeTitle),
                ) {
                    if (size == SampleTopAppBarSize.Medium) {
                        MediumTopAppBar(
                            modifier = modifier,
                            title = titleContent,
                            navigationIcon = navigationIcon,
                            actions = actionsContent,
                            expandedHeight = TopAppBarDefaults.MediumAppBarExpandedHeight + extraHeight,
                            scrollBehavior = scrollBehavior,
                            colors = colors,
                        )
                    } else {
                        LargeTopAppBar(
                            modifier = modifier,
                            title = titleContent,
                            navigationIcon = navigationIcon,
                            actions = actionsContent,
                            expandedHeight = TopAppBarDefaults.LargeAppBarExpandedHeight + extraHeight,
                            scrollBehavior = scrollBehavior,
                            colors = colors,
                        )
                    }
                }
            }
        }
    }
}

/** The most lines the expanded title of a medium or large [SampleTopAppBar] wraps to. */
private const val MAX_EXPANDED_TITLE_LINES = 3

/**
 * The width the M3 two-row bars take from the expanded title row, window insets aside: a 12dp
 * start inset, 4dp of padding on each side of the title box and the 4dp of the empty actions box.
 */
private val EXPANDED_TITLE_HORIZONTAL_INSET = 24.dp

/** Below the DS medium expanded size (28sp) and above the collapsed row's titleLarge. */
private const val EXPANDED_TITLE_MIN_FONT_SP = 27f

// --- Buttons ------------------------------------------------------------------------------------

private const val DISABLED_ALPHA = 0.45f

@Composable
private fun ButtonContent(
    text: String,
    ink: Color,
    isLoading: Boolean,
    icon: ImageVector?,
) {
    if (isLoading) {
        CircularProgressIndicator(
            modifier = Modifier.size(SampleSize.iconXs),
            strokeWidth = 2.dp,
            color = ink,
        )
        Spacer(Modifier.width(SampleSpacing.space2))
    } else if (icon != null) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(SampleSize.iconSm))
        Spacer(Modifier.width(SampleSpacing.space2))
    }
    Text(text, style = SampleTextStyles.buttonLabel)
}

/**
 * DS primary Button (`button-bg` / `button-text`, 48 high, 16/22 semibold): one per screen, the
 * action that moves the task on.
 */
@Composable
fun PrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    minHeight: Int = 48,
    icon: ImageVector? = null,
) {
    val c = SampleTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier.sampleFocusRing(interaction).heightIn(min = minHeight.dp),
        interactionSource = interaction,
        shape = SampleRadius.pillShape,
        contentPadding = buttonPadding(icon != null || isLoading),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = c.buttonBg,
                contentColor = c.buttonText,
                disabledContainerColor = c.buttonBg.copy(alpha = DISABLED_ALPHA),
                disabledContentColor = c.buttonText.copy(alpha = DISABLED_ALPHA),
            ),
    ) {
        ButtonContent(text, c.buttonText, isLoading, icon)
    }
}

/** DS secondary Button: transparent, 1px `outline` edge, label in `text`. */
@Composable
fun SecondaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    minHeight: Int = 48,
    icon: ImageVector? = null,
) {
    val c = SampleTheme.colors
    val active = enabled && !isLoading
    val interaction = remember { MutableInteractionSource() }
    OutlinedButton(
        onClick = onClick,
        enabled = active,
        modifier = modifier.sampleFocusRing(interaction).heightIn(min = minHeight.dp),
        interactionSource = interaction,
        shape = SampleRadius.pillShape,
        contentPadding = buttonPadding(icon != null || isLoading),
        border = BorderStroke(1.dp, if (active) c.outline else c.outline.copy(alpha = DISABLED_ALPHA)),
        colors =
            ButtonDefaults.outlinedButtonColors(
                contentColor = c.text,
                disabledContentColor = c.text.copy(alpha = DISABLED_ALPHA),
            ),
    ) {
        ButtonContent(text, c.text, isLoading, icon)
    }
}

/**
 * [SecondaryPillButton] with a free content slot, for a scenario action whose label is more than a
 * single string (a spinner, a computed label). Same DS edge and ink: Material's own
 * `OutlinedButton` draws its 1px edge in `outlineVariant`, the 8 % `hairline` of this theme, which
 * leaves a bare label floating on the page in dark mode — the scenario screens use this instead.
 */
@Composable
fun SecondaryPillButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    minHeight: Int = 48,
    contentPadding: PaddingValues = buttonPadding(leading = false),
    content: @Composable RowScope.() -> Unit,
) {
    val c = SampleTheme.colors
    val interaction = remember { MutableInteractionSource() }
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.sampleFocusRing(interaction).heightIn(min = minHeight.dp),
        interactionSource = interaction,
        shape = SampleRadius.pillShape,
        contentPadding = contentPadding,
        border = BorderStroke(1.dp, if (enabled) c.outline else c.outline.copy(alpha = DISABLED_ALPHA)),
        colors =
            ButtonDefaults.outlinedButtonColors(
                contentColor = c.text,
                disabledContentColor = c.text.copy(alpha = DISABLED_ALPHA),
            ),
    ) {
        ProvideTextStyle(SampleTextStyles.buttonLabel) { content() }
    }
}

internal fun dangerPillEdge(colors: SampleColors): Color = colors.danger

/**
 * DS danger Button, tonal since DS v14: `danger-tint` fill, `danger` ink, a solid 1px `danger` edge
 * (a 45 % edge measured 2.08:1 on white and 1.82:1 against its own fill, under the 3:1 non-text
 * floor). Reserved for a destructive confirmation.
 */
@Composable
fun DangerPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    minHeight: Int = 48,
) {
    val c = SampleTheme.colors
    val interaction = remember { MutableInteractionSource() }
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier.sampleFocusRing(interaction).heightIn(min = minHeight.dp),
        interactionSource = interaction,
        shape = SampleRadius.pillShape,
        contentPadding = buttonPadding(isLoading),
        border = BorderStroke(1.dp, dangerPillEdge(c)),
        colors =
            ButtonDefaults.outlinedButtonColors(
                containerColor = c.dangerTint,
                contentColor = c.danger,
                disabledContainerColor = c.dangerTint.copy(alpha = DISABLED_ALPHA),
                disabledContentColor = c.danger.copy(alpha = DISABLED_ALPHA),
            ),
    ) {
        ButtonContent(text, c.danger, isLoading, icon = null)
    }
}

/** DS text Button (`accent-text`): a low-weight action inside a block, on a 48 target. */
@Composable
fun SampleTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = SampleTheme.colors.accentText,
) {
    val interaction = remember { MutableInteractionSource() }
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.sampleFocusRing(interaction).heightIn(min = SampleSize.touchMin),
        interactionSource = interaction,
        shape = SampleRadius.pillShape,
        contentPadding = PaddingValues(horizontal = SampleSpacing.space3),
        colors = ButtonDefaults.textButtonColors(contentColor = color),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

private fun buttonPadding(leading: Boolean) =
    PaddingValues(
        start = if (leading) SampleSpacing.space4 else SampleSpacing.space6,
        end = SampleSpacing.space6,
    )

// --- Blocks -------------------------------------------------------------------------------------

/**
 * A titled level-1 card: `surface`, `radius-md`, 1px `border` plus `shadow-card`, 16 padding.
 */
@Composable
fun SampleCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .sampleCardEdge(SampleRadius.mdShape)
                .clip(SampleRadius.mdShape)
                .background(SampleTheme.colors.surface)
                // A card whose content changes (a status, a result, a row that appears) resizes on
                // the expressive spatial spring instead of jumping.
                .animateContentSize(SampleMotion.defaultSpatial())
                .padding(SampleSpacing.space4),
        verticalArrangement = Arrangement.spacedBy(SampleSpacing.space3),
    ) {
        if (title != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = SampleTheme.colors.text,
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (trailing != null) trailing()
            }
        }
        content()
    }
}

/**
 * The DS ScenarioBlock: a titled `radius-xl` block. The `try` tone is the `accent-tint` ground of
 * "Try it", with a 1px `accent-text` edge at 35 %; the default tone is a level-1 card on `surface`.
 */
@Composable
fun SampleScenarioBlock(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tryTone: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = SampleTheme.colors
    val shape = SampleRadius.xlShape
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    if (tryTone) {
                        Modifier
                            .clip(shape)
                            .background(c.accentTint)
                            .border(1.dp, c.accentText.copy(alpha = 0.35f), shape)
                    } else {
                        Modifier.sampleCardEdge(shape).clip(shape).background(c.surface)
                    },
                ).padding(start = SampleSpacing.space4, end = SampleSpacing.space4, top = 4.dp, bottom = SampleSpacing.space4),
        verticalArrangement = Arrangement.spacedBy(SampleSpacing.space3),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = SampleSize.touchMin),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SampleSpacing.space2),
        ) {
            if (icon != null) Icon(icon, contentDescription = null, tint = c.accentText, modifier = Modifier.size(SampleSize.iconSm))
            Text(title, style = SampleTextStyles.buttonLabel, color = c.text)
        }
        content()
    }
}

/**
 * The "Configuration" summary: a level-1 card listing what the SDK is pointed at, read-only.
 * Carried by Settings › Developer tools › Debug info since the 3-tab shell (DS v1) dropped the Home
 * and Explorer tabs that used to frame it. It names where to edit instead of offering its own
 * "Change": Settings › Configuration is the single entry into Config.
 */
@Composable
fun CurrentConfigurationBlock(
    lines: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    title: String = "Configuration",
) {
    val c = SampleTheme.colors
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .sampleCardEdge(SampleRadius.mdShape)
                .clip(SampleRadius.mdShape)
                .background(c.surface)
                .padding(SampleSpacing.space4),
        verticalArrangement = Arrangement.spacedBy(SampleSpacing.space2),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = c.text,
        )
        // Same wrapping behaviour as every other info row: a long value drops under its label.
        lines.forEach { (label, value) -> SampleInfoRow(label = label, value = value) }
        Text(
            text = "Edit in Settings \u203a Configuration",
            style = MaterialTheme.typography.bodySmall,
            color = c.textSecondary,
        )
    }
}

/** Monospace tag naming the SDK symbol a card or a scenario stands for: `accent-tint` / `accent-text`. */
@Composable
fun ApiPill(
    name: String,
    modifier: Modifier = Modifier,
) {
    val c = SampleTheme.colors
    Text(
        text = name,
        style = SampleTextStyles.monoSmall,
        color = c.accentText,
        modifier =
            modifier
                .clip(SampleRadius.smShape)
                .background(c.accentTint)
                .padding(horizontal = SampleSpacing.space2, vertical = 3.dp),
    )
}

/** Tag naming a parameter's type (`String`, `Set<String>`…): `surface-3` with a 1px `outline`. */
@Composable
fun TypePill(
    type: String,
    modifier: Modifier = Modifier,
) {
    val c = SampleTheme.colors
    Text(
        text = type,
        style = SampleTextStyles.monoSmall,
        color = c.text,
        modifier =
            modifier
                .clip(SampleRadius.smShape)
                .background(c.surface3)
                .border(1.dp, c.outline, SampleRadius.smShape)
                .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/**
 * A state line: a dot plus a word (Ready, Connected, Guest, Off), the colour never carrying the
 * meaning alone. Pass a status *ink* ([sampleSuccessColor], [sampleAmberColor], [sampleDangerColor]).
 */
@Composable
fun StatusDot(
    color: Color,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}

/** The six scenario states of the DS StatusBadge. */
enum class SampleStatus(
    val defaultLabel: String,
) {
    Ready("Ready"),
    Running("Running"),
    Passed("Passed"),
    Failed("Failed"),
    Blocked("Blocked"),
    NotApplicable("N/A"),
}

/**
 * The DS StatusBadge, tonal since DS v13: a word plus an icon, 28 high, 12/16 semibold, a 1.5px
 * edge. Outline (Ready), tint (Running), tonal success / danger / warning (Passed, Failed, Blocked)
 * and a dashed outline (N/A, never a success) keep the six apart for colour-blind readers.
 */
@Composable
fun StatusBadge(
    status: SampleStatus,
    modifier: Modifier = Modifier,
    label: String = status.defaultLabel,
) {
    val c = SampleTheme.colors
    val (fill, ink) = statusBadgeColors(status, c)
    val shape = SampleRadius.pillShape
    Row(
        modifier =
            modifier
                .height(SampleSize.badgeHeight)
                .clip(shape)
                .background(fill)
                .then(
                    if (status == SampleStatus.NotApplicable) {
                        Modifier.drawBehind {
                            val stroke = 1.5.dp.toPx()
                            drawRoundRect(
                                color = c.outline,
                                topLeft = Offset(stroke / 2, stroke / 2),
                                size = Size(size.width - stroke, size.height - stroke),
                                cornerRadius = CornerRadius(size.height / 2),
                                style =
                                    Stroke(
                                        width = stroke,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
                                    ),
                            )
                        }
                    } else {
                        Modifier.border(1.5.dp, ink, shape)
                    },
                ).padding(start = SampleSpacing.space2, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (status == SampleStatus.Running) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = ink,
            )
        } else {
            Icon(
                imageVector = statusBadgeIcon(status),
                contentDescription = null,
                modifier = Modifier.size(SampleSize.iconXs),
                tint = ink,
            )
        }
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = ink)
    }
}

/** Fill and ink of a [StatusBadge], per the DS v13 table. */
internal fun statusBadgeColors(
    status: SampleStatus,
    c: SampleColors,
): Pair<Color, Color> =
    when (status) {
        SampleStatus.Ready -> Color.Transparent to c.accentText
        SampleStatus.Running -> c.accentTint to c.accentText
        SampleStatus.Passed -> c.successTint to c.successText
        SampleStatus.Failed -> c.dangerTint to c.danger
        SampleStatus.Blocked -> c.warningTint to c.warningText
        SampleStatus.NotApplicable -> Color.Transparent to c.textSecondary
    }

private fun statusBadgeIcon(status: SampleStatus): ImageVector =
    when (status) {
        SampleStatus.Ready -> Icons.Default.PlayArrow
        SampleStatus.Running -> Icons.Default.PlayArrow
        SampleStatus.Passed -> Icons.Default.Check
        SampleStatus.Failed -> Icons.Default.Close
        SampleStatus.Blocked -> Icons.Default.Lock
        SampleStatus.NotApplicable -> Icons.Default.Remove
    }

/**
 * The raw `OctopusResult` of the last call: a StatusBadge ("Success · 124 ms" / "Failure"), then
 * the DS code block (`code-bg`, `code-text`, mono 13/18, `radius-md`).
 *
 * The value is printed *raw* on purpose: an integrator reading the sample wants the shape the SDK
 * actually returned, not a friendly sentence the sample wrote over it.
 */
@Composable
fun CodeResultBlock(
    result: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    durationMs: Long? = null,
) {
    val c = SampleTheme.colors
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SampleSpacing.space2),
    ) {
        StatusBadge(
            status = if (isError) SampleStatus.Failed else SampleStatus.Passed,
            label = resultBadgeLabel(isError, durationMs),
        )
        Text(
            text = result,
            style = SampleTextStyles.mono,
            color = c.codeText,
            // Wrapped, not horizontally scrolled: a single-line payload (a JWT, a long failure
            // message) was cut at the right edge with nothing saying the rest existed.
            softWrap = true,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(SampleRadius.mdShape)
                    .background(c.codeBg)
                    .padding(SampleSpacing.space3),
        )
    }
}

/** "Success · 124 ms", "Failure", in the DS sentence case. */
internal fun resultBadgeLabel(
    isError: Boolean,
    durationMs: Long?,
): String {
    val word = if (isError) "Failure" else "Success"
    return if (durationMs != null) "$word · $durationMs ms" else word
}

/**
 * The DS ResultCompare: what should happen next to what the SDK did, on `surface-2` columns, the
 * observed one turning `success-tint` or `danger-tint`, and a one-line verdict in words.
 *
 * [observed] must be written from real SDK state, never a canned string. [match] is null while
 * nothing has been observed yet.
 */
@Composable
fun SampleResultCompare(
    expected: String,
    observed: String,
    match: Boolean?,
    verdict: String,
    modifier: Modifier = Modifier,
) {
    val c = SampleTheme.colors
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(SampleSpacing.space2)) {
        Row(horizontalArrangement = Arrangement.spacedBy(SampleSpacing.space2)) {
            CompareColumn("Expected", expected, c.surface2, Modifier.weight(1f))
            CompareColumn(
                "Observed",
                observed,
                when (match) {
                    true -> c.successTint
                    false -> c.dangerTint
                    null -> c.surface2
                },
                Modifier.weight(1f),
            )
        }
        val ink =
            when (match) {
                true -> c.successText
                false -> c.danger
                null -> c.textSecondary
            }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                imageVector =
                    when (match) {
                        true -> Icons.Default.CheckCircle
                        false -> Icons.Default.Error
                        null -> Icons.Default.Info
                    },
                contentDescription = null,
                tint = ink,
                modifier = Modifier.size(SampleSize.iconXs),
            )
            Text(verdict, style = MaterialTheme.typography.labelLarge, color = ink)
        }
    }
}

@Composable
private fun CompareColumn(
    label: String,
    text: String,
    fill: Color,
    modifier: Modifier,
) {
    val shape = SampleRadius.lgShape
    Column(
        modifier =
            modifier
                .widthIn(min = 136.dp)
                .clip(shape)
                .background(fill)
                .sampleNestedEdge(shape)
                .padding(SampleSpacing.space3),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = SampleTheme.colors.text)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = SampleTheme.colors.text)
    }
}

/**
 * Whether a DS Segmented control must become a horizontally scrolling chip row (the DS rule, word
 * for word): more than four options, any label longer than twelve characters, or four options
 * whose longest label passes eight — four such labels no longer fit one 320dp-wide pill.
 */
internal fun segmentedNeedsChipRow(labels: List<String>): Boolean {
    val longest = labels.maxOfOrNull { it.length } ?: 0
    return labels.size > SEGMENTED_MAX_OPTIONS ||
        longest > SEGMENTED_MAX_LABEL ||
        (labels.size > SEGMENTED_DENSE_OPTIONS && longest > SEGMENTED_DENSE_LABEL)
}

private const val SEGMENTED_MAX_OPTIONS = 4
private const val SEGMENTED_MAX_LABEL = 12
private const val SEGMENTED_DENSE_OPTIONS = 3
private const val SEGMENTED_DENSE_LABEL = 8

/** Width of the fade on each edge of the scrolling chip row (DS `mask-image`, 20px). */
private val SEGMENTED_FADE = 20.dp

/**
 * The DS Segmented control, the variant switch of a ScenarioBlock and of the settings screens.
 *
 * Two to four short options: an outlined pill 48 high with `outline` dividers, labels 14/20
 * semibold, the selected option on `accent-container` with a 16px check. More options, or a longer
 * label ([segmentedNeedsChipRow]): a horizontally scrolling chip row — chips 40 high, 8 apart, a
 * 1px `outline`, labels 14/20 medium that never wrap; the selected chip is filled, semibold, with
 * the check, and is scrolled to the centre of the row; both edges fade over 20dp and the row keeps
 * a 16dp inset, so it reads as scrollable and bleeds to the screen edges.
 *
 * [scrollable] forces either behaviour, as the DS `scrollable` prop does. A chip row is meant to
 * sit edge to edge: give it no horizontal padding of its own, its 16dp inset is the gutter.
 */
@Composable
fun <T> SampleSegmentedChoice(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    scrollable: Boolean? = null,
) {
    val c = SampleTheme.colors
    if (scrollable ?: segmentedNeedsChipRow(options.map { it.second })) {
        SegmentedChipRow(options, selected, onSelect, modifier)
        return
    }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = SampleSize.touchMin)
                .clip(SampleRadius.pillShape)
                .border(1.dp, c.outline, SampleRadius.pillShape)
                .selectableGroup(),
    ) {
        options.forEachIndexed { index, (value, label) ->
            val isSelected = value == selected
            val interaction = remember { MutableInteractionSource() }
            if (index > 0) {
                Box(
                    Modifier
                        .width(1.dp)
                        .heightIn(min = SampleSize.touchMin)
                        .background(c.outline),
                )
            }
            Row(
                modifier =
                    Modifier
                        .weight(1f)
                        .heightIn(min = SampleSize.touchMin)
                        .background(if (isSelected) c.accentContainer else Color.Transparent)
                        // Inside the segment: the pill clips its options.
                        .sampleFocusRing(interaction, SampleRadius.pillShape, gap = (-4).dp)
                        .selectable(
                            selected = isSelected,
                            interactionSource = interaction,
                            indication = ripple(),
                            role = Role.RadioButton,
                        ) { onSelect(value) }
                        .padding(horizontal = SampleSpacing.space3),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            ) {
                if (isSelected) {
                    Icon(Icons.Default.Check, null, tint = c.onAccentContainer, modifier = Modifier.size(SampleSize.iconXs))
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) c.onAccentContainer else c.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun <T> SegmentedChipRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier,
) {
    val c = SampleTheme.colors
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    // Where each chip sits in the row, so the selected one can be centred (DS: scrolled into view).
    val chipBounds = remember(options.size) { mutableStateMapOf<Int, IntRange>() }
    var viewport by remember { mutableIntStateOf(0) }
    var centredOnce by remember { mutableStateOf(false) }
    val selectedIndex = options.indexOfFirst { it.first == selected }
    LaunchedEffect(selectedIndex, viewport, chipBounds[selectedIndex]) {
        val bounds = chipBounds[selectedIndex] ?: return@LaunchedEffect
        if (viewport == 0) return@LaunchedEffect
        val target = ((bounds.first + bounds.last) / 2 - viewport / 2).coerceIn(0, scroll.maxValue)
        // The first frame lands on the selection; a later pick glides to it.
        if (centredOnce) scroll.animateScrollTo(target) else scroll.scrollTo(target)
        centredOnce = true
    }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .onSizeChanged { viewport = it.width }
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val fade = with(density) { SEGMENTED_FADE.toPx() }.coerceAtMost(size.width / 2f)
                    // DS `mask-image`: transparent → opaque over 20dp on both edges.
                    drawRect(
                        brush = Brush.horizontalGradient(listOf(Color.Transparent, Color.Black), startX = 0f, endX = fade),
                        size = Size(fade, size.height),
                        blendMode = BlendMode.DstIn,
                    )
                    drawRect(
                        brush =
                            Brush.horizontalGradient(
                                listOf(Color.Black, Color.Transparent),
                                startX = size.width - fade,
                                endX = size.width,
                            ),
                        topLeft = Offset(size.width - fade, 0f),
                        size = Size(fade, size.height),
                        blendMode = BlendMode.DstIn,
                    )
                }.horizontalScroll(scroll)
                .selectableGroup()
                .padding(horizontal = SampleSpacing.space4, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(SampleSpacing.space2),
    ) {
        options.forEachIndexed { index, (value, label) ->
            val isSelected = value == selected
            val interaction = remember { MutableInteractionSource() }
            Row(
                modifier =
                    Modifier
                        .onPlaced {
                            val x = it.positionInParent().x.toInt()
                            chipBounds[index] = x..(x + it.size.width)
                        }.heightIn(min = SampleSize.chipHeight)
                        .sampleFocusRing(interaction)
                        .clip(SampleRadius.pillShape)
                        .background(if (isSelected) c.accentContainer else Color.Transparent)
                        .then(if (isSelected) Modifier else Modifier.border(1.dp, c.outline, SampleRadius.pillShape))
                        .selectable(
                            selected = isSelected,
                            interactionSource = interaction,
                            indication = ripple(),
                            role = Role.RadioButton,
                        ) { onSelect(value) }
                        // The check pulls 4dp into the start padding (DS `margin-left: -4px`).
                        .padding(start = if (isSelected) 12.dp else 16.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (isSelected) {
                    Icon(Icons.Default.Check, null, tint = c.onAccentContainer, modifier = Modifier.size(SampleSize.iconXs))
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) c.onAccentContainer else c.text,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

/**
 * A DS SettingsRow with a toggle: title 16/22, optional supporting line 14/20 in `text-secondary`,
 * the DS Toggle trailing, 56 minimum. The caller owns the horizontal inset.
 */
@Composable
fun SampleSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = SampleSize.rowMin)
                .toggleable(
                    value = checked,
                    enabled = enabled,
                    role = Role.Switch,
                    interactionSource = interaction,
                    indication = LocalIndication.current,
                    onValueChange = onCheckedChange,
                ).sampleFocusRing(interaction),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SampleSpacing.space4),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = SampleTheme.colors.text,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = sampleSwitchColors(),
            interactionSource = interaction,
        )
    }
}

/**
 * A DS SettingsRow with a chevron: bare icon in `text-secondary` (M3), title 16/22, the current
 * value as a supporting line, chevron in `text-secondary`, 56 minimum. `destructive` turns the
 * title and icon `danger` (Reset sample only; the action asks for confirmation).
 */
@Composable
fun SummaryRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
) {
    val c = SampleTheme.colors
    val tint = if (destructive) c.danger else c.textSecondary
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = SampleSize.rowMin)
                // Inside the row: the list group clips its children, so a ring outside would be cut.
                .sampleFocusRing(interaction, SampleRadius.mdShape, gap = (-4).dp)
                .clickable(interactionSource = interaction, indication = ripple(), onClick = onClick)
                .padding(horizontal = SampleSpacing.space4, vertical = SampleSpacing.space2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SampleSpacing.space4),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(SampleSize.icon),
            tint = tint,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (destructive) c.danger else c.text,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            // Decoration, not content: the row's own label already says it navigates.
            tint = c.textSecondary,
        )
    }
}

/**
 * The DS SectionHeader: `titleSmall` 14/20 semibold in `text`, sentence case, 24 above, 8 below,
 * 16 side margins, no divider. An optional [count] rides as a `surface-3` tag with an `outline`.
 * [countText] replaces the number when the tag reads as a ratio (the DS `3/4`), and [countLabel]
 * is what a screen reader says for it (the DS `countLabel`, e.g. "3 of 4 on").
 */
@Composable
fun SummaryGroupHeader(
    text: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    countText: String? = count?.toString(),
    countLabel: String? = null,
) {
    val c = SampleTheme.colors
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    start = SampleSpacing.space4,
                    end = SampleSpacing.space4,
                    top = SampleSpacing.space6,
                    bottom = SampleSpacing.space2,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SampleSpacing.space2),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = c.text,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (countText != null) {
            Text(
                text = countText,
                style = MaterialTheme.typography.labelMedium,
                color = c.text,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .widthIn(min = 24.dp)
                        .clip(SampleRadius.pillShape)
                        .background(c.surface3)
                        .border(1.dp, c.outline, SampleRadius.pillShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .then(
                            if (countLabel != null) {
                                Modifier.semantics { contentDescription = countLabel }
                            } else {
                                Modifier
                            },
                        ),
            )
        }
    }
}

/**
 * The DS ListGroup, Android flavour: a flat M3 list on `surface`; put [SampleListDivider] between
 * rows (inset 16 both sides, `hairline`, decorative).
 */
@Composable
fun SampleListGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth().background(SampleTheme.colors.surface),
        content = content,
    )
}

/** Separator of a [SampleListGroup]: `hairline`, inset 16 on both sides. */
@Composable
fun SampleListDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(horizontal = SampleSpacing.space4),
        thickness = 1.dp,
        color = SampleTheme.colors.hairline,
    )
}

/**
 * Label/value line, the read-only counterpart of [SampleSwitchRow].
 *
 * Label at the start, value at the end; when both no longer fit on one line (a long value, or a
 * large font scale) the value drops to its own line under the label instead of squeezing the label
 * into a mid-word wrap ("Communit" / "y" at 200 %, ISO QA of 2026-10-06).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SampleInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color? = null,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = SampleSpacing.space3),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
        )
    }
}

// --- Feedback: banner, state view, snackbar ------------------------------------------------------

/** The three tones of a DS Banner. */
enum class SampleBannerTone { Info, Warning, Error }

/**
 * The DS Banner: says something about the current screen the reader must see.
 *
 * Tonal surface (`accent-tint`, `warning-tint`, `danger-tint`) with a 1px edge in the tone ink at
 * 45 %, no side stripe, no shadow. A 20px icon and the title in the tone ink, the message 14/20 in
 * `text`, at most one action as a text button on a 48 target. [inline] drops the radius and keeps
 * only the bottom edge, for a full-width band anchored under the app bar. Error is announced
 * assertively, the others politely.
 */
@Composable
fun SampleBanner(
    tone: SampleBannerTone,
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    inline: Boolean = false,
    icon: ImageVector? = null,
    actionModifier: Modifier = Modifier,
) {
    val c = SampleTheme.colors
    val (fill, ink) = bannerColors(tone, c)
    val edge = ink.copy(alpha = 0.45f)
    val shape = SampleRadius.lgShape
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .then(
                    if (inline) {
                        Modifier
                            .background(fill)
                            .drawBehind {
                                val stroke = 1.dp.toPx()
                                drawRect(
                                    color = edge,
                                    topLeft = Offset(0f, size.height - stroke),
                                    size = Size(size.width, stroke),
                                )
                            }
                    } else {
                        Modifier
                            .clip(shape)
                            .background(fill)
                            .border(1.dp, edge, shape)
                    },
                ).semantics {
                    liveRegion = if (tone == SampleBannerTone.Error) LiveRegionMode.Assertive else LiveRegionMode.Polite
                }.padding(
                    start = SampleSpacing.space4,
                    end = SampleSpacing.space3,
                    top = SampleSpacing.space3,
                    bottom = SampleSpacing.space3,
                ),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(SampleSpacing.space3),
    ) {
        Icon(
            imageVector = icon ?: bannerIcon(tone),
            contentDescription = null,
            tint = ink,
            modifier = Modifier.size(SampleSize.iconSm),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = ink)
            if (message != null) {
                Text(message, style = MaterialTheme.typography.bodyMedium, color = c.text)
            }
        }
        if (actionLabel != null && onAction != null) {
            SampleTextButton(
                text = actionLabel,
                onClick = onAction,
                color = ink,
                modifier = actionModifier.align(Alignment.CenterVertically),
            )
        }
    }
}

/** Fill and ink of a [SampleBanner] tone. */
internal fun bannerColors(
    tone: SampleBannerTone,
    c: SampleColors,
): Pair<Color, Color> =
    when (tone) {
        SampleBannerTone.Info -> c.accentTint to c.accentText
        SampleBannerTone.Warning -> c.warningTint to c.warningText
        SampleBannerTone.Error -> c.dangerTint to c.danger
    }

private fun bannerIcon(tone: SampleBannerTone): ImageVector =
    when (tone) {
        SampleBannerTone.Info -> Icons.Default.Info
        SampleBannerTone.Warning -> Icons.Default.Warning
        SampleBannerTone.Error -> Icons.Default.Error
    }

/**
 * The DS StateView: fills a screen or a block that has nothing to show yet. Centred, a 64 icon
 * disc on `surface-3` (`danger-tint` for an error), title 18/24, one sentence 14/20, at most one
 * button. An error always offers Retry and says what failed in plain words.
 */
@Composable
fun SampleStateView(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    isError: Boolean = false,
) {
    val c = SampleTheme.colors
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = SampleSpacing.space6, vertical = SampleSpacing.space8),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(SampleSpacing.space3, Alignment.CenterVertically),
    ) {
        Box(
            modifier =
                Modifier
                    .size(SampleSize.stateDisc)
                    .clip(CircleShape)
                    .background(if (isError) c.dangerTint else c.surface3),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = if (isError) c.danger else c.textSecondary, modifier = Modifier.size(32.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, color = c.text, textAlign = TextAlign.Center)
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = c.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 300.dp),
        )
        if (actionLabel != null && onAction != null) {
            PrimaryPillButton(text = actionLabel, onClick = onAction, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/**
 * The DS Snackbar: navy with light text in light theme, light grey with navy text in dark, one
 * action, `radius-md`. The DS defines these two pairs inline rather than as tokens (reported back).
 */
@Composable
fun SampleSnackbar(
    data: SnackbarData,
    modifier: Modifier = Modifier,
) {
    val dark = LocalSampleDarkTheme.current
    Snackbar(
        snackbarData = data,
        modifier = modifier,
        shape = SampleRadius.mdShape,
        containerColor = if (dark) SnackbarDarkContainer else SampleColors.Light.navy,
        contentColor = if (dark) SampleColors.Light.navy else SampleColors.Dark.text,
        actionColor = if (dark) SnackbarDarkAction else SampleColors.Dark.accentText,
        dismissActionContentColor = if (dark) SampleColors.Light.navy else SampleColors.Dark.text,
    )
}

/** DS Snackbar, dark theme: `#e6eaf0` container. Not a DS token. */
private val SnackbarDarkContainer = Color(0xFFE6EAF0)

/** DS Snackbar, dark theme: `#0a66cc` action. Not a DS token. */
private val SnackbarDarkAction = Color(0xFF0A66CC)
