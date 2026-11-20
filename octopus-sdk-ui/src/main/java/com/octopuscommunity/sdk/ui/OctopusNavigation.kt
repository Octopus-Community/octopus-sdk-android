package com.octopuscommunity.sdk.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDeepLink
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.NavType
import androidx.navigation.Navigator
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.navigation.toRoute
import com.octopuscommunity.sdk.InternalOctopusApi
import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.domain.model.ClientPost
import com.octopuscommunity.sdk.domain.model.CreatePostScreenInfo
import com.octopuscommunity.sdk.domain.model.Post
import com.octopuscommunity.sdk.domain.model.ProfileField
import com.octopuscommunity.sdk.ui.OctopusDestination.Activity
import com.octopuscommunity.sdk.ui.OctopusDestination.CommentDetails
import com.octopuscommunity.sdk.ui.OctopusDestination.CommentMedias
import com.octopuscommunity.sdk.ui.OctopusDestination.CreatePost
import com.octopuscommunity.sdk.ui.OctopusDestination.CurrentUserProfileEdit
import com.octopuscommunity.sdk.ui.OctopusDestination.CurrentUserProfileGraph
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
import com.octopuscommunity.sdk.ui.comments.details.CommentDetailsScreen
import com.octopuscommunity.sdk.ui.comments.medias.CommentMediasScreen
import com.octopuscommunity.sdk.ui.comments.replies.medias.ReplyMediasScreen
import com.octopuscommunity.sdk.ui.components.OctopusContent
import com.octopuscommunity.sdk.ui.components.OctopusNavigationHandler
import com.octopuscommunity.sdk.ui.components.UrlOpeningStrategy
import com.octopuscommunity.sdk.ui.groups.GroupsScreen
import com.octopuscommunity.sdk.ui.groups.details.GroupDetailsScreen
import com.octopuscommunity.sdk.ui.home.OctopusHomeScreen
import com.octopuscommunity.sdk.ui.posts.create.CreatePostScreen
import com.octopuscommunity.sdk.ui.posts.create.decodeCtaLabelTranslations
import com.octopuscommunity.sdk.ui.posts.create.encodeCtaLabelTranslations
import com.octopuscommunity.sdk.ui.posts.create.labelLanguage
import com.octopuscommunity.sdk.ui.posts.create.labelTranslations
import com.octopuscommunity.sdk.ui.posts.details.PostDetailsScreen
import com.octopuscommunity.sdk.ui.posts.medias.PostMediasScreen
import com.octopuscommunity.sdk.ui.profile.activity.ActivityScreen
import com.octopuscommunity.sdk.ui.profile.avatar.ProfileAvatarScreen
import com.octopuscommunity.sdk.ui.profile.current.edit.CurrentUserProfileEditScreen
import com.octopuscommunity.sdk.ui.profile.current.nickname.ValidateNicknameScreen
import com.octopuscommunity.sdk.ui.profile.current.summary.CurrentUserProfileSummaryScreen
import com.octopuscommunity.sdk.ui.profile.others.summary.ProfileSummaryScreen
import com.octopuscommunity.sdk.ui.settings.report.HowToReportContentScreen
import kotlin.reflect.KType

