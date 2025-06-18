# Octopus SDK for Android

Octopus is an SDK that enables you to **integrate a fully customizable social network** into your app, perfectly **aligned with your branding**.

## Dependencies

Add the dependencies to your `build.gradle` file:

```kotlin
dependencies {
  // Core SDK functionalities
  implementation("com.octopuscommunity:octopus-sdk:1.4.0")
  // SDK UI Components (optional)
  implementation("com.octopuscommunity:octopus-sdk-ui:1.4.0")
}
```

*(Note: Octopus is available on Maven Central)*

## How to use the samples

Add this line to the root project `local.properties` file:

 ```properties
 OCTOPUS_API_KEY=YOUR_API_KEY
 ```

Replace `YOUR_API_KEY` with your own API key. See [Get an API Key](https://doc.octopuscommunity.com/) for more infos.

## Use cases

Depending on your targeted UI integration and user connection mode/profile, choose the corresponding
sample and documentation.

### UI Integration Mode

- #### Launch the Octopus Home from an action

  Navigate to the `OctopusDestination.Home` just like any of your screens

  - [Sample](/samples/octopus-auth/fullscreen)
  - [Documentation](https://doc.octopuscommunity.com/SDK/octopus-auth/android)

OR

- #### Integrate OctopusHome Composable Content

  Octopus can be embed as a composable component within your existing app Screen.
  Include the `OctopusHomeContent` or `OctopusHomeScreen` just like any other Composable and apply your own `Modifier` to it.

  - [Sample](/samples/octopus-auth/embed)
  - [Documentation](https://doc.octopuscommunity.com/SDK/octopus-auth/android)

### User Connection and Profile Mode

- #### Octopus Authentication

  This method allows you to integrate Octopus quickly, with no backend development required. Octopus handles both user account creation and authentication.

  - [Sample](/samples/octopus-auth)
  - [Documentation](https://doc.octopuscommunity.com/SDK/octopus-auth/android)

- #### SSO

  This integration mode allows users to be automatically logged into the community within your mobile application. If a user has an account in your application and is already logged in, they won’t need to authenticate again to access the community.

  - Provide a Client User Token Secret

    If you want to test the SSO connection mode, you need to provide a Client User Token Secret in the
root project `local.properties` file

    ```properties
    OCTOPUS_SSO_CLIENT_USER_TOKEN_SECRET=YOUR_USER_TOKEN_SECRET
    ```
    Replace `YOUR_USER_TOKEN_SECRET` with your own Client User Token Secret. See [Generate a signed JWT for SSO](https://doc.octopuscommunity.com/backend/sso)
for more infos.

  - You need to know the app managed fields (also called as associated fields) that your community is configured for.
    - ##### SSO without any app managed fields

      All profile fields are managed by Octopus Community. This means that the info you provide in
      the connectUser are only used as prefilled values when the user creates its community profile.
      After that, they are not synchronized between your app and the community profile.

      - [Sample](/samples/sso/octopus-profile)
      - [Documentation](https://doc.octopuscommunity.com/SDK/sso/android)

      In order to test this scenario:
      - your community should be configured to have no app managed fields

    - ##### SSO with some app managed fields

      Some profile fields are managed by your app. This means these fields are the ones
      that will be used in the community. It also means that Octopus Community won't moderate those
      fields (nickname, bio, or picture profile). It also means that, if the nickname is part of
      these fields, you have to ensure that it is unique.

        - [Sample](/samples/sso/hybrid-profile)
        - [Documentation](https://doc.octopuscommunity.com/SDK/sso/android)

      In order to test this scenario:
        - your community should be configured to have some app managed fields
        - you must set those fields in the OctopusSDK.initialize() function

    - ##### SSO with all app managed fields

      All profile fields are managed by your app. This means that your user profile is the one that
      will be used in the community. It also means that Octopus Community won't moderate the content
      of the profile (nickname, bio, and picture profile). It also means that you have to ensure
      that the nickname is unique.

        - [Sample](/samples/sso/client-profile)
        - [Documentation](https://doc.octopuscommunity.com/SDK/sso/android)

      In order to test this scenario:
        - your community should be configured to have all fields as app managed
        - you must set those fields in the OctopusSDK.initialize() function

## Architecture

If you want to know more about how the SDK's architecture, [here is a document](ARCHITECTURE.md) that explains it.
