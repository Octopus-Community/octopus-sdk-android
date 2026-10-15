package com.octopuscommunity.sdk.sample

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import com.octopuscommunity.sdk.ApiServer
import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.sample.data.AppUpdateChecker
import com.octopuscommunity.sdk.sample.data.CommunitySessionTracker
import com.octopuscommunity.sdk.sample.data.SDKConfigResolver
import com.octopuscommunity.sdk.sample.data.SDKTestFakes
import com.octopuscommunity.sdk.sample.data.SDKTestFakesLauncher
import com.octopuscommunity.sdk.sample.data.datastore.SDKConfigDataStore
import com.octopuscommunity.sdk.sample.data.datastore.UserDataStore
import com.octopuscommunity.sdk.sample.data.model.ServerOverride
import com.octopuscommunity.sdk.sample.data.model.toOverrideOrNull
import com.octopuscommunity.sdk.sample.data.usecase.CommunityConnectUseCase
import com.octopuscommunity.sdk.sample.extensions.LocalExtensions
import com.octopuscommunity.sdk.sample.ui.community.ClientProfileViewModel
import com.octopuscommunity.sdk.sample.ui.community.HostOverrides
import com.octopuscommunity.sdk.sample.ui.config.ConfigViewModel
import com.octopuscommunity.sdk.sample.ui.scenarios.ScenariosViewModel
import com.octopuscommunity.sdk.sample.ui.settings.AccountViewModel
import com.octopuscommunity.sdk.sample.ui.settings.EditUserViewModel
import com.octopuscommunity.sdk.sample.ui.settings.HostIntegrationViewModel
import com.octopuscommunity.sdk.sample.ui.settings.LoginViewModel
import com.octopuscommunity.sdk.sample.ui.settings.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

class SampleApplication : Application() {
    private val preferencesName = "octopus_sample_preferences"
    private var sdkStarted = false

