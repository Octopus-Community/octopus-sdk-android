package com.octopuscommunity.sdk.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import com.octopuscommunity.sdk.ui.OctopusDestination.Activity
import com.octopuscommunity.sdk.ui.OctopusDestination.CommentDetails
import com.octopuscommunity.sdk.ui.OctopusDestination.CommentMedias
import com.octopuscommunity.sdk.ui.OctopusDestination.CreatePost
import com.octopuscommunity.sdk.ui.OctopusDestination.CurrentUserProfileEdit
import com.octopuscommunity.sdk.ui.OctopusDestination.CurrentUserProfileSummary
import com.octopuscommunity.sdk.ui.OctopusDestination.GroupDetails
import com.octopuscommunity.sdk.ui.OctopusDestination.Groups
import com.octopuscommunity.sdk.ui.OctopusDestination.Home
import com.octopuscommunity.sdk.ui.OctopusDestination.HowToReportContent
import com.octopuscommunity.sdk.ui.OctopusDestination.PostDetails
import com.octopuscommunity.sdk.ui.OctopusDestination.PostMedias
import com.octopuscommunity.sdk.ui.OctopusDestination.ProfileAvatar
import com.octopuscommunity.sdk.ui.OctopusDestination.ProfileSummary
import com.octopuscommunity.sdk.ui.OctopusDestination.ReplyMedias
import com.octopuscommunity.sdk.ui.OctopusDestination.ValidateNickname
import kotlin.reflect.KClass

/**
 * Interface for defining transition configurations for Octopus destinations.
 *
 * Provides a way to customize navigation animations for different destination types.
 */
interface OctopusTransitions {
    /**
     * Gets the transition configuration for a specific destination type.
     *
     * @param T The destination type that extends [OctopusDestination]
     * @param destination The KClass of the destination
     * @return The transition configuration for the destination
     */
    fun <T : OctopusDestination> getTransition(destination: KClass<T>): OctopusTransition
}

/**
 * Predefined implementation of [OctopusTransitions]: the SDK's own transition set of the 1.14.x
 * releases, except that the nickname choice now comes up from the bottom like post creation.
 *
 * It is **not** applied by default: `octopusComposables` sets no transition on the SDK destinations
 * unless one is passed, so they inherit the transitions of the host `NavHost`. Pass
 * `transitions = OctopusTransitionsDefaults()` to opt back into this set.
 *
 * Provides three types of transitions:
 * - Horizontal: Slide transitions for main navigation flows
 * - Vertical: Slide up/down transitions for modal-like screens
 * - Fade: Fade in/out transitions for media and overlay screens
 *
 * @property horizontal Horizontal slide transition configuration
 * @property vertical Vertical slide transition configuration
 * @property fade Fade transition configuration
 */
data class OctopusTransitionsDefaults(
    val horizontal: OctopusTransitionDefaults = OctopusTransitionDefaults.Horizontal(),
    val vertical: OctopusTransitionDefaults = OctopusTransitionDefaults.Vertical(),
    val fade: OctopusTransitionDefaults = OctopusTransitionDefaults.Fade(),
) : OctopusTransitions {
    /**
     * Returns the configuration whose `destinations` list [destination], or [horizontal] when none
     * does, so that a custom configuration with partial lists never throws.
     */
    override fun <T : OctopusDestination> getTransition(destination: KClass<T>): OctopusTransitionDefaults =
        listOf(horizontal, vertical, fade).firstOrNull { it.destinations.contains(destination) }
            ?: horizontal
}

/**
 * Interface for defining navigation transition animations.
 *
 * Provides hooks for all four transition states in navigation animations.
 */
sealed interface OctopusTransition {
    /** Transition when entering a destination */
    val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?

    /** Transition when exiting a destination */
    val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?

    /** Transition when returning to a destination (pop enter) */
    val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?

    /** Transition when leaving a destination on back navigation (pop exit) */
    val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?
}

/**
 * Sealed interface for predefined transition configurations.
 *
 * Extends [OctopusTransition] and adds a list of destinations that use this transition style.
 */
sealed interface OctopusTransitionDefaults : OctopusTransition {
    /** List of destination classes that use this transition configuration */
    val destinations: List<KClass<out OctopusDestination>>

