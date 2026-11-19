package com.octopuscommunity.sdk.sample.ui.scenarios.sections

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HostPostDwellTest {
    @Test
    fun `loading time does not count toward three seconds after content appears`() =
        runTest {
            var displayed = false
            var completed = false
            launch {
                awaitHostPostContent { displayed }
                completed = true
            }
            runCurrent()
            advanceTimeBy(5_000)
            assertFalse(completed)
            displayed = true
            runCurrent()
            advanceTimeBy(2_999)
            runCurrent()
            assertFalse(completed)
            advanceTimeBy(1)
            runCurrent()
            assertTrue(completed)
        }

    @Test
    fun `content disappearing resets the continuous dwell`() =
        runTest {
            var displayed = true
            var completed = false
            launch {
                awaitHostPostContent { displayed }
                completed = true
            }
            runCurrent()
            advanceTimeBy(2_000)
            displayed = false
            runCurrent()
            advanceTimeBy(100)
            displayed = true
            runCurrent()
            advanceTimeBy(2_999)
            runCurrent()
            assertFalse(completed)
            advanceTimeBy(1)
            runCurrent()
            assertTrue(completed)
        }

    @Test
    fun `an unavailable post never produces a successful dwell`() =
        runTest {
            assertFailsWith<TimeoutCancellationException> { awaitHostPostContent { false } }
        }
}
