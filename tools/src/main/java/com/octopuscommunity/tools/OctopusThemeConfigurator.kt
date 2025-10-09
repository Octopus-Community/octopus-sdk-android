package com.octopuscommunity.tools

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.util.Size
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.octopuscommunity.sdk.domain.model.Comment
import com.octopuscommunity.sdk.test.mock.MockComments
import com.octopuscommunity.sdk.test.mock.MockPosts
import com.octopuscommunity.sdk.ui.OctopusDrawablesDefaults
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.OctopusTypographyDefaults
import com.octopuscommunity.sdk.ui.comments.components.OctopusItemInputField
import com.octopuscommunity.sdk.ui.comments.components.OctopusItemInputFieldViewModel
import com.octopuscommunity.sdk.ui.comments.details.CommentDetailsScreen
import com.octopuscommunity.sdk.ui.common.validation.ValidateImage
import com.octopuscommunity.sdk.ui.common.validation.ValidateText
import com.octopuscommunity.sdk.ui.components.OctopusTopAppBarDefaults
import com.octopuscommunity.sdk.ui.home.OctopusHomeContent
import com.octopuscommunity.sdk.ui.home.OctopusHomeDefaults
import com.octopuscommunity.sdk.ui.octopusDarkColorScheme
import com.octopuscommunity.sdk.ui.octopusLightColorScheme
import com.octopuscommunity.sdk.ui.onboarding.OnboardingContent
import com.octopuscommunity.sdk.ui.posts.create.CreatePostScreen
import com.octopuscommunity.sdk.ui.posts.details.PostDetailsScreen
import com.octopuscommunity.sdk.ui.profile.current.edit.CurrentUserProfileEditScreen
import com.octopuscommunity.sdk.ui.profile.current.nickname.ValidateNicknameScreen
import com.octopuscommunity.sdk.ui.profile.current.summary.CurrentUserProfileSummaryScreen
import com.octopuscommunity.sdk.ui.settings.SettingsScreen

/**
 * Theme Configuration and Preview Tools
 *
 * This file provides theme customization and preview configurations for the Octopus Community SDK
 * UI components. It allows developers to customize the visual appearance of the community interface
 * and preview different screens in various configurations.
 */

// ================================================================================================
// Preview Configuration Constants
// ================================================================================================

/**
 * UI mode for previews
 * - UI_MODE_NIGHT_YES: Dark mode enabled
 * - UI_MODE_NIGHT_NO: Light mode enabled
 */
const val uiMode = UI_MODE_NIGHT_YES

/**
 * Device type for preview rendering
 * Options: Devices.PHONE, Devices.TABLET, Devices.FOLDABLE, etc.
 */
const val device = Devices.TABLET

/**
 * Locale for preview (ISO 639-1 language code)
 * Examples: "en", "fr", "es", "de"
 */
const val locale = "fr"

/**
 * Font scale multiplier for accessibility testing
 * - 1.0f: Normal size
 * - 1.5f: 150% larger (useful for accessibility testing)
 * - 0.85f: Smaller text
 */
const val fontScale = 1.5f

/**
 * Whether to show system UI (status bar, navigation bar) in previews
 */
const val showUi = false


// ================================================================================================
// Custom Theme Configuration
// ================================================================================================

