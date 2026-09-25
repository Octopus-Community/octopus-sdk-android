# Migrating

This guide consolidates the migration notes for the recent minor releases of the
Octopus Android SDK (`com.octopuscommunity:octopus-sdk` / `octopus-sdk-ui`).
Each section lists the breaking changes with before/after code. The most recent
version is at the top.

Full release notes (including the additive API surface of each release) live on
[GitHub Releases](https://github.com/Octopus-Community/octopus-sdk-android/releases),
and a condensed history in [CHANGELOG.md](CHANGELOG.md).

- [To 1.14.0 (from 1.13.x)](#to-1140-from-113x)
- [To 1.13.2 (from 1.13.x)](#to-1132-from-113x)
- [To 1.13.0 (from 1.12.x)](#to-1130-from-112x)
- [To 1.12.0 (from 1.11.x)](#to-1120-from-111x)
- [To 1.11.0 (from 1.10.x)](#to-1110-from-110x)

---

## To 1.14.0 (from 1.13.x)

### Breaking — `GuestError` gained a `UserBanned` variant

`connectAsGuest()` can now fail with `GuestError.UserBanned` when the member is
banned. Its `errorMessage` is written and localized by the server, and is meant
to be shown as-is. `GuestError` used to have a single variant, so an exhaustive
`when` over it compiled with one branch; it no longer does.

```kotlin
// Before (1.13.x)
val message = when (error) {
    is GuestError.Unknown -> genericRetryMessage()
}

// After
val message = when (error) {
    is GuestError.UserBanned -> error.errorMessage // display verbatim
    is GuestError.Unknown -> genericRetryMessage()
}
```

Use `else ->` if you do not want to track future variants. Code compiled against
1.13.x with an exhaustive `when` expression throws `NoWhenBranchMatchedException`
once a ban is reported, so rebuild it.

### Breaking (binary only) — recompile against 1.14.0

These types gained a trailing parameter with a default value:

| Type | New trailing parameter |
|---|---|
| `CommunityConfig` | `showCommentsOnOtherProfiles` |
| `CurrentUserProfile` | `commentsFeeds` |
| `OtherUserProfile` | `commentsFeeds` |
| `OctopusIcons` and `OctopusIconsDefaults.icons()` | `screenStates` |

Kotlin source that builds or copies them compiles unchanged:

```kotlin
// Compiles against 1.13.x and 1.14.0 alike — the new parameter takes its default.
val profile = fakeProfile.copy(nickname = "Ada")
```

A binary compiled against 1.13.x — test fixtures, previews, or a prebuilt
library depending on the SDK — throws `NoSuchMethodError` on the first such call
once 1.14.0 is on the classpath: recompile it. Java callers pass the new argument
explicitly. The `Profile` interface also gained `commentsFeeds`; a class of yours
implementing it must be recompiled (and a Java one must add `getCommentsFeeds()`).

### Behaviour change — profile and Activity tab indices shifted

The new Comments tab sits in the middle of both tab bars:

| Destination | 0 | 1 | 2 |
|---|---|---|---|
| `CurrentUserProfileSummary` (before) | Posts | Notifications | — |
| `CurrentUserProfileSummary` (after) | Posts | **Comments** | Notifications |
| `Activity` (before) | Notifications | Posts | — |
| `Activity` (after) | Notifications | **Comments** | Posts |

Nothing fails to compile, but a raw index lands on a different tab:

```kotlin
// Before: opened Notifications. After: opens Comments.
navController.navigate(OctopusDestination.CurrentUserProfileSummary(selectedTabIndex = 1))

// Do this instead — stable across releases:
navController.navigate(OctopusDestination.CurrentUserProfileSummary.Notifications)
```

### Removed string resources

If you override SDK strings, these nine keys are no longer used and their
overrides can be deleted: `notifications_list_empty`,
`post_create_incentive_button1`, `post_create_incentive_button2`,
`post_create_incentive_button3`, `post_create_incentive_button4`,
`post_create_incentive_button6`, `post_create_incentive_explanation`,
`post_list_empty`, `post_list_other_user_empty`.

---

## To 1.13.2 (from 1.13.x)

### Breaking — `OctopusDateField` removed

`OctopusDateField` was public by accident. It backed the birth-date input of the
"consent over 16" profile-creation screen and has had no caller since that screen
was removed in 1.6.0. It sat on the SDK's field-primitive layer, which is
internal by design, and was never a supported entry point: Octopus screens are
composed through navigation destinations and the `Octopus*` wrappers, not through
individual field composables.

```kotlin
// Before (1.13.x) — never a supported use case
OctopusDateField(value = date, onValueChange = { date = it })

// After
// No replacement. Material3's DatePickerDialog is the underlying picker, but it is
// not a drop-in: the removed component also applied the Octopus typography and
// colors, wrapped the dialog in the SDK's locale override (a dialog opens its own
// window, which does not inherit it), and used the SDK's localized OK/Cancel
// labels. Reproduce whichever of those you need.
```

### Breaking — `ValidateDate`, `DateValidationError` and `Date.calculateAge()` removed

All three were leftovers of that same age-consent flow, `public` but unreachable
from any supported integration point. `Date.calculateAge()` was additionally
`@RequiresApi(26)` in a library whose `minSdk` is 21, so it could not be called
safely across the supported range.

```kotlin
// Before (1.13.x) — never a supported use case
val error = ValidateDate()(date = birthDate, minAge = 16)
val age = birthDate.calculateAge()

// After
// No replacement — the age-consent flow itself was removed in 1.6.0.
```

If your app needs an age computation, it is app-side code now: `calculateAge()`
was a one-line `ChronoUnit.YEARS.between(...)`, which is API 26+ — below that,
compute it from `Calendar` fields.

`ValidationError` and the other validators (`ValidateEmail`, `ValidateImage`,
`ValidateText`) are unaffected.

---

## To 1.13.0 (from 1.12.x)

> **Known limitation (non-breaking)** — `OctopusDestination.ProfileSummary` and
> `OctopusDestination.Activity` gained a `clientUserId` parameter, turning their
> single required `userId` path argument into optional query arguments. This is
> source- and binary-compatible, so existing calls keep working with no change.
> The only theoretical edge case: such a destination persisted on the saved back
> stack under 1.12.x, restored after an in-place upgrade to 1.13.0 following a
> process death, could fail to restore. No action needed unless observed.

### Breaking — `OctopusSDK.grpcClient` removed

`OctopusSDK.grpcClient` exposed the SDK's internal gRPC client. It was public by
accident and was never a supported integration point: there is no supported way
to call the community gRPC API directly. It is now private.

```kotlin
// Before (1.12.x) — never a supported use case
val client = OctopusSDK.grpcClient

// After (1.13.0)
// No replacement — use the public OctopusSDK API surface instead.
```

### Breaking — "About the community" screen removed (`OctopusDestination.About` + `ScreenDisplayed.SettingsAbout`)

The standalone About screen was a redundant hop: its three legal links
(Community Guidelines, Privacy Policy, Terms of Use) were already duplicated in
both the Activity and the current-user Profile overflow menus. The screen, its
`OctopusDestination.About` navigation destination (with its `settings/about`
deep link), and its `OctopusEvent.ScreenDisplayed.SettingsAbout` analytics event
are all gone.

```kotlin
// Before (1.12.x)
navController.navigate(OctopusDestination.About)

// After (1.13.0) — no replacement call needed: the legal links are already
// reachable from the Activity / current-user Profile "…" overflow menus.
```

If you branch on `ScreenDisplayed` in an analytics listener, drop the
`SettingsAbout` case — it no longer exists:

```kotlin
// Before (1.12.x)
when (event) {
    is OctopusEvent.ScreenDisplayed.SettingsAbout -> track("about")
    // …
}

// After (1.13.0) — the case is gone; remove it from your `when`.
```

### Breaking — `CommentDetailsContent` / `CurrentUserProfileEditContent` are now internal

`CommentDetailsScreen` / `CommentDetailsContent` and
`CurrentUserProfileEditScreen` / `CurrentUserProfileEditContent` were public by
accident — leftovers that pre-date the `Octopus*` bridge wrapper pattern
(`OctopusGroupDetailsContent`, `OctopusPostDetailsContent`,
`OctopusCreatePostScreen`, …). They required an internal `NavHostController`
wired to the SDK's own navigation graph and had no supported bridge usage. They
are now `internal`.

```kotlin
// Before (1.12.x) — never a supported use case
CommentDetailsContent(navController = navController, commentId = commentId)
CurrentUserProfileEditContent(navController = navController)

// After (1.13.0)
// No replacement — these were reached exclusively through the SDK's own
// navigation destinations, which is still the supported way to display them.
```

---

## To 1.12.0 (from 1.11.x)

### Breaking — `ReportContent` / `ReportUser` navigation destinations removed

Reporting is no longer a separate navigation destination. The screens that show
content or profiles (post details, comment lists, profile menu, …) now host the
report flow themselves as a Material 3 bottom sheet, driven by the new
`ReportTarget` type (`ReportTarget.Content` / `ReportTarget.Profile`) — you no
longer navigate to a report screen at all.

If your app pushed these destinations directly, remove the navigation calls:

```kotlin
// Before (1.11.x) — host-driven navigation to the report screens
navController.navigate(
    OctopusDestination.ReportContent(contentId = contentId, contentKind = contentKind)
)
navController.navigate(
    OctopusDestination.ReportUser(userId = userId)
)

// After (1.12.0) — nothing to call.
// The report bottom sheet is opened by the SDK's own screens from their
// item menus; there is no host-facing entry point to replace these calls.
```

Also remove any `OctopusDestination.ReportContent` / `OctopusDestination.ReportUser`
branches from exhaustive `when` blocks over `OctopusDestination` — they no longer
compile.

### Breaking — `setReaction(reaction, clientObjectRelatedPostId)` removed

Use `OctopusSDK.setReaction(reaction, postId)`, which works on any post
(bridge or community).

```kotlin
// Before (1.11.x)
OctopusSDK.setReaction(
    reaction = reaction,
    clientObjectRelatedPostId = "post-id",
)

// After (1.12.0)
OctopusSDK.setReaction(
    reaction = reaction,
    postId = "post-id",
)
```

### Breaking — `SetReactionOnBridgePostError` removed

Handle `SetReactionError` instead (cases: `UnknownReaction`, `PostNotFound`,
`ReactionError(message)`). Network and authentication failures are surfaced
separately as `OctopusResult.Failure.Connection.*`, per the Android SDK
convention.

```kotlin
// Before (1.11.x)
when (val e = (result as OctopusResult.Failure.InvalidArguments).error) {
    SetReactionOnBridgePostError.PostIsNotABridge -> { /* … */ }
    SetReactionOnBridgePostError.PostNotFound -> { /* … */ }
}

// After (1.12.0)
when (val e = (result as OctopusResult.Failure.InvalidArguments).error) {
    SetReactionError.PostNotFound -> { /* … */ }
    SetReactionError.UnknownReaction -> { /* … */ }
    is SetReactionError.ReactionError -> { /* … */ }
}
```

---

## To 1.11.0 (from 1.10.x)

### Breaking — `ClientPost.topicId` renamed to `groupId`

```kotlin
// Before (1.10.x)
ClientPost(objectId = "…", text = "…", topicId = "…")

// After (1.11.0)
ClientPost(objectId = "…", text = "…", groupId = "…")
```

> Kotlin data classes do not support deprecated parameter aliases, so this
> rename is a hard breaking change on Android (unlike on iOS, where `topicId`
> remains as a deprecated init parameter).

### Deprecated — "Topic" terminology renamed to "Group"

The following still compile with deprecation warnings; migrate at your own
pace before they are removed in a future major:

| Deprecated | Replacement |
| --- | --- |
| `Topic` | `OctopusGroup` (kept as a `typealias`) |
| `OctopusSDK.topics` | `OctopusSDK.groups` |
| `OctopusSDK.fetchTopics()` | `OctopusSDK.fetchGroups()` |
| `OctopusSDK.followTopic()` / `unfollowTopic()` | `OctopusSDK.syncFollowGroups(actions)` |
