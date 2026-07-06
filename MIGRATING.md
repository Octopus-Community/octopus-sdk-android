# Migrating

This guide consolidates the migration notes for the recent minor releases of the
Octopus Android SDK (`com.octopuscommunity:octopus-sdk` / `octopus-sdk-ui`).
Each section lists the breaking changes with before/after code. The most recent
version is at the top.

Full release notes (including the additive API surface of each release) live on
[GitHub Releases](https://github.com/Octopus-Community/octopus-sdk-android/releases),
and a condensed history in [CHANGELOG.md](CHANGELOG.md).

- [To 1.12.0 (from 1.11.x)](#to-1120-from-111x)
- [To 1.11.0 (from 1.10.x)](#to-1110-from-110x)

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
