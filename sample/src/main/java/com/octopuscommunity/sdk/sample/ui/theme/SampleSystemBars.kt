package com.octopuscommunity.sdk.sample.ui.theme

import android.os.Build
import android.view.Window
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

/**
 * The app bar's resting container: the page `bg` (DS AppBar, Android M3 Expressive). The DS
 * retired the navy app bar; it tints to [sampleTopBarScrolledColor] once content scrolls under it.
 */
internal val sampleTopBarColor: Color
    @Composable @ReadOnlyComposable
    get() = SampleTheme.colors.bg

/** The app bar's container once content has scrolled under it: `surface-2`. */
internal val sampleTopBarScrolledColor: Color
    @Composable @ReadOnlyComposable
    get() = SampleTheme.colors.surface2

/** The navigation bar's container: `surface-2`. */
internal val sampleBottomBarColor: Color
    @Composable @ReadOnlyComposable
    get() = SampleTheme.colors.surface2

/** Default container of the sample's own bottom sheets (not the community sheet, which is SDK-themed). */
internal val sampleSheetContainerColor: Color
    @Composable @ReadOnlyComposable
    get() = SampleTheme.colors.surface

/**
 * Colour the host Activity paints behind the status bar for the current route, provided by
 * `MainActivity` next to its own [SyncSampleSystemBars] call. A [SampleModalBottomSheet] that does
 * not reach the top leaves this strip visible under its scrim, so its status icons must follow it.
 */
internal val LocalSampleHostStatusBarColor = compositionLocalOf { Color.Unspecified }

/** The page container of a sample-owned screen: the flat DS `bg`. */
internal val samplePageColor: Color
    @Composable @ReadOnlyComposable
    get() = SampleTheme.colors.bg

/** The content colour of a sample-owned page container: `text`. */
internal val samplePageContentColor: Color
    @Composable @ReadOnlyComposable
    get() = SampleTheme.colors.text

/**
 * Paint and consume the window's safe area once, before nested app bars and scaffolds: the
 * status strip in [statusBarColor], everything else in [navigationBarColor] until the content
 * covers it. Pages are flat — the DS draws no backdrop glow.
 */
@Composable
internal fun SampleWindowFrame(
    statusBarColor: Color,
    navigationBarColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val insets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
    val density = LocalDensity.current
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(navigationBarColor)
                .drawBehind {
                    drawRect(statusBarColor, size = Size(size.width, insets.getTop(density).toFloat()))
                }.windowInsetsPadding(insets),
        content = content,
    )
}

/** Use painted colors, including forced appearance, rather than the device's night mode. */
@Suppress("DEPRECATION")
@Composable
internal fun SyncSampleSystemBars(
    statusBarColor: Color,
    navigationBarColor: Color,
    window: Window? = (LocalView.current.parent as? DialogWindowProvider)?.window,
) {
    if (window == null) return
    SideEffect {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = statusBarColor.luminance() > 0.5f
            isAppearanceLightNavigationBars = navigationBarColor.luminance() > 0.5f
        }
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        // API 23–25 cannot draw dark navigation icons; retain an opaque dark fallback there.
        window.navigationBarColor =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                android.graphics.Color.TRANSPARENT
            } else {
                android.graphics.Color.BLACK
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.navigationBarDividerColor = android.graphics.Color.TRANSPARENT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
    }
}

/**
 * [ModalBottomSheet] whose own window keeps readable system bar icons.
 *
 * Material3 sets the sheet window's icon appearance once, at open, from the content colour. The
 * sample paints strips that may disagree with it (the host strip under the scrim), so this
 * wrapper re-syncs the sheet window the same way the Activity window is synced: status icons follow
 * the sheet container once it slides under the status bar and the host strip (under the scrim)
 * otherwise, navigation icons follow the container that the sheet draws behind the navigation bar,
 * and the platform's 3-button contrast scrim is turned off on API 29+.
 *
 * The 32 % black scrim is not composited into the host colour: it darkens every strip the sample
 * paints without changing which icon tint reads better on it.
 *
 * The drag handle is the Material 3 default (32 x 4 dp) on the DS `outline` token, which holds
 * 3:1 on both the sample surface and the SDK background (DS card Host sheet, v19); the default
 * `onSurfaceVariant` tint is the sample's text colour, not a control colour.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SampleModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    containerColor: Color = sampleSheetContainerColor,
    content: @Composable ColumnScope.() -> Unit,
) {
    val hostStatusBarColor = LocalSampleHostStatusBarColor.current.takeOrElse { containerColor }
    val statusBarHeight = WindowInsets.statusBars.getTop(LocalDensity.current)
    val coversStatusBar by remember(sheetState, statusBarHeight) {
        derivedStateOf {
            // The sheet paints its container behind the status bar when it reaches the top.
            // Follow its animated offset, including dismissal back to the host.
            val offset = runCatching { sheetState.requireOffset() }.getOrDefault(Float.MAX_VALUE)
            offset <= statusBarHeight / 2f
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        containerColor = containerColor,
        dragHandle = { BottomSheetDefaults.DragHandle(color = SampleTheme.colors.outline) },
    ) {
        SyncSampleSystemBars(
            statusBarColor = if (coversStatusBar) containerColor else hostStatusBarColor,
            navigationBarColor = containerColor,
        )
        content()
    }
}
