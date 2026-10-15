package com.octopuscommunity.sdk.sample.data.datastore

import android.app.Application
import android.content.ContextWrapper
import com.octopuscommunity.sdk.sample.data.model.SDKConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SampleDataStoreTestFixtureTest {
    @Test
    fun `launch guard fails within virtual time when a background collector stays active`() =
        runTest {
            val dataStores = SampleDataStoreTestFixture(RuntimeEnvironment.getApplication(), backgroundScope)
            try {
                val collector = backgroundScope.launch { dataStores.configs.configFlow.collect {} }
                testScheduler.runCurrent()
                assertTrue(collector.isActive)

                val failure =
                    assertFailsWith<AssertionError> {
                        // Also bound the regression: the original unbounded join must fail in virtual time.
                        withTimeout(2.seconds) { dataStores.joinLaunchJobs() }
                    }
                val message = assertNotNull(failure.message)
                assertTrue(
                    message.startsWith(
                        "Launch jobs did not complete within one second of virtual time; " +
                            "check for an infinite collector or a launch job slower than 1 s. Still-active children: [",
                    ),
                )
                assertTrue(message.contains("{Active}"))
                assertEquals(1_000L, testScheduler.currentTime)
                assertTrue(collector.isActive)
                assertTrue(dataStores.job.isActive)
            } finally {
                dataStores.close()
            }
        }

    @Test
    fun `cancelled teardown still completes the store job and deletes its files`() =
        runTest {
            val application = RuntimeEnvironment.getApplication()
            val cache = Files.createTempDirectory(application.cacheDir.toPath(), "fixture-test-").toFile()
            val context =
                object : ContextWrapper(application) {
                    override fun getCacheDir() = cache
                }
            val dataStores = SampleDataStoreTestFixture(context, backgroundScope)
            val storeJob = dataStores.job
            try {
                dataStores.configs.updateConfig { SDKConfig(authKind = SDKConfig.AuthKind.SSO()) }
                val directory = cache.listFiles()!!.single()
                assertTrue(directory.resolve("config.json").isFile)
                var collectorClosed = false
                val collector =
                    CoroutineScope(backgroundScope.coroutineContext + storeJob).launch {
                        try {
                            dataStores.configs.configFlow.collect {}
                        } finally {
                            // The parent must join an active child even when its own caller is cancelled.
                            withContext(NonCancellable) {
                                delay(1)
                                collectorClosed = true
                            }
                        }
                    }
                testScheduler.runCurrent()
                assertTrue(collector.isActive)
                val test =
                    launch {
                        try {
                            awaitCancellation()
                        } finally {
                            dataStores.close()
                        }
                    }
                testScheduler.runCurrent()
                // Like a runTest timeout, cancel the caller before its suspending teardown.
                test.cancelAndJoin()
                assertTrue(storeJob.isCompleted)
                assertTrue(collector.isCompleted)
                assertTrue(collectorClosed)
                assertFalse(directory.exists())
            } finally {
                try {
                    dataStores.close()
                } finally {
                    cache.deleteRecursively()
                }
            }
        }

    @Test
    fun `launch guard waits for delayed writes on another dispatcher without joining the store job`() =
        runTest {
            val dataStores = SampleDataStoreTestFixture(RuntimeEnvironment.getApplication(), backgroundScope)
            try {
                val persisted = SDKConfig(authKind = SDKConfig.AuthKind.SSO(), appearance = SDKConfig.Appearance.Dark)
                dataStores.configs.updateConfig { persisted }
                val changed = persisted.copy(appearance = SDKConfig.Appearance.Light)
                val write =
                    backgroundScope.launch {
                        delay(1)
                        withContext(StandardTestDispatcher(testScheduler)) {
                            dataStores.configs.updateConfig { changed }
                        }
                    }
                dataStores.joinLaunchJobs()
                assertTrue(write.isCompleted)
                assertTrue(dataStores.job.isActive)
                assertEquals(changed, dataStores.configs.configFlow.first())
            } finally {
                dataStores.close()
            }
        }
}
