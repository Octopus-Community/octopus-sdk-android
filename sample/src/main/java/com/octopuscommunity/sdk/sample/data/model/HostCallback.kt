package com.octopuscommunity.sdk.sample.data.model

/**
 * One host callback the sample can wire or leave out, named the way the public SDK doc names it.
 *
 * The doc name comes first and the API symbol second on every surface that is not a
 * scenario's API pill: a tester or a PO reads "Open your sign-in screen", a developer still finds
 * `onNavigateToLogin` under it. One source, so Host app integration and Debug info cannot drift
 * into describing the same switch differently.
 *
 * The effect lines describe what a tester observes when the callback is wired or not. "Not wired"
 * means the sample hands the SDK `null` for that callback, so the SDK falls back to its own
 * behaviour — that is what a host that never implemented it would ship.
 */
enum class HostCallback(
    /** Stable short id, used in test tags (`host-integration-<id>-switch`). */
    val id: String,
    /** The public doc's name for the use case. */
    val docName: String,
    /** The SDK symbol, shown as secondary text. */
    val apiName: String,
    val effectWhenWired: String,
    val effectWhenNotWired: String,
) {
    Profile(
        id = "profile",
        docName = "Route profile taps to your own profile screen",
        apiName = "onNavigateToProfile",
        effectWhenWired = "Tapping a member opens the app's own profile screen.",
        effectWhenNotWired =
            "Unified Profile needs this callback and the backend flag; without it, taps open the Octopus profile.",
    ),
    ProfileEdit(
        id = "profile-edit",
        docName = "App profile editor",
        apiName = "onNavigateToProfileEdit",
        effectWhenWired = "Editing a field the app manages opens the app's profile editor.",
        effectWhenNotWired = "Not handled by the app: the SDK falls back to its own behaviour.",
    ),
    Login(
        id = "login",
        docName = "Open your sign-in screen",
        apiName = "onNavigateToLogin",
        effectWhenWired = "When sign-in is needed, the app's sign-in screen opens.",
        effectWhenNotWired = "Not handled by the app: the SDK falls back to its own behaviour.",
    ),
    ClientObject(
        id = "client-object",
        docName = "Handle taps on the content button",
        apiName = "onNavigateToClientObject",
        effectWhenWired = "Tapping a post's content button reaches the app (a message confirms it).",
        effectWhenNotWired = "Not handled by the app: the SDK falls back to its own behaviour.",
    ),
    GroupAccessDenied(
        id = "group-access-denied",
        docName = "Gate access to locked groups",
        apiName = "onGroupAccessDenied",
        effectWhenWired = "Opening a locked group lets the app answer with its own dialog.",
        effectWhenNotWired = "Not handled by the app: the SDK falls back to its own behaviour.",
    ),
    ;

    fun effectLine(wired: Boolean): String = if (wired) effectWhenWired else effectWhenNotWired

    fun isWired(callbacks: SDKConfig.HostCallbacks): Boolean =
        when (this) {
            Profile -> callbacks.profileWired
            ProfileEdit -> callbacks.profileEditWired
            Login -> callbacks.loginWired
            ClientObject -> callbacks.clientObjectWired
            GroupAccessDenied -> callbacks.groupAccessDeniedWired
        }

    /**
     * Why the SDK refuses to run without this callback under [config], or null when it is optional.
     *
     * `OctopusScreenContent` throws `MissingOnNavigateToLoginException` in SSO mode and
     * `MissingOnNavigateToProfileEditException` once a profile field is app-managed, the moment the
     * community opens. The sample keeps those wired whatever is stored
     * ([SDKConfig.effectiveHostCallbacks]) instead of offering a switch that crashes it.
     */
    fun requiredReason(config: SDKConfig): String? =
        when (val auth = config.authKind) {
            is SDKConfig.AuthKind.SSO ->
                when (this) {
                    Login -> "Required in SSO mode: the community does not open without it."
                    ProfileEdit ->
                        if (auth.appManagedFields.isNotEmpty()) {
                            "Required while the app manages profile fields (Unified profile)."
                        } else {
                            null
                        }
                    Profile, ClientObject, GroupAccessDenied -> null
                }
        }

    fun withWired(
        callbacks: SDKConfig.HostCallbacks,
        wired: Boolean,
    ): SDKConfig.HostCallbacks =
        when (this) {
            Profile -> callbacks.copy(profileWired = wired)
            ProfileEdit -> callbacks.copy(profileEditWired = wired)
            Login -> callbacks.copy(loginWired = wired)
            ClientObject -> callbacks.copy(clientObjectWired = wired)
            GroupAccessDenied -> callbacks.copy(groupAccessDeniedWired = wired)
        }

    companion object {
        /** Doc name of the URL policy, the sixth host decision (`onNavigateToUrl`). */
        const val URL_DOC_NAME = "Intercept URL openings"
        const val URL_API_NAME = "onNavigateToUrl"
    }
}

/** Human label of a URL policy, as Host app integration and Debug info show it. */
val SDKConfig.UrlHandling.label: String
    get() =
        when (this) {
            SDKConfig.UrlHandling.Default -> "Sample rule (default)"
            SDKConfig.UrlHandling.ForceHandledByApp -> "Always open in the app"
            SDKConfig.UrlHandling.ForceHandledByOctopus -> "Always let Octopus open"
        }

/** What a tester sees with this URL policy, plus the strategy name a developer looks for. */
val SDKConfig.UrlHandling.effectLine: String
    get() =
        when (this) {
            SDKConfig.UrlHandling.Default ->
                "The app opens its own contact link, Octopus opens every other link."
            SDKConfig.UrlHandling.ForceHandledByApp -> "Every link returns HandledByApp; the app opens it."
            SDKConfig.UrlHandling.ForceHandledByOctopus -> "Every link returns HandledByOctopus; the SDK opens it."
        }

/** Stable short id of a URL policy, used in test tags (`host-integration-url-<id>`). */
val SDKConfig.UrlHandling.tagId: String
    get() =
        when (this) {
            SDKConfig.UrlHandling.Default -> "default"
            SDKConfig.UrlHandling.ForceHandledByApp -> "app"
            SDKConfig.UrlHandling.ForceHandledByOctopus -> "octopus"
        }
