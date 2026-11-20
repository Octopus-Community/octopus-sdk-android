package com.octopuscommunity.sdk.di

import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.data.remote.client.OctopusGrpcClient
import com.octopuscommunity.sdk.data.remote.service.ApiKeyService
import com.octopuscommunity.sdk.data.remote.service.FeedService
import com.octopuscommunity.sdk.data.remote.service.NotificationService
import com.octopuscommunity.sdk.data.remote.service.ObjectService
import com.octopuscommunity.sdk.data.remote.service.TrackingService
import com.octopuscommunity.sdk.data.remote.service.UserService
import kotlinx.coroutines.CoroutineScope
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.koin.dsl.onClose

/**
 * This Koin module provides dependencies related to network operations,
 * specifically the [OctopusGrpcClient] and its gRPC stubs.
 */
internal fun networkModule(
    sdkScope: CoroutineScope,
    serverHost: String,
    serverPort: Int,
    apiKey: String,
) = module {
    single {
        // Deferred dependencies must outlive the navigation scope that first resolves this single.
        val koin = getKoin()
        OctopusGrpcClient(
            context = androidContext(),
            serverHost = serverHost,
            serverPort = serverPort,
            apiKey = apiKey,
            userRepositoryLazy = lazy { koin.get() },
            preferencesRepositoryLazy = lazy { koin.get() },
            sessionRepositoryLazy = lazy { koin.get() },
            languageRepositoryLazy = lazy { koin.get() },
            sdkIdentity = { OctopusSDK.sdkIdentity },
        ).also { client ->
            koin.setProperty("octopusGrpcClient", client)
        }
    } onClose { it?.shutdown() }

    single { ApiKeyService(grpcClient = get()) }
    single { FeedService(grpcClient = get()) }
    single { NotificationService(grpcClient = get()) }
    single { ObjectService(grpcClient = get()) }
    single { TrackingService(grpcClient = get()) }
    single { UserService(grpcClient = get()) }
}