/**
 * CommunityTheme
 *
 * A customizable wrapper around OctopusTheme that allows developers to define their own:
 * - Brand colors (light and dark mode)
 * - Typography styles
 * - Top app bar styling
 * - Drawable resources (logos, icons)
 *
 * Usage:
 * ```
 * CommunityTheme {
 *     // Your composable content here
 * }
 * ```
 *
 * Customization Guide:
 *
 * 1. Color Scheme (Dark Mode):
 *    - primary: Main brand color (default: #3AD9B1 - teal)
 *    - primaryLow: Low emphasis primary (default: #083B2F - dark teal)
 *    - primaryHigh: High emphasis primary (default: #D8F4F1 - light teal)
 *    - onPrimary: Text/icon color on primary surfaces (default: #141414)
 *    - background: Screen background color (default: #141414)
 *    - onHover: Hover state color (default: #242526)
 *
 * 2. Color Scheme (Light Mode):
 *    - primary: Main brand color (default: #068677 - teal)
 *    - primaryLow: Low emphasis primary (default: #D8F4F1)
 *    - primaryHigh: High emphasis primary (default: #15D1A2)
 *    - onPrimary: Text/icon color on primary surfaces (default: #FFFFFF)
 *    - background: Screen background color (default: #FFFFFF)
 *    - onHover: Hover state color (default: #FFFFFF)
 *
 * 3. Typography:
 *    Customize text styles for:
 *    - title1, title2: Heading styles
 *    - body1, body2: Body text styles
 *    - caption1, caption2: Small text styles
 *
 * 4. Top App Bar:
 *    Customize modifier, title, navigation icon, actions, and colors
 *
 * 5. Drawables:
 *    Replace the logo with your own: R.drawable.ic_logo
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityTheme(content: @Composable () -> Unit) {
    OctopusTheme(
        colorScheme = if (isSystemInDarkTheme()) {
            octopusDarkColorScheme(
                primary = Color(0xFF3AD9B1),
                primaryLow = Color(0xFF083B2F),
                primaryHigh = Color(0xFFD8F4F1),
                onPrimary = Color(0xFF141414),
                background = Color(0xFF141414),
                onHover = Color(0xFF242526),
                // Possibly complete with more fine grained values
            )
        } else {
            octopusLightColorScheme(
                primary = Color(0xFF068677),
                primaryLow = Color(0xFFD8F4F1),
                primaryHigh = Color(0xFF15D1A2),
                onPrimary = Color(0xFFFFFFFF),
                background = Color(0xFFFFFFFF),
                onHover = Color(0xFFFFFFFF)
                // Possibly complete with more fine grained values
            )
        },
        typography = OctopusTypographyDefaults.typography().let { defaultTypography ->
            // Override any desired property
            defaultTypography.copy(
                title1 = defaultTypography.title1.copy(
                    // Change TextStyle properties here
                ),
                title2 = defaultTypography.title2.copy(
                    // Change TextStyle properties here
                ),
                body1 = defaultTypography.body1.copy(
                    // Change TextStyle properties here
                ),
                body2 = defaultTypography.body2.copy(
                    // Change TextStyle properties here
                ),
                caption1 = defaultTypography.caption1.copy(
                    // Change TextStyle properties here
                ),
                caption2 = defaultTypography.caption2.copy(
                    // Change TextStyle properties here
                ),
            )
        },
        topAppBar = OctopusTopAppBarDefaults.topAppBar(
            modifier = Modifier,
            title = OctopusTopAppBarDefaults.title(
                // Change title properties here
            ),
            navigationIcon = OctopusTopAppBarDefaults.navigationIcon(
                // Change navigation icon properties here
            ),
            actions = OctopusTopAppBarDefaults.actions(
                // Change actions properties here
            ),
            colors = OctopusTopAppBarDefaults.colors(
                // Change colors properties here
            )
        ),
        drawables = OctopusDrawablesDefaults.drawables(
            // Define your own logo here
            logo = R.drawable.ic_logo
        )
    ) {
        content()
    }
}

// ================================================================================================
// Preview Functions
// ================================================================================================

/**
 * Preview: Home Screen
 *
 * Displays the main home feed with posts and navigation capabilities.
 * Uses custom preview configuration from the constants above.
 */
@Preview(
    showSystemUi = showUi,
    locale = locale,
    device = device,
    uiMode = uiMode,
    fontScale = fontScale
)
@Composable
private fun OctopusHomePreview() {
    CommunityTheme {
        OctopusHomeContent(
            navController = rememberNavController(),
            contentPadding = OctopusHomeDefaults.contentPadding(),
            onNavigateToLogin = {},
            onNavigateToProfileEdit = {}
        )
    }
}

/**
 * Preview: Onboarding Screen
 *
 * Shows the user onboarding flow for first-time users.
 */
@Preview
@Composable
private fun OctopusOnboardingPreview() {
    CommunityTheme {
        OnboardingContent()
    }
}

/**
 * Preview: Post Details Screen
 *
 * Displays a detailed view of a single post with comments.
 * Uses a random mock post from MockPosts.all for demonstration.
 */
@Preview
@Composable
private fun OctopusPostDetailsPreview() {
    CommunityTheme {
        PostDetailsScreen(
            navController = rememberNavController(),
            postId = MockPosts.all.random().id
        )
    }
}

