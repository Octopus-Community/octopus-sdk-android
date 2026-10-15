package com.octopuscommunity.sdk.sample.ui.theme

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

/**
 * The sample's motion: the Material 3 **expressive** motion scheme, for the sample's own chrome
 * only. The SDK screens keep the motion the SDK ships.
 *
 * Why not `MaterialTheme(motionScheme = MotionScheme.expressive())`: material3 1.4.0 (the version
 * the Compose BOM resolves for this repo) keeps `MotionScheme`, `MotionScheme.expressive()` and that
 * `MaterialTheme` overload internal. A newer material3 cannot be pulled for the sample alone —
 * Gradle resolves one material3 per app, so the SDK screens inside the sample would run on an alpha
 * that no host ships. These are the springs `MotionScheme.expressive()` returns, from the Material 3
 * motion tokens (the expressive scheme's spatial and effects springs), so swapping this object for
 * `MaterialTheme.motionScheme` once `MotionScheme` is public is a rename, not a redesign.
 *
 * Spatial springs move things (size, position) and may overshoot; effects springs change colour and
 * opacity and never do.
 */
object SampleMotion {
    /** Expressive `fastSpatial`: damping 0.6, stiffness 800. */
    fun <T> fastSpatial(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.6f, stiffness = 800f)

    /** Expressive `defaultSpatial`: damping 0.8, stiffness 380. */
    fun <T> defaultSpatial(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.8f, stiffness = 380f)

    /** Expressive `slowSpatial`: damping 0.8, stiffness 200. */
    fun <T> slowSpatial(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.8f, stiffness = 200f)

    /** Expressive `fastEffects`: critically damped, stiffness 3800. */
    fun <T> fastEffects(): FiniteAnimationSpec<T> = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 3800f)

    /** Expressive `defaultEffects`: critically damped, stiffness 1600. */
    fun <T> defaultEffects(): FiniteAnimationSpec<T> = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 1600f)

    /** Expressive `slowEffects`: critically damped, stiffness 800. */
    fun <T> slowEffects(): FiniteAnimationSpec<T> = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 800f)

    /** A block that appears in place (a banner, a section body): it grows, then fades in. */
    val reveal: EnterTransition
        get() =
            expandVertically(animationSpec = defaultSpatial<IntSize>()) +
                fadeIn(animationSpec = defaultEffects())

    /** The reverse of [reveal]. */
    val conceal: ExitTransition
        get() =
            shrinkVertically(animationSpec = defaultSpatial<IntSize>()) +
                fadeOut(animationSpec = fastEffects())

    /**
     * Material fade through between top-level tabs (`MaterialFadeThrough`): 300 ms, the incoming
     * tab fading in and scaling up from 92 % over 210 ms.
     *
     * The documented defaults fade the outgoing tab out first and only then the incoming one in, so
     * around the 90 ms mark neither tab is drawn and the window behind them flashes through. The
     * incoming tab is drawn on top, so it fades in from the first frame and the outgoing one stays
     * opaque underneath until it is covered — same overlap as [SharedAxisX].
     */
    val fadeThroughEnter: EnterTransition
        get() =
            fadeIn(animationSpec = fadeThroughInSpec()) +
                scaleIn(initialScale = 0.92f, animationSpec = fadeThroughInSpec())

    /** The outgoing half of the fade through. */
    val fadeThroughExit: ExitTransition
        get() =
            fadeOut(
                animationSpec = tween(FADE_THROUGH_OUT_MS, delayMillis = FADE_THROUGH_IN_MS, easing = FastOutSlowInEasing),
            )

    /**
     * A tab switch that pops the back stack (back from a tab to the start tab, or the start tab's
     * bottom-bar tap): the start tab is opaque underneath from the first frame, and the leaving tab
     * scales down to the fade through's 92 % and fades out over the second half. Not the fade
     * through itself: its two halves run one after the other, so when predictive back seeks it the
     * middle of the gesture shows neither tab — only the window behind them (internal change).
     */
    val fadeThroughPopEnter: EnterTransition
        get() = EnterTransition.None

    /** The leaving half of [fadeThroughPopEnter]. */
    val fadeThroughPopExit: ExitTransition
        get() =
            fadeOut(animationSpec = tween(FADE_THROUGH_POP_MS / 2, delayMillis = FADE_THROUGH_POP_MS / 2)) +
                scaleOut(targetScale = 0.92f, animationSpec = tween(FADE_THROUGH_POP_MS, easing = FastOutSlowInEasing))

    private fun <T> fadeThroughInSpec(): FiniteAnimationSpec<T> = tween(FADE_THROUGH_IN_MS, easing = FastOutSlowInEasing)

    private const val FADE_THROUGH_OUT_MS = 90
    private const val FADE_THROUGH_IN_MS = 210
    private const val FADE_THROUGH_POP_MS = 300
}

