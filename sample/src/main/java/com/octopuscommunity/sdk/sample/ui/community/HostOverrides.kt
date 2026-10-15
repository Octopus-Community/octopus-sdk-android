package com.octopuscommunity.sdk.sample.ui.community

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.octopuscommunity.sdk.InternalOctopusApi
import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.domain.model.CommunityConfig
import com.octopuscommunity.sdk.domain.model.ProfileFieldsLock

/**
 * Demo-only, in-memory debug overrides of backend-driven community config flags (e.g.
 * `exposeClientUserId` — the Unified Profile activation flag — and the terms-acceptance mode).
 * These overrides are applied live through the SDK's debug hooks and read reactively by the
 * scenario screens. Host callbacks also update live via Config and the recomposed navigation
 * handlers; changing Unified Profile activation additionally restarts the SDK to restore its flag
 * override (see `SDKConfig.sdkInitKey`).
 */
object HostOverrides {
    /**
     * Debug override of the backend `exposeClientUserId` (the Unified Profile activation flag), applied
     * through the SDK's internal debug hook. `null` = use the backend value; `true`/`false` force it.
     */
    var exposeClientUserIdOverride by mutableStateOf<Boolean?>(null)
        private set

    @OptIn(InternalOctopusApi::class)
    fun applyExposeClientUserIdOverride(enabled: Boolean?) {
        exposeClientUserIdOverride = enabled
        OctopusSDK.debugOverrideExposeClientUserId(enabled)
    }

    /**
     * Debug override of the backend `termsAcceptanceMode` (consent checkbox mode), applied through
     * the SDK's internal debug hook. `null` = use the backend value; a specific mode forces it.
     */
    var termsAcceptanceModeOverride by mutableStateOf<CommunityConfig.TermsAcceptanceMode?>(null)
        private set

    @OptIn(InternalOctopusApi::class)
    fun applyTermsAcceptanceModeOverride(mode: CommunityConfig.TermsAcceptanceMode?) {
        termsAcceptanceModeOverride = mode
        OctopusSDK.debugOverrideTermsAcceptanceMode(mode)
    }

    /**
     * Debug override of the community's own consent wordings, applied through the SDK's internal
     * debug hook. `null` = use the backend value; specific texts force them. They only show when their
     * case matches the effective terms-acceptance mode.
     */
    var consentTextsOverride by mutableStateOf<CommunityConfig.ConsentTexts?>(null)
        private set

    @OptIn(InternalOctopusApi::class)
    fun applyConsentTextsOverride(texts: CommunityConfig.ConsentTexts?) {
        consentTextsOverride = texts
        OctopusSDK.debugOverrideConsentTexts(texts)
    }

    /**
     * Debug override of the backend `forceNicknameChoice` (nickname typed before the first
     * contribution), applied through the SDK's internal debug hook. `null` = use the backend value.
     */
    var forceNicknameChoiceOverride by mutableStateOf<Boolean?>(null)
        private set

    @OptIn(InternalOctopusApi::class)
    fun applyForceNicknameChoiceOverride(enabled: Boolean?) {
        forceNicknameChoiceOverride = enabled
        OctopusSDK.debugOverrideForceNicknameChoice(enabled)
    }

    /**
     * Debug override of the backend per-field profile lock (internal ticket), applied through the SDK's
     * internal debug hook. `null` = use the backend value (all-editable); a specific [ProfileFieldsLock]
     * forces it.
     */
    var profileFieldsLockOverride by mutableStateOf<ProfileFieldsLock?>(null)
        private set

    @OptIn(InternalOctopusApi::class)
    fun applyProfileFieldsLockOverride(lock: ProfileFieldsLock?) {
        profileFieldsLockOverride = lock
        OctopusSDK.debugOverrideProfileFieldsLock(lock)
    }

    /**
     * Debug override of the backend per-content-type gating (internal ticket), applied through the SDK's
     * internal debug hook. `null` = use the backend value (all enabled, text required); specific
     * [CommunityConfig.ContentOptions] force it.
     */
    var contentOptionsOverride by mutableStateOf<CommunityConfig.ContentOptions?>(null)
        private set

    @OptIn(InternalOctopusApi::class)
    fun applyContentOptionsOverride(options: CommunityConfig.ContentOptions?) {
        contentOptionsOverride = options
        OctopusSDK.debugOverrideContentOptions(options)
    }

    /**
     * Last locale forced through `OctopusSDK.overrideDefaultLocale(locale)` — `null` means "system
     * default" (either never overridden, or explicitly reset). Unlike the `debugOverride*` hooks
     * above, `overrideDefaultLocale` is a suspend call already routed through [ScenariosViewModel]'s
     * (com.octopuscommunity.sdk.sample.ui.scenarios.ScenariosViewModel) own method-state / result-line
     * plumbing, so this object only mirrors the last-applied value for the stateful-control /
     * override-badge UI — it never calls the SDK itself.
     */
    var localeOverride by mutableStateOf<String?>(null)
        private set

    fun trackLocaleOverride(locale: String?) {
        localeOverride = locale
    }

    /**
     * Last value forced through `OctopusSDK.overrideCommunityAccess(hasAccess)` — `null` means
     * "not overridden this session". Mirrors [localeOverride]'s tracking-only role: the actual
     * (suspend) SDK call is already made by [ScenariosViewModel]. There is no public API to reset
     * this override back to the backend value once set — only re-granting or re-denying — so unlike
     * the others above, no "clear" state exists here by design; see `CommunityAccessPresets`.
     */
    var communityAccessOverride by mutableStateOf<Boolean?>(null)
        private set

    fun trackCommunityAccessOverride(hasAccess: Boolean) {
        communityAccessOverride = hasAccess
    }

    /**
     * `groupAccessDenied` scenario state — hoisted here (process-level) rather than held as
     * `rememberSaveable` inside the scenario's own composable, because the callback it observes
     * fires from a DIFFERENT NavHost destination: the tester registers it here, then taps a locked
     * group on the actual Octopus screen, which disposes this composition before the callback ever
     * runs. A `rememberSaveable` var inside that disposed composition keeps counting into an
     * orphaned `MutableState` nobody reads again; a state hoisted on this process-level object
     * keeps being the one both screens observe.
     *
     * `registered` tracks which of the two callbacks (`true` = the counting one, `false` = the
     * no-op substitute) was last installed via `setGroupAccessDeniedCallback` — `null` means
     * neither has been installed yet this process. It lives here rather than as this scenario's
     * OWN `rememberSaveable` (which is what its previous implementation used, and lost the count
     * to the exact orphaned-composition failure described above): a fresh process has genuinely
     * installed nothing, so resetting to `null` on process death (rather than restoring a stale
     * `true` with no callback actually registered) keeps the badge and result line honest.
     */
    var groupAccessDeniedRegistered by mutableStateOf<Boolean?>(null)
        private set

    /** How many times the counting callback registered below has actually fired this process. */
    var groupAccessDeniedFireCount by mutableStateOf(0)
        private set

    /** Installs the counting callback and marks it as the one currently registered. */
    fun registerGroupAccessDeniedCallback() {
        groupAccessDeniedRegistered = true
        OctopusSDK.setGroupAccessDeniedCallback { groupAccessDeniedFireCount++ }
    }

    /**
     * Installs a real no-op, distinct from the counting lambda above so the two presets are told
     * apart at runtime and not just by their labels — the setter's parameter can't be null, so this
     * is the closest this scenario has to "unregister".
     */
    fun unregisterGroupAccessDeniedCallback() {
        groupAccessDeniedRegistered = false
        OctopusSDK.setGroupAccessDeniedCallback { }
    }
}
