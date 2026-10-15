package com.octopuscommunity.sdk.sample.ui.scenarios.sections

import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.domain.model.CommunityConfig
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.CommunityConfigRepository
import com.octopuscommunity.sdk.sample.data.describe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.koin.core.error.ClosedScopeException

/** What the Live community config panel ([CommunityConfigPreview]) can be showing. */
sealed interface CommunityConfigPanelState {
    /**
     * The SDK has no open container, so there is nothing to read and nothing to fetch. Distinct
     * from [Loading] on purpose: the old panel merged the two into "loading… (or SDK not
     * initialised)" and could therefore never say anything else.
     */
    data object NotInitialised : CommunityConfigPanelState

    /** A `fetchCommunityConfig()` call is actually in flight. */
    data object Loading : CommunityConfigPanelState

    /** The config the SDK currently sees, local debug overrides included. */
    data class Loaded(
        val config: CommunityConfig,
    ) : CommunityConfigPanelState

    /** The fetch failed. The panel renders [message] and offers a retry. */
    data class Failed(
        val message: String,
    ) : CommunityConfigPanelState
}

/**
 * The slice of the SDK the panel reads, narrowed to one call so a JVM test can drive the state
 * machine below without an initialised SDK.
 */
fun interface CommunityConfigPanelSource {
    /**
     * The [CommunityConfigRepository] of the **current** community, or `null` when the SDK has no
     * open container.
     *
     * Resolved per tick rather than captured once: `switchCommunity` rebuilds the Koin container
     * (a fresh repository + DB), so a captured instance would keep answering for the community the
     * panel no longer targets.
     */
    fun repositoryOrNull(): CommunityConfigRepository?
}

/** Reads the live SDK. `null` while the SDK is not initialised — or is being stopped under us. */
val OctopusCommunityConfigPanelSource =
    CommunityConfigPanelSource {
        if (OctopusSDK.isInitialised) {
            // A `stop()` landing between the guard and the resolution throws `ClosedScopeException`
            // out of the getter. That is "no SDK right now", not a failed fetch, so it must not
            // reach the error state.
            runCatching { OctopusSDK.communityConfigRepository }.getOrNull()
        } else {
            null
        }
    }

/**
 * The panel's state machine, as a cold flow of states.
 *
 * Polls the **local** read every [pollIntervalMillis] rather than collecting
 * `getCommunityConfigFlow()` once: `switchCommunity` rebuilds the Koin container and its
 * `isInitialisedFlow` false->true edge happens synchronously inside `initialize()`, so a
 * StateFlow-keyed collector stays bound to the old, cleared repository. Re-reading through the
 * suspend getter resolves a fresh repository on every tick, and `getCommunityConfig()` applies the
 * local debug overrides too — which is what makes the override sub-sections flip a line here.
 *
 * Nothing cached means the config has to be fetched, and a failed fetch is surfaced as
 * [CommunityConfigPanelState.Failed] instead of looking like a request still in flight (internal change). Each repository identity gets one fetch attempt; both its success and failure are
 * retained while cheap local reads keep reflecting debug overrides. A fresh repository resets
 * the attempt, even when the SDK's brief uninitialised window was missed. Collecting again is the
 * Retry action. Read failures remain distinct from an empty cache, and closed scopes are skipped.
 */
fun communityConfigPanelStates(
    source: CommunityConfigPanelSource,
    pollIntervalMillis: Long = 1_000L,
): Flow<CommunityConfigPanelState> =
    flow {
        var previousRepository: CommunityConfigRepository? = null
        var lastFetch: CommunityConfigPanelState? = null
        while (true) {
            val repository = source.repositoryOrNull()
            if (repository !== previousRepository) {
                previousRepository = repository
                lastFetch = null
            }
            if (repository == null) {
                emit(CommunityConfigPanelState.NotInitialised)
            } else {
                try {
                    val cached = repository.readState()
                    if (cached is CommunityConfigPanelState.Loaded) lastFetch = cached
                    val state =
                        cached ?: lastFetch ?: run {
                            emit(CommunityConfigPanelState.Loading)
                            repository.fetchState().also { lastFetch = it }
                        }
                    // A read/fetch can suspend across a switch: never attribute A's result to B.
                    if (source.repositoryOrNull() === repository) emit(state)
                } catch (_: ClosedScopeException) {
                    // The old scope closed during a read/fetch. Resolve the new one next tick.
                }
            }
            delay(pollIntervalMillis)
        }
    }

/** A failed read is distinct from an empty cache and must not trigger repeated fetches. */
private suspend fun CommunityConfigRepository.readState(): CommunityConfigPanelState? =
    try {
        getCommunityConfig()?.let { CommunityConfigPanelState.Loaded(it) }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (closed: ClosedScopeException) {
        throw closed
    } catch (e: Exception) {
        CommunityConfigPanelState.Failed("getCommunityConfig() threw: ${e.safeDescription()}")
    }

private suspend fun CommunityConfigRepository.fetchState(): CommunityConfigPanelState =
    try {
        when (val result = fetchCommunityConfig()) {
            is OctopusResult.Success ->
                readState() ?: CommunityConfigPanelState.Loaded(result.data)
            is OctopusResult.Failure ->
                CommunityConfigPanelState.Failed("fetchCommunityConfig() failed: ${result.describe()}")
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (closed: ClosedScopeException) {
        throw closed
    } catch (e: Exception) {
        CommunityConfigPanelState.Failed("fetchCommunityConfig() threw: ${e.safeDescription()}")
    }

private fun Exception.safeDescription(): String = message?.takeIf { it.isNotBlank() } ?: this::class.simpleName ?: "unknown error"
