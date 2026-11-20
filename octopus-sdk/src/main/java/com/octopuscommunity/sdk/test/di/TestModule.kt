package com.octopuscommunity.sdk.test.di

import com.octopuscommunity.sdk.domain.BridgeShareTokenProviderHolder
import com.octopuscommunity.sdk.domain.DrivenLoginTracker
import com.octopuscommunity.sdk.domain.event.LifecycleEvents
import com.octopuscommunity.sdk.domain.repository.CommentRepository
import com.octopuscommunity.sdk.domain.repository.CommunityClosureRepository
import com.octopuscommunity.sdk.domain.repository.CommunityConfigRepository
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository
import com.octopuscommunity.sdk.domain.repository.EventRepository
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
import com.octopuscommunity.sdk.test.observer.FakeLifecycleEvents
import com.octopuscommunity.sdk.test.repository.FakeCommentRepository
import com.octopuscommunity.sdk.test.repository.FakeCommunityClosureRepository
import com.octopuscommunity.sdk.test.repository.FakeCommunityConfigRepository
import com.octopuscommunity.sdk.test.repository.FakeConnectionRepository
import com.octopuscommunity.sdk.test.repository.FakeEventRepository
import com.octopuscommunity.sdk.test.repository.FakeExternalLinkRepository
import com.octopuscommunity.sdk.test.repository.FakeLanguageRepository
import com.octopuscommunity.sdk.test.repository.FakeModerationRepository
import com.octopuscommunity.sdk.test.repository.FakeNotificationRepository
import com.octopuscommunity.sdk.test.repository.FakePostRepository
import com.octopuscommunity.sdk.test.repository.FakePreferenceRepository
import com.octopuscommunity.sdk.test.repository.FakeReactionsRepository
import com.octopuscommunity.sdk.test.repository.FakeReplyRepository
import com.octopuscommunity.sdk.test.repository.FakeRootFeedRepository
import com.octopuscommunity.sdk.test.repository.FakeSessionRepository
import com.octopuscommunity.sdk.test.repository.FakeTopicRepository
import com.octopuscommunity.sdk.test.repository.FakeTrackerRepository
import com.octopuscommunity.sdk.test.repository.FakeTranslationRepository
import com.octopuscommunity.sdk.test.repository.FakeUserCommentRepository
import com.octopuscommunity.sdk.test.repository.FakeUserRepository
import org.koin.dsl.module

/**
 * Koin module binding every repository and observer to its in-memory fake, serving mock data with
 * no network. Used by the sample's test mode, previews and screenshot tests.
 */
val octopusTestModule =
    module {
        // Mirror the real repositoryModule so previews / screenshot tests that render the post editor
        // can resolve the bridge-share holder the create-post entry points wire into.
        single { BridgeShareTokenProviderHolder() }
        single { DrivenLoginTracker() }
        // Observers
        single<LifecycleEvents> { FakeLifecycleEvents() }
        single<EventRepository> { FakeEventRepository() }

        // Repositories
        single<CommentRepository> { FakeCommentRepository().apply { profileRepository = get() } }
        single<CommunityClosureRepository> { FakeCommunityClosureRepository() }
        single<CommunityConfigRepository> { FakeCommunityConfigRepository() }
        single<ConnectionRepository> { FakeConnectionRepository() }
        single<ExternalLinkRepository> { FakeExternalLinkRepository() }
        single<LanguageRepository> { FakeLanguageRepository() }
        single<LifecycleEvents> { FakeLifecycleEvents() }
        single<ModerationRepository> { FakeModerationRepository() }
        single<NotificationRepository> { FakeNotificationRepository() }
        single<PostRepository> { FakePostRepository().apply { profileRepository = get() } }
        single<PreferenceRepository> { FakePreferenceRepository() }
        // ConnectionRepository extends ProfileRepository: the profile is always the connection's, so the
        // content fakes and a custom ConnectionRepository binding share one current user.
        single<ProfileRepository> { get<ConnectionRepository>() }
        single<ReactionsRepository> { FakeReactionsRepository() }
        single<ReplyRepository> { FakeReplyRepository().apply { profileRepository = get() } }
        single<RootFeedRepository> { FakeRootFeedRepository() }
        single<SessionRepository> { FakeSessionRepository() }
        single<GroupRepository> { FakeTopicRepository() }
        single<TrackerRepository> { FakeTrackerRepository() }
        single<TranslationRepository> { FakeTranslationRepository() }
        single<UserCommentRepository> { FakeUserCommentRepository() }
        single<UserRepository> { FakeUserRepository() }
    }
