# Octopus SDK for Android

## Purpose

The purpose of this Android Dependency is to provide every feature of Octopus Community to your
Android based apps:
add your own social network within your app. We handle all the work for you: UI, moderation,
storage…
You can easily integrate all of these features with our Android and iOS SDKs.

## Dependencies

Add the dependencies to your `build.gradle` file:

```kotlin
dependencies {
    // Core SDK functionalities
    implementation("com.octopuscommunity:octopus-sdk:1.1.0")
    // SDK UI Components (optional)
    implementation("com.octopuscommunity:octopus-sdk-ui:1.1.0")
}
```

*(Note: Octopus is available on Maven Central)*

## How to use the samples

Add this line to the root project `local.properties` file:

 ```groovy
 OCTOPUS_API_KEY = YOUR_API_KEY 
 ```

*Replace `YOUR_API_KEY` with your own API key.
See [Get an API Key](https://octopuscommunity.notion.site/octopus-developer-guide) for more infos.*

### SSO (Optional)

Provide an Client User Token Secret

If you want to test the SSO connection mode, you need to provide a Client User Token Secret in the
root project `local.properties` file

 ```groovy
OCTOPUS_SSO_CLIENT_USER_TOKEN_SECRET = YOUR_USER_TOKEN_SECRET
```

*Replace `YOUR_USER_TOKEN_SECRET` with your own Client User Token Secret.
See [Generate a signed JWT for SSO](https://octopuscommunity.notion.site/Generate-a-signed-JWT-for-SSO-1add0ed811a98136867af1347caa24b8)
for more infos.*

## Use cases

Depending on your targeted UI integration and user connection mode/profile, choose the corresponding
sample and documentation.

### UI Integration Mode

- #### Full Screen

  Octopus can be integrated within your app as
  a [nested navigation graph](https://developer.android.com/guide/navigation/design/nested-graphs)
  that you start like any of your screen by navigating to the `OctopusDestination.Home`

    - [Sample](/samples/standard/fullscreen)
    - [Documentation](https://octopuscommunity.notion.site/Android-SDK-Setup-Guide-1a4d0ed811a980c5ada0e19726a67051)

- #### Embed Composable Content

  Octopus can also be integrated as a composable component within your existing app Screen.
  You can add `OctopusHomeContent` just like any other Composable component and apply your
  own `Modifier` to it.

    - [Sample](/samples/standard/embed)
    - [Documentation](https://octopuscommunity.notion.site/Android-SDK-Setup-Guide-1a4d0ed811a980c5ada0e19726a67051)

### User Connection and Profile Mode

- #### Standard (Octopus Managed User Connection and Profile)

  Use the Octopus self managed authentication (Magic Link) to let users interact with the community.

    - [Sample](/samples/standard/)
    - [Documentation](https://octopuscommunity.notion.site/Android-SDK-Setup-Guide-1a4d0ed811a980c5ada0e19726a67051)

- #### SSO

  Use your login mechanism to let your users interact with the community.

  *In order to test those scenarios, your community should be configured to use SSO Authentication*

    - ##### Octopus Managed Profile

      All profile fields are managed by Octopus Community. This means that the info you provide in
      the connectUser are only used as prefilled values when the user creates its community profile.
      After that, they are not synchronized between your app and the community profile.

        - [Sample](/samples/sso/octopus-profile)
        - [Documentation](https://octopuscommunity.notion.site/Android-SDK-Setup-Guide-1a4d0ed811a980c5ada0e19726a67051)

      In order to test this scenario:
        - your community should be configured to have no app managed fields

    - ##### Hybrid = Octopus & App Managed Profile

      Some profile fields are managed by your app. This means these fields are the ones
      that will be used in the community. It also means that Octopus Community won't moderate those
      fields (nickname, bio, or picture profile). It also means that, if the nickname is part of
      these fields, you have to ensure that it is unique.

        - [Sample](/samples/sso/hybrid-profile)
        - [Documentation](https://octopuscommunity.notion.site/Android-SDK-Setup-Guide-1a4d0ed811a980c5ada0e19726a67051)

      In order to test this scenario:
        - your community should be configured to have some app managed fields
        - you must set those fields in the OctopusSDK.initialize() function

    - ##### App Managed Profile

      All profile fields are managed by your app. This means that your user profile is the one that
      will be used in the community. It also means that Octopus Community won't moderate the content
      of the profile (nickname, bio, and picture profile). It also means that you have to ensure
      that the nickname is unique.

        - [Sample](/samples/sso/client-profile)
        - [Documentation](https://octopuscommunity.notion.site/Android-SDK-Setup-Guide-1a4d0ed811a980c5ada0e19726a67051)

      In order to test this scenario:
        - your community should be configured to have all fields as app managed
        - you must set those fields in the OctopusSDK.initialize() function

## Architecture

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

`OctopusUI` exposes `OctopusHomeScreen`, `OctopusHomeContent`, `octopusNavigation()`
and `OctopusTheme`.

- `OctopusHomeScreen` is a Composable Screen with a root `Scafold`.
- `OctopusHomeContent` is a Composable Component that you can include in a root `Scafold`.
- `octopusNavigation` is the nested navigation Graph of OctopusUI.
- `OctopusTheme` is the theme used by all the composables of `OctopusUI`. This theme can be
  overridden or left as is (and so inherited from your App's Material Theme).
- `OctopusDestination`s are all the Screens available inside the UI SDK.

For the moment, those other objects are not public, so you should not use them (as long as they are
in the OctopusData package).
Here is a quick explanation of what they do, just so you can get a better understanding of how the
whole SDK works.

- `Repositories` are the public interface for each kind of interaction with the `Model` layer. For
  example PostsRepository, ConnectionRepository…
- `Mappers` are objects mappers between Database, Network and Model.
- `Database interfaces` are Daos that can do Database operations on a Room Database stack
- `Server interfaces` are part of another private target: `RemoteClient`. They are the interface to
  call the GRPC services.
