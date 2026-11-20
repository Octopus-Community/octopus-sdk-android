package com.octopuscommunity.sdk

import com.octopuscommunity.sdk.data.local.datastore.PreferencesDataStore
import com.octopuscommunity.sdk.data.mapper.CommentMapper
import com.octopuscommunity.sdk.data.remote.client.OctopusGrpcClient
import com.octopuscommunity.sdk.domain.BridgeShareTokenProviderHolder
import com.octopuscommunity.sdk.domain.event.LifecycleEvents
import com.octopuscommunity.sdk.domain.repository.CommentRepository
import com.octopuscommunity.sdk.domain.repository.CommunityConfigRepository
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository
import com.octopuscommunity.sdk.domain.repository.EventRepository
import com.octopuscommunity.sdk.domain.repository.GroupRepository
import com.octopuscommunity.sdk.domain.repository.LanguageRepository
import com.octopuscommunity.sdk.domain.repository.NotificationRepository
import com.octopuscommunity.sdk.domain.repository.PostRepository
import com.octopuscommunity.sdk.domain.repository.ProfileRepository
import com.octopuscommunity.sdk.domain.repository.ReactionsRepository
import com.octopuscommunity.sdk.domain.repository.RootFeedRepository
import com.octopuscommunity.sdk.domain.repository.SessionRepository
import com.octopuscommunity.sdk.domain.repository.TrackerRepository
import com.octopuscommunity.sdk.domain.repository.UserCommentRepository
import com.octopuscommunity.sdk.domain.repository.UserRepository
import com.octopuscommunity.sdk.domain.usecase.ConnectUseCase
import com.octopuscommunity.sdk.domain.usecase.DisconnectUseCase
import com.octopuscommunity.sdk.domain.usecase.OverrideDefaultLocaleUseCase
import com.octopuscommunity.sdk.domain.usecase.RefreshEntitlementsUseCase
import com.octopuscommunity.sdk.domain.usecase.StartCollectingDrivenLoginUseCase
import com.octopuscommunity.sdk.domain.usecase.StartCollectingSessionsUseCase
import org.koin.core.Koin
import kotlin.reflect.KClass

/** Resolves host entry points and cyclic edges before a graph can be retired by another thread. */
internal class ContainerDependencies(
    private val koin: Koin,
) {
    private inline fun <reified T : Any> MutableMap<KClass<*>, Any>.resolve() {
        this[T::class] = koin.get<T>()
    }

    private val values: Map<KClass<*>, Any> by lazy {
        buildMap {
            resolve<CommentRepository>()
            resolve<CommunityConfigRepository>()
            resolve<ConnectionRepository>()
            resolve<EventRepository>()
            resolve<NotificationRepository>()
            resolve<PostRepository>()
            resolve<ProfileRepository>()
            resolve<RootFeedRepository>()
            resolve<SessionRepository>()
            resolve<GroupRepository>()
            resolve<UserRepository>()
            resolve<UserCommentRepository>()
            resolve<ReactionsRepository>()
            resolve<TrackerRepository>()
            resolve<ClientABTestTracker>()
            resolve<LanguageRepository>()
            resolve<OctopusGrpcClient>()
            resolve<ConnectUseCase>()
            resolve<DisconnectUseCase>()
            resolve<PreferencesDataStore>()
            resolve<OverrideDefaultLocaleUseCase>()
            resolve<RefreshEntitlementsUseCase>()
            resolve<BridgeShareTokenProviderHolder>()
            resolve<LifecycleEvents>()
            resolve<StartCollectingSessionsUseCase>()
            resolve<StartCollectingDrivenLoginUseCase>()
            resolve<UserCacheClearer>()
            koin.get<CommentMapper>().postMapper
            val client = getValue(OctopusGrpcClient::class) as OctopusGrpcClient
            client.userRepository
            client.preferencesRepository
            client.sessionRepository
            client.languageRepository
        }
    }

    fun resolveDeferredEdges() {
        values
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(type: KClass<T>): T = values.getValue(type) as T
}
