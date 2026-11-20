package com.octopuscommunity.sdk.test.repository

import com.octopuscommunity.sdk.domain.model.CommunityConfig
import com.octopuscommunity.sdk.domain.model.ProfileFieldsLock
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.CommunityConfigRepository
import com.octopuscommunity.sdk.test.mock.MockCommunityConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [CommunityConfigRepository] serving [MockCommunityConfig.default], with the debug
 * overrides layered on top of it like the production repository does.
 */
class FakeCommunityConfigRepository : CommunityConfigRepository {
    private val baseConfig = MockCommunityConfig.default
    private val _communityConfig = MutableStateFlow(baseConfig)

    // The debug overrides in effect; null = no override = base value.
    private var profileFieldsLockOverride: ProfileFieldsLock? = null
    private var contentOptionsOverride: CommunityConfig.ContentOptions? = null
    private var forceNicknameChoiceOverride: Boolean? = null
    private var consentTextsOverride: CommunityConfig.ConsentTexts? = null

    override suspend fun getCommunityConfig(): CommunityConfig? = _communityConfig.value

    override fun getCommunityConfigFlow(): Flow<CommunityConfig?> = _communityConfig

    override suspend fun fetchCommunityConfig(): OctopusResult<CommunityConfig, Nothing> = OctopusResult.Success(_communityConfig.value)

    override fun debugOverrideProfileFieldsLock(lock: ProfileFieldsLock?) {
        profileFieldsLockOverride = lock
        applyOverrides()
    }

    override fun debugOverrideContentOptions(options: CommunityConfig.ContentOptions?) {
        contentOptionsOverride = options
        applyOverrides()
    }

    override fun debugOverrideForceNicknameChoice(enabled: Boolean?) {
        forceNicknameChoiceOverride = enabled
        applyOverrides()
    }

    override fun debugOverrideConsentTexts(texts: CommunityConfig.ConsentTexts?) {
        consentTextsOverride = texts
        applyOverrides()
    }

    // Layers every active override on top of the base config, like the prod impl: setting one keeps
    // the others, and null clears that one back to the base value.
    private fun applyOverrides() {
        var config = baseConfig
        profileFieldsLockOverride?.let { config = config.copy(profileFieldsLock = it) }
        contentOptionsOverride?.let { config = config.copy(contentOptions = it) }
        forceNicknameChoiceOverride?.let { config = config.copy(forceNicknameChoice = it) }
        consentTextsOverride?.let { config = config.copy(consentTexts = it) }
        _communityConfig.value = config
    }
}
