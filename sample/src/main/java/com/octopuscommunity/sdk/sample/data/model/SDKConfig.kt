package com.octopuscommunity.sdk.sample.data.model

import com.octopuscommunity.sdk.domain.model.ProfileField
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class SDKConfig(
    val authKind: AuthKind,
    val customCommunity: Boolean = false,
    val customApiKey: String? = null,
    val theme: Theme = Theme.Default,
    /**
     * Light/dark choice for the sample's own chrome *and* the SDK surface it hosts. Defaults to
     * following the system, which is what every build before this field did — old persisted
     * configs decode to exactly their previous behaviour.
     */
    val appearance: Appearance = Appearance.System,
    val serverEnvironment: ServerEnvironment = ServerEnvironment.Default,
    /** Optional local configuration, interpreted by the build extension. */
    @SerialName("featureEnv") val localConfiguration: JsonObject? = null,
    /**
     * Whether the sample hands new FCM device tokens to the SDK — the `PushRegistration` feature
     * toggle, flipped from the Notifications section header on the Scenarios tab.
     *
     * Defaults to `true`, which is exactly what every build before this field did (the messaging
     * service registered unconditionally), so a config persisted earlier decodes to its previous
     * behaviour instead of silently going quiet.
     */
    val pushRegistrationEnabled: Boolean = true,
    val hostCallbacks: HostCallbacks = HostCallbacks(),
    /** Where Community drill-down is hosted; applies live without reinitializing the SDK. */
    val communityNavigation: CommunityNavigation = CommunityNavigation.InTab,
) {
    /**
     * The fields whose change makes `SampleApplication` re-initialise the SDK (and reconnect the
     * stored user). Unified Profile activation is included so its flag override is reapplied even
     * when only profile callback wiring changes. Other host callbacks, theme and appearance apply
     * live. Config reads this too to decide whether Apply already reconnects the user.
     */
    val sdkInitKey: List<Any?>
        get() = listOf(authKind, customCommunity, customApiKey, serverEnvironment, localConfiguration, unifiedProfileEnabled)

    /**
     * The feature toggles this config carries, as the read-only surfaces show them.
     *
     * Force login is not a field of its own: it lives inside [AuthKind.SSO], where the SDK
     * connection mode is built from it. Reading it through here is what lets the Scenarios
     * headers, the scenario screens and Debug info depend on one small value rather than on the
     * shape of the auth config.
     */
    val featureToggles: FeatureToggles
        get() =
            FeatureToggles(
                forceLogin = (authKind as? AuthKind.SSO)?.forceLoginOnStrongActions == true,
                pushRegistration = pushRegistrationEnabled,
            )

    /** The scenario is on only when profile fields are app-managed AND the host profile callback is wired. */
    val unifiedProfileEnabled: Boolean
        get() = (authKind as? AuthKind.SSO)?.appManagedFields?.isNotEmpty() == true && hostCallbacks.profileWired

    /** Set both Unified Profile conditions together, preserving the remaining host configuration. */
    fun withUnifiedProfile(enabled: Boolean): SDKConfig =
        copy(
            authKind =
                (authKind as AuthKind.SSO).copy(
                    appManagedFields = if (enabled) ProfileField.ALL.toList() else emptyList(),
                ),
            hostCallbacks = hostCallbacks.copy(profileWired = enabled),
        )

    /** Update the auth preset without implicitly enabling the Unified Profile scenario. */
    fun withAuthKind(authKind: AuthKind): SDKConfig =
        copy(
            authKind = authKind,
            hostCallbacks =
                hostCallbacks.copy(
                    profileWired = unifiedProfileEnabled && (authKind as? AuthKind.SSO)?.appManagedFields?.isNotEmpty() == true,
                ),
        )

    /**
     * [hostCallbacks] as the community actually runs: a callback the SDK requires under this config
     * stays wired whatever is stored ([HostCallback.requiredReason]). Every surface that wires or
     * shows the callbacks reads this, never [hostCallbacks] directly. Host app integration and the
     * Unified Profile scenario write the stored value. With the scenario off, profile navigation stays
     * unwired even if the backend exposes client user ids.
     */
    val effectiveHostCallbacks: HostCallbacks
        get() =
            HostCallback.entries.fold(
                hostCallbacks.copy(profileWired = unifiedProfileEnabled),
            ) { callbacks, callback ->
                if (callback.requiredReason(this) != null) callback.withWired(callbacks, wired = true) else callbacks
            }

    /**
     * Which host navigation callbacks the sample wires on `octopusComposables(...)`. Config changes
     * recompose the host graph and its navigation handlers; existing event collectors read the
     * current handlers, so wiring changes take effect without restarting the app.
     */
    @Serializable
    data class HostCallbacks(
        val loginWired: Boolean = true,
        val profileWired: Boolean = false,
        val profileEditWired: Boolean = true,
        val clientObjectWired: Boolean = true,
        val groupAccessDeniedWired: Boolean = true,
        val urlHandling: UrlHandling = UrlHandling.Default,
    )

    /**
     * The Appearance the tester picked — Config → Theme, or Settings › Appearance.
     *
     * `System` is not the same thing as "Light": a sample that forces one of the two is unable to
     * show what a host app actually sees, and the appearance bug reports we get are all about the
     * theme the *device* was in.
     */
    @Serializable
    enum class Appearance {
        System,
        Light,
        Dark,
        ;

        val displayableString
            get() =
                when (this) {
                    System -> "Follow system"
                    Light -> "Light"
                    Dark -> "Dark"
                }
    }

    /** How the sample answers `onNavigateToUrl` for a link tapped inside SDK content. */
    @Serializable
    enum class UrlHandling { Default, ForceHandledByApp, ForceHandledByOctopus }

    @Serializable
    enum class CommunityNavigation(
        val label: String,
    ) {
        InTab("In tab"),
        FullScreen("Full screen"),
    }

    @Serializable
    sealed class ServerEnvironment {
        @Serializable
        data object Default : ServerEnvironment()

        @Serializable
        data object Prod : ServerEnvironment()

        @Serializable
        data class Custom(
            val host: String,
            val port: Int = 443,
        ) : ServerEnvironment()

        val displayableString
            get() =
                when (this) {
                    Default -> "Default (BuildConfig)"
                    Prod -> "Prod (api.8pus.io)"
                    is Custom -> "Custom ($host:$port)"
                }
    }

    @Serializable
    sealed class AuthKind {
        @Serializable
        data class SSO(
            val appManagedFields: List<ProfileField> = emptyList(),
            val forceLoginOnStrongActions: Boolean = false,
        ) : AuthKind()

        val displayableString
            get() =
                when (this) {
                    is SSO ->
                        "- SSO\n" +
                            "- App Managed Fields: " +
                            when (appManagedFields.size) {
                                0 -> "None"
                                else -> appManagedFields.joinToString(" | ") { it.name }
                            } + "\n- Force Login: ${forceLoginOnStrongActions.toString().uppercase()}"
                }

        /**
         * One-line compact label describing the auth configuration.
         * Examples:
         *  - "SSO — no managed fields"
         *  - "SSO — force login, managed: nickname, bio"
         *  - "SSO — force login, all fields managed"
         */
        val shortLabel: String
            get() =
                when (this) {
                    is SSO ->
                        buildString {
                            append("SSO")
                            val parts = mutableListOf<String>()
                            if (forceLoginOnStrongActions) parts += "force login"
                            parts +=
                                when {
                                    appManagedFields.isEmpty() -> "no managed fields"
                                    appManagedFields.containsAll(ProfileField.ALL) -> "all fields managed"
                                    else -> "managed: " + appManagedFields.joinToString(", ") { it.name.lowercase() }
                                }
                            append(" — ")
                            append(parts.joinToString(", "))
                        }
                }
    }

    @Serializable
    sealed class Theme {
        /**
         * The sample's own brand theme — the Octopus-navy `SampleBranding` quadruple handed to
         * the SDK. The default, and what the "Octopus navy" preset selects.
         */
        @Serializable
        data object Default : Theme()

        /**
         * No theme handed to the SDK at all — what a fresh integration looks like. The visual
         * reference control ("SDK default" preset). A separate subtype rather than a re-reading
         * of [Default] so configs persisted before it existed keep rendering the navy they
         * always rendered.
         */
        @Serializable
        data object SdkDefault : Theme()

        /**
         * The QA demo theme: brand colors plus custom fonts and icons, toggleable per axis
         * (scenario preset "Custom theme (brand colors + logo)").
         */
        @Serializable
        data class Custom(
            val colors: Boolean = true,
            val fonts: Boolean = true,
            val icons: Boolean = true,
        ) : Theme()
    }

    val displayableString get() = authKind.displayableString
}
