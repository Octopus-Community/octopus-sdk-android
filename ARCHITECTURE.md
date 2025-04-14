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
