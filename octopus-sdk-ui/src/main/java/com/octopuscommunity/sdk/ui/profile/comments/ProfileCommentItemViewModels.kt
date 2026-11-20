//
//  Copyright © 2026 Octopus Community. All rights reserved.
//
package com.octopuscommunity.sdk.ui.profile.comments

import com.octopuscommunity.sdk.domain.model.Comment
import com.octopuscommunity.sdk.domain.repository.TranslationRepository
import com.octopuscommunity.sdk.domain.usecase.PollVoteUseCase
import com.octopuscommunity.sdk.domain.usecase.SetReactionUseCase
import com.octopuscommunity.sdk.domain.usecase.ToggleTranslationUseCase
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusItemViewModel
import com.octopuscommunity.sdk.ui.posts.common.PostViewModel

/**
 * Generic, feed-agnostic action view model for the post shown as context in a profile Comments cell
 * (translation toggle / reaction / profile & CTA navigation via `PostCard`'s VM-aware overload).
 * Mirrors [com.octopuscommunity.sdk.ui.posts.list.PostCommentViewModel] — a single instance is shared
 * across every row of the tab (see `ProfileCommentsList`), the same way `PostCommentViewModel` is
 * shared across every row's embedded featured comment in `PostsList`. Navigation on tap is supplied
 * by the caller via `PostDisplayMode.Summary`, not owned here.
 */
class ProfileCommentPostViewModel internal constructor(
    commonParameters: CommonParameters,
    translationRepository: TranslationRepository,
    toggleTranslationUseCase: ToggleTranslationUseCase,
    setReactionUseCase: SetReactionUseCase,
    pollVoteUseCase: PollVoteUseCase,
) : PostViewModel(
        commonParameters,
        translationRepository,
        toggleTranslationUseCase,
        setReactionUseCase,
        pollVoteUseCase,
    )

/**
 * Generic, feed-agnostic action view model for the highlighted reply shown in a profile Comments
 * cell (translation toggle / reaction / profile navigation via `ResponseCard`'s reply-role VM-aware
 * overload). A reply cannot itself be replied to, so no override is needed beyond the base class.
 * Mirrors [com.octopuscommunity.sdk.ui.posts.list.PostCommentViewModel]'s role for comments.
 */
class ProfileCommentReplyViewModel internal constructor(
    commonParameters: CommonParameters,
    translationRepository: TranslationRepository,
    toggleTranslationUseCase: ToggleTranslationUseCase,
    setReactionUseCase: SetReactionUseCase,
) : OctopusItemViewModel<Comment.Reply>(
        commonParameters,
        translationRepository,
        toggleTranslationUseCase,
        setReactionUseCase,
    )
