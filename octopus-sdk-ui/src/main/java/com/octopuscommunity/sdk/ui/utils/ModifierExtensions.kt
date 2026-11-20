package com.octopuscommunity.sdk.ui.utils

import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.abs

/**
 * Reports the scroll direction of the scrollable below, once it has moved [threshold] px the same
 * way — what drives the quick-return chrome (the "Browse groups" bar, the extended FAB).
 *
 * It listens to what the list actually *consumed* (`onPostScroll`), not to the raw drag
 * (`onPreScroll`): a drag the list cannot follow — a short feed, the end of the list, the
 * pull-to-refresh stretch — no longer collapses or reveals the chrome while nothing moves.
 */
internal fun Modifier.onScrollDownChanged(
    threshold: Float = 50f,
    onDirectionChanged: (isScrollingDown: Boolean) -> Unit,
) = composed {
    var scrollOffset by remember { mutableFloatStateOf(0f) }

    nestedScroll(
        remember {
            object : NestedScrollConnection {
                override fun onPostScroll(
                    consumed: Offset,
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    // A reversal restarts the count, so a jittery finger does not flip the chrome.
                    if (consumed.y != 0f && (consumed.y < 0) != (scrollOffset < 0)) scrollOffset = 0f
                    scrollOffset += consumed.y

                    if (abs(scrollOffset) > threshold) {
                        onDirectionChanged(scrollOffset < 0)
                        scrollOffset = 0f
                    }
                    return Offset.Zero
                }
            }
        },
    )
}

@Composable
fun Modifier.safeImePadding(): Modifier {
    val activity = LocalActivity.current

    DisposableEffect(activity) {
        val window = activity?.window ?: return@DisposableEffect onDispose {}

        // Force SOFT_INPUT_ADJUST_RESIZE while any screen using this modifier is composed, and
        // restore the host's mode once the last one leaves (see SoftInputModeLeases).
        imeResizeLeases.acquire(window, SOFT_INPUT_ADJUST_RESIZE)
        onDispose { imeResizeLeases.release(window) }
    }

    // Always apply imePadding since we've forced the mode
    return this.then(Modifier.imePadding())
}

private val imeResizeLeases =
    SoftInputModeLeases<Window>(
        readMode = { it.attributes?.softInputMode },
        writeMode = { window, mode -> window.setSoftInputMode(mode) },
    )

internal fun Modifier.onKeyboardVisibilityChanged(onVisibilityChanged: (Boolean) -> Unit): Modifier =
    composed {
        val view = LocalView.current
        DisposableEffect(view) {
            val listener =
                ViewTreeObserver.OnGlobalLayoutListener {
                    val isKeyboardVisible =
                        ViewCompat
                            .getRootWindowInsets(view)
                            ?.isVisible(WindowInsetsCompat.Type.ime()) ?: false
                    onVisibilityChanged(isKeyboardVisible)
                }

            view.viewTreeObserver.addOnGlobalLayoutListener(listener)
            onDispose {
                view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
            }
        }
        this
    }

internal fun Modifier.onVisibilityChanged(onVisibilityChanged: (Boolean) -> Unit) =
    composed {
        var isVisible by remember { mutableStateOf(false) }
        LaunchedEffect(isVisible) { onVisibilityChanged.invoke(isVisible) }
        onGloballyPositioned { layoutCoordinates ->
            isVisible = layoutCoordinates.parentLayoutCoordinates
                ?.boundsInWindow()
                ?.overlaps(layoutCoordinates.boundsInWindow()) ?: false
        }
    }
