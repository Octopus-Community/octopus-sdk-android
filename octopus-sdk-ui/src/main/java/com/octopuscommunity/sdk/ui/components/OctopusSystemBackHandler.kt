package com.octopuscommunity.sdk.ui.components

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * Whether an SDK screen must take Android system back away from the `NavHost`.
 *
 * Any enabled `BackHandler` registered by a screen hides the system predictive-back preview (the
 * `NavHost` can only scrub its pop transition with the finger when it handles back itself), so a
 * screen intercepts only when the `NavHost`'s own pop would be wrong:
 * - [hasPendingConfirmation]: the screen asks before discarding something (an unsaved draft or
 *   profile edit). That dialog is self-contained UI, worth showing whatever is in scope.
 * - [isBackStackRoot] and [hasHostOnBack]: nothing is left to pop on the SDK's back stack (bridge
 *   mode, the host opened the SDK directly on this screen), so back is funnelled to the host's
 *   `onBack` through the same `NavigateUp` choke point as the top-bar chevron (see
 *   `OctopusScreenContent`) instead of being a dead no-op.
 *
 * Everywhere else the `NavHost` pops, which is what `NavigateUp` (`popBackStack()`) did there.
 *
 * Never intercept at the root without a host `onBack` (internal change): a screen embedded bare, with no
 * `OctopusNavigationHandler` in scope or a null `onBack`, would otherwise swallow system back dead
 * instead of leaving it to the host. Under `octopusComposables` a non-null `onBack` is always in
 * scope.
 */
internal fun shouldInterceptSystemBack(
    hasPendingConfirmation: Boolean,
    isBackStackRoot: Boolean,
    hasHostOnBack: Boolean,
): Boolean = hasPendingConfirmation || (isBackStackRoot && hasHostOnBack)

/**
 * System-back handler of the SDK screens that must intercept back in some states, enabled only
 * when [shouldInterceptSystemBack] says so — which leaves the predictive-back preview of the
 * `NavHost` working everywhere else in the flow.
 *
 * The back stack root is `navController.previousBackStackEntry == null`, re-read whenever the
 * current entry changes. A [navController] with no graph (a content embedded outside a `NavHost`)
 * has no previous entry, so it counts as the root and keeps the internal change guarantee.
 *
 * @param navController The controller of the `NavHost` hosting the screen
 * @param hasPendingConfirmation `true` while [onBack] would show a discard confirmation first
 * @param onBack Called on system back while the handler is enabled
 */
@Composable
internal fun OctopusSystemBackHandler(
    navController: NavHostController,
    hasPendingConfirmation: Boolean = false,
    onBack: () -> Unit,
) {
    if (LocalOnBackPressedDispatcherOwner.current == null) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            com.octopuscommunity.sdk.ui.utils.IntegrationDiagnostics.reportOnce(
                "OnBackPressedDispatcherOwner",
                "System back handling is skipped: the host must provide an OnBackPressedDispatcherOwner.",
            )
        }
        return
    }
    val hasHostOnBack = LocalNavigationHandler.current?.onBack != null
    val currentEntry by navController.currentBackStackEntryAsState()
    val isBackStackRoot =
        remember(navController, currentEntry) { navController.previousBackStackEntry == null }
    BackHandler(
        enabled =
            shouldInterceptSystemBack(
                hasPendingConfirmation = hasPendingConfirmation,
                isBackStackRoot = isBackStackRoot,
                hasHostOnBack = hasHostOnBack,
            ),
        onBack = onBack,
    )
}