/**
 * Add the Octopus SDK Composables to your NavGraphBuilder with SSO handling.
 *
 * This version automatically handles Single Sign-On (SSO) authentication and profile editing,
 * providing a complete navigation setup for all Octopus SDK screens with integrated
 * authentication flow management. This is the recommended approach for most applications
 * as it simplifies integration and provides automatic handling of common authentication scenarios.
 *
 * ## Features:
 * - **Complete Navigation Setup**: Registers all Octopus SDK screens and their routes
 * - **SSO Integration**: Automatic handling of user authentication flows
 * - **Profile Management**: Built-in profile editing and management
 * - **Client Object Integration**: Support for linking external app content
 * - **Deep Link Support**: Built-in deep linking for all screens
 * - **Custom Theming**: Container support for custom themes and layouts
 * - **Transition Animations**: Inherits the host `NavHost` transitions, or configurable ones
 *
 * ## Usage Examples:
 * ```kotlin
 * // Basic integration with SSO
 * NavHost(
 *     navController = navController,
 *     startDestination = MainScreen
 * ) {
 *     // Your app screens
 *     composable<MainScreen> { MainScreen() }
 *
 *     // Octopus SDK screens with SSO
 *     octopusComposables(
 *         navController = navController,
 *         onNavigateToLogin = {
 *             navController.navigate(LoginScreen)
 *         },
 *         onNavigateToProfileEdit = { field ->
 *             navController.navigate(EditProfileScreen(field))
 *         },
 *         onNavigateToClientObject = { objectId ->
 *             navController.navigate(ArticleDetailScreen(objectId))
 *         }
 *     )
 * }
 *
 * // With custom theming and transitions: by default the SDK screens inherit the transitions of
 * // this NavHost; pass `OctopusTransitionsDefaults()` to use the 1.14.x SDK slide instead
 * octopusComposables(
 *     navController = navController,
 *     transitions = OctopusTransitionsDefaults(),
 *     onNavigateToLogin = { navController.navigate(LoginScreen) },
 *     onNavigateToProfileEdit = { field ->
 *         when (field) {
 *             ProfileField.NICKNAME -> navController.navigate(EditNicknameScreen)
 *             ProfileField.BIO -> navController.navigate(EditBioScreen)
 *             else -> navController.navigate(EditProfileScreen(field))
 *         }
 *     },
 *     container = { backStackEntry, content ->
 *         OctopusTheme {
 *             content()
 *         }
 *     }
 * )
 * ```
 *
 * ## Authentication Flow:
 * When users need to authenticate:
 * 1. `onNavigateToLogin` is called
 * 2. Your app shows login UI
 * 3. After successful login, call `OctopusSDK.connectUser()`
 * 4. Users can now access all community features
 *
 * ## Embedding under your own `Scaffold`:
 * The registered screens (post details, create-post, comment details, …) manage their own window
 * insets — they re-apply `imePadding()` so the composer sits right above the keyboard. If you host
 * this `NavHost` inside your own `Scaffold` (e.g. under a bottom navigation bar) and pass the
 * Scaffold's `innerPadding` down, you MUST also consume those insets so they are not counted twice:
 * ```kotlin
 * Scaffold(bottomBar = { /* … */ }) { innerPadding ->
 *     NavHost(
 *         navController = navController,
 *         startDestination = /* … */,
 *         modifier = Modifier
 *             .padding(innerPadding)
 *             .consumeWindowInsets(innerPadding), // <-- required for nested Scaffolds
 *     ) {
 *         octopusComposables(navController = navController, /* … */)
 *     }
 * }
 * ```
 * Without `consumeWindowInsets`, the IME/system-bar insets are applied twice, leaving a gap below
 * the composer and floating it well above the keyboard.
 *
 * ## Profile Editing Flow:
 * When users want to edit their profile:
 * 1. `onNavigateToProfileEdit` is called with the specific field
 * 2. Your app shows the appropriate editing UI
 * 3. After changes are saved, update the user via `OctopusSDK.connectUser()`
 *
 * @param navController The navigation controller for handling all Octopus SDK navigation
 * @param transitions Transition configurations for navigation animations between screens. The
 * default, `null`, sets no transition on the SDK destinations: they inherit the
 * `enterTransition` / `exitTransition` / `popEnterTransition` / `popExitTransition` of the host
 * `NavHost`, and a predictive-back gesture seeks those through Navigation Compose, like any other
 * screen of the app. Pass [OctopusTransitionsDefaults] (`OctopusTransitionsDefaults()`) for the
 * 1.14.x SDK slide, or any [OctopusTransitions] for a per-destination configuration.
 * Missing optional login or host profile-edit callbacks never prevent opening a screen. A requested
 * action without its callback logs the missing parameter once, then throws the documented
 * [com.octopuscommunity.sdk.MissingOnNavigateToLoginException] or
 * [com.octopuscommunity.sdk.MissingOnNavigateToProfileEditException] at action time.
 *
 * @param onNavigateToLogin Called when OctopusSDK needs a logged in user. This should launch the
 * navigation to your login composable/screen. Once your login process is done, you should call
 * [OctopusSDK.connectUser] with your retrieved user's information.
 * @param onNavigateToProfileEdit Called when the user tries to modify fields related to their
 * profile. When this block is called, you should open the profile edition screen for the specific
 * field. The parameter indicates which field should be focused, or null for general editing.
 * @param onNavigateToClientObject Called when a user taps on the Button with the label
 * [ClientPost.viewObjectButtonText] that is displayed on a post related to a client object
 * (article, product, etc.). The parameter is the object id you set when creating the post.
 * This allows deep linking back to your app's content.
 * @param onNavigateToUrl Called when a user tries to open a link inside the community.
 * This link can come from a Post, Comment, Reply content, or when tapping on a Post's CTA button.
 * Return [UrlOpeningStrategy.HandledByApp] if you handle the URL yourself, or
 * [UrlOpeningStrategy.HandledByOctopus] to let the SDK open it in Chrome Custom Tabs
 * with fallback to default browser.
 * By default, will be opened by the SDK in Chrome Custom Tabs or the default browser.
 * @param onNavigateToProfile Called when a user taps a member's profile (including their own),
 * with that member's `clientUserId`, so the host can open its own profile screen. Unified Profile
 * requires BOTH the community's backend flag (`CommunityConfig.exposeClientUserId`) and this
 * callback. When active, members without a client id (guest / BO / admin) open the Octopus activity
 * screen; this callback never receives a `null` id. With the flag off or this callback absent,
 * the SDK keeps native profiles.
 * @param onBack Called when the SDK's top-bar back chevron is tapped on a screen that is the
 * START/root destination of the SDK's internal back stack — i.e. bridge mode, where the host opened
 * the SDK directly on a sub-screen (a post, group, create-post, member activity, or the
 * current-user profile) so there is nothing left to pop internally. Pass a callback that dismisses your own container (e.g. pop your
 * route / close the activity). The default (`{ navController.navigateUp() }`) acts on the SDK's own
 * controller and is therefore a no-op at that root, so bridge hosts must supply a real callback. On
 * non-root screens the SDK pops its own internal back stack and this is not called.
 * @param container Composable container that wraps each screen content, providing theming, layout,
 * and any additional UI wrapper components. Receives the current NavBackStackEntry for context.
 */
fun NavGraphBuilder.octopusComposables(
    navController: NavHostController,
    transitions: OctopusTransitions? = null,
    onNavigateToLogin: (() -> Unit)? = null,
    onNavigateToProfileEdit: ((fieldToEdit: ProfileField?) -> Unit)? = null,
    onNavigateToClientObject: ((id: String) -> Unit)? = null,
    onGroupAccessDenied: ((groupId: String) -> Unit)? = null,
    onNavigateToUrl: ((url: String) -> UrlOpeningStrategy) = { UrlOpeningStrategy.HandledByOctopus },
    onNavigateToProfile: ((clientUserId: String) -> Unit)? = null,
    onBack: () -> Unit = { navController.navigateUp() },
    container: @Composable AnimatedVisibilityScope.(
        backStackEntry: NavBackStackEntry,
        content: @Composable () -> Unit,
    ) -> Unit = { backStackEntry, content ->
        OctopusTheme(content = content)
    },
) = octopusComposables(
    navController = navController,
    homeTitleText = null,
    transitions = transitions,
    onNavigateToLogin = onNavigateToLogin,
    onNavigateToProfileEdit = onNavigateToProfileEdit,
    onNavigateToClientObject = onNavigateToClientObject,
    onGroupAccessDenied = onGroupAccessDenied,
    onNavigateToUrl = onNavigateToUrl,
    onNavigateToProfile = onNavigateToProfile,
    onBack = onBack,
    container = container,
)

/**
 * Adds the Octopus navigation graph with a main feed title override.
 *
 * Accepts the same navigation callbacks and container as the default [octopusComposables] overload.
 * The existing overload supplies a null title, preserving existing calls and trailing lambdas;
 * [homeTitleText] sits just before [container], so positional calls keep resolving to it.
 *
 * @param navController The navigation controller for handling all Octopus SDK navigation
 * @param transitions Transition configurations for navigation animations between screens. The
 * default, `null`, sets no transition on the SDK destinations: they inherit the
 * `enterTransition` / `exitTransition` / `popEnterTransition` / `popExitTransition` of the host
 * `NavHost`, and a predictive-back gesture seeks those through Navigation Compose, like any other
 * screen of the app. Pass [OctopusTransitionsDefaults] (`OctopusTransitionsDefaults()`) for the
 * 1.14.x SDK slide, or any [OctopusTransitions] for a per-destination configuration.
 * @param onNavigateToLogin Called when OctopusSDK needs a logged in user. This should launch the
 * navigation to your login composable/screen. Once your login process is done, you should call
 * [OctopusSDK.connectUser] with your retrieved user's information.
 * @param onNavigateToProfileEdit Called when the user tries to modify fields related to their
 * profile. When this block is called, you should open the profile edition screen for the specific
 * field. The parameter indicates which field should be focused, or null for general editing.
 * @param onNavigateToClientObject Called when a user taps on the Button with the label
 * [ClientPost.viewObjectButtonText] that is displayed on a post related to a client object
 * (article, product, etc.). The parameter is the object id you set when creating the post.
 * This allows deep linking back to your app's content.
 * @param onNavigateToUrl Called when a user tries to open a link inside the community.
 * This link can come from a Post, Comment, Reply content, or when tapping on a Post's CTA button.
 * Return [UrlOpeningStrategy.HandledByApp] if you handle the URL yourself, or
 * [UrlOpeningStrategy.HandledByOctopus] to let the SDK open it in Chrome Custom Tabs
 * with fallback to default browser.
 * By default, will be opened by the SDK in Chrome Custom Tabs or the default browser.
 * @param onNavigateToProfile Called when a user taps a member's profile (including their own),
 * with that member's `clientUserId`, so the host can open its own profile screen. Unified Profile
 * requires BOTH the community's backend flag (`CommunityConfig.exposeClientUserId`) and this
 * callback. When active, members without a client id (guest / BO / admin) open the Octopus activity
 * screen; this callback never receives a `null` id. With the flag off or this callback absent,
 * the SDK keeps native profiles.
 * @param onBack Called when the SDK's top-bar back chevron is tapped on a screen that is the
 * START/root destination of the SDK's internal back stack — i.e. bridge mode, where the host opened
 * the SDK directly on a sub-screen (a post, group, create-post, member activity, or the
 * current-user profile) so there is nothing left to pop internally. Pass a callback that dismisses your own container (e.g. pop your
 * route / close the activity). The default (`{ navController.navigateUp() }`) acts on the SDK's own
 * controller and is therefore a no-op at that root, so bridge hosts must supply a real callback. On
 * non-root screens the SDK pops its own internal back stack and this is not called.
 * @param homeTitleText Main feed text title, taking precedence over the theme's logo. Null keeps
 * the theme logo or the localized default title. This is the text-only counterpart of iOS's
 * `OctopusHomeScreen(mainFeedNavBarTitle:)` with `OctopusMainFeedTitle.Content.text`; it affects
 * no other destination.
 * @param container Composable container that wraps each screen content, providing theming, layout,
 * and any additional UI wrapper components. Receives the current NavBackStackEntry for context.
 */
fun NavGraphBuilder.octopusComposables(
    navController: NavHostController,
    transitions: OctopusTransitions? = null,
    onNavigateToLogin: (() -> Unit)? = null,
    onNavigateToProfileEdit: ((fieldToEdit: ProfileField?) -> Unit)? = null,
    onNavigateToClientObject: ((id: String) -> Unit)? = null,
    onGroupAccessDenied: ((groupId: String) -> Unit)? = null,
    onNavigateToUrl: ((url: String) -> UrlOpeningStrategy) = { UrlOpeningStrategy.HandledByOctopus },
    onNavigateToProfile: ((clientUserId: String) -> Unit)? = null,
    onBack: () -> Unit = { navController.navigateUp() },
    homeTitleText: String? = null,
    container: @Composable AnimatedVisibilityScope.(
        backStackEntry: NavBackStackEntry,
        content: @Composable () -> Unit,
    ) -> Unit = { backStackEntry, content ->
        OctopusTheme(content = content)
    },
) = octopusComposables(
    navController = navController,
    homeTitleText = homeTitleText,
    transitions = transitions,
    onBack = onBack,
    container = { backStackEntry, content ->
        // The handler sits *inside* the container, not around it: it resolves the palette it hands to
        // Chrome Custom Tabs from OctopusTheme, and the container is what installs OctopusTheme —
        // the default one literally is it. Wrapping the container instead left the handler above
        // every theme, so a host passing an explicit OctopusColorScheme there (an in-app theme
        // toggle) got its links opened with the Material-fallback palette instead of the one its
        // screens are painted with.
        container(backStackEntry) {
            OctopusNavigationHandler(
                onNavigateToLogin = onNavigateToLogin,
                onNavigateToProfileEdit = onNavigateToProfileEdit,
                onNavigateToClientObject = onNavigateToClientObject,
                // If no per-screen callback is provided, fall back to the SDK-level callback
                // set via `OctopusSDK.setGroupAccessDeniedCallback(...)` — mirrors iOS where
                // `OctopusSDK.groupAccessDeniedCallback` is the single source of truth.
                onGroupAccessDenied =
                    onGroupAccessDenied
                        ?: OctopusSDK.groupAccessDeniedCallback,
                onNavigateToUrl = onNavigateToUrl,
                onNavigateToProfile = onNavigateToProfile,
                // Expose the host back callback to every SDK screen (via
                // LocalNavigationHandler) so a sub-screen that is the START/root of the
                // internal back stack (bridge mode) can fall back to it when there is
                // nothing to pop internally — see OctopusScreenContent's NavigateUp handler.
                onBack = onBack,
            ) {
                content()
            }
        }
    },
)

/**
 * Add the Octopus SDK Composables to your NavGraphBuilder with manual handling.
 *
 * This is the base version that requires manual handling of authentication and profile editing.
 * Use this when you need full control over the authentication flow, want to implement custom
 * authentication logic, or need to integrate with existing authentication systems that don't
 * fit the standard SSO pattern.
 *
 * ## When to Use This Version:
 * - **Custom Authentication**: When you have complex authentication requirements
 * - **Existing Auth Systems**: Integration with established authentication infrastructure
 * - **Advanced Control**: When you need granular control over navigation behavior
 * - **Custom Error Handling**: When you want to handle authentication errors differently
 * - **Testing**: For unit testing scenarios where you want to mock authentication
 *
 * ## Features:
 * - **Complete Screen Registration**: All Octopus SDK screens are registered
 * - **Custom Container Support**: Full control over theming and layout
 * - **Transition Configuration**: Configurable navigation animations
 * - **Deep Link Support**: Built-in deep linking capabilities
 * - **Manual Auth Handling**: You control when and how authentication occurs
 *
 * ## Usage Examples:
 * ```kotlin
 * // Basic manual integration
 * NavHost(
 *     navController = navController,
 *     startDestination = MainScreen
 * ) {
 *     octopusComposables(
 *         navController = navController,
 *         container = { backStackEntry, content ->
 *             OctopusTheme {
 *                 content()
 *             }
 *         }
 *     )
 * }
 *
 * // With custom authentication handling
 * octopusComposables(
 *     navController = navController,
 *     container = { backStackEntry, content ->
 *         OctopusTheme {
 *             content()
 *         }
 *     }
 * )
 *
 * // With advanced theming and error handling
 * octopusComposables(
 *     navController = navController,
 *     transitions = customTransitions,
 *     container = { backStackEntry, content ->
 *         OctopusTheme() {
 *             content()
 *         }
 *     }
 * )
 * ```
 *
 * @param navController The navigation controller for handling all Octopus SDK navigation
 * @param transitions Transition configurations for navigation animations between screens, or
 * `null` (default) to inherit the transitions of the host `NavHost`
 * @param container Composable container that wraps each screen content, providing theming, layout,
 * and any additional UI components. This is where you implement your custom authentication,
 * theming, analytics, or other cross-cutting concerns.
 */
internal fun NavGraphBuilder.octopusComposables(
    navController: NavHostController,
    transitions: OctopusTransitions? = null,
    onBack: () -> Unit = { navController.navigateUp() },
    homeTitleText: String? = null,
    container: @Composable AnimatedVisibilityScope.(
        backStackEntry: NavBackStackEntry,
        content: @Composable () -> Unit,
    ) -> Unit = { backStackEntry, content ->
        OctopusTheme(content = content)
    },
) {
    installOctopusPendingDeepLinks(navController)
    // Home - Main community feed and discovery
    octopusComposable<Home>(
        container = container,
        transitions = transitions,
    ) {
        OctopusHomeScreen(
            navController = navController,
            titleText = homeTitleText,
            onBack = onBack,
        )
    }

    // Groups - Topic follow/unfollow management
    octopusComposable<Groups>(
        container = container,
        transitions = transitions,
    ) {
        GroupsScreen(navController = navController)
    }

    // Topic Feed - Posts filtered by topic
    octopusComposable<GroupDetails>(
        container = container,
        transitions = transitions,
    ) { backStackEntry ->
        val destination = backStackEntry.toRoute<GroupDetails>()
        GroupDetailsScreen(
            navController = navController,
            groupId = destination.groupId,
            origin = destination.origin,
        )
    }

    // Posts - Content creation and viewing
    octopusComposable<PostDetails>(
        container = container,
        transitions = transitions,
    ) { backStackEntry ->
        val destination = backStackEntry.toRoute<PostDetails>()
        PostDetailsScreen(
            navController = navController,
            postId = destination.postId,
            commentId = destination.commentId,
            scrollToLatestComment = destination.scrollToLatestComment,
            focusCommentInputField = destination.focusCommentInputField,
            origin = destination.origin,
            hasFeaturedComment = destination.hasFeaturedComment,
        )
    }

    octopusComposable<CreatePost>(
        container = container,
        transitions = transitions,
    ) { backStackEntry ->
        val destination = backStackEntry.toRoute<CreatePost>()
        CreatePostScreen(
            navController = navController,
            type = destination.type,
            groupId = destination.groupId,
            prefilledText = destination.prefilledText,
            prefilledImageUris = destination.resolvedPrefilledImageUris,
            prefilledCustomActionLabel = destination.prefilledCustomActionLabel,
            prefilledCustomActionUrl = destination.prefilledCustomActionUrl,
            prefilledCustomActionLabelLanguage = destination.prefilledCustomActionLabelLanguage,
            prefilledCustomActionLabelTranslations =
                decodeCtaLabelTranslations(destination.prefilledCustomActionLabelTranslations),
        )
    }

    octopusComposable<PostMedias>(
        container = container,
        transitions = transitions,
    ) { backStackEntry ->
        val destination = backStackEntry.toRoute<PostMedias>()
        PostMediasScreen(
            navController = navController,
            postId = destination.postId,
            selectedIndex = destination.selectedIndex,
        )
    }

    // Comments - Discussion threads and interactions
    octopusComposable<CommentDetails>(
        container = container,
        transitions = transitions,
    ) { backStackEntry ->
        val destination = backStackEntry.toRoute<CommentDetails>()
        CommentDetailsScreen(
            navController = navController,
            commentId = destination.commentId,
            replyId = destination.replyId,
            showViewPostButton = destination.showViewPostButton,
            scrollToLatestReply = destination.scrollToLatestReply,
            focusReplyInputField = destination.focusReplyInputField,
        )
    }

    octopusComposable<CommentMedias>(
        container = container,
        transitions = transitions,
    ) { backStackEntry ->
        val destination = backStackEntry.toRoute<CommentMedias>()
        CommentMediasScreen(
            navController = navController,
            commentId = destination.commentId,
            selectedIndex = destination.selectedIndex,
        )
    }

    // Replies - Nested discussion responses
    octopusComposable<ReplyMedias>(
        container = container,
        transitions = transitions,
    ) { backStackEntry ->
        val destination = backStackEntry.toRoute<ReplyMedias>()
        ReplyMediasScreen(
            navController = navController,
            replyId = destination.replyId,
            selectedIndex = destination.selectedIndex,
        )
    }

    // Current User Profile - User's own profile management
    navigation<CurrentUserProfileGraph>(
        startDestination = CurrentUserProfileSummary.Posts,
    ) {
        octopusComposable<CurrentUserProfileSummary>(
            container = container,
            transitions = transitions,
        ) { backStackEntry ->
            val destination = backStackEntry.toRoute<CurrentUserProfileSummary>()
            CurrentUserProfileSummaryScreen(
                navController = navController,
                selectedTabIndex = destination.selectedTabIndex,
            )
        }

        octopusComposable<CurrentUserProfileEdit>(
            container = container,
            transitions = transitions,
        ) { backStackEntry ->
            val destination = backStackEntry.toRoute<CurrentUserProfileEdit>()
            CurrentUserProfileEditScreen(
                navController = navController,
                focusedField = destination.focusedField,
            )
        }

        octopusComposable<HowToReportContent>(
            container = container,
            transitions = transitions,
        ) {
            HowToReportContentScreen(navController)
        }
    }

    octopusComposable<ValidateNickname>(
        container = container,
        transitions = transitions,
    ) {
        ValidateNicknameScreen(navController)
    }

    // Profile - Other user profiles and interactions
    octopusComposable<ProfileAvatar>(
        container = container,
        transitions = transitions,
    ) { backStackEntry ->
        val destination = backStackEntry.toRoute<ProfileAvatar>()
        ProfileAvatarScreen(
            navController = navController,
            userId = destination.userId,
        )
    }

    octopusComposable<ProfileSummary>(
        container = container,
        transitions = transitions,
    ) { backStackEntry ->
        val destination = backStackEntry.toRoute<ProfileSummary>()
        ProfileSummaryScreen(
            navController = navController,
            userId = destination.userId,
            clientUserId = destination.clientUserId,
        )
    }

    // Activity - Unified Profile community activity entry point (internal ticket)
    octopusComposable<Activity>(
        container = container,
        transitions = transitions,
    ) { backStackEntry ->
        val destination = backStackEntry.toRoute<Activity>()
        ActivityScreen(
            navController = navController,
            userId = destination.userId,
            clientUserId = destination.clientUserId,
            selectedTabIndex = destination.selectedTabIndex,
        )
    }
}

/**
 * Creates composable destinations within the Octopus navigation setup with default configuration.
 *
 * This internal function provides a standardized way to create Octopus SDK screen destinations
 * with consistent theming, transitions, and deep link support. It automatically applies
 * the Octopus deep links and the transitions passed in, if any.
 *
 * ## Features:
 * - **Automatic Deep Links**: Applies Octopus-specific deep link patterns
 * - **Host Transitions**: Sets no transition unless one is passed, inheriting the host `NavHost`
 * - **Type Safety**: Leverages Kotlin's type system for route safety
 * - **Container Support**: Integrates with the container theming system
 *
 * ## Internal Usage:
 * This function is used internally by `octopusComposables` to create each screen
 * destination with consistent configuration and behavior.
 *
 * @param T The destination type that extends [OctopusDestination]
 * @param container Composable container that wraps the screen content for theming and layout
 * @param typeMap Map of types to NavType for custom parameter serialization
 * @param transitions Transition configurations for navigation animations, or `null` (default) to
 * inherit the transitions of the host `NavHost`
 * @param content The composable content for this destination screen
 */
internal inline fun <reified T : OctopusDestination> NavGraphBuilder.octopusComposable(
    noinline container: @Composable AnimatedVisibilityScope.(
        backStackEntry: NavBackStackEntry,
        content: @Composable () -> Unit,
    ) -> Unit,
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    transitions: OctopusTransitions? = null,
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) = octopusComposable<T>(
    container = container,
    typeMap = typeMap,
    deepLinks = octopusNavDeepLinks<T>(),
    transition = octopusTransition<T>(transitions),
    content = content,
)

/**
 * Creates a composable destination within the Octopus navigation setup with full configuration.
 *
 * This internal function provides complete control over the navigation setup for Octopus SDK screens,
 * allowing customization of deep links, transitions, and other navigation behavior. It's the
 * most flexible version for advanced navigation customization scenarios.
 *
 * ## Features:
 * - **Custom Deep Links**: Specify exact deep link patterns for the destination
 * - **Custom Transitions**: Override default animations with custom transitions
 * - **Type Mapping**: Handle complex parameter serialization scenarios
 * - **Full Control**: Complete customization of navigation behavior
 *
 * ## Internal Usage:
 * This function is used when specific destinations need custom configuration
 * that differs from the standard Octopus defaults.
 *
 * @param T The destination type that extends [OctopusDestination]
 * @param container Composable container that wraps the screen content for theming and layout
 * @param typeMap Map of types to NavType for custom parameter serialization
 * @param deepLinks List of specific deep links for this destination
 * @param transition Specific transition configuration overriding defaults
 * @param content The composable content for this destination screen
 */
internal inline fun <reified T : OctopusDestination> NavGraphBuilder.octopusComposable(
    noinline container: @Composable AnimatedVisibilityScope.(
        backStackEntry: NavBackStackEntry,
        content: @Composable () -> Unit,
    ) -> Unit,
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    deepLinks: List<NavDeepLink> = emptyList(),
    transition: OctopusTransition? = null,
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
) = composable<T>(
    typeMap = typeMap,
    deepLinks = deepLinks,
    enterTransition = transition?.enterTransition,
    exitTransition = transition?.exitTransition,
    popEnterTransition = transition?.popEnterTransition,
    popExitTransition = transition?.popExitTransition,
) { backStackEntry ->
    OctopusContent {
        container(backStackEntry) {
            content(backStackEntry)
        }
    }
}

/**
 * Navigates to the Octopus Home route from the current NavGraph.
 *
 * This function provides a convenient way to navigate to the main Octopus community feed
 * from anywhere in your application. It's commonly used for "back to community" actions
 * or when linking to the community from external content.
 *
 * ## Usage Examples:
 * ```kotlin
 * // Simple navigation to Octopus home
 * navController.navigateToOctopusHome()
 *
 * // Navigation with single top behavior (avoid duplicates)
 * navController.navigateToOctopusHome(
 *     navOptions = NavOptions.Builder()
 *         .setLaunchSingleTop(true)
 *         .build()
 * )
 *
 * // Navigation with custom back stack management
 * navController.navigateToOctopusHome(
 *     navOptions = NavOptions.Builder()
 *         .setPopUpTo(MainScreen::class, inclusive = false)
 *         .setLaunchSingleTop(true)
 *         .build()
 * )
 *
 * // In a click handler
 * Button(
 *     onClick = { navController.navigateToOctopusHome() }
 * ) {
 *     Text("View Community")
 * }
 * ```
 *
 * ## Navigation Behavior:
 * - **Default**: Adds Home to the back stack
 * - **With SingleTop**: Reuses existing Home if it's the current destination
 * - **With PopUpTo**: Can clear intermediate screens from back stack
 * - **With RestoreState**: Can restore previous Home screen state
 *
 * ## Error Handling:
 * If the Octopus composables are not defined in the current NavGraph,
 * this function will throw an IllegalArgumentException. Ensure you've
 * called `octopusComposables()` in your NavHost setup.
 *
 * @param navOptions Special options for this navigation operation, such as launch mode,
 * back stack management, and state restoration behavior
 * @param navigatorExtras Extras to pass to the Navigator for platform-specific behavior
 * @throws IllegalArgumentException If the Octopus Home route is not registered in the current NavGraph
 * @see octopusComposables for setting up the navigation graph
 * @see NavOptions for available navigation options
 */
fun NavController.navigateToOctopusHome(
    navOptions: NavOptions? = null,
    navigatorExtras: Navigator.Extras? = null,
) = navigate(
    route = Home,
    navOptions = navOptions,
    navigatorExtras = navigatorExtras,
)

fun NavController.navigateToOctopusPost(
    postId: String,
    navOptions: NavOptions? = null,
    navigatorExtras: Navigator.Extras? = null,
) = navigate(
    route =
        PostDetails(
            postId = postId,
            origin = OctopusDestination.Origin.CLIENT_APP,
        ),
    navOptions = navOptions,
    navigatorExtras = navigatorExtras,
)

fun NavController.navigateToOctopusGroup(
    groupId: String,
    navOptions: NavOptions? = null,
    navigatorExtras: Navigator.Extras? = null,
) = navigate(
    route =
        GroupDetails(
            groupId = groupId,
            origin = OctopusDestination.Origin.CLIENT_APP,
        ),
    navOptions = navOptions,
    navigatorExtras = navigatorExtras,
)

/**
 * Navigates to the Octopus Activity screen for a specific user, showing their community activity.
 *
 * Use this to open a member's community activity from your own UI — for example, from a Unified
 * Profile screen your app owns. The screen lists [userId]'s posts under a "{author}'s Posts" title,
 * with no profile header — or their posts *and* comments, as two tabs under a "{author}'s Activity"
 * title, when the community exposes other members' comments
 * ([com.octopuscommunity.sdk.domain.model.CommunityConfig.showCommentsOnOtherProfiles]). Passing the
 * connected user's own id opens their own tabbed Activity view (notifications, comments, posts)
 * instead.
 *
 * This entry point is always available; it does not depend on whether Unified Profile is active,
 * which only governs the in-community floating-button switch.
 *
 * ## Usage Examples:
 * ```kotlin
 * // Open another member's community activity from your own profile screen
 * Button(onClick = { navController.navigateToOctopusActivity(userId) }) {
 *     Text("See their activity")
 * }
 * ```
 *
 * Requires `octopusComposables()` to be registered on the current `NavGraph`.
 *
 * @param userId Id of the user whose activity to display.
 * @param navOptions Special options for this navigation operation, such as launch mode, back stack
 * management, and state restoration behavior.
 * @param navigatorExtras Extras to pass to the Navigator for platform-specific behavior.
 * @throws IllegalArgumentException If the Octopus Activity route is not registered in the current
 * NavGraph. Ensure you've called `octopusComposables()` in your NavHost setup.
 * @see octopusComposables for setting up the navigation graph
 * @see NavOptions for available navigation options
 */
fun NavController.navigateToOctopusActivity(
    userId: String,
    navOptions: NavOptions? = null,
    navigatorExtras: Navigator.Extras? = null,
) = navigate(
    route = Activity(userId = userId),
    navOptions = navOptions,
    navigatorExtras = navigatorExtras,
)

/**
 * Navigates to the Octopus Activity screen for the member identified by the host app's own
 * [clientUserId] (Unified Profile), showing their community activity.
 *
 * The client-user-id counterpart of [navigateToOctopusActivity]: use it from your own profile
 * screen — which only knows your app's user ids — to open a member's Octopus content without
 * handling Octopus ids. The screen resolves [clientUserId] to its Octopus id via the
 * `GetPublicProfile` client-user-id lookup, then shows exactly what [navigateToOctopusActivity]
 * shows — posts under a "{author}'s Posts" title, or posts and comments under a "{author}'s
 * Activity" title when the community exposes other members' comments. Requires the community to
 * expose client user ids ([com.octopuscommunity.sdk.domain.model.CommunityConfig.exposeClientUserId]).
 *
 * The two cannot be overloads (identical JVM signature), hence the distinct name.
 *
 * Requires `octopusComposables()` to be registered on the current `NavGraph`.
 *
 * @param clientUserId The host's own id of the member whose activity to display.
 * @param navOptions Special options for this navigation operation.
 * @param navigatorExtras Extras to pass to the Navigator for platform-specific behavior.
 * @throws IllegalArgumentException If the Octopus Activity route is not registered in the current
 * NavGraph. Ensure you've called `octopusComposables()` in your NavHost setup.
 * @see navigateToOctopusActivity
 */
fun NavController.navigateToOctopusActivityByClientUserId(
    clientUserId: String,
    navOptions: NavOptions? = null,
    navigatorExtras: Navigator.Extras? = null,
) = navigate(
    route = Activity(clientUserId = clientUserId),
    navOptions = navOptions,
    navigatorExtras = navigatorExtras,
)

/**
 * Navigates to the Octopus profile screen for a specific user, showing their public profile.
 *
 * Use this to open a member's profile from your own UI — for example, from a member list your app
 * owns. The screen shows [userId]'s public profile information and posts, read-only (no edit
 * affordances). This entry point always opens the read-only profile view, even when [userId] happens
 * to be the connected user's own id; to open the connected user's own **editable** profile, navigate
 * to [OctopusDestination.CurrentUserProfileGraph] instead (the existing self-profile path — not a
 * function this entry point provides).
 *
 * ## Usage Examples:
 * ```kotlin
 * // Open another member's profile from your own UI
 * Button(onClick = { navController.navigateToOctopusProfile(userId) }) {
 *     Text("View profile")
 * }
 * ```
 *
 * Requires `octopusComposables()` to be registered on the current `NavGraph`.
 *
 * @param userId Id of the user whose profile to display.
 * @param navOptions Special options for this navigation operation, such as launch mode, back stack
 * management, and state restoration behavior.
 * @param navigatorExtras Extras to pass to the Navigator for platform-specific behavior.
 * @throws IllegalArgumentException If the Octopus ProfileSummary route is not registered in the
 * current NavGraph. Ensure you've called `octopusComposables()` in your NavHost setup.
 * @see octopusComposables for setting up the navigation graph
 * @see NavOptions for available navigation options
 */
fun NavController.navigateToOctopusProfile(
    userId: String,
    navOptions: NavOptions? = null,
    navigatorExtras: Navigator.Extras? = null,
) = navigate(
    route = ProfileSummary(userId = userId),
    navOptions = navOptions,
    navigatorExtras = navigatorExtras,
)

/**
 * Navigates to the Octopus profile screen for the member identified by the host app's own
 * [clientUserId] (Unified Profile), showing their public profile.
 *
 * The client-user-id counterpart of [navigateToOctopusProfile]: use it from your own UI — which only
 * knows your app's user ids — to open a member's Octopus profile without handling Octopus ids. The
 * screen resolves [clientUserId] to its Octopus id via the `GetPublicProfile` client-user-id lookup,
 * then shows that member's public profile, read-only. Requires the community to expose client user
 * ids ([com.octopuscommunity.sdk.domain.model.CommunityConfig.exposeClientUserId]). A [clientUserId]
 * that does not resolve (unknown/deleted mapping, network failure, or the community not exposing
 * client ids) shows the generic error state — it never falls back to the connected user's own
 * profile.
 *
 * The two cannot be overloads (identical JVM signature), hence the distinct name.
 *
 * Requires `octopusComposables()` to be registered on the current `NavGraph`.
 *
 * @param clientUserId The host's own id of the member whose profile to display.
 * @param navOptions Special options for this navigation operation.
 * @param navigatorExtras Extras to pass to the Navigator for platform-specific behavior.
 * @throws IllegalArgumentException If the Octopus ProfileSummary route is not registered in the
 * current NavGraph. Ensure you've called `octopusComposables()` in your NavHost setup.
 * @see navigateToOctopusProfile
 */
fun NavController.navigateToOctopusProfileByClientUserId(
    clientUserId: String,
    navOptions: NavOptions? = null,
    navigatorExtras: Navigator.Extras? = null,
) = navigate(
    route = ProfileSummary(clientUserId = clientUserId),
    navOptions = navOptions,
    navigatorExtras = navigatorExtras,
)

/**
 * Navigates to the Octopus post editor (Bridge Share entry point).
 *
 * When [info] carries a non-null `prefilledPost`, the editor opens
 * prefilled with the provided text / image / group / CTA. The CTA is
 * **invisible in the editor** — the user cannot view, edit or remove
 * it before publishing.
 *
 * Requires `octopusComposables()` to be registered on the current
 * `NavGraph`. Use [com.octopuscommunity.sdk.ui.posts.create.OctopusCreatePostScreen]
 * if you prefer to host the screen under your own `NavHost`.
 *
 * @param info Optional payload prefilling the editor. Defaults to an
 * empty [com.octopuscommunity.sdk.domain.model.CreatePostScreenInfo],
 * which opens an empty editor. Its
 * [com.octopuscommunity.sdk.domain.model.CreatePostScreenInfo.bridgeShareTokenProvider],
 * when set, is wired for this editor session so a prefilled share can
 * be signed at publish time.
 * @param navOptions Special options for this navigation operation.
 * @param navigatorExtras Extras passed to the Navigator for
 * platform-specific behavior.
 * @throws IllegalArgumentException If the Octopus CreatePost route is
 * not registered in the current NavGraph.
 */
@OptIn(InternalOctopusApi::class)
fun NavController.navigateToOctopusCreatePost(
    info: CreatePostScreenInfo = CreatePostScreenInfo(),
    navOptions: NavOptions? = null,
    navigatorExtras: Navigator.Extras? = null,
) {
    // The bridge-share signer can't ride the type-safe nav route (a suspend lambda isn't a
    // Serializable nav argument), so hand it to the SDK for this editor session before navigating.
    // Set on every open — `null` for a non-share editor — so it stays scoped to this share and
    // never lingers as a global registration.
    OctopusSDK.setBridgeShareTokenProvider(info.bridgeShareTokenProvider)
    navigate(
        route =
            CreatePost(
                type = Post.Draft.Type.TEXT,
                groupId = info.prefilledPost?.topicId,
                prefilledText = info.prefilledPost?.text,
                prefilledImageUris =
                    info.prefilledPost
                        ?.images
                        ?.map { it.toString() }
                        .orEmpty(),
                prefilledCustomActionLabel = info.prefilledPost?.cta?.label,
                prefilledCustomActionUrl =
                    info.prefilledPost
                        ?.cta
                        ?.url
                        ?.toString(),
                prefilledCustomActionLabelLanguage = info.prefilledPost?.cta?.labelLanguage,
                prefilledCustomActionLabelTranslations =
                    encodeCtaLabelTranslations(
                        info.prefilledPost
                            ?.cta
                            ?.labelTranslations
                            .orEmpty(),
                    ),
            ),
        navOptions = navOptions,
        navigatorExtras = navigatorExtras,
    )
}
