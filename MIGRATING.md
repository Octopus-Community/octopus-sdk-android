# Migrating

This guide consolidates the migration notes for the recent minor releases of the
Octopus Android SDK (`com.octopuscommunity:octopus-sdk` / `octopus-sdk-ui`).
Each section lists the breaking changes with before/after code. The most recent
version is at the top.

Full release notes (including the additive API surface of each release) live on
[GitHub Releases](https://github.com/Octopus-Community/octopus-sdk-android/releases),
and a condensed history in [CHANGELOG.md](CHANGELOG.md).

- [To 1.16.0 (from 1.15.x)](#to-1160-from-115x)
- [To 1.15.0 (from 1.14.x)](#to-1150-from-114x)
- [To 1.14.0 (from 1.13.x)](#to-1140-from-113x)
- [To 1.13.2 (from 1.13.x)](#to-1132-from-113x)
- [To 1.13.0 (from 1.12.x)](#to-1130-from-112x)
- [To 1.12.0 (from 1.11.x)](#to-1120-from-111x)
- [To 1.11.0 (from 1.10.x)](#to-1110-from-110x)

---

## To 1.16.0 (from 1.15.x)
<a id="to-1160-from-115x"></a>

### Breaking — report ViewModels are internal

`ReportViewModel`, `ReportViewModel.UiState`, `ReportViewModel.Event`, `ReportContentViewModel`
and `ReportUserViewModel` are now internal to `octopus-sdk-ui`. They are owned by the SDK report sheet; hosts must remove direct imports,
subclasses and state/event handling. There are no deprecated compatibility aliases.

Before (directly referencing report implementation types):

```kotlin
import com.octopuscommunity.sdk.ui.report.common.ReportViewModel

fun isSendingReport(state: ReportViewModel.UiState): Boolean = state.isSending
```

After:

```kotlin
// Remove direct ReportViewModel references and custom state/event handling.
// Open Report from the SDK's content or profile menu; the SDK owns the sheet and its state.
```

Recompile host integrations against 1.16 after removing these references. Integrations that
already use the SDK report UI need no changes. The report and block flows keep the same behaviour.

### Breaking — deprecated Notifications icon group removed

1.16 removes `OctopusIcons.Notifications`, `OctopusIcons.notifications`,
`OctopusIconsDefaults.notifications()` and the `notifications` argument of
`OctopusIconsDefaults.icons()` directly after their WARNING deprecation in 1.15.

- **Why**: the group's only icon was no longer rendered. The notifications empty state already
  uses `screenStates.emptyNotifications`, as on iOS, and keeps its built-in illustration.
- **Recompile required**: the `OctopusIcons` constructor, `copy()` and component functions and the
  `icons()` factory have changed JVM signatures. Rebuild your app and any integration libraries
  that use them against 1.16; there is no compatibility alias.

Before (1.15.x — the override had no visible effect):

```kotlin
val icons = OctopusIconsDefaults.icons(
    notifications = OctopusIconsDefaults.notifications(
        emptyNotifications = { painterResource(R.drawable.my_empty_notifications) },
    ),
)
```

After (1.16 — the override is rendered):

```kotlin
val icons = OctopusIconsDefaults.icons(
    screenStates = OctopusIconsDefaults.screenStates(
        emptyNotifications = { painterResource(R.drawable.my_empty_notifications) },
    ),
)
```

To keep the SDK default, simply omit `notifications`. If you construct `OctopusIcons` directly or
call `copy()`, remove its `notifications` argument too. Update positional arguments and destructuring
to account for the removed fourth property. The deprecated empty-state parameters of `post()` and
`comment()` are unchanged.

### Breaking — nickname length errors belong to `NicknameError`

`ProfileUpdateError.NicknameError.CharLimitReached` now extends `NicknameError`, rather than
`BioError`. Its name, constructor and `errorMessage` remain unchanged. There is no deprecated alias
or compatibility bridge.

- **Why**: a nickname length refusal was incorrectly caught by `BioError` handlers and missed by
  `NicknameError` handlers. The error now belongs to the nickname field, matching iOS's
  `ProfileValidationError.ErrorKind.maxCharLimitReached` with `Field.nickname`.
- **Recompile required**: the public superclass changes. Rebuild host apps and any libraries using
  this type against 1.16. Add `CharLimitReached` to exhaustive `when` expressions on `NicknameError`,
  and remove it from exhaustive `when` expressions on `BioError` if present. Code compiled against
  1.15 and not rebuilt can throw `NoWhenBranchMatchedException` from an exhaustive `when` on
  `NicknameError`, and its `is NicknameError.CharLimitReached` branch inside a `when` on `BioError`
  no longer matches.

The source break is the exhaustive `when` on `NicknameError`, which no longer compiles without the
new branch:

Before:

```kotlin
fun nicknameMessage(error: ProfileUpdateError.NicknameError): String =
    when (error) {
        is ProfileUpdateError.NicknameError.AlreadyUsed -> error.errorMessage
        is ProfileUpdateError.NicknameError.BannedWords -> error.errorMessage
        is ProfileUpdateError.NicknameError.BadFormat -> error.errorMessage
        is ProfileUpdateError.NicknameError.Unknown -> error.errorMessage
    }
```

After:

```kotlin
fun nicknameMessage(error: ProfileUpdateError.NicknameError): String =
    when (error) {
        is ProfileUpdateError.NicknameError.AlreadyUsed -> error.errorMessage
        is ProfileUpdateError.NicknameError.BannedWords -> error.errorMessage
        is ProfileUpdateError.NicknameError.BadFormat -> error.errorMessage
        is ProfileUpdateError.NicknameError.CharLimitReached -> error.errorMessage
        is ProfileUpdateError.NicknameError.Unknown -> error.errorMessage
    }
```

Optionally, remove any special handling that worked around the old inheritance (this one still
compiles on 1.16):

Before:

```kotlin
when (error) {
    is ProfileUpdateError.NicknameError.CharLimitReached -> showNicknameError(error.errorMessage)
    is ProfileUpdateError.NicknameError -> showNicknameError(error.errorMessage)
    is ProfileUpdateError.BioError -> showBioError(error.errorMessage)
    else -> showOtherError(error.errorMessage)
}
```

After:

```kotlin
when (error) {
    is ProfileUpdateError.NicknameError -> showNicknameError(error.errorMessage)
    is ProfileUpdateError.BioError -> showBioError(error.errorMessage)
    else -> showOtherError(error.errorMessage)
}
```

`BioError.CharLimitReached` is unchanged and still belongs to the bio field. Nickname validation
messages and limits are unchanged.

### Breaking — legacy secured serializer is internal

`SecuredDataStore.LegacySecuredDataSerializer` is now internal to `octopus-sdk-data`.
It is a legacy storage implementation detail, not a host integration API. Remove direct
references and let the SDK own session persistence; no replacement serializer API is added.

Before (direct use of the legacy implementation):

```kotlin
import com.octopuscommunity.sdk.data.local.datastore.SecuredDataStore

val serializer = SecuredDataStore.LegacySecuredDataSerializer
```

After:

```kotlin
// Remove the serializer reference. The SDK restores and persists its session automatically.
```

Upgrading from 1.15 preserves existing sessions without host migration code.

### Breaking — data-layer cache-cleanup helpers removed

1.16 removes these public members from the published, ungated `octopus-sdk-data` module:

- `FeedDao.getAllFeedItemIds()`.
- `PostDao.getNonBridgePostIds()`, `deletePostsIn(ids)` and `deletePostsNotIn(ids)`.
- `CommentDao.getAllCommentIds()`, `deleteCommentsIn(ids)` and `deleteCommentsNotIn(ids)`.
- `ReplyDao.getAllReplyIds()`, `deleteRepliesIn(ids)` and `deleteRepliesNotIn(ids)`.
- `OctopusItemRepositoryImpl.deleteLocalItemsNotIn(ids): Int` and its implementations in
  `PostRepositoryImpl`, `CommentRepositoryImpl` and `ReplyRepositoryImpl`.

**Why**: orphan cleanup now runs atomically per table in SQLite, avoiding full-cache ID reads
and large parameter lists. Direct callers must replace the removed calls and recompile their
integrations. For orphan cleanup, use the existing domain repository method
`deleteLocalOrphanedItems()`; the domain repository API is unchanged.

Before (direct data-layer cleanup with a keep-list):

```kotlin
import com.octopuscommunity.sdk.data.repository.PostRepositoryImpl

suspend fun cleanPostCache(repository: PostRepositoryImpl, feedItemIds: List<String>) {
    repository.deleteLocalItemsNotIn(feedItemIds)
}
```

After:

```kotlin
import com.octopuscommunity.sdk.domain.repository.PostRepository

suspend fun cleanPostCache(repository: PostRepository) {
    repository.deleteLocalOrphanedItems()
}
```

The method uses references from all cached feeds, takes no caller-supplied keep-list and returns
`Unit`; remove any deleted-count handling. An empty feed cache preserves all cached content, and
bridge posts are retained. Call `deleteLocalOrphanedItems()` on `CommentRepository` or
`ReplyRepository` separately when those caches need cleanup. The constructor of
`OctopusItemRepositoryImpl` is `internal`, so only Java code can subclass it outside the SDK: such a
subclass must now implement `deleteLocalOrphanedItems(): Unit` instead of
`deleteLocalItemsNotIn(ids): Int`.

### Breaking — internal repository getters require opt-in

`OctopusSDK.reactionsRepository` and `OctopusSDK.userCommentRepository` now require
`@OptIn(InternalOctopusApi::class)` in Kotlin.

- **Why**: these SDK-internal repositories back the SDK's own "Who reacted" list and profile
  Comments tab. iOS exposes no public equivalent. Use the SDK UI to show these lists; there is no
  equivalent public use case or flow for fetching them.
- **Binary-compatible**: the getters keep their JVM signatures, so already compiled callers keep
  running and Java callers are unaffected. Kotlin source that accesses either getter must opt in
  when recompiled against 1.16.

For existing code that still needs these internals, add the opt-in at the call site. It acknowledges
that these APIs may change or be removed without notice, including in patch releases.

Before:

```kotlin
import com.octopuscommunity.sdk.OctopusSDK

fun accessRepositories() {
    val reactions = OctopusSDK.reactionsRepository
    val userComments = OctopusSDK.userCommentRepository
}
```

After:

```kotlin
import com.octopuscommunity.sdk.InternalOctopusApi
import com.octopuscommunity.sdk.OctopusSDK

@OptIn(InternalOctopusApi::class)
fun accessRepositories() {
    val reactions = OctopusSDK.reactionsRepository
    val userComments = OctopusSDK.userCommentRepository
}
```

### Breaking — `ScreenDisplayed` gained `CommunityAccessDenied`

`OctopusEvent.ScreenDisplayed.CommunityAccessDenied` is sent when the screen shown instead of the
community is displayed, matching iOS (`OctopusEvent.Screen.communityAccessDenied`). An exhaustive
`when` over `ScreenDisplayed` without an `else` branch no longer compiles. Add the new branch, or an
`else` branch so that later screens do not break it again.

Before:

```kotlin
fun screenName(event: OctopusEvent.ScreenDisplayed): String =
    when (event) {
        is OctopusEvent.ScreenDisplayed.MainFeed -> "feed"
        OctopusEvent.ScreenDisplayed.Groups -> "groups"
        // ... one branch per screen
    }
```

After:

```kotlin
fun screenName(event: OctopusEvent.ScreenDisplayed): String =
    when (event) {
        is OctopusEvent.ScreenDisplayed.MainFeed -> "feed"
        OctopusEvent.ScreenDisplayed.Groups -> "groups"
        // ... one branch per screen
        OctopusEvent.ScreenDisplayed.CommunityAccessDenied -> "community_access_denied"
    }
```

### Breaking (Java source) — `UserConfig` gained `accessDenial`

`UserConfig` carries why the access is denied, with the details of a ban, as a trailing, defaulted
`accessDenial`. Kotlin source compiles unchanged, and code compiled against 1.15.x keeps linking:
the previous constructor and `copy` stay in the binary, hidden from source. Java source, which sees
no default values, passes the new argument; `null` keeps the previous meaning.

Before (Java):

```java
UserConfig config = new UserConfig(false, "Not available yet");
```

After (Java):

```java
UserConfig config = new UserConfig(false, "Not available yet", null);
```

### Breaking — `CommunityAccessDeniedViewModel.UiState` no longer takes `accessDeniedMessage`

The screen shown instead of the community now shows a title, a subtitle and a button that depend on
the reason of the denial, and its state in `octopus-sdk-ui` holds those. `accessDeniedMessage` is
no longer a constructor or `copy` argument of `UiState`, so code that builds or copies one with it
no longer compiles. Reading it still compiles with a deprecation warning, for one release: it gives
the message of the server, the sentences of a ban, or the title when there is no message. The
message alone no longer describes the screen, so host code that read the state of this SDK screen
reads the reason from the entry point instead.

Before:

```kotlin
val message: UiText = viewModel.state.value.accessDeniedMessage
```

After:

```kotlin
val reason: Flow<CommunityAccessDeniedReason?> = OctopusSDK.communityAccessDeniedReason
```

### Breaking (binary only) — `OctopusIcons.Content` gained `translation`

The content icon set in `octopus-sdk-ui` carries the icon shown before the "See original" / "See
translation" toggle, as a trailing, defaulted `translation` on `OctopusIcons.Content` and on
`OctopusIconsDefaults.content()`. As with `clearText` in 1.15.0, Kotlin keeps no overload for the
old parameter list: source that builds or copies an `OctopusIcons.Content`, positionally or by
name, compiles unchanged, Java source passes the new argument, and a binary compiled against 1.15.x
that does — typically a host's theme setup, test fixture or preview — throws `NoSuchMethodError`
once this version is on the classpath. Recompile it.

### Breaking (binary only) — repositories that read the closure of the community

The SDK now stops its calls while the community is closed, and the repositories that make or gate
them read that closure. In `octopus-sdk-data`, `CommunityConfigRepositoryImpl`,
`ConnectionRepositoryImpl`, `SSOConnectionRepositoryImpl`, `ProfileRepositoryImpl`,
`SessionRepositoryImpl`, `TrackerRepositoryImpl` and `UserRepositoryImpl` take a defaulted
`closureRepository` constructor parameter, and `ApiKeyService.getConfig()` a defaulted
`bypassingCallsGate` parameter. In `octopus-sdk-ui`, `OctopusViewModel.CommonParameters` takes a
defaulted `closureRepository`. Kotlin keeps no overload for the old parameter lists: source that
calls them compiles unchanged, Java source passes the new arguments, and a binary compiled against
1.15.x that calls them directly — a host that builds these classes itself, or a test fixture —
throws `NoSuchMethodError` once this version is on the classpath. Recompile it. A host that only
goes through `OctopusSDK` is not concerned.

### Breaking — `octopus-sdk-data` cache entities and DAOs

These concern only a host that builds the SDK's Room entities or implements its DAOs itself,
typically in a test fixture; a host that goes through `OctopusSDK` is not concerned.

- Entities (binary only): `PublicProfileEntity` gains a trailing, nullable `gamificationScore`;
  `UserConfigEntity` five trailing, defaulted properties for the reason of a denied access and the
  details of a ban; `CommunityConfigEntity` a defaulted `informalLanguages`, and its content
  options `postImageTapOpensPost` and `postGroupNameTapOpensGroup`. Kotlin source compiles
  unchanged; Java source passes the new arguments; a binary compiled against 1.15.x that builds or
  copies one throws `NoSuchMethodError` until recompiled.
- DAOs (source): several DAO interfaces gain abstract members that a direct implementation must
  add — `ProfileDao.updatePublicProfileGamification`; `CommentDao.getFeedComments`,
  `updateCommentReaction` and `deleteOrphanedComments`; `FeedDao.getRefreshPosts`,
  `getRefreshComments`, `getRefreshReplies`, `getMaxPosition`, `getMinPosition` and
  `deleteFeedItemsMarked`; `PostDao.deleteOrphanedPosts`; `ReplyDao.deleteOrphanedReplies`;
  `TrackerEventDao.insertAppSessionEndReceipt`. For instance, for `ProfileDao`:

Before:

```kotlin
class FakeProfileDao : ProfileDao {
    // 1.15.x members
}
```

After:

```kotlin
class FakeProfileDao : ProfileDao {
    // 1.15.x members

    override suspend fun updatePublicProfileGamification(
        userId: String,
        gamificationLevelIndex: Int?,
        gamificationScore: Int?,
    ) {
        // Update the level and score of a cached public profile already marked as gamified.
    }
}
```

### Behaviour change — overrides of the texts that address the member

The SDK now reads each text that addresses the member from a `_formal` and an `_informal`
resource, to follow the tone a community sets per language. The unsuffixed resource of these texts
is no longer read, so a host override of it stops applying, silently. The concerned keys are the
ones the SDK's `values/strings.xml` defines with both suffixes. Move each such override to both
variants.

Before (host `res/values/strings.xml`):

```xml
<string name="error_unknown">Something went wrong, try again.</string>
```

After:

```xml
<string name="error_unknown_formal">Something went wrong, please try again.</string>
<string name="error_unknown_informal">Something went wrong, try again.</string>
```

### Deprecations — multi-photo UI and mapper compatibility

`PostMedia.Images(medias)` holds every image of a post, in order. The deprecated
`PostMedia.Image(media)` remains available to Kotlin and Java hosts, including its constructor,
`media` property and `copy`. Migrate new rendering code to `PostMedia.Images` and handle its
`medias` list; the SDK mapper now returns this multi-photo model.

`CreatePostViewModel.UiState` adds `isPrefilled`, `communityOptionsResolved`, `faultyImage` and
`isReadingImages`. Its 1.15.x constructor and `copy` signatures (18 arguments), plus their
pre-multi-photo equivalents (19 arguments), remain as compatibility overloads, including their default
argument bridges. They do not emit deprecation warnings on ordinary calls. Copying through the old
overload preserves the new fields.
`OctopusDestination.CreatePost` likewise retains its six-argument constructor and `copy`, visible
to Kotlin and Java. Prefer its `prefilledImageUris` parameter for new integrations.

`CreatePostViewModel.updatePicture` and `applyPrefilledImage` are deprecated: use `addPictures` /
`removePicture` and `applyPrefilledImages`.

`PostMapper.toProto()` for `Post.Draft` and `ClientPost`, and `PostMapper.toFingerprint()` for
`ClientPost`, retain their public signatures as deprecated compatibility paths. They encode every image from
`images` (whether supplied through the legacy field or `medias`) in display order, preserving the
legacy encoding and joining fingerprint image values with the backend separator. Use the post repository or
`OctopusSDK.fetchOrCreateClientObjectRelatedPost` for the upload flow that supports all images.

### Behaviour change — client post images are no longer resized

The local images of a `ClientPost` are uploaded as the host supplies them: Android no longer resizes
them to 1500 px nor re-encodes them before hashing and sending them, as on iOS. The bridge
fingerprint is now the one the backend JWT documentation specifies, computed on those bytes, so a
host backend can compute it from its own copy of each image. Pass images no larger than you want
stored and served, in JPEG or PNG. `ClientPost.IMAGE_UPLOAD_MAX_SIZE` and
`ClientPost.IMAGE_UPLOAD_QUALITY` are deprecated with no replacement: they are no longer applied.

### Deprecations — single image fields

`ClientPost.attachment`, `Post.Draft.media`, `OctopusPrefilledPost.image` and
`OctopusDestination.CreatePost.prefilledImageUri` are deprecated: use `medias`, `medias`, `images`
and `prefilledImageUris`. Setting both the old and the new field of an `OctopusPrefilledPost`, a
`ClientPost` or a `Post.Draft` throws `IllegalArgumentException`. `ClientPost.images` and
`Post.Draft.images` read the images whichever field set them, and so does
`OctopusPrefilledPost.images`. The constructors these classes had keep compiling, in Kotlin and in
Java, `new OctopusPrefilledPost(text, null, topicId, cta)` included.

More than 10 images throws `OctopusPrefilledPost.TooManyImagesError`. It extends
`IllegalArgumentException` but is not an `OctopusPrefilledPost.ValidationError`, so a `when` over
`ValidationError` does not cover it.

Before:

```kotlin
ClientPost(objectId = "recipe-1", text = "Our recipe", attachment = Resource.Remote(url))
```

After (pass `viewObjectButton` explicitly, `null` for no button, to reach the non-deprecated
constructor):

```kotlin
ClientPost(
    objectId = "recipe-1",
    viewObjectButton = null,
    text = "Our recipe",
    medias = listOf(Resource.Remote(url1), Resource.Remote(url2)),
)
```

### Breaking — repository and UI state changes

`GroupRepository.fetchGroup` has a default body for Kotlin source. Java implementations must implement it; Kotlin implementations compiled against 1.15 must be recompiled.

`CommentDetailsViewModel.UiState` adds `hasLoadFailure` and `loadFailureIsNoNetwork` after `isRefreshing`. Update positional constructor and `copy` calls past that argument and destructuring, and recompile affected binaries. Named calls remain source-compatible.

`OctopusItemsListViewModel.UiState` and `OctopusItemsListViewModel` have changed constructor signatures; `FeedService.initializeFeed()` takes an anchor parameter. `OctopusItemInputFieldViewModel.UiState` also gained a property. Recompile binaries that directly construct, copy or call these declarations.

### Removed and unused string resources

Direct `R.string` references to removed resources must be deleted. These include the former `connection_magic_link_*`, `settings_profile_delete_account_*`, `validate_nickname_*` strings and unused empty-state strings, plus `accessibility_view_count_count`, `content_aggregated_info_comment_count`, `content_aggregated_info_view_count` and `content_aggregated_info_reaction_count_empty`. Delete overrides that are no longer read; move an override of `content_aggregated_info_like` to `content_aggregated_info_reaction_heart`. `post_create_incentive_button5` is no longer read.

### Build requirements — compileSdk 36

All four published modules now compile against Android 16 (API 36). Host apps must use
`compileSdk` 36 or higher and Android Gradle Plugin (AGP) 8.9.1 or higher, the
[minimum supported AGP for API 36](https://developer.android.com/build/releases/about-agp#api-level-support).
Keep a newer AGP version if your project already uses one.

Update the host app's `build.gradle.kts`:

Before:

```kotlin
android {
    compileSdk = 35
}
```

After:

```kotlin
android {
    compileSdk = 36
}
```

If needed, update the AGP version in the root build script or version catalog to at least
8.9.1, together with its required Gradle version. The SDK's `minSdk` stays at 23, and its
libraries do not set `targetSdk`; this change does not require a host `targetSdk` change.

---

## To 1.15.0 (from 1.14.x)

### Breaking — host-owned authentication only

Your app now owns sign-in, sign-out and account deletion. All integrations use
`ConnectionMode.SSO`; `OctopusSDK.initialize()` and `switchCommunity()` require
an explicit `connectionMode`, even if you previously relied on the SSO default.
Recompile the host and prebuilt integrations: old binaries that omitted this
argument fail at runtime even though `initialize$default` retains its JVM signature.

```kotlin
// Before (1.14.x)
OctopusSDK.initialize(
    context = applicationContext,
    apiKey = "YOUR_API_KEY",
    connectionMode = ConnectionMode.OctopusAuth,
)
// Or initialize(context = applicationContext, apiKey = "YOUR_API_KEY")

// After
OctopusSDK.initialize(
    context = applicationContext,
    apiKey = "YOUR_API_KEY",
    connectionMode = ConnectionMode.SSO(),
)
OctopusHomeScreen(navController = navController, onNavigateToLogin = { openAppLogin() })
// connectUser, disconnectUser and switchCommunity suspend: call them from a coroutine.
// Once your app's user is signed in:
OctopusSDK.connectUser(ClientUser(userId = signedInUser.id)) { backendToken() }
// When your app's user signs out:
OctopusSDK.disconnectUser()
// A community switch also needs an explicit mode:
OctopusSDK.switchCommunity(
    context = applicationContext,
    apiKey = "OTHER_API_KEY",
    connectionMode = ConnectionMode.SSO(),
)
```

Remove the email link APIs (`MagicLinkRequest`, `SendMagicLinkError`,
`MagicLinkError`), `ProfileRepository.requestProfileDelete`, `DeleteProfileReason`
and their fake equivalents. Remove navigation to `ConnectionGraph`,
`MagicLinkEmail`, `MagicLinkConfirmation`, `Settings`, `ProfileSettings` and
`DeleteAccount`. Deep-link sub-paths `settings`, `settings/profile`,
`settings/delete-account`, `magic-link/email` and `magic-link/confirm` are gone.
Use your app's account flows; SDK profile editing, nickname validation, legal
links and reporting remain.

Pending email link requests are cleared at startup; existing sessions remain.
Call `connectUser` after initialization whenever your app has a signed-in user,
including after a stored session is restored: `refreshEntitlements` returns
`NoClientTokenProvider` until a token provider has been registered.

`ScreenDisplayed.SettingsList`, `SettingsAccount` and `DeleteAccount` remain
warning-deprecated but are no longer emitted. Replace the old settings icon factory:

```kotlin
// Before
OctopusIconsDefaults.settings(
    account = accountIcon, help = helpIcon, info = infoIcon,
    logout = logoutIcon, deleteAccountWarning = warningIcon,
)
// After
OctopusIconsDefaults.settings(help = helpIcon, info = infoIcon)
```

### Breaking — minimum Android API raised to 23

All four published modules require Android API 23 or higher. Raise a host app's
`minSdk` from 21 or 22 in its app module:

```kotlin
// Before (build.gradle.kts)
android { defaultConfig { minSdk = 21 } } // or 22
// After
android { defaultConfig { minSdk = 23 } }
```

The upgraded app no longer supports API 21 or 22. Do not bypass the requirement
with `tools:overrideLibrary`: SDK code calls API 23 methods unconditionally.
Apps already using `minSdk` 23 or higher need no configuration change.

### Breaking — post render model and nickname implementation

The UI render model replaces `PostAttachment` and
`PostContent.Published.attachment` with independent media and poll fields, so an
image and a poll can coexist. If you used this unsupported implementation model:

```kotlin
// Before (1.14.x)
when (val attachment = (uiModel.content as PostContent.Published).attachment) {
    is PostAttachment.Image -> showImage(attachment.media)
    is PostAttachment.Video -> showVideo(attachment.media)
    is PostAttachment.Poll -> showPoll(attachment.poll)
    null -> Unit
}
// After
val published = uiModel.content as PostContent.Published
when (val media = published.media) {
    is PostMedia.Image -> showImage(media.media)
    is PostMedia.Video -> showVideo(media.media)
    null -> Unit
}
published.poll?.let { showPoll(it.poll) }
```

`ValidateNicknameViewModel` and `ValidateNicknameContent` are now internal;
supported integrations continue using navigation destinations and `Octopus*`
wrappers. `ValidateNicknameScreen` keeps its signature. A direct navigation to
`OctopusDestination.ValidateNickname` with no contribution waiting closes at once.
The screen now runs before the first contribution, which is sent after confirmation.

`OctopusViewModel.checkAuthenticated(action, block)` now takes a block returning `Unit`: a lambda
ending in `true`/`false` still compiles, but explicit `return@checkAuthenticated true/false` returns
and stored callbacks typed `suspend (CurrentUserProfile) -> Boolean` must be updated.

### Breaking — recompile against 1.15.0 and update positional entity calls

These declarations gained defaulted parameters. Kotlin source keeps compiling with the new
trailing, defaulted parameters, except for two positional constructor or `copy()` cases:
`CommunityConfigEntity.forceNicknameChoice` was inserted before `poweredByOctopus`, and
`CreatePostViewModel.UiState.textRequired` before `termsAcceptanceMode`. Calls passing
`poweredByOctopus` or a later entity argument, or `termsAcceptanceMode` or a later UI-state
argument, by position must name those arguments or insert the new Boolean. Prebuilt code that
constructs, copies or calls them can throw `NoSuchMethodError`. Recompile it against 1.15.0;
Java callers pass the new arguments explicitly.

| Declaration | New parameters |
|---|---|
| `CommunityConfig` | `forceNicknameChoice: Boolean = false`, `consentTexts: CommunityConfig.ConsentTexts? = null` |
| `CommunityConfigEntity` | `forceNicknameChoice: Boolean = false` before `poweredByOctopus`; trailing `consentTexts: CommunityConfigEntity.ConsentTextsEntity? = null` |
| `CreatePostViewModel.UiState` | `textRequired` and `consentTexts` |
| `OctopusItemInputFieldViewModel.UiState<U>` | `consentTexts` |
| `OctopusIcons` and `OctopusIconsDefaults.icons()` | `clearText` |
| `OctopusItemInputField` | `onNavigateToUrl: (String) -> Unit = {}` |

`ProfileRepository` (including its subinterface `ConnectionRepository`) and
`CommunityConfigRepository` gained methods with default bodies: the four-argument
`ProfileRepository.updateProfile`, `CommunityConfigRepository.debugOverrideForceNicknameChoice`
and `CommunityConfigRepository.debugOverrideConsentTexts`. Calling a new method on an
implementation outside the SDK compiled against 1.14.x throws `AbstractMethodError` until
recompiled; a Java implementation must implement all new methods on its interface.

If you embed `OctopusItemInputField`, wire the community consent links explicitly;
the default URL callback does nothing:

```kotlin
OctopusItemInputField(
    // … existing arguments …
    onNavigateToUrl = { url -> openUrl(url) },
)
```

### Breaking (Java source and outside implementations) — default avatars

`MinimalProfile` (and therefore `Profile`) gains `defaultAvatar: DefaultAvatar?`
with a default getter returning `null`. `Author` and `OtherUserProfile` gain a
trailing `defaultAvatar`; `CurrentUserProfile` also gains
`defaultAvatarIfPictureRemoved`. All constructor parameters default to `null`.

The previous constructors and `copy` methods of those three data classes remain
as hidden members: existing binaries keep linking, and Kotlin source compiles
unchanged. Java constructor and `copy` calls must pass the new arguments;
`null` retains the previous behavior.

Recompile any outside implementation of `MinimalProfile` or `Profile` to avoid
`AbstractMethodError` when the SDK reads `defaultAvatar`. A Java implementation
must add `getDefaultAvatar()`; returning `null` retains the computed avatar.
Keep an `else` branch when matching the sealed `DefaultAvatar` interface
(`Color`, `Image`), so future avatar styles can be handled.

### Behaviour change — default navigation transitions
<a id="behaviour-change--default-navigation-transitions"></a>

`octopusComposables` now defaults `transitions` to `null`. SDK screens inherit
your `NavHost` transitions, including post creation and media screens; with no
host customization, Navigation Compose supplies its cross-fade. Customize your
`NavHost`'s `enterTransition`, `exitTransition`, `popEnterTransition` and
`popExitTransition` to apply the same motion across your app.

```kotlin
// Before: SDK slide and fade
octopusComposables(navController = navController /* , … */)
// After: same call inherits the host's transitions.
// To keep the previous SDK slide and fade explicitly:
octopusComposables(
    navController = navController,
    transitions = OctopusTransitionsDefaults(),
    // …
)
```

`OctopusTransitions`, `OctopusTransitionsDefaults` and
`OctopusTransitionDefaults.Horizontal` / `Vertical` / `Fade` remain available.
For predictive-back previews on Android 14+ with gesture navigation, opt in
on the host's application element:

```xml
<application android:enableOnBackInvokedCallback="true" ... >
```

Apps targeting API 36 get this by default on Android 16; set it explicitly to
cover Android 14 and 15. Without it, back still works but the close animation
plays after the gesture. Draft and unsaved-profile confirmation stays available.

At the root of the SDK back stack, system back invokes your `onBack` with no
predictive preview. The default `navigateUp()` does nothing at that root. If you
open the SDK directly on a sub-screen, pass your host exit action:

```kotlin
octopusComposables(
    navController = navController,
    onBack = { activity.finish() },
    // Or: onBack = { hostNavController.popBackStack() },
    // …
)
```

### Lifecycle and resource overrides

After `OctopusSDK.stop()` or `switchCommunity()`, rebuild your SDK navigation
entries: existing ViewModels stay bound to the closed SDK container.

Move any `accessibility_common_close` string override to `common_close`.
The removed `ic_octopus_congrats` drawable and unused
`validate_nickname_action_later`, `validate_nickname_action_modify`,
`validate_nickname_modification`, `validate_nickname_nickname` and
`validate_nickname_title` overrides can be deleted. Review overrides of the
reworded report reasons, "Terms of Use" and "Powered by" strings.

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
