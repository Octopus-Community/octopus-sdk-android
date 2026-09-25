# Changelog

All notable changes to the Octopus Android SDK (`com.octopuscommunity:octopus-sdk` and
`com.octopuscommunity:octopus-sdk-ui`, published on Maven Central) are documented in this file.

Full, detailed release notes — including API listings and migration code samples — live on
[GitHub Releases](https://github.com/Octopus-Community/octopus-sdk-android/releases).
For upgrade instructions across breaking changes, see [MIGRATING.md](MIGRATING.md).

## Unreleased

## [1.14.0](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.14.0) — 2026-09-24

### Breaking
- `GuestError` gained a `UserBanned` variant, carrying the server's localized ban reason in `errorMessage`: `connectAsGuest()` now fails with it when the member is banned. An exhaustive `when` over `GuestError` must handle it. See [MIGRATING.md](MIGRATING.md).
- Binary-only: `CommunityConfig`, `CurrentUserProfile`, `OtherUserProfile`, `OctopusIcons` and `OctopusIconsDefaults.icons()` gained a trailing defaulted parameter, and the `Profile` interface gained `commentsFeeds`. Kotlin source compiles unchanged; code compiled against 1.13.x (test fixtures, previews, prebuilt libraries) must be recompiled. See [MIGRATING.md](MIGRATING.md).
- Profile and Activity tab indices shifted for the new Comments tab: `CurrentUserProfileSummary` is Posts=0, Comments=1, Notifications=2; `Activity` is Notifications=0, Comments=1, Posts=2. Use the predefined instances rather than raw indices. See [MIGRATING.md](MIGRATING.md).
- Nine string resources removed (replaced by the screen states below) — an override of `notifications_list_empty`, `post_create_incentive_button1`–`4`, `post_create_incentive_button6`, `post_create_incentive_explanation`, `post_list_empty` or `post_list_other_user_empty` is now unused.

### Added
- Who reacted: tapping the reaction counters under a post or comment lists the members who reacted, with an "All" tab plus one tab per reaction type. Reacting stays on the like button. New `ReactionsRepository`, `ProfileReaction`, `ReactionsPage` and `FakeReactionsRepository`.
- Comments tab on the profile and Activity screens: a member's comments and replies, each with the post (and parent comment) it belongs to. Always shown on the connected user's own profile; on another member's profile when the community enables `CommunityConfig.showCommentsOnOtherProfiles`. New `OctopusSDK.userCommentRepository`, `UserComment`, `UserCommentsPage`, `Profile.commentsFeeds`, `FakeUserCommentRepository` and `OctopusDestination.CurrentUserProfileSummary.Comments`.
- Empty, loading and error screen states on every list and detail screen, with a Retry on the offline and generic error states. When content is already displayed, a failed refresh shows a snackbar with Retry instead of replacing it.
- Theming: `OctopusIcons.ScreenStates` (`emptyContent`, `emptyNotifications`, `networkError`, `error`) through the new `screenStates` parameter of `OctopusIconsDefaults.icons()`. The illustrations keep their own colors.

### Changed
- Links in post and comment bodies are tappable in feeds, not only on the detail screen, and feeds render markdown like the detail screens do.

### Deprecated
- The `error` property of the list ViewModels' `UiState` (`PostsListViewModel`, `NotificationsListViewModel`, `GroupsListViewModel`, `OctopusItemsListViewModel`) — no longer set; removed in the next major.

### Fixed
- `OctopusSDK.registerNotificationsToken()` called before `OctopusSDK.initialize()` crashed and lost the token; the SDK now keeps it and registers it on the next `initialize()`. `OctopusSDK.trackAccessToCommunity()` no longer crashes before `initialize()`.
- An image the SDK cannot read or encode now fails the call with the existing `FileError` / `PictureError` cases instead of being published missing; an SSO avatar that cannot be encoded no longer resets the member's picture.
- `OctopusEvent.PostCreated` could omit an attachment the member did publish.
- Time spent in the app and in the community was reported inaccurately to analytics.
- A right-to-left `overrideDefaultLocale` on a left-to-right device did not mirror the layout, and reaction counts showed their digits reversed in right-to-left languages.
- A URL containing two underscores opened the wrong page, and "See more" could appear on content with nothing more to read.
- Icons and text shown above the SDK's first surface could inherit the host app's colors.

## [1.13.4](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.13.4) — 2026-09-04

Two-fix patch over 1.13.3. No API change, no migration.

### Fixed
- The Octopus UI no longer crashes when Android restores a host activity in a process where
  `OctopusSDK.initialize()` has not run yet (a recreation after a low-memory kill, before the host
  has initialised the SDK — routine when the host boots an engine such as Unity or Flutter first).
  The screen used to bring the process down with `lateinit property koinApp has not been
  initialized`; it now logs a warning and renders nothing until the host initialises the SDK, then
  the content appears on its own. Apps that initialise before showing Octopus UI see no difference.
- The SDK no longer dies at `OctopusSDK.initialize()` in a host app that minifies its build. The
  consumer R8 rules kept gRPC but not the Guava it calls at runtime, so a shrunk host hit
  `NoSuchMethodError … com.google.common.base.Strings.isNullOrEmpty` from `io.grpc.internal.GrpcUtil`
  followed by `NoClassDefFoundError: io.grpc.LoadBalancerRegistry`. The SDK now ships the Guava
  keep rules itself; nothing to add on the host side. Hosts that do not minify were never affected.

## [1.13.3](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.13.3) — 2026-09-04

Single-fix patch over 1.13.2. No API change, no migration.

### Fixed
- Text-selection "process text" actions (Translate and any other app-provided action) and the fullscreen
  image viewer crashed `OctopusActivity` when `overrideDefaultLocale` was set: the locale override
  composed the SDK screens against a context that did not lead back to the host Activity. The override
  now keeps the Activity reachable while resources still resolve in the overridden locale. If your app
  stopped setting `overrideDefaultLocale` to avoid the crash, you can set it again.

## [1.13.2](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.13.2) — 2026-08-13

### Breaking
- `OctopusDateField` removed from `octopus-sdk-ui` — an unused public composable, orphaned when the "consent over 16" profile-creation screen was deleted in 1.6.0. See [MIGRATING.md](MIGRATING.md).
- `ValidateDate`, `DateValidationError` (with its `Missing` / `TooYoung` subtypes) and the `Date.calculateAge()` extension removed from `octopus-sdk-ui` — dead code from that same removed flow. See [MIGRATING.md](MIGRATING.md).

### Fixed
- Public Flows collected before `OctopusSDK.initialize()` never saw the real state. `connectionState`, `isUserConnected`, `profile`, `notSeenNotificationsCount`, `hasAccessToCommunity`, `groups` / `topics` and `events` bound to the SDK's Compose-preview fakes for the whole lifetime of the collection, so a later successful `connectUser()` stayed invisible to that collector. They now bind to the SDK's active dependency container: nothing is emitted while there is none, then the real state is mirrored, and the Flow re-binds itself on every re-initialization — `switchCommunity()` included, which therefore no longer requires the Flows to be re-collected. Collecting before `initialize()` needs no ordering guard on the host side.
- Chrome Custom Tabs could be painted with a dark toolbar and light content, or the reverse, leaving the page title unreadable. Nothing told Chrome which color scheme the Octopus colors belonged to, so it took its own decision from the device setting — breaking any theme decoupled from the OS. The scheme is now pinned to the palette the toolbar is painted from, and picked by which of Chrome's own content colors stays legible on it. One deliberate consequence: an already-open tab no longer re-themes itself when the system setting flips mid-session.
- Links now open with the palette the SDK screens are actually painted with. A host passing an explicit `OctopusColorScheme` to `octopusComposables(container = …)` — the shape an in-app theme toggle takes — had its links opened with the host's Material-theme colors instead.
- A `background` or `primary` slot left translucent, or never wired, is no longer forwarded to Chrome. Custom Tabs take raw ARGB and do not composite alpha, so such a slot rendered as a see-through — in practice black — toolbar; Chrome now falls back to its own default for it.
- Unreadable text on Material3 components in host apps whose Material theme darkens `background` without redefining its content colors — most visibly the profile overflow menu, where the labels were invisible. `onBackground`, `onSurface` and `onSurfaceVariant` now follow the resolved Octopus palette, which also covers unstyled leading icons and radio buttons.
- Invisible alert dialog titles, info snackbars and video controls in the same configuration. The host's `surfaceContainerHigh` is still adopted as `onHover`, but only when it is opaque and keeps both palette grays above the WCAG AA 4.5:1 contrast ratio; otherwise the palette's own elevated surface is used.

## [1.13.1](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.13.1) — 2026-07-29

### Fixed
- Dropdown and popup menus (most visibly the profile overflow menu) rendered with a transparent background in host apps whose Material theme does not set `surfaceContainer` — SDK menus now always use their own opaque surface.

## [1.13.0](https://github.com/Octopus-Community/octopus-sdk-android/releases/tag/v1.13.0) — 2026-07-24

### Breaking
- `OctopusSDK.grpcClient` removed — it exposed the SDK's internal gRPC client and was never a supported integration point. See [MIGRATING.md](MIGRATING.md).
- `OctopusDestination.About` and `OctopusEvent.ScreenDisplayed.SettingsAbout` removed along with the "About the community" screen — its legal links are already in the Activity and profile overflow menus. See [MIGRATING.md](MIGRATING.md).
- `CommentDetailsScreen` / `CommentDetailsContent` and `CurrentUserProfileEditScreen` / `CurrentUserProfileEditContent` are now `internal` — they were public by accident and had no supported bridge usage. See [MIGRATING.md](MIGRATING.md).

### Added
- Unified Profile — hand member-profile taps back to your app and enrich your own screens with community data: `onNavigateToProfile` callback (passing a non-null callback is the activation switch), read-only `OctopusCommunityData` / `OctopusGamification` via `OctopusSDK.communityDataFlow` and `OctopusSDK.fetchCommunityData`, `OctopusDestination.Activity` with `NavController.navigateToOctopusActivity(userId)` and `navigateToOctopusProfile(userId)`, plus the `…ByClientUserId` variants that address members by your host app's own user id. It activates only when the backend exposes client user ids **and** the host wired `onNavigateToProfile`; otherwise the SDK's native profile screens are kept. Includes an automatic local database migration on upgrade.
- Explicit terms acceptance: `CommunityConfig.termsAcceptanceMode` (`TermsAcceptanceMode.IMPLICIT`, the default, `EXPLICIT_MULTI_CHECKBOX`, `EXPLICIT_SINGLE_CHECKBOX`) — a community can require explicit consent to its legal documents through a sheet shown at the first contribution.
- "View group" entry in first position of the post "⋯" menu, navigating to the post's group, with the new themeable icon `OctopusIcons.Groups.viewGroup`.
- Theming: the link (URL) color and the community background color are customizable through the SDK color scheme.
- 15 new languages — including Arabic with right-to-left layout — bringing the total to 24.
- Large-screen support: content is capped and centered on tablets and other large screens instead of stretching edge-to-edge.

### Changed
- Design-system polish: 24px line height on body content, refreshed notification-bell (Activity) icon, current-user profile overflow menu uniformized with the Activity one (leading icons, visible dividers, legal links reordered).

### Fixed
- Bridge back navigation: the back chevron and system back now route to the host's `onBack` on bridge entry points (post details, group details, activity, current-user profile, post editor), and the `Octopus*Content` wrappers take an `onBack` parameter. Screens embedded directly outside `octopusComposables` no longer swallow system back.
- `OctopusGroupDetailsContent` rendered a blank screen.
- Guests no longer see "View my profile" / "Edit my profile" in the Activity overflow menu.
- Media viewer draws edge-to-edge behind the system bars.
- The New Post composer shows the author's avatar (display-only) instead of the clickable Activity bell in Unified Profile mode.

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
