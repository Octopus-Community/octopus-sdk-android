package com.octopuscommunity.sdk.ui.posts.common

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.domain.model.OctopusEvent.CommentButtonClicked
import com.octopuscommunity.sdk.domain.model.Post
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.TranslationRepository
import com.octopuscommunity.sdk.domain.usecase.PollVoteUseCase
import com.octopuscommunity.sdk.domain.usecase.SetReactionUseCase
import com.octopuscommunity.sdk.domain.usecase.ToggleTranslationUseCase
import com.octopuscommunity.sdk.ui.OctopusDestination.GroupDetails
import com.octopuscommunity.sdk.ui.common.OctopusNavigationEvent.NavigateToGroupAccessDenied
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusItemViewModel
import com.octopuscommunity.sdk.ui.posts.components.PollVoteOutcome
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

@OptIn(FlowPreview::class)
open class PostViewModel internal constructor(
    commonParameters: CommonParameters,
    translationRepository: TranslationRepository,
    toggleTranslationUseCase: ToggleTranslationUseCase,
    setReactionUseCase: SetReactionUseCase,
    val pollVoteUseCase: PollVoteUseCase,
) : OctopusItemViewModel<Post>(
        commonParameters,
        translationRepository,
        toggleTranslationUseCase,
        setReactionUseCase,
    ) {
    /** Group labels use the same access-denied flow as the groups list. */
    internal fun navigateToGroup(post: Post) {
        if (post.group?.canAccess == false) {
            viewModelScope.launch {
                navigator.sendNavigationEvent(NavigateToGroupAccessDenied(groupId = post.parentId))
            }
        } else {
            navigateTo(GroupDetails(groupId = post.parentId))
        }
    }

    fun onCommentClicked(post: Post) {
        viewModelScope.launch {
            eventRepository.emitEvent(CommentButtonClicked(postId = post.id))
        }
    }

    fun pollVote(
        post: Post,
        answer: Post.Poll.Answer,
    ) {
        pollVoteAttempt(post, answer)
    }

    /** Vote is a weak action: keep guest/SSO policy, while reporting every non-vote exit. */
    internal fun pollVoteAttempt(
        post: Post,
        answer: Post.Poll.Answer,
    ): Deferred<PollVoteOutcome> {
        val completion = CompletableDeferred<PollVoteOutcome>()
        var outcome = PollVoteOutcome.Refused
        val operation =
            launchCheckAuthenticated(
                action = UserAction.Vote,
                // The Job's completion handler also reports exceptions through the attempt's outcome.
                exceptionHandler =
                    CoroutineExceptionHandler { _, throwable ->
                        Log.w("OctopusSDK", "Poll vote failed unexpectedly", throwable)
                    },
                onConfigFailure = { outcome = PollVoteOutcome.Failed },
            ) {
                outcome = vote(post, answer)
                true
            }
        operation.invokeOnCompletion { cause ->
            completion.complete(
                when (cause) {
                    is CancellationException -> PollVoteOutcome.Cancelled
                    null -> outcome
                    else -> PollVoteOutcome.Failed
                },
            )
        }
        completion.invokeOnCompletion {
            if (completion.isCancelled) operation.cancel()
        }
        return completion
    }

    private suspend fun vote(
        post: Post,
        answer: Post.Poll.Answer,
    ): PollVoteOutcome =
        when (val result = pollVoteUseCase(post, answer)) {
            is OctopusResult.Success -> PollVoteOutcome.Voted
            is OctopusResult.Failure -> {
                showError(result)
                when (result) {
                    is OctopusResult.Failure.UserNotAuthenticated,
                    is OctopusResult.Failure.PermissionDenied,
                    -> PollVoteOutcome.Refused
                    else -> PollVoteOutcome.Failed
                }
            }
        }
}
