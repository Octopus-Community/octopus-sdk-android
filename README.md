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

### Local properties

- Add this line to the root project `local.properties` file:

```properties
OCTOPUS_API_KEY=YOUR_API_KEY
```

Replace `YOUR_API_KEY` with your own API key. See [Get an API Key](https://doc.octopuscommunity.com/) for more infos.

- To test SSO connection mode, provide a Client User Token Secret in the root project `local.properties` file:

```properties
OCTOPUS_SSO_CLIENT_USER_TOKEN_SECRET=YOUR_USER_TOKEN_SECRET
```

Replace `YOUR_USER_TOKEN_SECRET` with your own Client User Token Secret. See [Generate a signed JWT for SSO](https://doc.octopuscommunity.com/backend/sso) for more info.

### UI Integration Mode

Depending on your targeted UI integration, choose how you want to integrate the Octopus Community UI into your app:

#### 1. Full Screen Navigation

Display the `OctopusHomeContent` on a dedicated full-screen route, just like any other composable in your other screens.

**Best for:** Apps where community is a primary feature with equal prominence to other main sections.

- [Sample](/samples/src/fullscreen)

#### 2. Bottom Navigation Tabs

Integrate community as a tab content in a bottom navigation alongside your other main sections.

**Best for:** Apps with 2-5 main sections where community deserves a dedicated, always-accessible tab.

- [Sample](/samples/src/bottomnavigationbar)

#### 3. Floating Bottom Navigation 

Similar to bottom navigation tabs but with content padding to add padding around the edges of the content.

**Best for:** Apps requiring custom navigation bar styling that matches a specific design system.

- [Sample](/samples/src/contentpadding)

#### 4. Modal Bottom Sheet

Display the community in a modal bottom sheet that overlays your content. Users can swipe down to dismiss.

**Best for:** Apps where community is a secondary feature accessed occasionally, without disrupting the main flow.

- [Sample](/samples/src/modalbottomsheet)

---

### Profile Management Options

You need to configure which profile fields are managed by your app vs. Octopus Community:
Edit the `appManagedFields` list on the OctopusSDK.initialise() call in the [SampleApplication](/samples/src/main/java/com/octopuscommunity/sample/SampleApplication.kt)

#### Option 1: Octopus-Managed Profile (No App-Managed Fields)

All profile fields are managed by Octopus Community. Info you provide in `connectUser` is only used as prefilled values when the user creates their community profile. Fields are not synchronized between your app and community profile.

- [Documentation](https://doc.octopuscommunity.com/SDK/sso/android)

**Requirements:**
- Your community must be configured with no app-managed fields

#### Option 2: Hybrid Profile (Some App-Managed Fields)

Some profile fields are managed by your app. These fields will be used in the community. Octopus Community won't moderate app-managed fields. If nickname is app-managed, you must ensure it's unique.

- [Documentation](https://doc.octopuscommunity.com/SDK/sso/android)

**Requirements:**
- Your community must be configured with some app-managed fields
- You must specify these fields in `OctopusSDK.initialize()`

#### Option 3: Client-Managed Profile (All App-Managed Fields)

All profile fields are managed by your app. Your user profile is used directly in the community. Octopus Community won't moderate any profile content. You must ensure nickname uniqueness.

- [Documentation](https://doc.octopuscommunity.com/SDK/sso/android)

**Requirements:**
- Your community must be configured with all fields as app-managed
- You must set all fields in `OctopusSDK.initialize()`

## Architecture

If you want to know more about the SDK's architecture, [here is a document](ARCHITECTURE.md) that explains it.