    private val sdkConfigDataStore: SDKConfigDataStore by inject()
    private val userDataStore: UserDataStore by inject()
    private val communityConnectUseCase: CommunityConnectUseCase by inject()
    private val communitySessionTracker: CommunitySessionTracker by inject()

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@SampleApplication)
            modules(
                // Data layer
                module {
                    single { SDKConfigDataStore(context = androidContext()) }
                    single { UserDataStore(context = androidContext()) }
                    single<SharedPreferences> {
                        androidContext().getSharedPreferences(
                            preferencesName,
                            Context.MODE_PRIVATE,
                        )
                    }
                    // Share successful sign-ins with every screen observing disconnect errors.
                    single { CommunityConnectUseCase(userDataStore = get()) }
                    single { AppUpdateChecker(context = androidContext()) }
                    single { CommunitySessionTracker() }
                    single<CoroutineScope> { MainScope() }
                },
                // ViewModels
                module {
                    viewModel { MainViewModel(configDataStore = get(), appUpdateChecker = get()) }
                    viewModel {
                        ConfigViewModel(
                            configDataStore = get(),
                            userDataStore = get(),
                            communityConnectUseCase = get(),
                            appScope = get(),
                            savedStateHandle = get(),
                        )
                    }
                    viewModel { HostIntegrationViewModel(configDataStore = get()) }
                    viewModel {
                        ScenariosViewModel(
                            userDataStore = get(),
                            communityConnectUseCase = get(),
                            configDataStore = get(),
                            communitySessionTracker = get(),
                        )
                    }
                    viewModel {
                        SettingsViewModel(
                            configDataStore = get(),
                            userDataStore = get(),
                            preferences = get(),
                        )
                    }
                    viewModel {
                        AccountViewModel(
                            userDataStore = get(),
                            communityConnectUseCase = get(),
                        )
                    }
                    viewModel {
                        LoginViewModel(
                            userDataStore = get(),
                            communityConnectUseCase = get(),
                            configDataStore = get(),
                        )
                    }
                    viewModel {
                        EditUserViewModel(
                            userDataStore = get(),
                            communityConnectUseCase = get(),
                        )
                    }
                    viewModel { ClientProfileViewModel() }
                },
            )
        }
        if (!SDKTestFakesLauncher.isAvailable) {
            startSDK()
        }
    }

    /** In demoDebug, start after the Activity reads QA extras, or when FCM starts the process. */
    fun startSDK() {
        if (sdkStarted) return
        sdkStarted = true

        // SDK (re)initialization based on stored config.
        //
        // Only the fields that actually decide *which SDK* is running are allowed to re-trigger it.
        // `configFlow` carries the whole SDKConfig, cosmetics included: collecting it raw meant an
        // Appearance switch (System -> Dark) wrote a new config, re-entered this collector and called
        // switchCommunity(), which tears the session down and reconnects it asynchronously — the
        // account momentarily reading "Not connected" and the connection status flashing GUEST after a change that
        // announces no restart. Theme, appearance and other host callbacks apply live through
        // Compose. Unified Profile activation is in the key: toggling just its callback must also
        // restart the SDK and reapply (or clear) its backend flag override.
        MainScope().launch {
            val configAndFakes =
                combine(sdkConfigDataStore.configFlow, SDKTestFakesLauncher.request) { config, fakes ->
                    config to fakes
                }
            configAndFakes
                .distinctUntilChangedBy { (config, fakes) ->
                    fakes ?: config?.sdkInitKey
                }.collect { (config, fakes) ->
                    if (fakes != null) {
                        SDKTestFakes.start(applicationContext, fakes)
                        communitySessionTracker.onCommunityTargeted()
                        // Never reconnect the persisted host user into a synthetic QA session.
                        return@collect
                    }
                    if (config != null) {
                        val localTarget = LocalExtensions.resolve(config)
                        if (config.localConfiguration != null && localTarget == null) {
                            Log.w("OctopusSDKSample", "Local backend selection is unusable. Falling back to the default community.")
                            sdkConfigDataStore.updateConfig { it?.copy(localConfiguration = null) }
                            return@collect
                        }

                        val apiKey = SDKConfigResolver.resolveApiKey(config)
                        val connectionMode = SDKConfigResolver.resolveConnectionMode(config)
                        // A selected environment wins over the Config screen's server setting: it is the more
                        // specific intent, and mixing the two would point the SDK at one backend with
                        // another one's API key.
                        val serverOverride =
                            localTarget?.let { ServerOverride(host = it.apiHost, port = 443) }
                                ?: config.serverEnvironment.toOverrideOrNull()
                        // ApiServer's throwing constructor validates the host. A persisted
                        // Custom config with an invalid host (e.g. typo like "https://x")
                        // would otherwise crash the sample on every relaunch — recovery
                        // would require `adb shell pm clear`. Catch the validation error
                        // and fall back to the BuildConfig default; the user keeps their
                        // typo visible in the Config screen so they can fix it manually.
                        //
                        // Only the Custom path can reach the catch: a selected host was already
                        // validated by the selected configuration resolver (same host, same validation), so this
                        // fallback can no longer pair a null server with a selected environment's API key.
                        val apiServer =
                            serverOverride?.let { override ->
                                try {
                                    ApiServer(host = override.host, port = override.port)
                                } catch (e: ApiServer.ValidationError) {
                                    Log.e(
                                        "OctopusSDKSample",
                                        "Invalid ApiServer config " +
                                            "(${override.host}:${override.port}). " +
                                            "Falling back to the default endpoint. Fix the " +
                                            "host in the Config screen to override again.",
                                        e,
                                    )
                                    Toast
                                        .makeText(
                                            applicationContext,
                                            "Invalid server host: ${e.message}",
                                            Toast.LENGTH_LONG,
                                        ).show()
                                    null
                                }
                            }
                        if (OctopusSDK.isInitialised) {
                            OctopusSDK.switchCommunity(
                                context = applicationContext,
                                apiKey = apiKey,
                                connectionMode = connectionMode,
                                apiServer = apiServer,
                            )
                        } else {
                            OctopusSDK.initialize(
                                context = applicationContext,
                                apiKey = apiKey,
                                connectionMode = connectionMode,
                                apiServer = apiServer,
                            )
                        }
                        // Announced here — after the call, so nothing reacting to it can resolve
                        // against the container that has just been torn down, and before the first
                        // suspension point below, so it cannot land *after* the UI that reads it.
                        // `initialize` is what flips `isInitialisedFlow`, which is what lets
                        // `MainScaffold` compose; a session announced after the reconnect suspended
                        // would reach a Community tab that had already taken the previous number as
                        // its baseline and make it rebuild a surface it had just built (internal change). This
                        // collector runs on `MainScope`, so no frame can slip in between.
                        // The re-init just dropped every SDK debug override: put the Unified Profile
                        // flag back to what the Unified Profile scenario asks for, and keep
                        // HostOverrides in step instead of showing a value the SDK lost.
                        HostOverrides.applyExposeClientUserIdOverride(
                            SDKConfigResolver.resolveExposeClientUserIdOverride(config),
                        )
                        communitySessionTracker.onCommunityTargeted()
                        // SSO is the only auth mode the sample supports — reconnect the
                        // stored user after (re)initialization, unless they signed out.
                        userDataStore.getUserToConnect()?.let {
                            communityConnectUseCase(it)
                        }
                    } else {
                        OctopusSDK.reset()
                        OctopusSDK.stop()
                    }
                }
        }
    }
}