/**
 * Material shared axis X, the standard motion for a hierarchical push and pop: 300 ms on the
 * emphasized easing, the screens sliding 30 dp along the axis (`MaterialSharedAxis`, X axis).
 *
 * The fades overlap instead of running one after the other. The documented defaults fade the
 * outgoing screen out over the first 35 % and only then the incoming one in, so around that mark
 * neither screen is drawn and the window behind them shows for a frame or two — the white flash
 * reported on a light-mode push. On a push the incoming screen is drawn on top, so it fades in from the first frame over the
 * outgoing one, which stays opaque until it is covered and fades out under it afterwards.
 *
 * A pop is the predictive-back motion instead (internal change). Navigation seeks the pop transition while the
 * back gesture is in progress, and the shared axis's sequential fades left the middle of the gesture
 * with neither screen drawn — the window background alone, a flash of the theme colour. So a pop
 * keeps the previous screen opaque underneath from the first frame and shrinks the closing one on
 * top of it, the in-app predictive-back animation of the Android guidelines
 * (`scaleOut(targetScale = 0.9f)` over `EnterTransition.None`), fading it out only over the last
 * third so it stays solid while the finger is down. Navigation Compose has a single pop transition
 * per NavHost, so the back key and a quick swipe run the same motion, unseeked.
 *
 * Every value is a plain [EnterTransition] / [ExitTransition], so Navigation's predictive back can
 * seek the pop. Set on the sample's own NavHosts only: SDK screens hosted there inherit it (internal change),
 * the SDK itself never reads it.
 */
@Immutable
internal class SharedAxisX(
    private val slidePx: Int,
) {
    /** The pushed screen comes in from the end side, fading in on top from the first frame. */
    val enter: EnterTransition
        get() = slideInHorizontally(axisSpec()) { slidePx } + fadeIn(fadeInSpec())

    /** The covered screen leaves towards the start side, opaque until the pushed one covers it. */
    val exit: ExitTransition
        get() = slideOutHorizontally(axisSpec()) { -slidePx } + fadeOut(fadeOutSpec())

    /** The revealed screen is already in place, opaque, under the closing one. */
    val popEnter: EnterTransition
        get() = EnterTransition.None

    /** The popped screen shrinks to 90 % on top of the revealed one, then fades out. */
    val popExit: ExitTransition
        get() =
            scaleOut(targetScale = POP_TARGET_SCALE, animationSpec = tween(DURATION_MS, easing = FastOutSlowInEasing)) +
                fadeOut(tween(DURATION_MS - POP_FADE_DELAY_MS, delayMillis = POP_FADE_DELAY_MS))

    private fun <T> axisSpec(): FiniteAnimationSpec<T> = tween(DURATION_MS, easing = EmphasizedEasing)

    private fun <T> fadeInSpec(): FiniteAnimationSpec<T> = tween(FADE_IN_MS, easing = EmphasizedEasing)

    private fun <T> fadeOutSpec(): FiniteAnimationSpec<T> =
        tween(DURATION_MS - FADE_IN_MS, delayMillis = FADE_IN_MS, easing = EmphasizedEasing)

    private companion object {
        const val DURATION_MS = 300

        /**
         * The pushed screen's fade-in, from the first frame: the 65 % of [DURATION_MS] the Material
         * shared axis gives it, without the 35 % wait that left a frame with no screen drawn.
         */
        const val FADE_IN_MS = 195

        /** The closing screen's scale at the end of a pop, the guidelines' predictive-back value. */
        const val POP_TARGET_SCALE = 0.9f

        /** A pop's fade starts after two thirds of [DURATION_MS]: opaque while the gesture runs. */
        const val POP_FADE_DELAY_MS = 200

        /** The Material 3 emphasized easing curve, `cubic-bezier(0.2, 0, 0, 1)`. */
        val EmphasizedEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    }
}

/** [SharedAxisX] with its 30 dp slide resolved for the current density. */
@Composable
internal fun rememberSharedAxisX(): SharedAxisX {
    val density = LocalDensity.current
    return remember(density) { SharedAxisX(with(density) { 30.dp.roundToPx() }) }
}
