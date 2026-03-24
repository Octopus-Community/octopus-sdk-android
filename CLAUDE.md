# Octopus SDK for Android — Public Mirror

## What this is

This is the **public mirror** of `octopus-sdk-android-sources`. It is published to Maven Central. Code changes happen in the source repo, NOT here.

## Rules — CRITICAL

- **NEVER** commit code changes directly to this repo
- **NEVER** push to this repo without explicit approval
- This repo receives synced releases from the private source repo
- The only legitimate changes here are: README updates, license changes, release tags

## Build Commands

```bash
./gradlew compileDebugKotlin    # Verify the build compiles
./gradlew build                 # Full build
```

## Related

| Project | Role |
|---------|------|
| `octopus-sdk-android-sources` | Private source — all dev happens here |
| Maven Central | `com.octopuscommunity:octopus-sdk-*` |
