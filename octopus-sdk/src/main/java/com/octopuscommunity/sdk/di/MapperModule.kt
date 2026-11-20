package com.octopuscommunity.sdk.di

import com.octopuscommunity.sdk.data.mapper.CommentMapper
import com.octopuscommunity.sdk.data.mapper.CommunityConfigMapper
import com.octopuscommunity.sdk.data.mapper.FeedMapper
import com.octopuscommunity.sdk.data.mapper.GroupMapper
import com.octopuscommunity.sdk.data.mapper.MediaMapper
import com.octopuscommunity.sdk.data.mapper.ModerationMapper
import com.octopuscommunity.sdk.data.mapper.NotificationMapper
import com.octopuscommunity.sdk.data.mapper.PostMapper
import com.octopuscommunity.sdk.data.mapper.ProfileMapper
import com.octopuscommunity.sdk.data.mapper.ReactionMapper
import com.octopuscommunity.sdk.data.mapper.ReplyMapper
import com.octopuscommunity.sdk.data.mapper.SectionMapper
import com.octopuscommunity.sdk.data.mapper.SessionMapper
import com.octopuscommunity.sdk.data.mapper.SettingsMapper
import com.octopuscommunity.sdk.data.mapper.StatusMapper
import com.octopuscommunity.sdk.data.mapper.TrackerEventMapper
import com.octopuscommunity.sdk.data.mapper.TranslationMapper
import com.octopuscommunity.sdk.data.mapper.UserConfigMapper
import com.octopuscommunity.sdk.data.mapper.UserMapper
import kotlinx.serialization.json.Json
import org.koin.dsl.module

internal val mapperModule get() =
    module {
        single<Json> { Json { ignoreUnknownKeys = true } }
        single<CommentMapper> {
            val postMapper = get<PostMapper>()
            CommentMapper(
                preferencesDataStore = get(),
                statusMapper = get(),
                translationMapper = get(),
                profileMapper = get(),
                postMapper = lazyOf(postMapper),
                mediaMapper = get(),
                feedMapper = get(),
                reactionMapper = get(),
            )
        }
        single<CommunityConfigMapper> { CommunityConfigMapper() }
        single<FeedMapper> { FeedMapper() }
        single<MediaMapper> { MediaMapper(imageLoader = get(), imageRequestBuilder = get()) }
        single<ModerationMapper> { ModerationMapper() }
        single<NotificationMapper> { NotificationMapper(profileMapper = get()) }
        single<PostMapper> {
            PostMapper(
                preferencesDataStore = get(),
                statusMapper = get(),
                translationMapper = get(),
                profileMapper = get(),
                groupMapper = get(),
                mediaMapper = get(),
                reactionMapper = get(),
                feedMapper = get(),
            )
        }
        single<ProfileMapper> {
            ProfileMapper(
                mediaMapper = get(),
                feedMapper = get(),
            )
        }
        single<ReactionMapper> { ReactionMapper() }
        single<ReplyMapper> {
            ReplyMapper(
                preferencesDataStore = get(),
                statusMapper = get(),
                translationMapper = get(),
                profileMapper = get(),
                commentMapper = get(),
                mediaMapper = get(),
                reactionMapper = get(),
            )
        }
        single<SectionMapper> { SectionMapper() }
        single<SessionMapper> { SessionMapper() }
        single<SettingsMapper> { SettingsMapper() }
        single<StatusMapper> { StatusMapper(moderationMapper = get()) }
        single<GroupMapper> {
            GroupMapper(
                preferencesDataStore = get(),
                statusMapper = get(),
                sectionMapper = get(),
                translationMapper = get(),
            )
        }
        single<TrackerEventMapper> { TrackerEventMapper() }
        single<TranslationMapper> { TranslationMapper() }
        single<UserConfigMapper> { UserConfigMapper() }
        single<UserMapper> { UserMapper(mediaMapper = get()) }
    }
