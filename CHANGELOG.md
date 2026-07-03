# Changelog

All notable changes to the Octopus Android SDK (`com.octopuscommunity:octopus-sdk` and
`com.octopuscommunity:octopus-sdk-ui`, published on Maven Central) are documented in this file.

Full, detailed release notes — including API listings and migration code samples — live on
[GitHub Releases](https://github.com/Octopus-Community/octopus-sdk-android/releases).
For upgrade instructions across breaking changes, see [MIGRATING.md](MIGRATING.md).

## Unreleased

## [1.12.1](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.12.1) — 2026-06-25

### Added
- Per-field profile lock: `CommunityConfig.profileFieldsLock` exposes fields marked read-only or hidden by the community configuration.
- Per-content-type media and poll gating via `CommunityConfig.contentOptions` (pictures / polls per post, comment, reply).
- Bridge Share signed prefilled posts: `CreatePostScreenInfo.bridgeShareTokenProvider` lets the host sign a prefilled post's image for communities that forbid member pictures.
- Host-driven leading navigation icon on the home screen: `OctopusHomeScreen(leadingNavigationIcon = NavigationIconType.Back / Close)`.

### Fixed
- Follow state no longer dropped when syncing followed groups (`syncFollowGroups`).
- "Invalid token" bridge authentication failure on devices whose clock runs ahead of server time.
- Default color scheme now resolved from the actual background luminance (fixes low-contrast foregrounds when app and system themes diverge).
- Accessibility: raw view/comment counts exposed to TalkBack; home Close icon announced as "Close".

## [1.12.0](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.12.0) — 2026-05-27

### Breaking
- `OctopusSDK.setReaction(reaction, clientObjectRelatedPostId)` removed — use `setReaction(reaction, postId)`.
- `SetReactionOnBridgePostError` removed — catch `SetReactionError` instead.
- `OctopusDestination.ReportContent` and `OctopusDestination.ReportUser` removed — reporting is now a bottom sheet hosted by the parent screens. See [MIGRATING.md](MIGRATING.md).

### Added
- Content rights management: `OctopusSDK.profile` flow, `refreshEntitlements()`, `setGroupAccessDeniedCallback()`, `OctopusGroup.canAccess` / `canCreateChildren`.
- Open post creation as initial screen: `navigateToOctopusCreatePost(CreatePostScreenInfo)`, `OctopusCreatePostScreen`, `OctopusPrefilledPost`, `OctopusPostCTA`.
- `OctopusSDK.setReaction(reaction, postId)` — set or remove a reaction on any post (bridge or community).
- Custom API endpoint: optional `apiServer` parameter on `OctopusSDK.Configuration`.

### Changed
- Topics navigation refactor (single client-driven order), group CTA button, locked-group UI states, compact view-count formatter, report view redesigned as a Material 3 bottom sheet.

## [1.11.0](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.11.0) — 2026-04-29

### Breaking
- `ClientPost.topicId` renamed to `groupId`. See [MIGRATING.md](MIGRATING.md).

### Added
- Reactions UI revamp with a six-reaction picker, haptic feedback, and customizable reaction icons (`OctopusTheme.icons.content.reaction`).
- Group follow/unfollow sync between host app and community: `OctopusSDK.syncFollowGroups(actions)`, `OctopusSDK.groups`, `fetchGroups()`.
- `OctopusGroup` public type (replaces `Topic`, kept as a `typealias`) with `isFollowed` and `canChangeFollowStatus`.
- Open a specific group: `OctopusGroupDetailsScreen` / `OctopusGroupDetailsContent` composables and `NavController.navigateToOctopusGroup(groupId)`.
- `OctopusEvent.ScreenDisplayed.GroupDetail.source` (bridge vs community).

### Deprecated
- `Topic`, `OctopusSDK.topics`, `fetchTopics()`, `followTopic()`, `unfollowTopic()` — "Topic" terminology renamed to "Group" across the public API.

### Fixed
- Followed groups no longer duplicated into "More"; group name header always shown on Group Details; Room migration safety net (98 → 102); visual polish aligned with iOS.

## [1.10.2](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.10.2) — 2026-04-23

### Fixed
- Rare `SQLiteException` on some Android 15 devices upgrading from SDK 1.9.x: explicit Room migration + database rebuild safety net at open.

## [1.10.1](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.10.1) — 2026-04-07

### Fixed
- Local database cleanup could fail on communities with a high volume of posts, causing unresponsiveness.

## [1.10.0](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.10.0) — 2026-03-19

### Added
- Follow / unfollow topics with feed personalization and topic-based navigation.
- SDK icon customization; configurable title alignment on `OctopusHomeScreen`.
- Community switch at runtime via `OctopusSDK.switchCommunity()`.

### Deprecated
- `fetchOrCreateClientObjectRelatedPost(content)` — use the `(content, tokenProvider)` variant.

### Security
- Reinforced anti-spam detection; strengthened post creation validation through bridge signatures.

## [1.9.1](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.9.1) — 2026-02-18

### Added
- Polish (PL) and Swedish (SV) localization.
- Reactions on bridge posts, with exposed reaction counts.

## [1.9.0](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.9.0) — 2026-02-05

### Added
- Video posts, action posts (CTA button), in-app link opening.
- SDK language auto-alignment with the host app.
- Local analytics event dispatching to the host app.

### Security
- Bridge Share security fix.

## [1.8.1](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.8.1) — 2026-01-09

### Fixed
- Possible ANRs on Android 12.0.

## [1.8.0](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.8.0) — 2025-12-19

### Added
- Gamification V1 (badges, points, notifications).
- Spanish, Italian, Portuguese, and Turkish localization.
- Accessibility improvements (EAA / WCAG alignment).

## [1.7.2](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.7.2) — 2025-12-11

### Fixed
- Downgraded protobuf to 3.25.6 for Firebase compatibility.

## [1.7.1](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.7.1) — 2025-12-05

### Fixed
- Unexpected connection initialization calls; profile fetching issues.

## [1.7.0](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.7.0) — 2025-11-05

### Added
- Bridge posts API: `fetchOrCreateClientObjectRelatedPost()` with reactions, comment/reply/view counts, and the reactive `getClientObjectRelatedPostFlow()`.
- Translated content support (switch between original and translated).

### Removed
- Onboarding screen — users now see the community directly.

### Deprecated
- `getOrCreateClientObjectPost()` — use `fetchOrCreateClientObjectRelatedPost()`.

## [1.6.2](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.6.2) — 2025-10-09

### Added
- `OctopusThemeConfigurator` tool, integration samples, complete preview mocks.

### Fixed
- Possible SSO issues; IME padding; reactions alignment; TopAppBar default typography.

## [1.6.1](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.6.1) — 2025-10-03

### Added
- `OctopusSDK.hasAccessToCommunity` and `OctopusSDK.isUserConnected` flows.

### Fixed
- Composable previews; default connection mode moved to SSO; theme logo display.

## [1.6.0](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.6.0) — 2025-09-26

### Added
- `OctopusSDK.overrideCommunityAccess(Boolean)`; advanced reactions; contextual comment display; revamped first-time user flow.

### Deprecated
- `ClientUser.ageInformation`; `OctopusSDK.setAccessToCommunity()` → `trackAccessToCommunity()`.

## [1.5.1](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.5.1) — 2025-09-11

### Fixed
- Back button enabled by default with `onBack` callback on `octopusComposables`; missing ProGuard rules; AGP 8.11.1 / Koin 4.1.1 updates.

## [1.5.0](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.5.0) — 2025-07-04

### Added
- Bridge: link an object from your app to an Octopus post.

## [1.4.1](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.4.1) — 2025-06-26

### Fixed
- R8 and Java desugaring compilation issues.

## [1.4.0](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.4.0) — 2025-06-18

### Added
- Push notifications linked to relevant content; client-side analytics; top bar title/color customization; German localization; post-creation UX improvements.

## [1.3.0](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.3.0) — 2025-05-27

### Added
- Configurable community visibility (A/B testing scenarios); notification badge on the Community button.

## [1.2.1](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.2.1) — 2025-05-20

### Fixed
- Possible user disconnection after token expiration.

## [1.2.0](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.2.0) — 2025-05-06

### Added
- Photo zoom; Notification Center.

## [1.1.3](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.1.3) — 2025-05-02

### Changed
- Dependency updates.

## [1.1.2](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.1.2) — 2025-04-28

### Added
- Session tracking.

## [1.1.1](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.1.1) — 2025-04-14

### Changed
- Simplified initialization.

## [1.1.0](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.1.0) — 2025-04-04

### Added
- Polls; replies on comments; SSO hybrid profile.

## [1.0.5](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.0.5) — 2025-04-04

### Fixed
- SSO logout access; post details author typography; nickname label in profile edition; multiple user token fetching.

## [1.0.4](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.0.4) — 2025-03-31

### Added
- `bottomFloatingActionsPadding` parameter.

## [1.0.3](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.0.3) — 2025-03-31

### Fixed
- Encryption error after app reinstallation.

## [1.0.2](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.0.2) — 2025-03-27

### Fixed
- Image picking on Android API < 31.

## [1.0.1](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.0.1) — 2025-03-19

### Fixed
- Dependency updates; added ProGuard rules.

## [1.0.0](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.0.0) — 2025-03-13

First stable release: post detail rendering, profile bio display, dark mode polish, image upload processing improvements, text copy support, and more.

## [1.0.0-beta01](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.0.0-beta01) — 2025-03-03

Initial release.
