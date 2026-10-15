package com.octopuscommunity.sdk.sample.ui.scenarios.sections

import com.octopuscommunity.sdk.domain.model.CommunityConfig
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.CommunityConfigRepository
import com.octopuscommunity.sdk.test.mock.MockCommunityConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.koin.core.error.ClosedScopeException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * internal change: the Live community config panel polled the *cached* config only, so a backend that
 * rejects the calls left it on "loading… (or SDK not initialised)" forever — the failure had
 * nowhere to surface. These tests drive [communityConfigPanelStates] directly, which is where the
 * whole loading / loaded / failed decision now lives; the composable only renders its output.
 *
 * `runTest` skips the poll delay, so a `take(n)` reads n ticks of the loop instantly.
 */
class CommunityConfigPanelStateTest {
    private val config = MockCommunityConfig.default

    /**
     * A [CommunityConfigRepository] whose cached read and fetch outcome are both scripted, and
     * which counts its fetches — the counter is what proves the panel does not hammer a backend
     * that is down.
     */
    private class ScriptedRepository(
        var cached: CommunityConfig? = null,
        var fetch: () -> OctopusResult<CommunityConfig, Nothing> = { error("unexpected fetch") },
    ) : CommunityConfigRepository {
        var read: (() -> CommunityConfig?)? = null
        var fetchCount = 0
            private set

        override suspend fun getCommunityConfig(): CommunityConfig? = read?.invoke() ?: cached

        override fun getCommunityConfigFlow(): Flow<CommunityConfig?> = MutableStateFlow(cached)

        override suspend fun fetchCommunityConfig(): OctopusResult<CommunityConfig, Nothing> {
            fetchCount++
            return fetch()
        }
    }

    private fun source(repository: CommunityConfigRepository?) = CommunityConfigPanelSource { repository }

    @Test
    fun `a healthy cached read replaces an earlier failed attempt`() =
        runTest {
            val repository = ScriptedRepository(fetch = { OctopusResult.Failure.NoNetwork })
            var loaded = false
            val states =
                communityConfigPanelStates(source(repository))
                    .onEach {
                        if (it is CommunityConfigPanelState.Failed) repository.cached = config
                        if (it is CommunityConfigPanelState.Loaded) {
                            loaded = true
                            repository.cached = null
                        }
                    }.take(4)
                    .toList()
            assertTrue(loaded)
            assertEquals(CommunityConfigPanelState.Loaded(config), states[2])
            assertEquals(CommunityConfigPanelState.Loaded(config), states[3])
            assertEquals(1, repository.fetchCount)
        }

    @Test
    fun `a failure on A does not prevent fetching B with an empty cache`() =
        runTest {
            val a = ScriptedRepository(fetch = { OctopusResult.Failure.NoNetwork })
            val b = ScriptedRepository(fetch = { OctopusResult.Success(config) })
            var current: CommunityConfigRepository = a
            val states =
                communityConfigPanelStates(CommunityConfigPanelSource { current })
                    .onEach { if (it is CommunityConfigPanelState.Failed) current = b }
                    .take(4)
                    .toList()
            assertEquals(CommunityConfigPanelState.Loading, states[2])
            assertEquals(CommunityConfigPanelState.Loaded(config), states[3])
            assertEquals(1, a.fetchCount)
            assertEquals(1, b.fetchCount)
        }

    @Test
    fun `a throwing local read is a read failure and never fetches every tick`() =
        runTest {
            val repository = ScriptedRepository()
            repository.read = { throw IllegalStateException("local read unavailable") }
            val states = communityConfigPanelStates(source(repository)).take(3).toList()
            assertTrue(states.all { it is CommunityConfigPanelState.Failed && it.message.contains("getCommunityConfig()") })
            assertEquals(0, repository.fetchCount)
        }

    @Test
    fun `a successful fetch remains loaded when the cache stays empty`() =
        runTest {
            val repository = ScriptedRepository(fetch = { OctopusResult.Success(config) })
            val states = communityConfigPanelStates(source(repository)).take(4).toList()
            assertTrue(states.drop(1).all { it == CommunityConfigPanelState.Loaded(config) })
            assertEquals(1, repository.fetchCount)
        }

    @Test
    fun `closed scope during a read or fetch skips errors and follows the new repository`() =
        runTest {
            for (closeDuringRead in listOf(false, true)) {
                val b = ScriptedRepository(cached = config)
                var current: CommunityConfigRepository? = null
                val a =
                    ScriptedRepository(fetch = {
                        current = b
                        throw ClosedScopeException("closed during fetch")
                    })
                if (closeDuringRead) {
                    a.read = {
                        current = b
                        throw ClosedScopeException("closed during read")
                    }
                }
                current = a
                val states =
                    communityConfigPanelStates(CommunityConfigPanelSource { current })
                        .take(if (closeDuringRead) 1 else 2)
                        .toList()
                assertEquals(CommunityConfigPanelState.Loaded(config), states.last())
                assertTrue(states.none { it is CommunityConfigPanelState.Failed })
            }
        }

