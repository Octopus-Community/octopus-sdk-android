package com.octopuscommunity.sdk.ui.di

import android.os.Handler
import android.os.Looper
import com.octopuscommunity.sdk.domain.model.Comment
import com.octopuscommunity.sdk.domain.model.Comment.Reply
import com.octopuscommunity.sdk.domain.model.OctopusItem.ContentKind
import com.octopuscommunity.sdk.domain.model.OctopusItem.ReactionCount
import com.octopuscommunity.sdk.domain.model.Post
import com.octopuscommunity.sdk.domain.model.RootFeed
import com.octopuscommunity.sdk.ui.OctopusDestination
import com.octopuscommunity.sdk.ui.access.CommunityAccessDeniedViewModel
import com.octopuscommunity.sdk.ui.comments.create.CommentInputFieldViewModel
import com.octopuscommunity.sdk.ui.comments.details.CommentDetailsViewModel
import com.octopuscommunity.sdk.ui.comments.list.CommentsListViewModel
import com.octopuscommunity.sdk.ui.comments.medias.CommentMediasViewModel
import com.octopuscommunity.sdk.ui.comments.menu.CommentMenuSheetViewModel
import com.octopuscommunity.sdk.ui.comments.replies.create.ReplyInputFieldViewModel
import com.octopuscommunity.sdk.ui.comments.replies.list.RepliesListViewModel
import com.octopuscommunity.sdk.ui.comments.replies.medias.ReplyMediasViewModel
import com.octopuscommunity.sdk.ui.comments.replies.menu.ReplyMenuSheetViewModel
import com.octopuscommunity.sdk.ui.common.OctopusNavigator
import com.octopuscommunity.sdk.ui.common.snackbar.OctopusSnackbarViewModel
import com.octopuscommunity.sdk.ui.common.viewmodel.GamificationHomeAppearanceCounter
import com.octopuscommunity.sdk.ui.common.viewmodel.GamificationHomeAppearanceStore
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusScreenViewModel
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusUiOpenings
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusViewModel
import com.octopuscommunity.sdk.ui.components.octopusNavigationScopeQualifier
import com.octopuscommunity.sdk.ui.components.reactions.ReactionsListViewModel
import com.octopuscommunity.sdk.ui.components.video.OctopusVideoViewModel
import com.octopuscommunity.sdk.ui.events.OctopusSnackbarEvents
import com.octopuscommunity.sdk.ui.groups.GroupsScreenViewModel
import com.octopuscommunity.sdk.ui.groups.details.GroupDetailsViewModel
import com.octopuscommunity.sdk.ui.groups.details.GroupPostsListViewModel
import com.octopuscommunity.sdk.ui.groups.list.GroupsListViewModel
import com.octopuscommunity.sdk.ui.home.HomePostsListViewModel
import com.octopuscommunity.sdk.ui.home.OctopusHomeViewModel
import com.octopuscommunity.sdk.ui.notifications.list.NotificationsListViewModel
import com.octopuscommunity.sdk.ui.posts.blocks.CarouselPositions
import com.octopuscommunity.sdk.ui.posts.create.CreatePostViewModel
import com.octopuscommunity.sdk.ui.posts.details.PostDetailsViewModel
import com.octopuscommunity.sdk.ui.posts.list.PostCommentViewModel
import com.octopuscommunity.sdk.ui.posts.medias.PostMediasViewModel
import com.octopuscommunity.sdk.ui.posts.menu.PostMenuSheetViewModel
import com.octopuscommunity.sdk.ui.profile.activity.ActivityViewModel
import com.octopuscommunity.sdk.ui.profile.avatar.ProfileAvatarViewModel
import com.octopuscommunity.sdk.ui.profile.comments.ProfileCommentPostViewModel
import com.octopuscommunity.sdk.ui.profile.comments.ProfileCommentReplyViewModel
import com.octopuscommunity.sdk.ui.profile.comments.ProfileCommentsListViewModel
import com.octopuscommunity.sdk.ui.profile.current.avatar.CurrentUserProfileAvatarViewModel
import com.octopuscommunity.sdk.ui.profile.current.edit.CurrentUserProfileEditViewModel
import com.octopuscommunity.sdk.ui.profile.current.nickname.NicknameChoiceSession
import com.octopuscommunity.sdk.ui.profile.current.nickname.ValidateNicknameViewModel
import com.octopuscommunity.sdk.ui.profile.current.summary.CurrentUserProfileSummaryViewModel
import com.octopuscommunity.sdk.ui.profile.others.menu.ProfileMenuSheetViewModel
import com.octopuscommunity.sdk.ui.profile.others.summary.ProfileSummaryViewModel
import com.octopuscommunity.sdk.ui.profile.posts.ProfilePostsListViewModel
import com.octopuscommunity.sdk.ui.report.content.ReportContentViewModel
import com.octopuscommunity.sdk.ui.report.user.ReportUserViewModel
import com.octopuscommunity.sdk.ui.settings.report.HowToReportContentViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

