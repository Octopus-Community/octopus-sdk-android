package com.octopuscommunity.sdk.data.repository

import com.octopuscommunity.sdk.data.local.dao.CommunityConfigDao
import com.octopuscommunity.sdk.data.mapper.CommunityConfigMapper
import com.octopuscommunity.sdk.data.mapper.toCommunityClosure
import com.octopuscommunity.sdk.data.remote.service.ApiKeyService
import com.octopuscommunity.sdk.data.utils.call
import com.octopuscommunity.sdk.domain.model.CommunityConfig
import com.octopuscommunity.sdk.domain.model.ProfileFieldsLock
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.CommunityClosureRepository
import com.octopuscommunity.sdk.domain.repository.CommunityConfigRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class CommunityConfigRepositoryImpl(
    private val communityConfigDao: CommunityConfigDao,
    private val apiKeyService: ApiKeyService,
    private val communityConfigMapper: CommunityConfigMapper,
    private val closureRepository: CommunityClosureRepository = OpenCommunityClosureRepository,
) : CommunityConfigRepository {
    // Internal test override (sample app, before the BE serves the lock). Applied on top of the
    // stored config; null = no override = backend value.
    private val profileFieldsLockOverride = MutableStateFlow<ProfileFieldsLock?>(null)

    // Same pattern as [profileFieldsLockOverride], for the per-content-type pictures/polls gating
    // (sample app, before/independently of the BE config). null = no override = backend value.
    private val contentOptionsOverride = MutableStateFlow<CommunityConfig.ContentOptions?>(null)

    // Same pattern as [profileFieldsLockOverride], for the Unified Profile activation flag (sample
    // app, before/independently of the BE config). null = no override = backend value.
    private val exposeClientUserIdOverride = MutableStateFlow<Boolean?>(null)

    // Same pattern as [profileFieldsLockOverride], for the explicit terms-acceptance mode (sample
    // app, before/independently of the BE config). null = no override = backend value.
    private val termsAcceptanceModeOverride = MutableStateFlow<CommunityConfig.TermsAcceptanceMode?>(null)

    // Same pattern as [profileFieldsLockOverride], for the nickname choice flag (sample app,
    // before/independently of the BE config). null = no override = backend value.
    private val forceNicknameChoiceOverride = MutableStateFlow<Boolean?>(null)

    // Same pattern as [profileFieldsLockOverride], for the community's own consent wordings (sample
    // app, independently of the dashboard). null = no override = backend value.
    private val consentTextsOverride = MutableStateFlow<CommunityConfig.ConsentTexts?>(null)

    // The typed overloads of `combine` stop at five flows: the overrides are folded into one trigger
    // first, the sixth chained on. They are read from their `.value` in [applyOverride]; here they
    // only trigger a new emission, so the stored config stays a typed argument of the config flow.
    private val overrides =
        combine(
            profileFieldsLockOverride,
            contentOptionsOverride,
            exposeClientUserIdOverride,
            termsAcceptanceModeOverride,
            forceNicknameChoiceOverride,
        ) { _, _, _, _, _ -> }
            .combine(consentTextsOverride) { _, _ -> }

    override suspend fun getCommunityConfig() =
        with(communityConfigMapper) {
            communityConfigDao.getCommunityConfig()?.toDomain()?.applyOverride()
        }

    override fun getCommunityConfigFlow() =
        with(communityConfigMapper) {
            combine(
                communityConfigDao.getCommunityConfigFlow().map { it?.toDomain() },
                overrides,
            ) { config, _ -> config?.applyOverride() }
        }

    /**
     * The fetch in flight, shared by the callers that ask meanwhile (the launch, a foreground check, a refused call, a
     * screen): a closed community is checked through the gate, and several of these checks must not pass it at once.
     */
    private val inFlightLock = Any()
    private var inFlight: CompletableDeferred<OctopusResult<CommunityConfig, Nothing>>? = null

    override suspend fun fetchCommunityConfig(): OctopusResult<CommunityConfig, Nothing> {
        while (true) {
            val (shared, runsIt) =
                synchronized(inFlightLock) {
                    inFlight?.let { it to false }
                        ?: CompletableDeferred<OctopusResult<CommunityConfig, Nothing>>().also { inFlight = it }.let { it to true }
                }
            if (runsIt) return runSharedFetch(shared)
            try {
                return shared.await()
            } catch (e: CancellationException) {
                // The caller that ran it was cancelled, not this one: run it again.
                currentCoroutineContext().ensureActive()
            }
        }
    }

    private suspend fun runSharedFetch(
        shared: CompletableDeferred<OctopusResult<CommunityConfig, Nothing>>,
    ): OctopusResult<CommunityConfig, Nothing> =
        try {
            doFetchCommunityConfig().also { shared.complete(it) }
        } catch (e: Throwable) {
            shared.completeExceptionally(e)
            throw e
        } finally {
            synchronized(inFlightLock) { if (inFlight === shared) inFlight = null }
        }

    private suspend fun doFetchCommunityConfig(): OctopusResult<CommunityConfig, Nothing> {
        // A closed community is only checked once its date has come: a language change or a screen asking for the
        // config does not bring it forward. Read in the shared fetch: the answer of the previous one may have moved it.
        if (closureRepository.isClosed && !closureRepository.isCheckDue()) {
            return getCommunityConfig()?.let { OctopusResult.Success(it) } ?: closedCommunityFailure()
        }
        return call(mapper = communityConfigMapper) {
            // Only a due check reaches this point while the community is closed: the one call let through.
            val apiKeyConfig = apiKeyService.getConfig(bypassingCallsGate = closureRepository.isClosed).apiKeyConfig
            apiKeyConfig
                .toEntity()
                .also { configEntity ->
                    communityConfigDao.upsertCommunityConfig(configEntity)
                }.toDomain()
                .applyOverride()
                .also { closureRepository.handle(apiKeyConfig.toCommunityClosure(System.currentTimeMillis())) }
        }
    }

    override fun debugOverrideProfileFieldsLock(lock: ProfileFieldsLock?) {
        profileFieldsLockOverride.value = lock
    }

    override fun debugOverrideContentOptions(options: CommunityConfig.ContentOptions?) {
        contentOptionsOverride.value = options
    }

    override fun debugOverrideExposeClientUserId(enabled: Boolean?) {
        exposeClientUserIdOverride.value = enabled
    }

    override fun debugOverrideTermsAcceptanceMode(mode: CommunityConfig.TermsAcceptanceMode?) {
        termsAcceptanceModeOverride.value = mode
    }

    override fun debugOverrideForceNicknameChoice(enabled: Boolean?) {
        forceNicknameChoiceOverride.value = enabled
    }

    override fun debugOverrideConsentTexts(texts: CommunityConfig.ConsentTexts?) {
        consentTextsOverride.value = texts
    }

    private fun CommunityConfig.applyOverride(): CommunityConfig {
        var config = this
        profileFieldsLockOverride.value?.let { config = config.copy(profileFieldsLock = it) }
        contentOptionsOverride.value?.let { config = config.copy(contentOptions = it) }
        exposeClientUserIdOverride.value?.let { config = config.copy(exposeClientUserId = it) }
        termsAcceptanceModeOverride.value?.let { config = config.copy(termsAcceptanceMode = it) }
        forceNicknameChoiceOverride.value?.let { config = config.copy(forceNicknameChoice = it) }
        consentTextsOverride.value?.let { config = config.copy(consentTexts = it) }
        return config
    }
}
