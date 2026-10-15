# Android sample architecture

This repository contains the canonical `sample/` application and `tools/`,
a Compose theme configurator. The SDK libraries are resolved from Maven Central
using the `octopusSdk` pin in `gradle/libs.versions.toml`.

The sample has a single navigation shell: Config, Scenarios, Community and
Settings, with a Debug console for events and API results. Scenario presets
exercise SDK capabilities, including tab, full-screen and modal navigation.
The `demo` and `prod` build flavors select backend behavior; integration patterns
are scenarios rather than separate build variants.

`sample/src/main/` contains shared data models, persistence, view models and
Compose screens. Default local extensions are inert in this standalone build.
`tools/` remains an independently maintained library module.
`theme-preview-compat/` supplies notices for its retired authentication/account
screen previews; the root build adds those sources to `tools` only.

Build with JDK 17 and Android SDK 36:

```bash
./gradlew :sample:assembleDemoDebug :sample:assembleProdDebug :tools:assembleDebug
```

See [the integration guide](docs/integration-guide.md) for host integration and
[the README](README.md) for sandbox configuration.
