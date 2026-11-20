package com.octopuscommunity.sdk.di

import com.octopuscommunity.sdk.UserCacheClearer
import com.octopuscommunity.sdk.communityClosurePreferences
import com.octopuscommunity.sdk.data.remote.client.OctopusGrpcClient
import com.octopuscommunity.sdk.data.repository.CommentRepositoryImpl
import com.octopuscommunity.sdk.data.repository.CommunityClosureRepositoryImpl
import com.octopuscommunity.sdk.data.repository.CommunityConfigRepositoryImpl
import com.octopuscommunity.sdk.data.repository.ExternalLinkRepositoryImpl
import com.octopuscommunity.sdk.data.repository.GroupRepositoryImpl
import com.octopuscommunity.sdk.data.repository.LanguageRepositoryImpl
import com.octopuscommunity.sdk.data.repository.ModerationRepositoryImpl
import com.octopuscommunity.sdk.data.repository.NotificationRepositoryImpl
import com.octopuscommunity.sdk.data.repository.PostRepositoryImpl
import com.octopuscommunity.sdk.data.repository.PreferenceRepositoryImpl
import com.octopuscommunity.sdk.data.repository.ProfileRepositoryImpl
import com.octopuscommunity.sdk.data.repository.ReactionsRepositoryImpl
import com.octopuscommunity.sdk.data.repository.ReplyRepositoryImpl
import com.octopuscommunity.sdk.data.repository.RootFeedRepositoryImpl
import com.octopuscommunity.sdk.data.repository.SSOConnectionRepositoryImpl
import com.octopuscommunity.sdk.data.repository.SessionRepositoryImpl
import com.octopuscommunity.sdk.data.repository.TrackerRepositoryImpl
import com.octopuscommunity.sdk.data.repository.TranslationRepositoryImpl
import com.octopuscommunity.sdk.data.repository.UserCommentRepositoryImpl
import com.octopuscommunity.sdk.data.repository.UserRepositoryImpl
import com.octopuscommunity.sdk.domain.BridgeShareTokenProviderHolder
import com.octopuscommunity.sdk.domain.DrivenLoginTracker
import com.octopuscommunity.sdk.domain.model.ConnectionMode
import com.octopuscommunity.sdk.domain.repository.CommentRepository
import com.octopuscommunity.sdk.domain.repository.CommunityClosureRepository
import com.octopuscommunity.sdk.domain.repository.CommunityConfigRepository
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository
import com.octopuscommunity.sdk.domain.repository.ExternalLinkRepository
import com.octopuscommunity.sdk.domain.repository.GroupRepository
import com.octopuscommunity.sdk.domain.repository.LanguageRepository
import com.octopuscommunity.sdk.domain.repository.ModerationRepository
import com.octopuscommunity.sdk.domain.repository.NotificationRepository
import com.octopuscommunity.sdk.domain.repository.PostRepository
import com.octopuscommunity.sdk.domain.repository.PreferenceRepository
import com.octopuscommunity.sdk.domain.repository.ProfileRepository
import com.octopuscommunity.sdk.domain.repository.ReactionsRepository
import com.octopuscommunity.sdk.domain.repository.ReplyRepository
import com.octopuscommunity.sdk.domain.repository.RootFeedRepository
import com.octopuscommunity.sdk.domain.repository.SessionRepository
import com.octopuscommunity.sdk.domain.repository.TrackerRepository
import com.octopuscommunity.sdk.domain.repository.TranslationRepository
import com.octopuscommunity.sdk.domain.repository.UserCommentRepository
import com.octopuscommunity.sdk.domain.repository.UserRepository
import com.octopuscommunity.sdk.purgeClosedCommunity
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

