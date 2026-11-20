package com.octopuscommunity.sdk.ui

import androidx.navigation.NavDeepLink
import androidx.navigation.navDeepLink
import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.ui.OctopusDestination.CommentDetails
import com.octopuscommunity.sdk.ui.OctopusDestination.CommentMedias
import com.octopuscommunity.sdk.ui.OctopusDestination.CreatePost
import com.octopuscommunity.sdk.ui.OctopusDestination.CurrentUserProfileEdit
import com.octopuscommunity.sdk.ui.OctopusDestination.CurrentUserProfileSummary
import com.octopuscommunity.sdk.ui.OctopusDestination.Home
import com.octopuscommunity.sdk.ui.OctopusDestination.HowToReportContent
import com.octopuscommunity.sdk.ui.OctopusDestination.PostDetails
import com.octopuscommunity.sdk.ui.OctopusDestination.PostMedias
import com.octopuscommunity.sdk.ui.OctopusDestination.ProfileAvatar
import com.octopuscommunity.sdk.ui.OctopusDestination.ProfileSummary
import com.octopuscommunity.sdk.ui.OctopusDestination.ReplyMedias
import com.octopuscommunity.sdk.ui.OctopusDestination.ValidateNickname

/**
 * Configuration for deep link URL patterns for Octopus destinations.
 *
 * Defines static sub-paths and parameterized sub-path patterns that can be used
 * to navigate directly to specific destinations via deep links.
 *
 * @property subPaths List of static URL sub-paths (without parameters)
 * @property subPathPatterns List of parameterized URL patterns with placeholders like {id}
 */
data class OctopusDeepLinks(
    val subPaths: List<String> = emptyList(),
    val subPathPatterns: List<String> = emptyList(),
)

/**
 * Gets the deep link configuration for a specific destination type.
 *
 * Returns the appropriate [OctopusDeepLinks] configuration based on the destination type,
 * including both static paths and parameterized patterns for that destination.
 *
 * @param T The destination type that extends [OctopusDestination]
 * @return Deep link configuration for the specified destination type
 */
inline fun <reified T : OctopusDestination> octopusDeepLinks(): OctopusDeepLinks =
    when (T::class) {
        Home::class ->
            OctopusDeepLinks(
                subPaths = listOf("", "home"),
            )

        PostDetails::class ->
            OctopusDeepLinks(
                subPaths = listOf("post"),
                subPathPatterns = listOf("post/{postId}/comment/{commentId}"),
            )

        CreatePost::class ->
            OctopusDeepLinks(
                subPaths = listOf("post/create"),
            )

        PostMedias::class ->
            OctopusDeepLinks(
                subPathPatterns =
                    listOf(
                        "post/{postId}/medias",
                        "post/{postId}/medias/{selectedIndex}",
                    ),
            )

        CommentDetails::class ->
            OctopusDeepLinks(
                subPaths = listOf("comment"),
                subPathPatterns = listOf("comment/{commentId}/reply/{replyId}"),
            )

        CommentMedias::class ->
            OctopusDeepLinks(
                subPathPatterns =
                    listOf(
                        "comment/{commentId}/medias",
                        "comment/{commentId}/medias/{selectedIndex}",
                    ),
            )

        ReplyMedias::class ->
            OctopusDeepLinks(
                subPathPatterns =
                    listOf(
                        "reply/{replyId}/medias",
                        "reply/{replyId}/medias/{selectedIndex}",
                    ),
            )

        ValidateNickname::class ->
            OctopusDeepLinks(
                subPaths = listOf("validate-nickname"),
            )

        CurrentUserProfileSummary::class ->
            OctopusDeepLinks(
                subPaths = listOf("current-profile"),
            )

        CurrentUserProfileEdit::class ->
            OctopusDeepLinks(
                subPaths = listOf("current-profile/edit"),
            )

        HowToReportContent::class ->
            OctopusDeepLinks(
                subPaths = listOf("settings/how-to-report-content"),
            )

        ProfileSummary::class ->
            OctopusDeepLinks(
                subPaths = listOf("profile"),
            )

        ProfileAvatar::class ->
            OctopusDeepLinks(
                subPathPatterns = listOf("profile/{userId}/avatar"),
            )

        else -> OctopusDeepLinks()
    }

/**
 * Creates Navigation Component deep links for a destination with a custom base path.
 *
 * Combines the provided base path with the destination's configured sub-paths and patterns
 * to generate complete [NavDeepLink] objects for navigation.
 *
 * @param T The destination type that extends [OctopusDestination]
 * @param basePath The base URL path to prepend to all deep link patterns
 * @return List of configured [NavDeepLink] objects for the destination
 */
inline fun <reified T : OctopusDestination> octopusNavDeepLinks(basePath: String): List<NavDeepLink> =
    octopusDeepLinks<T>().let { deepLinks ->
        deepLinks.subPaths.map { subPath ->
            navDeepLink<T>(basePath + subPath)
        } +
            deepLinks.subPathPatterns.map { customPath ->
                navDeepLink { uriPattern = basePath + customPath }
            }
    }

/**
 * Creates internal deep links for Octopus SDK destinations.
 *
 * Uses the SDK's internal deep link base path to generate navigation deep links
 * for internal SDK navigation purposes.
 *
 * @param T The destination type that extends [OctopusDestination]
 * @return List of internal [NavDeepLink] objects for the destination
 * @see OctopusSDK.INTERNAL_DEEP_LINK_BASE_PATH
 */
inline fun <reified T : OctopusDestination> octopusInternalNavDeepLinks(): List<NavDeepLink> =
    octopusNavDeepLinks<T>(basePath = OctopusSDK.INTERNAL_DEEP_LINK_BASE_PATH)

/**
 * Creates app-specific deep links for Octopus SDK destinations.
 *
 * Uses the configured app deep link base paths to generate navigation deep links
 * that can be triggered from external sources like web browsers or other apps.
 *
 * @param T The destination type that extends [OctopusDestination]
 * @return List of app-specific [NavDeepLink] objects for the destination
 * @see OctopusSDK.appDeepLinkBasePaths
 */
inline fun <reified T : OctopusDestination> octopusAppNavDeepLinks(): List<NavDeepLink> =
    OctopusSDK.appDeepLinkBasePaths
        .map {
            octopusNavDeepLinks<T>(
                basePath = if (it.endsWith("/")) it else "$it/",
            )
        }.flatten()

/**
 * Creates all deep links for a destination (both internal and app-specific).
 *
 * Combines internal SDK deep links and app-specific deep links to provide
 * comprehensive deep link support for the destination.
 *
 * @param T The destination type that extends [OctopusDestination]
 * @return Complete list of [NavDeepLink] objects for the destination
 */
inline fun <reified T : OctopusDestination> octopusNavDeepLinks(): List<NavDeepLink> =
    (octopusInternalNavDeepLinks<T>() + octopusAppNavDeepLinks<T>())
