package com.octopuscommunity.sdk.ui.profile.posts

import com.octopuscommunity.sdk.domain.repository.PostRepository
import com.octopuscommunity.sdk.domain.repository.TranslationRepository
import com.octopuscommunity.sdk.domain.usecase.PollVoteUseCase
import com.octopuscommunity.sdk.domain.usecase.SetReactionUseCase
import com.octopuscommunity.sdk.domain.usecase.ToggleTranslationUseCase
import com.octopuscommunity.sdk.ui.posts.list.PostsListViewModel

class ProfilePostsListViewModel internal constructor(
    commonParameters: CommonParameters,
    postRepository: PostRepository,
    translationRepository: TranslationRepository,
    toggleTranslationUseCase: ToggleTranslationUseCase,
    setReactionUseCase: SetReactionUseCase,
    pollVoteUseCase: PollVoteUseCase,
    profileFeedId: String
) : PostsListViewModel(
    commonParameters = commonParameters,
    postRepository = postRepository,
    translationRepository = translationRepository,
    toggleTranslationUseCase = toggleTranslationUseCase,
    setReactionUseCase = setReactionUseCase,
    pollVoteUseCase = pollVoteUseCase,
    feedId = profileFeedId
)