internal val octopusViewModelModule get() =
    module {
        single<GamificationHomeAppearanceCounter> { GamificationHomeAppearanceStore(androidContext()) }
        single<OctopusUiOpenings> {
            val mainHandler = Handler(Looper.getMainLooper())
            OctopusUiOpenings(postToMainThread = { mainHandler.post(it) })
        }
        // Hosts also collect this shared navigator for direct consumers and standalone components.
        // Events emitted inside a host use its scoped navigator instead.
        single<OctopusNavigator> { OctopusNavigator() }
        // Links a composer holding a first contribution to the nickname choice screen it opened.
        single<NicknameChoiceSession> { NicknameChoiceSession() }
        scope(octopusNavigationScopeQualifier) {
            scoped<OctopusNavigator> { OctopusNavigator() }
            // The page each post's carousel was left on, shared by the screens of one SDK host so the
            // feed follows the photo the reader went to in the post's detail, and the other way
            // round. It goes with the host, as iOS keeps it per entry screen.
            scoped { CarouselPositions() }
        }
        single<OctopusSnackbarEvents> {
            OctopusSnackbarEvents(
                eventRepository = get(),
                sessionRepository = get(),
            )
        }

        // A singleton here would capture the first host's navigator for every ViewModel.
        factory<OctopusViewModel.CommonParameters> {
            OctopusViewModel
                .CommonParameters(
                    communityConfigRepository = get(),
                    eventRepository = get(),
                    connectionRepository = get(),
                    trackerRepository = get(),
                    drivenLoginTracker = get(),
                    linksRepository = get(),
                    lifecycleEvents = get(),
                    navigator = get(),
                    snackbarEvents = get(),
                    closureRepository = getOrNull(),
                ).apply {
                    containerScope = getKoin().getProperty("octopusViewModelScope")
                    sdkScope =
                        getOrNull<com.octopuscommunity.sdk.ui.components.OctopusHostCoroutineScope>()?.scope
                            ?: containerScope
                }
        }
        viewModel { (commentId: String, replyId: String?) ->
            CommentDetailsViewModel(
                commonParameters = get(),
                commentRepository = get(),
                translationRepository = get(),
                toggleTranslationUseCase = get(),
                setReactionUseCase = get(),
                commentId = commentId,
                replyId = replyId,
            )
        }
        viewModel { (post: Post) ->
            CommentInputFieldViewModel(
                commonParameters = get(),
                sendCommentUseCase = get(),
                validateText = get(),
                validateImage = get(),
                nicknameChoiceSession = get(),
                post = post,
            )
        }
        viewModel { (post: Post, scrollToLatest: Boolean, scrollToId: String?) ->
            CommentsListViewModel(
                commonParameters = get(),
                commentRepository = get(),
                translationRepository = get(),
                toggleTranslationUseCase = get(),
                setReaction = get(),
                post = post,
                scrollToLatest = scrollToLatest,
                scrollToId = scrollToId,
            )
        }
        viewModel { (contentId: String, reactionCounts: List<ReactionCount>) ->
            ReactionsListViewModel(
                commonParameters = get(),
                reactionsRepository = get(),
                contentId = contentId,
                reactionCounts = reactionCounts,
            )
        }
        viewModel { (commentId: String) ->
            CommentMediasViewModel(
                commonParameters = get(),
                commentRepository = get(),
                commentId = commentId,
            )
        }
        viewModel { (comment: Comment) ->
            CommentMenuSheetViewModel(
                commonParameters = get(),
                deleteCommentUseCase = get(),
                comment = comment,
            )
        }
        viewModel {
            CommunityAccessDeniedViewModel(
                commonParameters = get(),
            )
        }
        viewModel { params ->
            CreatePostViewModel(
                commonParameters = get(),
                sendPostUseCase = get(),
                validateText = get(),
                validateImage = get(),
                groupRepository = get(),
                nicknameChoiceSession = get(),
                params = params.get(),
            )
        }
        viewModel {
            CurrentUserProfileAvatarViewModel(
                commonParameters = get(),
            )
        }
        viewModel {
            CurrentUserProfileEditViewModel(
                commonParameters = get(),
                updateProfileUseCase = get(),
                validateText = get(),
                validateImage = get(),
            )
        }
        viewModel {
            CurrentUserProfileSummaryViewModel(
                commonParameters = get(),
                groupRepository = get(),
            )
        }
        viewModel { (userId: String?, clientUserId: String?) ->
            ActivityViewModel(
                commonParameters = get(),
                groupRepository = get(),
                profileRepository = get(),
                userId = userId,
                clientUserId = clientUserId,
            )
        }
        viewModel {
            GroupsScreenViewModel(commonParameters = get())
        }
        viewModel {
            GroupsListViewModel(
                commonParameters = get(),
                groupRepository = get(),
                followGroupUseCase = get(),
            )
        }
        viewModel { (groupId: String) ->
            GroupDetailsViewModel(
                commonParameters = get(),
                groupRepository = get(),
                followGroupUseCase = get(),
                groupId = groupId,
            )
        }
        viewModel { (feedId: String, groupId: String, origin: OctopusDestination.Origin) ->
            GroupPostsListViewModel(
                commonParameters = get(),
                postRepository = get(),
                translationRepository = get(),
                toggleTranslationUseCase = get(),
                setReactionUseCase = get(),
                pollVoteUseCase = get(),
                feedId = feedId,
                groupId = groupId,
                origin = origin,
            )
        }
        viewModel {
            HowToReportContentViewModel(
                commonParameters = get(),
            )
        }
        viewModel {
            NotificationsListViewModel(
                commonParameters = get(),
                notificationRepository = get(),
            )
        }
        viewModel {
            OctopusHomeViewModel(
                groupRepository = get(),
                commonParameters = get(),
                postRepository = get(),
                commentRepository = get(),
                rootFeedRepository = get(),
            )
        }
        viewModel { (feed: RootFeed) ->
            HomePostsListViewModel(
                commonParameters = get(),
                postRepository = get(),
                translationRepository = get(),
                toggleTranslationUseCase = get(),
                setReactionUseCase = get(),
                pollVoteUseCase = get(),
                feed = feed,
            )
        }
        viewModel {
            OctopusScreenViewModel(
                commonParameters = get(),
                preferenceRepository = get(),
                homeAppearanceCounter = get(),
                uiOpenings = get(),
            )
        }
        viewModel {
            OctopusSnackbarViewModel(
                commonParameters = get(),
            )
        }
        viewModel {
            PostCommentViewModel(
                commonParameters = get(),
                translationRepository = get(),
                toggleTranslation = get(),
                setReaction = get(),
            )
        }
        viewModel { (postId: String, origin: OctopusDestination.Origin, hasFeaturedComment: Boolean) ->
            PostDetailsViewModel(
                commonParameters = get(),
                translationRepository = get(),
                toggleTranslationUseCase = get(),
                setReactionUseCase = get(),
                pollVoteUseCase = get(),
                postRepository = get(),
                postId = postId,
                origin = origin,
                hasFeaturedComment = hasFeaturedComment,
            )
        }
        viewModel { (postId: String) ->
            PostMediasViewModel(
                commonParameters = get(),
                postRepository = get(),
                postId = postId,
            )
        }
        viewModel { (post: Post) ->
            PostMenuSheetViewModel(
                commonParameters = get(),
                deletePostUseCase = get(),
                post = post,
            )
        }
        viewModel { (feedId: String) ->
            ProfilePostsListViewModel(
                commonParameters = get(),
                postRepository = get(),
                translationRepository = get(),
                toggleTranslationUseCase = get(),
                setReactionUseCase = get(),
                pollVoteUseCase = get(),
                profileFeedId = feedId,
            )
        }
        viewModel { parameters ->
            ProfileCommentsListViewModel(
                commonParameters = get(),
                userCommentRepository = get(),
                postRepository = get(),
                commentRepository = get(),
                replyRepository = get(),
                feedId = parameters.get<String>(),
                isOwnProfile = parameters.getOrNull<Boolean>() ?: true,
            )
        }
        viewModel {
            ProfileCommentPostViewModel(
                commonParameters = get(),
                translationRepository = get(),
                toggleTranslationUseCase = get(),
                setReactionUseCase = get(),
                pollVoteUseCase = get(),
            )
        }
        viewModel {
            ProfileCommentReplyViewModel(
                commonParameters = get(),
                translationRepository = get(),
                toggleTranslationUseCase = get(),
                setReactionUseCase = get(),
            )
        }
        viewModel { (userId: String) ->
            ProfileAvatarViewModel(
                commonParameters = get(),
                profileRepository = get(),
                userId = userId,
            )
        }
        viewModel {
            ProfileMenuSheetViewModel(
                commonParameters = get(),
            )
        }
        viewModel { (userId: String?, clientUserId: String?) ->
            ProfileSummaryViewModel(
                commonParameters = get(),
                profileRepository = get(),
                userId = userId,
                clientUserId = clientUserId,
            )
        }
        viewModel { (comment: Comment) ->
            ReplyInputFieldViewModel(
                commonParameters = get(),
                sendReplyUseCase = get(),
                validateText = get(),
                validateImage = get(),
                nicknameChoiceSession = get(),
                comment = comment,
            )
        }
        viewModel { (comment: Comment, scrollToLatest: Boolean, scrollToId: String?) ->
            RepliesListViewModel(
                commonParameters = get(),
                replyRepository = get(),
                translationRepository = get(),
                toggleTranslationUseCase = get(),
                setReactionUseCase = get(),
                comment = comment,
                scrollToLatest = scrollToLatest,
                scrollToId = scrollToId,
            )
        }
        viewModel { (replyId: String) ->
            ReplyMediasViewModel(
                replyId = replyId,
                commonParameters = get(),
                replyRepository = get(),
            )
        }
        viewModel { (reply: Reply) ->
            ReplyMenuSheetViewModel(
                commonParameters = get(),
                deleteReplyUseCase = get(),
                reply = reply,
            )
        }
        viewModel { (contentId: String, contentKind: ContentKind) ->
            ReportContentViewModel(
                commonParameters = get(),
                moderationRepository = get(),
                reportContentUseCase = get(),
                contentId = contentId,
                contentKind = contentKind,
            )
        }
        viewModel { (userId: String) ->
            ReportUserViewModel(
                commonParameters = get(),
                moderationRepository = get(),
                reportProfileUseCase = get(),
                userId = userId,
            )
        }
        viewModel {
            ValidateNicknameViewModel(
                commonParameters = get(),
                updateProfileUseCase = get(),
                nicknameChoiceSession = get(),
                validateImage = get(),
            )
        }
        viewModel { (contentId: String, videoId: String) ->
            OctopusVideoViewModel(
                commonParameters = get(),
                sessionRepository = get(),
                itemId = contentId,
                videoId = videoId,
            )
        }
    }
