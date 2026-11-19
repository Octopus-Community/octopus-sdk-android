package com.octopuscommunity.sdk.sample.ui.scenarios.sections

import android.content.Context
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import com.octopuscommunity.sdk.ApiServer
import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.sample.data.SDKConfigResolver
import com.octopuscommunity.sdk.sample.data.SampleFixtures
import com.octopuscommunity.sdk.sample.data.availableAuthPresets
import com.octopuscommunity.sdk.sample.data.model.SDKConfig
import com.octopuscommunity.sdk.sample.data.model.toOverrideOrNull
import com.octopuscommunity.sdk.sample.extensions.LocalExtensions
import com.octopuscommunity.sdk.sample.ui.community.HostOverrides
import com.octopuscommunity.sdk.sample.ui.scenarios.ScenarioResult
import com.octopuscommunity.sdk.ui.OctopusDestination
import com.octopuscommunity.sdk.ui.navigateToOctopusPost
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@Composable
fun HostLifecyclesSection(
    config: SDKConfig?,
    onSdkSessionEnded: () -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val bench = remember(config) { HostLifecycleBench(context, config) }
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf("Not run yet") }
    val resultIntoView = remember { BringIntoViewRequester() }
    val communityIntoView = remember { BringIntoViewRequester() }
    // A physical Back during this preset is also confined to its embedded NavDisplay.
    // Normal scenario navigation resumes once the run has written its final result.
    BackHandler(enabled = running && bench.preset == HostLifecyclePreset.TabBack) {
        bench.session?.popTab?.invoke()
    }
    LaunchedEffect(result) {
        when {
            result.startsWith("Running:") -> communityIntoView.bringIntoView()
            result != "Not run yet" -> resultIntoView.bringIntoView()
        }
    }

    DisposableEffect(bench) {
        onDispose {
            if (bench.dispose()) onSdkSessionEnded()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("1 · Mount once, then unmount/remount five times; open the post and its comments.")
        Text("2 · Pop a Navigation 3 bottom-bar tab; clear its store before the exit animation ends.")
        Text("3 · Push a host screen, detach the ComposeView, then re-attach the same view.")
        Text("4 · Switch to another configured demo community while composed; click the first SDK post.")
        Text("5 · Stop and initialize with the same configuration while composed; click the first SDK post.")
        Text("Process-death restore: background the app, then run adb shell am kill <package> from QA.")
        Text(
            "OK waits for rendered post content and a further 3 seconds for deferred comment mapping. " +
                "Missing setup is SKIPPED. SDK crashes remain visible. Leaving restores the configured community.",
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            value = bench.postId,
            onValueChange = { bench.postId = it },
            label = { Text("Post with comments (in the target demo community)") },
            enabled = !running,
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("hostLifecycles-post-edit"),
        )

        // Literal ids are intentional: the QA catalog's source scanner reads these verbatim.
        val ids =
            listOf(
                "qa-preset-hostLifecycles-1",
                "qa-preset-hostLifecycles-2",
                "qa-preset-hostLifecycles-3",
                "qa-preset-hostLifecycles-4",
                "qa-preset-hostLifecycles-5",
            )
        HostLifecyclePreset.entries.forEachIndexed { index, preset ->
            Button(
                onClick = {
                    running = true
                    result = "Running: ${preset.label}"
                    scope.launch {
                        try {
                            bench.preset = preset
                            result = runAndReportHostLifecyclePreset(preset, bench)
                        } finally {
                            running = false
                        }
                    }
                },
                enabled = !running,
                modifier = Modifier.fillMaxWidth().testTag(ids[index]),
            ) { Text(preset.label) }
        }
        Box(Modifier.bringIntoViewRequester(resultIntoView)) {
            ScenarioResult(text = result, isError = result.startsWith("FAILED:"), resultTestTag = "hostLifecycles-result")
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(520.dp)
                .bringIntoViewRequester(communityIntoView)
                .testTag("hostLifecycles-community-view"),
        ) {
            bench.session?.let { session ->
                key(session) { HostLifecycleContent(session) }
            }
        }
    }
}

internal suspend fun runAndReportHostLifecyclePreset(
    preset: HostLifecyclePreset,
    actions: HostLifecycleActions,
): String = runHostLifecyclePreset(preset, actions).also { Log.i("HostLifecycles", it) }

internal class HostTabStoreOwner : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
}

internal class HostLifecycleSession(
    val preset: HostLifecyclePreset,
    val theme: SDKConfig.Theme,
) {
    val communityStore = HostTabStoreOwner()
    val ready = CompletableDeferred<Unit>()
    val disposed = CompletableDeferred<Unit>()
    val tabExited = CompletableDeferred<Unit>()
    var navController: NavHostController? = null
    var popTab: () -> Unit = {}
    var covered by mutableStateOf(false)
    var coverApplied = CompletableDeferred<Unit>()
    var revision by mutableStateOf(0)
    val appliedRevision = MutableStateFlow(-1)
    var semantics: HostCommunitySemantics? = null
}

