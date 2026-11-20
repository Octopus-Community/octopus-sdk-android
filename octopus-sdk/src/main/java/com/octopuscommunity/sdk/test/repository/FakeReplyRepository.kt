package com.octopuscommunity.sdk.test.repository

import com.octopuscommunity.sdk.domain.model.Comment.Reply
import com.octopuscommunity.sdk.domain.model.Feed
import com.octopuscommunity.sdk.domain.model.OctopusItem.Reaction
import com.octopuscommunity.sdk.domain.model.OctopusItem.ReactionCount
import com.octopuscommunity.sdk.domain.model.TranslatableText
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.network.ServerError
import com.octopuscommunity.sdk.domain.repository.FetchRequest
import com.octopuscommunity.sdk.domain.repository.ProfileRepository
import com.octopuscommunity.sdk.domain.repository.ReplyRepository
import com.octopuscommunity.sdk.test.mock.FakeContentFixtures
import com.octopuscommunity.sdk.test.mock.MockFeeds
import com.octopuscommunity.sdk.test.mock.MockReplies
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.Date
import java.util.UUID

/**
 * In-memory [ReplyRepository] serving [MockReplies].
 *
 * Filtering blocked authors requires [com.octopuscommunity.sdk.test.di.octopusTestModule], which
 * shares the profile repository. A directly constructed fake uses its own private profile state.
 */
class FakeReplyRepository : ReplyRepository {
    internal var profileRepository: ProfileRepository = FakeProfileRepository()

    private val _replies = MutableStateFlow(FakeContentFixtures.replies)
    private val _feedItems =
        MutableStateFlow(
            (MockFeeds.Replies.all + FakeContentFixtures.comments.map { it.repliesFeeds })
                .flatMap { listOfNotNull(it.descendingId, it.ascendingId) }
                .associateWith { feedId ->
                    val parent =
                        FakeContentFixtures.comments.firstOrNull {
                            feedId == it.repliesFeeds.descendingId || feedId == it.repliesFeeds.ascendingId
                        }
                    FakeContentFixtures.replies.filter { parent == null || it.parentId == parent.id }.map { item ->
                        Feed.Item(feedId = feedId, itemId = item.id, updateDate = item.updateDate)
                    }
                },
        )

    override fun getLocalItemFlow(itemId: String): Flow<Reply?> =
        combine(_replies, profileRepository.getProfileFlow()) { items, profile ->
            items.find { it.id == itemId }?.takeIf { it.author?.isBlocked(profile?.blockedProfiles) != true }
        }

    override fun getLocalItemsFlow(feedId: String): Flow<List<Reply>> =
        combine(_replies, _feedItems, profileRepository.getProfileFlow()) { items, feeds, profile ->
            val ids = feeds[feedId].orEmpty().map { it.itemId }
            items
                .filter { it.id in ids && it.author?.isBlocked(profile?.blockedProfiles) != true }
                .sortedBy { ids.indexOf(it.id) }
        }

    override suspend fun fetchItem(
        itemId: String,
        hasVideo: Boolean?,
        incrementViewCount: Boolean,
    ) = OctopusResult.Success(_replies.value.find { it.id == itemId })

    override suspend fun fetchItems(
        feedId: String,
        pageSize: Int?,
        pageCursor: String?,
    ): OctopusResult<Feed<Reply>, Nothing> {
        val currentReplies =
            _feedItems.value[feedId]?.mapNotNull { feedItem ->
                _replies.value.find { it.id == feedItem.itemId }
            } ?: List((5..15).random()) {
                MockReplies.create()
            }.sortedBy { it.creationDate }

        val feedItems =
            currentReplies.map { reply ->
                Feed.Item(
                    feedId = feedId,
                    itemId = reply.id,
                    updateDate = Date(),
                )
            }

        _feedItems.update { current ->
            current + (feedId to feedItems)
        }

        _replies.update { current ->
            val newReplies =
                currentReplies.filter { reply ->
                    current.none { it.id == reply.id }
                }
            current + newReplies
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

    override suspend fun sendReply(
        draftItem: Reply.Draft,
        isParentTranslated: Boolean,
    ): OctopusResult<Reply, Reply.CreationError> {
        if (draftItem.text.isBlank()) {
            return OctopusResult.Failure.InvalidArguments(
                listOf(Reply.CreationError.TextError.Missing("Reply text is required")),
            )
        }

        val newReply =
            MockReplies.create(
                parentId = draftItem.parentId,
                text =
                    TranslatableText(
                        original =
                            TranslatableText.Content(
                                text = draftItem.text,
                                language = "en-US",
                            ),
                    ),
            )

        _replies.update { currentReplies ->
            currentReplies + newReply
        }

        return OctopusResult.Success(newReply)
    }

    override suspend fun deleteReply(reply: Reply): OctopusResult<Unit, Nothing> {
        _replies.update { replies ->
            replies.filter { it.id != reply.id }
        }
        return OctopusResult.Success(Unit)
    }

    override suspend fun setReaction(
        item: Reply,
        reactionKind: Reaction.Kind?,
        isContentTranslated: Boolean?,
    ): OctopusResult<Reaction?, ServerError> {
        if (reactionKind == null) {
            // Remove reaction
            var removedReaction: Reaction? = null
            _replies.update { currentReplies ->
                currentReplies.map { existingReply ->
                    if (existingReply.id == item.id) {
                        removedReaction = existingReply.userReaction
                        val updatedReactions =
                            existingReply.reactions
                                .map { reactions ->
                                    var newCount = reactions.count
                                    if (reactions.reactionKind == removedReaction?.kind) {
                                        newCount = maxOf(0, newCount - 1)
                                    }
                                    reactions.copy(count = newCount)
                                }.filter { it.count > 0 }

                        existingReply.copy(userReaction = null, reactions = updatedReactions)
                    } else {
                        existingReply
                    }
                }
            }
            return OctopusResult.Success(removedReaction)
        }

        // Set reaction
        var newReaction: Reaction? = null
        _replies.update { currentReplies ->
            currentReplies.map { existingReply ->
                if (existingReply.id == item.id) {
                    val currentReaction = existingReply.userReaction
                    newReaction =
                        Reaction(
                            kind = reactionKind,
                            id = UUID.randomUUID().toString(),
                        )

                    val updatedReactions =
                        existingReply.reactions
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
                                        ReactionCount(
                                            reactionKind = reactionKind,
                                            count = 1,
                                        )
                                } else {
                                    reactionsList
                                }
                            }.filter { it.count > 0 }

                    existingReply.copy(userReaction = newReaction, reactions = updatedReactions)
                } else {
                    existingReply
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
