package com.octopuscommunity.sdk.di

import com.octopuscommunity.sdk.domain.usecase.ConnectUseCase
import com.octopuscommunity.sdk.domain.usecase.CreateAppSessionUseCase
import com.octopuscommunity.sdk.domain.usecase.CreateProfileUseCase
import com.octopuscommunity.sdk.domain.usecase.DeleteCommentUseCase
import com.octopuscommunity.sdk.domain.usecase.DeletePostUseCase
import com.octopuscommunity.sdk.domain.usecase.DeleteReplyUseCase
import com.octopuscommunity.sdk.domain.usecase.DisconnectUseCase
import com.octopuscommunity.sdk.domain.usecase.FollowGroupUseCase
import com.octopuscommunity.sdk.domain.usecase.OverrideDefaultLocaleUseCase
import com.octopuscommunity.sdk.domain.usecase.PollVoteUseCase
import com.octopuscommunity.sdk.domain.usecase.RefreshEntitlementsUseCase
import com.octopuscommunity.sdk.domain.usecase.ReportContentUseCase
import com.octopuscommunity.sdk.domain.usecase.ReportProfileUseCase
import com.octopuscommunity.sdk.domain.usecase.SendCommentUseCase
import com.octopuscommunity.sdk.domain.usecase.SendPostUseCase
import com.octopuscommunity.sdk.domain.usecase.SendReplyUseCase
import com.octopuscommunity.sdk.domain.usecase.SetReactionUseCase
import com.octopuscommunity.sdk.domain.usecase.StartAppSessionUseCase
import com.octopuscommunity.sdk.domain.usecase.StartCollectingDrivenLoginUseCase
import com.octopuscommunity.sdk.domain.usecase.StartCollectingSessionsUseCase
import com.octopuscommunity.sdk.domain.usecase.StartOctopusSessionUseCase
import com.octopuscommunity.sdk.domain.usecase.StopAppSessionUseCase
import com.octopuscommunity.sdk.domain.usecase.StopOctopusSessionUseCase
import com.octopuscommunity.sdk.domain.usecase.ToggleTranslationUseCase
import com.octopuscommunity.sdk.domain.usecase.UpdateProfileUseCase
import org.koin.dsl.module

/** Koin module providing the domain use cases. */
val octopusUseCaseModule =
    module {
        factory {
            ConnectUseCase(
                connectionRepository = get(),
                configRepository = get(),
                eventRepository = get(),
            )
        }
        factory {
            CreateAppSessionUseCase(
                sessionRepository = get(),
                userRepository = get(),
                connectionRepository = get(),
                communityConfigRepository = get(),
            )
        }
        factory {
            CreateProfileUseCase(
                profileRepository = get(),
                configRepository = get(),
                eventRepository = get(),
            )
        }
        factory {
            DeleteCommentUseCase(
                commentRepository = get(),
                configRepository = get(),
                eventRepository = get(),
            )
        }
        factory {
            DeletePostUseCase(
                postRepository = get(),
                configRepository = get(),
                eventRepository = get(),
            )
        }
        factory {
            DeleteReplyUseCase(
                replyRepository = get(),
                configRepository = get(),
                eventRepository = get(),
            )
        }
        factory {
            DisconnectUseCase(
                connectionRepository = get(),
                eventRepository = get(),
            )
        }
        factory {
            PollVoteUseCase(
                postRepository = get(),
                translationRepository = get(),
                configRepository = get(),
                eventRepository = get(),
            )
        }
        factory {
            SendCommentUseCase(
                commentRepository = get(),
                configRepository = get(),
                translationRepository = get(),
                eventRepository = get(),
            )
        }
        factory {
            SendPostUseCase(
                postRepository = get(),
                configRepository = get(),
                eventRepository = get(),
            )
        }
        factory {
            SendReplyUseCase(
                replyRepository = get(),
                configRepository = get(),
                translationRepository = get(),
                eventRepository = get(),
            )
        }
        factory {
            FollowGroupUseCase(
                groupRepository = get(),
                eventRepository = get(),
            )
        }
        factory {
            SetReactionUseCase(
                postRepository = get(),
                commentRepository = get(),
                replyRepository = get(),
                configRepository = get(),
                translationRepository = get(),
                eventRepository = get(),
                trackerRepository = get(),
            )
        }
        factory {
            StartAppSessionUseCase(
                sessionRepository = get(),
                eventRepository = get(),
                trackerRepository = get(),
            )
        }
        factory {
            StartCollectingSessionsUseCase(
                sessionRepository = get(),
                createAppSessionUseCase = get(),
                startAppSessionUseCase = get(),
                startOctopusSessionUseCase = get(),
                stopAppSessionUseCase = get(),
                stopOctopusSessionUseCase = get(),
            )
        }
        factory {
            StartCollectingDrivenLoginUseCase(
                connectionRepository = get(),
                trackerRepository = get(),
                drivenLoginTracker = get(),
            )
        }
        factory {
            StopAppSessionUseCase(
                sessionRepository = get(),
                eventRepository = get(),
                trackerRepository = get(),
            )
        }
        factory {
            StartOctopusSessionUseCase(
                sessionRepository = get(),
                configRepository = get(),
                preferenceRepository = get(),
                connectionRepository = get(),
                groupRepository = get(),
                userRepository = get(),
                eventRepository = get(),
                trackerRepository = get(),
            )
        }
        factory {
            StopOctopusSessionUseCase(
                sessionRepository = get(),
                eventRepository = get(),
                trackerRepository = get(),
            )
        }
        factory {
            ToggleTranslationUseCase(
                translationRepository = get(),
                trackerRepository = get(),
                eventRepository = get(),
            )
        }
        factory {
            UpdateProfileUseCase(
                profileRepository = get(),
                configRepository = get(),
                eventRepository = get(),
            )
        }
        factory {
            RefreshEntitlementsUseCase(
                connectionRepository = get(),
                groupRepository = get(),
                postRepository = get(),
                commentRepository = get(),
                replyRepository = get(),
            )
        }
        factory {
            ReportContentUseCase(
                moderationRepository = get(),
                eventRepository = get(),
            )
        }
        factory {
            ReportProfileUseCase(
                moderationRepository = get(),
                eventRepository = get(),
            )
        }
        factory {
            OverrideDefaultLocaleUseCase(
                languageRepository = get(),
                communityConfigRepository = get(),
            )
        }
    }