/** Owns only sample host state; never changes the SDK's crash/exception policy. */
internal class HostLifecycleBench(
    private val context: Context,
    private val config: SDKConfig?,
) : HostLifecycleActions {
    var preset = HostLifecyclePreset.Remount
    var postId by mutableStateOf(SampleFixtures.postText)
    var session by mutableStateOf<HostLifecycleSession?>(null)
        private set
    private var activeConfig: SDKConfig? = null
    private var sdkChanged = false

    override fun setupIssue(): String? {
        val current = activeConfig ?: config
        return hostLifecycleSetupIssue(
            preset = preset,
            config = current,
            configuredKey = current?.let(SDKConfigResolver::resolveApiKey).orEmpty(),
            postId = postId,
            sdkInitialized = OctopusSDK.isInitialised,
            hasOtherCommunity = current?.let { alternateConfig(it) != null } == true,
        )
    }

    override suspend fun mount() {
        // Presets 1/2/3/5 use the configured target, including prod builds with no named presets.
        val configured = checkNotNull(config)
        session = HostLifecycleSession(preset, configured.theme)
        withTimeout(10_000) { session!!.ready.await() }
        settleHostFrames()
    }

    override suspend fun unmount() {
        val previous = session ?: return
        session = null
        withTimeout(10_000) { previous.disposed.await() }
        settleHostFrames()
    }

    override suspend fun openPost() {
        val nav = checkNotNull(session?.navController)
        nav.navigateToOctopusPost(postId.trim())
        awaitPostDestination(nav)
    }

    override suspend fun clickPostInCommunity() {
        val current = checkNotNull(session)
        withTimeout(30_000) {
            while (current.semantics?.clickFirstPost() != true) delay(100)
        }
        awaitPostDestination(checkNotNull(current.navController))
    }

    private suspend fun awaitPostDestination(nav: NavHostController) {
        withTimeout(10_000) {
            nav.currentBackStackEntryFlow.first { it.destination.hasRoute<OctopusDestination.PostDetails>() }
        }
        settleHostFrames()
    }

    override suspend fun awaitPostContent() {
        val current = checkNotNull(session)
        awaitHostPostContent { current.semantics?.postContentDisplayed() == true }
    }

    override suspend fun clearTabOnBack() {
        val current = checkNotNull(session)
        current.popTab()
        val revision = current.revision
        withTimeout(10_000) { current.appliedRevision.first { it >= revision } }
        withTimeout(10_000) { current.tabExited.await() }
        settleHostFrames()
    }

    override suspend fun detachAndReattach() {
        val current = checkNotNull(session)
        current.coverApplied = CompletableDeferred()
        current.covered = true
        withTimeout(10_000) { current.coverApplied.await() }
        settleHostFrames()
        current.coverApplied = CompletableDeferred()
        current.covered = false
        withTimeout(10_000) { current.coverApplied.await() }
        settleHostFrames()
    }

    override suspend fun switchCommunity() {
        val current = activeConfig ?: checkNotNull(config)
        val next = checkNotNull(alternateConfig(current))
        // Even a failure after switchCommunity rebuilt its container requires restoration on exit.
        activeConfig = next
        sdkChanged = true
        OctopusSDK.switchCommunity(
            context = context,
            apiKey = SDKConfigResolver.resolveApiKey(next),
            connectionMode = SDKConfigResolver.resolveConnectionMode(next),
            apiServer = server(next),
        )
    }

    override suspend fun restartSdk() {
        val current = activeConfig ?: checkNotNull(config)
        sdkChanged = true
        OctopusSDK.stop()
        initialize(current)
    }

    override suspend fun recompose() {
        val current = checkNotNull(session)
        val revision = ++current.revision
        withTimeout(10_000) { current.appliedRevision.first { it == revision } }
        settleHostFrames()
    }

    fun dispose(): Boolean {
        session?.communityStore?.viewModelStore?.clear()
        // Keep other scenarios aligned with the persisted config after a temporary switch.
        if (activeConfig != null) config?.let(::initialize)
        if (sdkChanged && config != null) {
            HostOverrides.applyExposeClientUserIdOverride(SDKConfigResolver.resolveExposeClientUserIdOverride(config))
        }
        return sdkChanged
    }

    private fun initialize(target: SDKConfig) {
        OctopusSDK.initialize(
            context = context,
            apiKey = SDKConfigResolver.resolveApiKey(target),
            connectionMode = SDKConfigResolver.resolveConnectionMode(target),
            apiServer = server(target),
        )
    }

    private fun alternateConfig(current: SDKConfig): SDKConfig? {
        if (current.localConfiguration != null) return null
        val key = SDKConfigResolver.resolveApiKey(current)
        val other = availableAuthPresets.firstOrNull { it.key != key } ?: return null
        return current.copy(
            authKind = SDKConfig.AuthKind.SSO(other.managedFields.toList(), other.forceLogin),
            customCommunity = true,
            customApiKey = other.key,
        )
    }

    private fun server(target: SDKConfig): ApiServer? =
        LocalExtensions.resolve(target)?.let { ApiServer(it.apiHost, 443) }
            ?: target.serverEnvironment.toOverrideOrNull()?.let { ApiServer(it.host, it.port) }
}

internal fun hostLifecycleSetupIssue(
    preset: HostLifecyclePreset,
    config: SDKConfig?,
    configuredKey: String,
    postId: String,
    sdkInitialized: Boolean,
    hasOtherCommunity: Boolean,
): String? =
    when {
        config == null -> "Configure a community first"
        configuredKey.isBlank() -> "The configured community has no API key"
        !sdkInitialized -> "Initialize the SDK before running this preset"
        preset in listOf(HostLifecyclePreset.Remount, HostLifecyclePreset.Detach) && postId.isBlank() -> "Set a post with comments first"
        preset == HostLifecyclePreset.Switch && !hasOtherCommunity ->
            "Preset 4 needs two distinct demo communities configured on the same backend"
        else -> null
    }

internal suspend fun settleHostFrames() {
    // Two frame boundaries let the pending snapshot apply and its effects/disposal run.
    repeat(2) { withFrameNanos { } }
}