    /**
     * Horizontal slide transition configuration.
     *
     * Used for main navigation flows where screens slide in from left/right.
     * Includes fade animation combined with slide for smoother transitions.
     *
     * @property destinations List of destinations using horizontal transitions
     * @property enterTransition Slide in from left with fade
     * @property exitTransition Slide out to left
     * @property popEnterTransition Slide in from right with fade (back navigation)
     * @property popExitTransition Slide out to right (back navigation)
     */
    data class Horizontal(
        override val destinations: List<KClass<out OctopusDestination>> = DESTINATIONS,
        override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                tween(),
            ) + fadeIn(tween())
        },
        override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            if ((Vertical.DESTINATIONS + Fade.DESTINATIONS).none {
                    targetState.destination.hasRoute(
                        it,
                    )
                }
            ) {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween())
            } else {
                fadeOut(tween())
            }
        },
        override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            if ((Vertical.DESTINATIONS + Fade.DESTINATIONS).none {
                    initialState.destination.hasRoute(
                        it,
                    )
                }
            ) {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    tween(),
                ) + fadeIn(tween())
            } else {
                fadeIn(tween())
            }
        },
        override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween())
        },
    ) : OctopusTransitionDefaults {
        companion object {
            val DESTINATIONS: List<KClass<out OctopusDestination>> =
                listOf(
                    Home::class,
                    Groups::class,
                    GroupDetails::class,
                    PostDetails::class,
                    CommentDetails::class,
                    CurrentUserProfileSummary::class,
                    CurrentUserProfileEdit::class,
                    Activity::class,
                    ProfileSummary::class,
                    HowToReportContent::class,
                )
        }
    }

    /**
     * Vertical slide transition configuration.
     *
     * Used for modal-like screens that appear from bottom and dismiss downward.
     * Typically used for creation flows and similar modal interactions.
     *
     * @property destinations List of destinations using vertical transitions
     * @property enterTransition Slide in from bottom with fade
     * @property exitTransition Slide out to bottom
     * @property popEnterTransition Slide in from bottom with fade
     * @property popExitTransition Slide out to bottom
     */
    data class Vertical(
        override val destinations: List<KClass<out OctopusDestination>> = DESTINATIONS,
        override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Up,
                tween(),
            ) + fadeIn(tween())
        },
        override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            fadeOut(tween())
        },
        override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            fadeIn(tween())
        },
        override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Down,
                tween(),
            ) + fadeOut(tween())
        },
    ) : OctopusTransitionDefaults {
        companion object {
            val DESTINATIONS: List<KClass<out OctopusDestination>> =
                listOf(
                    CreatePost::class,
                    ValidateNickname::class,
                )
        }
    }

    /**
     * Fade transition configuration.
     *
     * Used for overlay-style screens like media viewers and profile avatars.
     * Provides smooth fade in/out animations with longer duration for dramatic effect.
     *
     * @property destinations List of destinations using fade transitions
     * @property enterTransition Fade in with 500ms duration
     * @property exitTransition Fade out with 500ms duration
     * @property popEnterTransition Fade in with 500ms duration
     * @property popExitTransition Fade out with 500ms duration
     */
    data class Fade(
        override val destinations: List<KClass<out OctopusDestination>> = DESTINATIONS,
        override val enterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            fadeIn(tween(500))
        },
        override val exitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            fadeOut(tween(500))
        },
        override val popEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
            fadeIn(tween(500))
        },
        override val popExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
            fadeOut(tween(500))
        },
    ) : OctopusTransitionDefaults {
        companion object {
            val DESTINATIONS: List<KClass<out OctopusDestination>> =
                listOf(
                    PostMedias::class,
                    CommentMedias::class,
                    ReplyMedias::class,
                    ProfileAvatar::class,
                )
        }
    }

    companion object {
        val DESTINATIONS: List<KClass<out OctopusDestination>> =
            Horizontal.DESTINATIONS + Vertical.DESTINATIONS + Fade.DESTINATIONS
    }
}

/**
 * Gets the transition configuration for a specific destination type.
 *
 * @param T The destination type that extends [OctopusDestination]
 * @param transitions The transitions configuration, or `null` to set no transition on the
 * destination, which then inherits the transitions of the host `NavHost`
 * @return The transition configuration for the destination, or `null` if [transitions] is `null`
 */
inline fun <reified T : OctopusDestination> octopusTransition(transitions: OctopusTransitions?) = transitions?.getTransition(T::class)
