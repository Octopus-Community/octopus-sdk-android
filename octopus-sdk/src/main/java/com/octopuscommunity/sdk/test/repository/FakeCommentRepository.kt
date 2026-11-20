package com.octopuscommunity.sdk.test.repository

import com.octopuscommunity.sdk.domain.model.Comment
import com.octopuscommunity.sdk.domain.model.Feed
import com.octopuscommunity.sdk.domain.model.OctopusItem
import com.octopuscommunity.sdk.domain.model.OctopusItem.Reaction
import com.octopuscommunity.sdk.domain.model.TranslatableText
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.network.ServerError
import com.octopuscommunity.sdk.domain.repository.CommentRepository
import com.octopuscommunity.sdk.domain.repository.FetchRequest
import com.octopuscommunity.sdk.domain.repository.ProfileRepository
import com.octopuscommunity.sdk.test.mock.FakeContentFixtures
import com.octopuscommunity.sdk.test.mock.MockComments
import com.octopuscommunity.sdk.test.mock.MockFeeds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.Date
import java.util.UUID

/**
 * In-memory [CommentRepository] serving [MockComments].
 *
 * Filtering blocked authors requires [com.octopuscommunity.sdk.test.di.octopusTestModule], which
 * shares the profile repository. A directly constructed fake uses its own private profile state.
 */
class FakeCommentRepository : CommentRepository {
    internal var profileRepository: ProfileRepository = FakeProfileRepository()

    private val _comments = MutableStateFlow(FakeContentFixtures.comments)
    private val _feedItems =
        MutableStateFlow(
            (MockFeeds.Comment.all + FakeContentFixtures.posts.map { it.commentsFeeds })
                .flatMap { listOfNotNull(it.descendingId, it.ascendingId) }
                .associateWith { feedId ->
                    val parent =
                        FakeContentFixtures.posts.firstOrNull {
                            feedId == it.commentsFeeds.descendingId || feedId == it.commentsFeeds.ascendingId
                        }
                    FakeContentFixtures.comments.filter { parent == null || it.parentId == parent.id }.map { item ->
                        Feed.Item(feedId = feedId, itemId = item.id, updateDate = item.updateDate)
                    }
                },
        )

    override fun getLocalItemFlow(itemId: String): Flow<Comment?> =
        combine(_comments, profileRepository.getProfileFlow()) { items, profile ->
            items.find { it.id == itemId }?.takeIf { it.author?.isBlocked(profile?.blockedProfiles) != true }
        }

    override fun getLocalItemsFlow(feedId: String): Flow<List<Comment>> =
        combine(_comments, _feedItems, profileRepository.getProfileFlow()) { items, feeds, profile ->
            val ids = feeds[feedId].orEmpty().map { it.itemId }
            items
                .filter { it.id in ids && it.author?.isBlocked(profile?.blockedProfiles) != true }
                .sortedBy { ids.indexOf(it.id) }
        }

    override suspend fun fetchItem(
        itemId: String,
        hasVideo: Boolean?,
        incrementViewCount: Boolean,
    ) = OctopusResult.Success(_comments.value.find { it.id == itemId })

    override suspend fun fetchItems(
        feedId: String,
        pageSize: Int?,
        pageCursor: String?,
    ): OctopusResult<Feed<Comment>, Nothing> {
        val currentComments =
            _feedItems.value[feedId]?.mapNotNull { feedItem ->
                _comments.value.find { it.id == feedItem.itemId }
            } ?: List((5..15).random()) {
                MockComments.create()
            }.sortedBy { it.creationDate }

        fun feedItems(feedId: String) =
            currentComments.map { comment ->
                Feed.Item(
                    feedId = feedId,
                    itemId = comment.id,
                    updateDate = Date(),
                )
            }

        _feedItems.update { current ->
            current + (feedId to feedItems(feedId))
        }

        _comments.update { current ->
            val newComments =
                currentComments.filter { comment ->
                    current.none { it.id == comment.id }
                }
            current + newComments
        }

        return OctopusResult.Success(
            Feed(
                id = feedId,
                items = getLocalItemsFlow(feedId),
                nextPageCursor = null,
            ),
        )
    }

