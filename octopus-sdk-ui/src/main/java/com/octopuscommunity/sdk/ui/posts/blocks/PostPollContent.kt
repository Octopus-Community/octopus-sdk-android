package com.octopuscommunity.sdk.ui.posts.blocks

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.domain.model.Post
import com.octopuscommunity.sdk.test.mock.MockPosts
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.posts.components.Poll
import com.octopuscommunity.sdk.ui.posts.components.PollContent
import com.octopuscommunity.sdk.ui.posts.components.PollVoteOutcome
import kotlinx.coroutines.Deferred

/**
 * Poll attachment — wraps the existing [Poll] composable with horizontal padding so it aligns
 * with the rest of the text content blocks.
 */
@Composable
internal fun PostPollContent(
    poll: Post.Poll,
    currentUserVote: Post.Poll.Answer?,
    translated: Boolean,
    onVote: (Post.Poll.Answer) -> Unit,
    modifier: Modifier = Modifier,
    onVoteAttempt: ((Post.Poll.Answer) -> Deferred<PollVoteOutcome>)? = null,
) {
    // Figma `Sondage` (node 1870:18622): `px-[16px] py-[8px]` on the Poll container.
    val pollModifier =
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    if (onVoteAttempt != null) {
        PollContent(
            modifier = pollModifier,
            poll = poll,
            vote = currentUserVote,
            translate = translated,
            onVoteAttempt = onVoteAttempt,
        )
    } else {
        Poll(
            modifier = pollModifier,
            poll = poll,
            vote = currentUserVote,
            translate = translated,
            onVote = onVote,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PostPollContentPreview() {
    OctopusTheme {
        val post = MockPosts.withPoll
        PostPollContent(
            poll = post.poll!!,
            currentUserVote = post.currentUserVote,
            translated = false,
            onVote = {},
        )
    }
}
