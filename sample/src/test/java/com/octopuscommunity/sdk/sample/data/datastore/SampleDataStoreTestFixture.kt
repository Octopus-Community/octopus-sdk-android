package com.octopuscommunity.sdk.sample.data.datastore

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import kotlin.time.Duration.Companion.seconds

/**
 * Real, isolated files with DataStore actors and serialization on the test scheduler. The Context
 * delegates instead retain application-lifetime IO scopes across Robolectric tests; clearing their
 * data does not cancel those scopes. Pass runTest's backgroundScope and close before resetting Main.
 */
internal class SampleDataStoreTestFixture(
    context: Context,
    private val backgroundScope: CoroutineScope,
    private val closeFailure: Exception? = null,
) {
    private val directory = Files.createTempDirectory(context.cacheDir.toPath(), "sample-stores-").toFile()
    val job = SupervisorJob(backgroundScope.coroutineContext[Job])
    private val scope = CoroutineScope(backgroundScope.coroutineContext + job)

    val configs =
        SDKConfigDataStore(
            context,
            DataStoreFactory.create(JsonTestSerializer(SDKConfigData(), SDKConfigData.serializer()), scope = scope) {
                directory.resolve("config.json")
            },
        )
    val users =
        UserDataStore(
            context,
            DataStoreFactory.create(JsonTestSerializer(UserData(), UserData.serializer()), scope = scope) {
                directory.resolve("user.json")
            },
        )

    /**
     * Wait for finite launch jobs, including delayed writes, without joining the live store job.
     * Fail after one second of virtual time if another background child (such as a collector) stays active.
     * Joined jobs must stay on the test scheduler: awaiting a real dispatcher such as [Dispatchers.IO]
     * can hit the virtual bound before that work completes.
     */
    suspend fun joinLaunchJobs() {
        val launchJobs =
            backgroundScope.coroutineContext.job.children
                .filter { it !== job }
                .toList()
        val completed =
            withTimeoutOrNull(1.seconds) {
                launchJobs.joinAll()
                true
            }
        if (completed != true) {
            throw AssertionError(
                "Launch jobs did not complete within one second of virtual time; " +
                    "check for an infinite collector or a launch job slower than 1 s. " +
                    "Still-active children: ${launchJobs.filter { it.isActive }}",
            )
        }
    }

    suspend fun close() {
        withContext(NonCancellable) {
            try {
                job.cancelAndJoin()
            } finally {
                // Best effort: cleanup must not replace an assertion failure or timeout.
                runCatching { directory.deleteRecursively() }
            }
        }
        closeFailure?.let { throw it }
    }
}

/** Clear ViewModels before closing their stores, and always release the test Main dispatcher. */
@OptIn(ExperimentalCoroutinesApi::class)
internal suspend fun TestScope.closeSampleViewModels(
    store: ViewModelStore,
    dataStores: SampleDataStoreTestFixture,
    afterClose: () -> Unit = {},
) {
    try {
        store.clear()
        testScheduler.runCurrent()
    } finally {
        try {
            dataStores.close()
        } finally {
            try {
                afterClose()
            } finally {
                Dispatchers.resetMain()
            }
        }
    }
}

/** Small fixture files need no IO dispatcher hop; all reads and writes stay in virtual time. */
private class JsonTestSerializer<T>(
    override val defaultValue: T,
    private val serializer: KSerializer<T>,
) : Serializer<T> {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun readFrom(input: InputStream): T = json.decodeFromString(serializer, input.readBytes().decodeToString())

    override suspend fun writeTo(
        t: T,
        output: OutputStream,
    ) {
        output.write(json.encodeToString(serializer, t).encodeToByteArray())
    }
}