    override suspend fun fetchItems(
        requests: List<FetchRequest>,
        childRequests: List<FetchRequest>,
    ) = OctopusResult.Success(Unit)

    override suspend fun fetchCounters(
        itemId: String,
        hasVideo: Boolean?,
        incrementViewCount: Boolean,
    ) = OctopusResult.Success(Unit)

    override suspend fun fetchCounters(
        itemIds: List<String>,
        childIds: List<String>,
        videoItemIds: List<String>?,
        incrementViewCount: Boolean,
    ) = OctopusResult.Success(Unit)

    override suspend fun sendComment(
        draftComment: Comment.Draft,
        isParentTranslated: Boolean,
    ): OctopusResult<Comment, Comment.CreationError> {
        if (draftComment.text.isBlank()) {
            return OctopusResult.Failure.InvalidArguments(
                listOf(Comment.CreationError.TextError.Missing("Comment text is required")),
            )
        }

        val newComment =
            MockComments.create(
                parentId = draftComment.parentId,
                text =
                    TranslatableText(
                        original =
                            TranslatableText.Content(
                                text = draftComment.text,
                                language = "en-US",
                            ),
                    ),
            )

        _comments.update { currentComments ->
            currentComments + newComment
        }

        return OctopusResult.Success(newComment)
    }

    override suspend fun deleteComment(comment: Comment): OctopusResult<Unit, Nothing> {
        _comments.update { comments ->
            comments.filter { it.id != comment.id }
        }
        return OctopusResult.Success(Unit)
    }

    override suspend fun setReaction(
        item: Comment,
        reactionKind: Reaction.Kind?,
        isContentTranslated: Boolean?,
    ): OctopusResult<Reaction?, ServerError> {
        if (reactionKind == null) {
            var removedReaction: Reaction? = null
            _comments.update { currentComments ->
                currentComments.map { existingComment ->
                    if (existingComment.id == item.id) {
                        removedReaction = existingComment.userReaction
                        val updatedReactions =
                            existingComment.reactions
                                .map { reactions ->
                                    var newCount = reactions.count
                                    if (reactions.reactionKind == removedReaction?.kind) {
                                        newCount = maxOf(0, newCount - 1)
                                    }
                                    reactions.copy(count = newCount)
                                }.filter { it.count > 0 }

                        existingComment.copy(userReaction = null, reactions = updatedReactions)
                    } else {
                        existingComment
                    }
                }
            }
            return OctopusResult.Success(removedReaction)
        }

        var newReaction: Reaction? = null
        _comments.update { currentComments ->
            currentComments.map { existingComment ->
                if (existingComment.id == item.id) {
                    val currentReaction = existingComment.userReaction
                    newReaction =
                        Reaction(
                            kind = reactionKind,
                            id = UUID.randomUUID().toString(),
                        )

                    val updatedReactions =
                        existingComment.reactions
                            .map { reactions ->
                                var newCount = reactions.count
                                if (reactions.reactionKind == currentReaction?.kind) {
                                    newCount = maxOf(0, newCount - 1)
                                }
                                if (reactions.reactionKind == reactionKind) {
                                    newCount += 1
                                }
                                reactions.copy(count = newCount)
                            }.let { reactionsList ->
                                if (reactionsList.none { it.reactionKind == reactionKind }) {
                                    reactionsList +
                                        OctopusItem.ReactionCount(
                                            reactionKind = reactionKind,
                                            count = 1,
                                        )
                                } else {
                                    reactionsList
                                }
                            }.filter { it.count > 0 }

                    existingComment.copy(userReaction = newReaction, reactions = updatedReactions)
                } else {
                    existingComment
                }
            }
        }

        return OctopusResult.Success(newReaction)
    }

    override suspend fun deleteLocalOrphanedItems() {
        // No-op for fake implementation
    }

    override suspend fun resetCache() {
        // No-op for fake implementation
    }
}