internal fun repositoryModule(
    apiKey: String,
    connectionMode: ConnectionMode,
    linksHost: String,
) = module {
    single { UserCacheClearer(getKoin()) }
    single { BridgeShareTokenProviderHolder() }
    single { DrivenLoginTracker() }
    single<LanguageRepository> {
        LanguageRepositoryImpl(preferencesDataStore = get())
    }
    single<CommentRepository> {
        CommentRepositoryImpl(
            securedDataStore = get(),
            preferencesDataStore = get(),
            userConfigDao = get(),
            profileDao = get(),
            feedDao = get(),
            commentDao = get(),
            userService = get(),
            notificationService = get(),
            feedService = get(),
            objectService = get(),
            userMapper = get(),
            userConfigMapper = get(),
            profileMapper = get(),
            feedMapper = get(),
            reactionMapper = get(),
            commentMapper = get(),
            replyMapper = get(),
        )
    }
    single<CommunityClosureRepository> {
        val grpcClient: OctopusGrpcClient = get()
        CommunityClosureRepositoryImpl(
            preferences = communityClosurePreferences(androidContext(), apiKey),
            setCallsBlocked = { grpcClient.callsBlocked = it },
            purge = { getKoin().purgeClosedCommunity() },
        )
    }
    single<CommunityConfigRepository> {
        CommunityConfigRepositoryImpl(
            communityConfigDao = get(),
            apiKeyService = get(),
            communityConfigMapper = get(),
            closureRepository = get(),
        )
    }
    single<ConnectionRepository> {
        val cacheClearer = get<UserCacheClearer>()
        when (connectionMode) {
            is ConnectionMode.SSO ->
                SSOConnectionRepositoryImpl(
                    connectionMode = connectionMode,
                    securedDataStore = get(),
                    preferencesDataStore = get(),
                    userService = get(),
                    notificationService = get(),
                    userConfigDao = get(),
                    profileDao = get(),
                    userMapper = get(),
                    userConfigMapper = get(),
                    profileMapper = get(),
                    clearUserCaches = { cacheClearer.clear() },
                    closureRepository = get(),
                )
        }
    }
    single<ExternalLinkRepository> {
        ExternalLinkRepositoryImpl(linksHost = linksHost, apiKey = apiKey)
    }
    single<ReactionsRepository> {
        ReactionsRepositoryImpl(
            objectService = get(),
            reactionMapper = get(),
            profileMapper = get(),
        )
    }
    single<ModerationRepository> {
        ModerationRepositoryImpl(
            objectService = get(),
            userService = get(),
            moderationMapper = get(),
        )
    }
    single<NotificationRepository> {
        NotificationRepositoryImpl(
            securedDataStore = get(),
            preferencesDataStore = get(),
            settingsDataStore = get(),
            notificationDao = get(),
            notificationService = get(),
            settingsMapper = get(),
            notificationMapper = get(),
        )
    }
    single<PostRepository> {
        PostRepositoryImpl(
            securedDataStore = get(),
            preferencesDataStore = get(),
            userConfigDao = get(),
            profileDao = get(),
            feedDao = get(),
            postDao = get(),
            commentDao = get(),
            userService = get(),
            notificationService = get(),
            feedService = get(),
            objectService = get(),
            userMapper = get(),
            userConfigMapper = get(),
            profileMapper = get(),
            feedMapper = get(),
            reactionMapper = get(),
            postMapper = get(),
            commentMapper = get(),
            bridgeShareTokenProviderHolder = get(),
        )
    }
    single<PreferenceRepository> {
        PreferenceRepositoryImpl(
            preferencesDataStore = get(),
        )
    }
    single<ProfileRepository> {
        ProfileRepositoryImpl(
            securedDataStore = get(),
            preferencesDataStore = get(),
            userConfigDao = get(),
            profileDao = get(),
            userService = get(),
            notificationService = get(),
            userMapper = get(),
            userConfigMapper = get(),
            profileMapper = get(),
            closureRepository = get(),
        )
    }
    single<ReplyRepository> {
        ReplyRepositoryImpl(
            securedDataStore = get(),
            preferencesDataStore = get(),
            userConfigDao = get(),
            profileDao = get(),
            feedDao = get(),
            replyDao = get(),
            userService = get(),
            notificationService = get(),
            feedService = get(),
            objectService = get(),
            userMapper = get(),
            userConfigMapper = get(),
            profileMapper = get(),
            feedMapper = get(),
            reactionMapper = get(),
            replyMapper = get(),
        )
    }
    single<RootFeedRepository> {
        RootFeedRepositoryImpl(
            feedDao = get(),
            feedService = get(),
            feedMapper = get(),
        )
    }
    single<SessionRepository> {
        SessionRepositoryImpl(
            sessionsDataStore = get(),
            userService = get(),
            sessionMapper = get(),
            closureRepository = get(),
        )
    }
    single<GroupRepository> {
        GroupRepositoryImpl(
            topicDao = get(),
            sectionDao = get(),
            objectService = get(),
            userService = get(),
            groupMapper = get(),
            sectionMapper = get(),
        )
    }
    single<TrackerRepository> {
        TrackerRepositoryImpl(
            trackerEventDao = get(),
            sessionsDataStore = get(),
            trackingService = get(),
            trackerEventMapper = get(),
            closureRepository = get(),
        )
    }
    single<TranslationRepository> {
        TranslationRepositoryImpl(
            sessionsDataStore = get(),
        )
    }
    single<UserCommentRepository> {
        UserCommentRepositoryImpl(
            feedService = get(),
            postDao = get(),
            commentDao = get(),
            replyDao = get(),
            postMapper = get(),
            commentMapper = get(),
            replyMapper = get(),
        )
    }
    single<UserRepository> {
        UserRepositoryImpl(
            securedDataStore = get(),
            preferencesDataStore = get(),
            userConfigDao = get(),
            userService = get(),
            notificationService = get(),
            userMapper = get(),
            userConfigMapper = get(),
            closureRepository = get(),
        )
    }
}