    @Test
    fun `a fetch completing after a switch never emits the old community config`() =
        runTest {
            val newConfig = config.copy(displayAccountAge = !config.displayAccountAge)
            val b = ScriptedRepository(cached = newConfig)
            var current: CommunityConfigRepository? = null
            val a =
                ScriptedRepository(fetch = {
                    current = b
                    OctopusResult.Success(config)
                })
            current = a
            val states = communityConfigPanelStates(CommunityConfigPanelSource { current }).take(2).toList()
            assertEquals(listOf(CommunityConfigPanelState.Loading, CommunityConfigPanelState.Loaded(newConfig)), states)
        }

    @Test
    fun `a failed fetch leaves loading for an error state carrying the failure`() =
        runTest {
            val repository =
                ScriptedRepository(
                    cached = null,
                    fetch = { OctopusResult.Failure.UserNotAuthenticated(reason = "no session") },
                )

            val states = communityConfigPanelStates(source(repository)).take(2).toList()

            assertEquals(CommunityConfigPanelState.Loading, states[0])
            val failed = states[1] as CommunityConfigPanelState.Failed
            assertTrue(
                failed.message.contains("UserNotAuthenticated"),
                "the panel must name the failure, was: ${failed.message}",
            )
        }

    @Test
    fun `a payload-less failure is named, not printed as an object reference`() =
        runTest {
            val repository =
                ScriptedRepository(
                    cached = null,
                    fetch = { OctopusResult.Failure.NoNetwork },
                )

            val states = communityConfigPanelStates(source(repository)).take(2).toList()

            val failed = states[1] as CommunityConfigPanelState.Failed
            assertEquals("fetchCommunityConfig() failed: NoNetwork", failed.message)
        }

    @Test
    fun `a fetch that throws is surfaced as an error state, not as loading`() =
        runTest {
            val repository =
                ScriptedRepository(
                    cached = null,
                    fetch = { throw IllegalStateException("transport down") },
                )

            val states = communityConfigPanelStates(source(repository)).take(2).toList()

            assertEquals(CommunityConfigPanelState.Loading, states[0])
            val failed = states[1] as CommunityConfigPanelState.Failed
            assertTrue(
                failed.message.contains("transport down"),
                "the panel must name the failure, was: ${failed.message}",
            )
        }

    @Test
    fun `the error state sticks across poll ticks without re-fetching`() =
        runTest {
            val repository =
                ScriptedRepository(
                    cached = null,
                    fetch = { OctopusResult.Failure.NoNetwork },
                )

            val states = communityConfigPanelStates(source(repository)).take(4).toList()

            assertEquals(CommunityConfigPanelState.Loading, states[0])
            assertTrue(
                states.drop(1).all { it is CommunityConfigPanelState.Failed },
                "the error state must survive the next ticks, was: $states",
            )
            assertEquals(1, repository.fetchCount, "a backend that is down must not be polled")
        }

    @Test
    fun `collecting again re-attempts the fetch, which is what Retry does`() =
        runTest {
            val repository =
                ScriptedRepository(
                    cached = null,
                    fetch = { OctopusResult.Failure.NoNetwork },
                )
            val states = communityConfigPanelStates(source(repository))

            assertTrue(states.take(2).toList()[1] is CommunityConfigPanelState.Failed)
            assertEquals(1, repository.fetchCount)

            // Retry re-keys the panel's `produceState`, i.e. collects the cold flow again — a fresh
            // run of the state machine, so the fetch the failed one parked is attempted again.
            repository.fetch = { OctopusResult.Success(config) }
            val retried = states.take(2).toList()

            assertEquals(CommunityConfigPanelState.Loading, retried[0])
            assertEquals(CommunityConfigPanelState.Loaded(config), retried[1])
            assertEquals(2, repository.fetchCount)
        }

    @Test
    fun `a cached config is shown straight away, with no fetch at all`() =
        runTest {
            val repository = ScriptedRepository(cached = config)

            val states = communityConfigPanelStates(source(repository)).take(2).toList()

            assertEquals(CommunityConfigPanelState.Loaded(config), states[0])
            assertEquals(CommunityConfigPanelState.Loaded(config), states[1])
            assertEquals(0, repository.fetchCount, "nothing to fetch when the config is already cached")
        }

    @Test
    fun `a successful fetch re-reads, so a local debug override still wins`() =
        runTest {
            val overridden = config.copy(displayAccountAge = !config.displayAccountAge)
            val repository = ScriptedRepository(cached = null)
            repository.fetch = {
                // The prod repository persists the fetched config; the read that follows is the one
                // that layers the debug override on top.
                repository.cached = overridden
                OctopusResult.Success(config)
            }

            val states = communityConfigPanelStates(source(repository)).take(2).toList()

            assertEquals(CommunityConfigPanelState.Loading, states[0])
            assertEquals(CommunityConfigPanelState.Loaded(overridden), states[1])
        }

    @Test
    fun `no SDK container reads as not initialised rather than as an error`() =
        runTest {
            val states = communityConfigPanelStates(source(null)).take(2).toList()

            assertEquals(
                listOf(CommunityConfigPanelState.NotInitialised, CommunityConfigPanelState.NotInitialised),
                states,
            )
        }
}
