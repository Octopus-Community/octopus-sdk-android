## Architecture of the SDK

The Android SDK is divided into two public products:

- `octopus-sdk` that contains the model objects.
- `octopus-sdk-ui` that contains all the code related to the UI

`octopus-sdk` exposes `OctopusSDK`.

- `OctopusSDK` is the main object of the Octopus SDK. It is initialized with the API key that
  uniquely identifies a client. It creates all the other main objects of the SDK. These objects are
  created and retained using a dependency injection module. They are
  mainly `Repositories`, `Mappers`, `Database Daos`  and `Network Services`.
- `ConnectionMode` is a configuration of the SDK. It is either `Octopus` for a user connection
  handled by us, or `SSO` for a user connection based on your user management.
- `ClientUser` is only used when `connectionMode` is `SSO`. It represents how the SDK will be fed
  with your user information.

`octopus-sdk-ui` exposes composable screens, content composables, navigation, and theming.

### Screens and Content Composables

Each feature is exposed in two variants:
- **Screen** composables (e.g., `OctopusHomeScreen`, `SettingsScreen`) include a Scaffold with a top bar. Use them when you want a ready-to-use full screen.
- **Content** composables (e.g., `OctopusHomeContent`, `OctopusPostDetailsContent`) provide just the content without a Scaffold. Use them when embedding into your own layout (custom Scaffold, tabs, bottom sheets).

Available screens include:
- `OctopusHomeScreen` / `OctopusHomeContent` — Main community feed
- `OctopusPostDetailsScreen` / `OctopusPostDetailsContent` — Post detail with comments
- `CreatePostScreen` / `CreatePostContent` — Post creation
- `CommentDetailsScreen` / `CommentDetailsContent` — Comment thread
- `CurrentUserProfileSummaryScreen` / `CurrentUserProfileSummaryContent` — Current user profile
- `ProfileSummaryScreen` / `ProfileSummaryContent` — Other user profile
- `ProfileAvatarScreen` — Full-screen avatar view
- `GroupsScreen` — Groups/topics browsing
- `SettingsScreen` / `SettingsContent` — Settings
- `MagicLinkEmailScreen` / `MagicLinkConfirmationScreen` — Magic link authentication
- `NotificationsList` — Standalone notifications component

### Navigation

- `octopusComposables()` is the extension function to add the SDK's navigation graph into your `NavHost`. Use it when you want SDK sub-screens (post details, profiles, settings, etc.) to navigate within your app's navigation graph.
- `OctopusNavigationHandler` wraps content and provides callbacks for login, profile edit, client object navigation, and URL opening.
- `UrlOpeningStrategy` controls whether URLs are opened by the SDK or by your app.
- `OctopusDestination`s are all the destinations available inside the UI SDK.

### Theming

- `OctopusTheme` is the theme used by all the composables of `octopus-sdk-ui`. This theme can be
  overridden or left as is (and so inherited from your App's Material Theme).
- `OctopusColorScheme` — Light and dark color schemes with primary, background, gray scale, alert, success, and hover colors.
- `OctopusTypography` — Text styles for title, body, and caption levels.
- `OctopusImages` — Logo and icons customization.

### Internal Architecture

For the moment, those other objects are not public, so you should not use them (as long as they are
in the OctopusData package).
Here is a quick explanation of what they do, just so you can get a better understanding of how the
whole SDK works.

- `Repositories` are the public interface for each kind of interaction with the `Model` layer. For
  example PostsRepository, ConnectionRepository...
- `Mappers` are objects mappers between Database, Network and Model.
- `Database interfaces` are Daos that can do Database operations on a Room Database stack
- `Server interfaces` are part of another private target: `RemoteClient`. They are the interface to
  call the GRPC services.
