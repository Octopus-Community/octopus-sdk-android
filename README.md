# Octopus SDK for Android

Octopus is an SDK that enables you to **integrate a fully customizable social network** into your app, perfectly **aligned with your branding**.

## Dependencies

Add the dependencies to your `build.gradle` file:

```kotlin
dependencies {
    // Core SDK functionalities
    implementation("com.octopuscommunity:octopus-sdk:1.6.1")
    // SDK UI Components (optional)
    implementation("com.octopuscommunity:octopus-sdk-ui:1.6.1")
}
```

*(Note: Octopus is available on Maven Central)*

## How to use the samples

### 1. Add your API Key and Client User Secret in the `local.properties`

Add those lines to the root project `local.properties` file:

```properties
OCTOPUS_API_KEY=YOUR_API_KEY
// To test the SSO connection mode, provide a Client User Token Secret
OCTOPUS_SSO_CLIENT_USER_TOKEN_SECRET=YOUR_USER_TOKEN_SECRET
```

- Replace `YOUR_API_KEY` with your own API key. See [Get an API Key](https://doc.octopuscommunity.com/) for more infos.
- Replace `YOUR_USER_TOKEN_SECRET` with your own Client User Token Secret. See [Generate a signed JWT for SSO](https://doc.octopuscommunity.com/backend/sso) for more info.

### 2. Choose the build variant corresponding to your UI Integration Mode

Depending on your targeted UI integration, choose how you want to integrate the Octopus Community UI into your app:

#### 1. Full Screen Navigation

Add the `OctopusHomeContent` `@Composable` to a dedicated full-screen route, just like any other composable in your other screens.

[Full Screen Navigation](https://github.com/user-attachments/assets/f37e7bbe-7b4f-40d9-af79-c2ed9e09fc46)

**Best for:** Apps where community is a primary feature with equal prominence to other main sections.

- [FullScreen Sample](/samples/src/fullscreen/java/com/octopuscommunity/sample/screens/MainScreen.kt)

#### 2. Bottom Navigation Tabs

Integrate the community as a tab content in a bottom navigation alongside your other main sections.

[Bottom Navigation Tabs](https://github.com/user-attachments/assets/a10e201c-7e83-40e9-b8f9-ba1ddbd35af1)

**Best for:** Apps with 2-5 main sections where community deserves a dedicated, always-accessible tab.

- [Bottom Navigation Tabs Sample](/samples/src/bottomnavigationbar/java/com/octopuscommunity/sample/screens/MainScreen.kt)

#### 3. Floating Bottom Navigation

Similar to bottom navigation tabs but with content padding to add padding around the edges of the content.

[Floating Bottom Navigation](https://github.com/user-attachments/assets/7d27d77b-cdf6-498b-8748-f79fb8a5e31d)

**Best for:** Apps requiring custom navigation bar styling that matches a specific design system.

- [Floating Bottom Navigation Sample](/samples/src/contentpadding/java/com/octopuscommunity/sample/screens/MainScreen.kt)

#### 4. Modal Bottom Sheet

Display the community in a modal bottom sheet that overlays your content. Users can swipe down to dismiss.

[Modal Bottom Sheet](https://github.com/user-attachments/assets/c7775f4c-5f0f-44ee-a669-606fecedab1e)

**Best for:** Apps where community is a secondary feature accessed occasionally, without disrupting the main flow.

- [Modal Bottom Sheet Sample](/samples/src/modalbottomsheet/java/com/octopuscommunity/sample/screens/MainScreen.kt)

### 3. Edit the `appManagedFields` depending on your profile management

You need to configure which profile fields are managed by your app vs. Octopus Community:
Edit the `appManagedFields` list parameter on the `OctopusSDK.initialise()` call in the [SampleApplication](/samples/src/main/java/com/octopuscommunity/sample/SampleApplication.kt)

#### 1. Octopus-Managed Profile (No App-Managed Fields)

All profile fields are managed by Octopus Community. Info you provide in `connectUser` is only used as prefilled values when the user creates their community profile. Fields are not synchronized between your app and community profile.

- [Documentation](https://doc.octopuscommunity.com/SDK/sso/android)

**Requirements:**
- Your community must be configured with no app-managed fields

#### 2. Hybrid Profile (Some App-Managed Fields)

Some profile fields are managed by your app. These fields will be used in the community. Octopus Community won't moderate app-managed fields. If nickname is app-managed, you must ensure it's unique.

- [Documentation](https://doc.octopuscommunity.com/SDK/sso/android)

**Requirements:**
- Your community must be configured with some app-managed fields
- You must specify these fields in `OctopusSDK.initialize()`

#### 3. Client-Managed Profile (All App-Managed Fields)

All profile fields are managed by your app. Your user profile is used directly in the community. Octopus Community won't moderate any profile content. You must ensure nickname uniqueness.

- [Documentation](https://doc.octopuscommunity.com/SDK/sso/android)

**Requirements:**
- Your community must be configured with all fields as app-managed
- You must set all fields in `OctopusSDK.initialize()`

## Customizing the Theme

The Octopus SDK UI is fully customizable to match your app's branding. Use the `OctopusThemeConfigurator.kt` file to customize colors, typography, and other visual elements.

### Quick Start

1. **Locate the configurator file**: `/tools/src/main/java/com/octopuscommunity/tools/OctopusThemeConfigurator.kt`

2. **Customize your theme** by editing the `CommunityTheme` composable:

```kotlin
@Composable
fun CommunityTheme(content: @Composable () -> Unit) {
    OctopusTheme(
        colorScheme = if (isSystemInDarkTheme()) {
            octopusDarkColorScheme(
                primary = Color(0xFF3AD9B1),        // Your brand color
                primaryLow = Color(0xFF083B2F),     // Lighter variant
                primaryHigh = Color(0xFFD8F4F1),    // Darker variant
                onPrimary = Color(0xFF141414),      // Text on primary
                background = Color(0xFF141414),     // Background color
                onHover = Color(0xFF242526),        // Hover states
            )
        } else {
            octopusLightColorScheme(
                // Light mode colors...
            )
        },
        // Typography, top app bar, drawables...
    )
}
```

3. **Wrap your Octopus content** with `CommunityTheme`:

```kotlin
CommunityTheme {
    OctopusHomeContent(
        navController = navController,
        contentPadding = OctopusHomeDefaults.contentPadding(),
        onNavigateToLogin = { /* ... */ },
        onNavigateToProfileEdit = { /* ... */ }
    )
}
```

### What You Can Customize

#### Colors
- **Primary colors**: Main brand colors for buttons, links, and accents
- **Background colors**: Screen backgrounds and surface colors
- **Text colors**: Colors for text on various backgrounds
- **Interactive states**: Hover, pressed, and focus states

Both light and dark mode color schemes are supported with automatic system theme detection.

#### Typography
Customize all text styles:
- **title1, title2**: Heading styles
- **body1, body2**: Body text styles
- **caption1, caption2**: Small text and labels

#### Top App Bar
- Title styling and positioning
- Navigation icon appearance
- Action button styles
- Background and elevation

#### Drawables
- Custom logo: Replace `R.drawable.ic_logo` with your own logo resource

### Preview Configuration

The configurator includes constants to customize how previews render in Android Studio:

```kotlin
const val uiMode = UI_MODE_NIGHT_YES    // Test dark/light mode
const val device = Devices.TABLET        // Preview device type
const val locale = "fr"                  // Test localization
const val fontScale = 1.5f               // Test accessibility
const val showUi = false                 // Show system UI bars
```

### Testing Your Theme

The configurator includes preview functions for all major screens:
- Home feed
- Post details and creation
- User profile and settings
- Comments and input fields
- Onboarding flow

Simply open the `OctopusThemeConfigurator.kt` file in Android Studio and use the preview panel to see your changes in real-time across all screens.

### Best Practices

1. **Brand consistency**: Match your primary colors to your app's existing brand palette
2. **Accessibility**: Test with different font scales (0.85f to 2.0f) to ensure readability
3. **Dark mode**: Ensure both light and dark themes provide good contrast and visual hierarchy
4. **Preview early**: Use the preview functions frequently to catch visual issues before runtime

For more detailed theming documentation, visit [Octopus Documentation](https://doc.octopuscommunity.com/).

## Architecture

If you want to know more about the SDK's architecture, [here is a document](ARCHITECTURE.md) that explains it.