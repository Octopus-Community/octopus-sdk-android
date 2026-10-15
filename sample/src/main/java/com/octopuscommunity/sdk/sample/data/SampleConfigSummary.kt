package com.octopuscommunity.sdk.sample.data

import com.octopuscommunity.sdk.sample.data.model.SDKConfig
import com.octopuscommunity.sdk.sample.data.model.User
import com.octopuscommunity.sdk.sample.data.model.toOverrideOrNull
import com.octopuscommunity.sdk.sample.extensions.LocalExtensions

/**
 * The single place the sample turns its persisted setup into readable lines.
 *
 * Two screens have to answer "what is the SDK pointed at right now": Debug info (the 8 lines of
 * its Configuration card) and the Settings summary (one subtitle per row). Screens used to each
 * phrase it their own way, which is how the sample managed to show a stale server on one screen and
 * the real one on another. Every wording lives here, once.
 *
 * Pure Kotlin (no Compose, no Android) so it is unit-testable.
 */
object SampleConfigSummary {
    /** Shown wherever a value is unknown because the SDK is not configured yet. */
    const val UNKNOWN = "—"

    /**
     * SharedPreferences key holding the SDK locale override. A host-side preference rather than
     * SDK state — the SDK is told about it through `OctopusSDK.overrideDefaultLocale`, it does not
     * persist it — so the sample owns both the key and the screens that read it.
     */
    const val LOCALE_OVERRIDE_KEY = "override_locale"

    /** Which backend the SDK talks to: "Demo", "Prod", a custom host, or a selected environment's ticket. */
    fun serverEnvironment(config: SDKConfig?): String {
        if (config == null) return UNKNOWN
        val target = LocalExtensions.resolve(config)
        if (target != null) return target.environmentLabel
        return when (config.serverEnvironment) {
            SDKConfig.ServerEnvironment.Default -> "Demo"
            SDKConfig.ServerEnvironment.Prod -> "Prod"
            is SDKConfig.ServerEnvironment.Custom -> "Custom"
        }
    }

    /**
     * The resolved host, which is not derivable from [serverEnvironment] alone: "Demo" means the
     * host baked into the build, and a selected environment overrides whatever the server row says.
     */
    fun host(config: SDKConfig?): String {
        if (config == null) return UNKNOWN
        val target = LocalExtensions.resolve(config)
        if (target != null) return target.apiHost
        return config.serverEnvironment.toOverrideOrNull()?.host ?: "default (build)"
    }

    /** The community the resolved API key opens. */
    fun community(config: SDKConfig?): String {
        if (config == null) return UNKNOWN
        LocalExtensions.resolve(config)?.let { return it.communityName }
        if (config.customCommunity) return "Custom community"
        return "Demo community"
    }

    /**
     * Where the API key came from — never the key itself beyond the mask
     * [SDKConfigResolver.maskApiKey] applies. No client key is ever embedded in this sample, and
     * this line is the one that says so out loud.
     */
    fun apiKeySource(config: SDKConfig?): String {
        if (config == null) return UNKNOWN
        val masked = SDKConfigResolver.maskApiKey(SDKConfigResolver.resolveApiKey(config))
        LocalExtensions.resolve(config)?.let { return "${it.keySourceLabel} · $masked" }
        if (config.customCommunity) return "Custom · $masked"
        val authKind = config.authKind as SDKConfig.AuthKind.SSO
        val preset =
            resolveAuthPreset(
                forceLogin = authKind.forceLoginOnStrongActions,
                appManagedFields = authKind.appManagedFields.toSet(),
            )
        return "Demo preset · ${preset.label}"
    }

    /** The SSO identity the host forged a token for, or the absence of one. */
    fun ssoUser(user: User?): String = user?.id?.takeIf { it.isNotBlank() } ?: "Not signed in"

    /** Entitlement count, spelled out — the sample never says "claim" on screen. */
    fun entitlements(user: User?): String =
        when (val count = user?.entitlements?.size ?: 0) {
            0 -> "None"
            1 -> "1 entitlement"
            else -> "$count entitlements"
        }

    /** Theme preset + appearance, the two halves of the Config screen's Theme section. */
    fun theme(config: SDKConfig?): String {
        if (config == null) return UNKNOWN
        val preset =
            when (config.theme) {
                SDKConfig.Theme.Default -> "Octopus navy"
                SDKConfig.Theme.SdkDefault -> "SDK default"
                is SDKConfig.Theme.Custom -> "Custom demo"
            }
        return "$preset · ${config.appearance.displayableString}"
    }

    /** Human name of the SDK locale override, or the absence of one. */
    fun language(localeOverride: String?): String {
        val language = localeOverride?.takeIf { it.isNotBlank() } ?: return "Follow system"
        return LANGUAGE_NAMES[language]?.let { "$it ($language)" } ?: language
    }

    /**
     * The full 8 configuration lines, in the order the shared design contract fixes — Debug info's
     * Configuration card since the 3-tab shell (DS v1) dropped Home, which used to frame them.
     * Changing the order here changes it on every sample screen that shows a subset.
     */
    fun configurationLines(
        config: SDKConfig?,
        user: User?,
        localeOverride: String?,
    ): List<Pair<String, String>> =
        listOf(
            "Server environment" to serverEnvironment(config),
            "Host" to host(config),
            "Community" to community(config),
            "API key source" to apiKeySource(config),
            "SSO user" to ssoUser(user),
            "Entitlements" to entitlements(user),
            "Theme" to theme(config),
            "Language" to language(localeOverride),
        )

    /** Settings → Account subtitle: who is connected, and with how many entitlements. */
    fun accountSubtitle(
        user: User?,
        isConnected: Boolean,
    ): String =
        when {
            !isConnected || user == null -> "Not connected · sign in with SSO"
            else -> "Connected as ${user.nickname ?: user.id ?: "user"} · ${entitlements(user)}"
        }

    /**
     * Locale language tag → display name, for the languages the Language screen offers. Kept here
     * rather than in the screen so the Debug info line and the Settings subtitle can name them too.
     */
    val LANGUAGE_NAMES: Map<String, String> =
        mapOf(
            "en" to "English",
            "fr" to "French",
            "de" to "German",
            "es" to "Spanish",
            "it" to "Italian",
            "pl" to "Polish",
            "pt" to "Portuguese",
            "sv" to "Swedish",
            "tr" to "Turkish",
        )
}
