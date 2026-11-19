package com.octopuscommunity.sdk.sample.ui.scenarios.sections

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HostLifecycleRunnerTest {
    private class Actions : HostLifecycleActions {
        val calls = mutableListOf<String>()
        var mountGate: CompletableDeferred<Unit>? = null
        var contentGate: CompletableDeferred<Unit>? = null
        var failure: String? = null

        private fun record(action: String) {
            calls += action
            if (failure == action) throw IllegalStateException("must not be included in the result")
        }

        override suspend fun mount() {
            record("mount")
            mountGate?.await()
        }

        override suspend fun unmount() = record("unmount")

        override suspend fun openPost() = record("post")

        override suspend fun clickPostInCommunity() = record("sdk-click")

        override suspend fun awaitPostContent() {
            record("content")
            contentGate?.await()
        }

        override suspend fun clearTabOnBack() = record("back")

        override suspend fun detachAndReattach() = record("detach/reattach")

        override suspend fun switchCommunity() = record("switch")

        override suspend fun restartSdk() = record("restart")

        override suspend fun recompose() = record("recompose")
    }

    @Test
    fun `remount completes five full cycles before opening comments in final mount`() =
        runTest {
            val actions = Actions()
            val result = runHostLifecyclePreset(HostLifecyclePreset.Remount, actions)
            assertEquals(
                listOf("unmount", "mount") + List(5) { listOf("unmount", "mount") }.flatten() + listOf("post", "content"),
                actions.calls,
            )
            assertEquals("OK: Preset 1 · Remount embedded view completed", result)
        }

    @Test
    fun `no post navigation before mount acknowledgement`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val actions = Actions().apply { mountGate = gate }
            val job = launch { runHostLifecyclePreset(HostLifecyclePreset.Remount, actions) }
            runCurrent()
            assertEquals(listOf("unmount", "mount"), actions.calls)
            assertFalse(job.isCompleted)
            gate.complete(Unit)
            job.join()
            assertEquals("content", actions.calls.last())
        }

    @Test
    fun `tab Back clears the tab without unmounting the host first`() =
        runTest {
            val actions = Actions()
            runHostLifecyclePreset(HostLifecyclePreset.TabBack, actions)
            assertEquals(listOf("unmount", "mount", "back"), actions.calls)
        }

    @Test
    fun `detach keeps its mounted session through return and navigation`() =
        runTest {
            val actions = Actions()
            runHostLifecyclePreset(HostLifecyclePreset.Detach, actions)
            assertEquals(listOf("unmount", "mount", "detach/reattach", "post", "content"), actions.calls)
        }

    @Test
    fun `switch and restart keep content mounted then recompose and navigate`() =
        runTest {
            listOf(HostLifecyclePreset.Switch to "switch", HostLifecyclePreset.Restart to "restart").forEach { (preset, action) ->
                val actions = Actions()
                assertEquals("OK: ${preset.label} completed", runHostLifecyclePreset(preset, actions))
                assertEquals(listOf("unmount", "mount", action, "recompose", "sdk-click", "content"), actions.calls)
            }
        }

    @Test
    fun `failed host action reports exception class and does not continue or expose its message`() =
        runTest {
            val actions = Actions().apply { failure = "switch" }
            assertEquals("FAILED: IllegalStateException", runHostLifecyclePreset(HostLifecyclePreset.Switch, actions))
            assertEquals(listOf("unmount", "mount", "switch"), actions.calls)
        }

    @Test
    fun `cancellation leaves the runner instead of reporting a completed preset`() =
        runTest {
            val actions =
                Actions().apply {
                    mountGate = CompletableDeferred<Unit>().apply { cancel() }
                }
            assertFailsWith<CancellationException> {
                runHostLifecyclePreset(HostLifecyclePreset.Remount, actions)
            }
        }

    @Test
    fun `a host acknowledgement timeout reports failure instead of leaving Running forever`() =
        runTest {
            val actions =
                object : HostLifecycleActions by Actions() {
                    override suspend fun mount() {
                        withTimeout(100) { CompletableDeferred<Unit>().await() }
                    }
                }
            assertEquals("FAILED: TimeoutCancellationException", runHostLifecyclePreset(HostLifecyclePreset.Remount, actions))
        }

    @Test
    fun `setup problems skip without performing a host action`() =
        runTest {
            val actions = Actions()
            val skipped =
                object : HostLifecycleActions by actions {
                    override fun setupIssue() = "Configure a community first"
                }
            assertEquals("SKIPPED: Configure a community first", runHostLifecyclePreset(HostLifecyclePreset.Remount, skipped))
            assertTrue(actions.calls.isEmpty())
        }

    @Test
    fun `no OK until the rendered content acknowledgement and dwell complete`() =
        runTest {
            HostLifecyclePreset.entries.filter { it != HostLifecyclePreset.TabBack }.forEach { preset ->
                val actions = Actions().apply { contentGate = CompletableDeferred() }
                var result = "Running"
                val job = launch { result = runHostLifecyclePreset(preset, actions) }
                runCurrent()
                assertEquals("content", actions.calls.last())
                assertEquals("Running", result)
                actions.contentGate!!.complete(Unit)
                job.join()
                assertEquals("OK: ${preset.label} completed", result)
            }
        }
}
