package com.octopuscommunity.sdk.domain.repository

import com.octopuscommunity.sdk.domain.model.CommunityConfig
import com.octopuscommunity.sdk.domain.model.Gamification
import com.octopuscommunity.sdk.domain.model.ProfileFieldsLock
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.network.ServerError
import kotlinx.coroutines.flow.Flow

/** Repository for the [CommunityConfig] served by the backend. */
interface CommunityConfigRepository {
    /** Returns the cached community config, or `null` when none was fetched yet. */
    suspend fun getCommunityConfig(): CommunityConfig?

    /** Observes the cached community config. */
    fun getCommunityConfigFlow(): Flow<CommunityConfig?>

    /** Fetches the community config from the backend and updates the cache. */
    suspend fun fetchCommunityConfig(): OctopusResult<CommunityConfig, Nothing>

    /**
     * Internal test affordance: locally override the per-field [ProfileFieldsLock] applied on top of
     * the backend-driven community config, without a backend config. Pass `null` to clear the
     * override and fall back to the backend value. Used by the sample app to exercise the per-field
     * profile lock before the backend serves it. No-op by default.
     */
    fun debugOverrideProfileFieldsLock(lock: ProfileFieldsLock?) {}

    /**
     * Internal test affordance: locally override [CommunityConfig.ContentOptions] applied on top of
     * the backend-driven community config, without a backend config. Pass `null` to clear the
     * override and fall back to the backend value. Used by the sample app to exercise the
     * per-content-type pictures/polls creation gating before the backend serves it. No-op by default.
     */
    fun debugOverrideContentOptions(options: CommunityConfig.ContentOptions?) {}

    /**
     * Internal test affordance: locally override [CommunityConfig.exposeClientUserId] applied on top
     * of the backend-driven community config, without a backend config. Pass `null` to clear the
     * override and fall back to the backend value. Used by the sample app to exercise the Unified
     * Profile activation flag before the backend serves it. No-op by default.
     */
    fun debugOverrideExposeClientUserId(enabled: Boolean?) {}

    /**
     * Internal test affordance: locally override [CommunityConfig.termsAcceptanceMode] applied on top
     * of the backend-driven community config, without a backend config. Pass `null` to clear the
     * override and fall back to the backend value. Used by the sample app to exercise the
     * explicit-consent modes before the backend serves it. No-op by default.
     */
    fun debugOverrideTermsAcceptanceMode(mode: CommunityConfig.TermsAcceptanceMode?) {}

    /**
     * Internal test affordance: locally override [CommunityConfig.forceNicknameChoice] applied on top
     * of the backend-driven community config. Pass `null` to clear the override and fall back to the
     * backend value. Used by the sample app to exercise the nickname choice screen. No-op by default.
     */
    fun debugOverrideForceNicknameChoice(enabled: Boolean?) {}

    /**
     * Internal test affordance: locally override [CommunityConfig.consentTexts] applied on top of
     * the backend-driven community config, without a backend config. Pass `null` to clear the
     * override and fall back to the backend value. Used by the sample app to exercise the
     * community's own consent wordings independently of its dashboard settings. No-op by default.
     */
    fun debugOverrideConsentTexts(texts: CommunityConfig.ConsentTexts?) {}

    /** Returns the gamification part of the cached community config, if any. */
    suspend fun getGamificationConfig() = getCommunityConfig()?.gamification

    /** Returns the points [action] earns, or `null` when it earns none or gamification is not configured. */
    suspend fun getGamificationPoints(action: Gamification.Action) = getGamificationConfig()?.getPoints(action)

    /** Returns the points toast to show for [action], or `null` when it earns no positive points. */
    suspend fun getGamificationToast(action: Gamification.Action) = getGamificationConfig()?.getToast(action)

    /** Errors returned by [UserRepository.overrideCommunityAccess]. */
    sealed interface OverrideCommunityAccessError : ServerError {
        /** The backend returned a generic error. */
        data class Unknown(
            override val errorMessage: String,
        ) : OverrideCommunityAccessError
    }
}