/**
 * Preview: Create Post Screen
 *
 * Shows the interface for creating a new post with text and media.
 */
@Preview
@Composable
private fun OctopusCreatePostPreview() {
    CommunityTheme {
        CreatePostScreen(
            navController = rememberNavController()
        )
    }
}

/**
 * Preview: Comment Details Screen
 *
 * Displays a detailed view of a single comment with replies.
 * Uses the default mock comment for demonstration.
 */
@Preview
@Composable
private fun OctopusCommentDetailsPreview() {
    CommunityTheme {
        CommentDetailsScreen(
            navController = rememberNavController(),
            commentId = MockComments.default.id
        )
    }
}

/**
 * Preview: Current User Profile - Posts Tab
 *
 * Shows the current user's profile with the posts tab selected (index 0).
 * Displays user information and their created posts.
 */
@Preview
@Composable
private fun OctopusCurrentUserProfileSummaryPostsPreview() {
    CommunityTheme {
        CurrentUserProfileSummaryScreen(
            navController = rememberNavController(),
            selectedTabIndex = 0
        )
    }
}

/**
 * Preview: Current User Profile - Notifications Tab
 *
 * Shows the current user's profile with the notifications tab selected (index 1).
 * Displays user notifications and activity updates.
 */
@Preview
@Composable
private fun OctopusCurrentUserProfileSummaryNotificationsPreview() {
    CommunityTheme {
        CurrentUserProfileSummaryScreen(
            navController = rememberNavController(),
            selectedTabIndex = 1
        )
    }
}

/**
 * Preview: Edit Profile Screen
 *
 * Shows the interface for editing the current user's profile information,
 * including avatar, bio, and other personal details.
 */
@Preview
@Composable
private fun OctopusCurrentUserProfileEditPreview() {
    CommunityTheme {
        CurrentUserProfileEditScreen(
            navController = rememberNavController(),
        )
    }
}

/**
 * Preview: Settings Screen
 *
 * Displays the app settings and preferences interface.
 */
@Preview
@Composable
private fun OctopusSettingsPreview() {
    CommunityTheme {
        SettingsScreen(
            navController = rememberNavController(),
        )
    }
}

/**
 * Preview: Validate Nickname Screen
 *
 * Shows the interface for validating and setting a user nickname.
 */
@Preview
@Composable
private fun OctopusValidateNicknamePreview() {
    CommunityTheme {
        ValidateNicknameScreen(
            navController = rememberNavController(),
        )
    }
}

/**
 * Preview: Input Field - Short Text with Validation Errors
 *
 * Demonstrates the OctopusItemInputField component with:
 * - Short text input
 * - Text validation error (maxLength = 2)
 * - Media validation error (oversized image)
 * - Attached image media
 *
 * Useful for testing error states and validation feedback.
 */
@Preview
@Composable
private fun OctopusItemInputFieldShortTextPreview() {
    OctopusTheme {
        OctopusItemInputField(
            state = OctopusItemInputFieldViewModel.UiState(
                item = Comment.Draft(
                    post = MockPosts.default,
                    text = MockComments.shortText.text,
                    media = MockComments.default.medias?.images?.firstOrNull()
                ),
                textError = ValidateText().invoke(
                    maxLength = 2,
                    text = MockComments.shortText.text
                ),
                mediaError = ValidateImage().invoke(Size(45000, 4500))
            ),
            placeholder = "Add a comment",
            onTextChanged = {},
            onImageChanged = {},
            onFocused = {},
            onSend = {}
        )
    }
}

/**
 * Preview: Input Field - Multiline Text with Sending State
 *
 * Demonstrates the OctopusItemInputField component with:
 * - Multiline text input
 * - Sending state (isSending = true)
 * - No validation errors
 *
 * Useful for testing the loading/sending state of the input field.
 */
@Preview
@Composable
private fun OctopusItemInputFieldMultilinePreview() {
    OctopusTheme {
        OctopusItemInputField(
            state = OctopusItemInputFieldViewModel.UiState(
                item = Comment.Draft(
                    post = MockPosts.default,
                    text = MockComments.default.text
                ),
                isSending = true
            ),
            placeholder = "Add a comment",
            onTextChanged = {},
            onImageChanged = {},
            onFocused = {},
            onSend = {}
        )
    }
}