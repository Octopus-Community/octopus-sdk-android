@file:Suppress("ktlint:standard:backing-property-naming")

package com.octopuscommunity.sdk.test.repository

import com.octopuscommunity.sdk.domain.model.Author
import com.octopuscommunity.sdk.domain.model.ClientPost
import com.octopuscommunity.sdk.domain.model.CreationSource
import com.octopuscommunity.sdk.domain.model.Feed
import com.octopuscommunity.sdk.domain.model.Medias
import com.octopuscommunity.sdk.domain.model.OctopusItem.Feeds
import com.octopuscommunity.sdk.domain.model.OctopusItem.Reaction
import com.octopuscommunity.sdk.domain.model.OctopusItem.ReactionCount
import com.octopuscommunity.sdk.domain.model.OctopusReactionKind
import com.octopuscommunity.sdk.domain.model.Post
import com.octopuscommunity.sdk.domain.model.Resource
import com.octopuscommunity.sdk.domain.model.TranslatableText
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.network.ServerError
import com.octopuscommunity.sdk.domain.repository.ClientPostError
import com.octopuscommunity.sdk.domain.repository.FetchRequest
import com.octopuscommunity.sdk.domain.repository.PollVoteError
import com.octopuscommunity.sdk.domain.repository.PostCreationError
import com.octopuscommunity.sdk.domain.repository.PostRepository
import com.octopuscommunity.sdk.domain.repository.ProfileRepository
import com.octopuscommunity.sdk.domain.repository.SetReactionError
import com.octopuscommunity.sdk.test.mock.FakeContentFixtures
import com.octopuscommunity.sdk.test.mock.MockFeeds
import com.octopuscommunity.sdk.test.mock.MockPosts
import com.octopuscommunity.sdk.test.mock.MockProfiles
import com.octopuscommunity.sdk.test.mock.MockTopics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.Date
import java.util.UUID

/**
 * In-memory [PostRepository] serving [MockPosts].
 *
 * Filtering blocked authors requires [com.octopuscommunity.sdk.test.di.octopusTestModule], which
 * shares the profile repository. A directly constructed fake uses its own private profile state.
 */
class FakePostRepository : PostRepository {
    internal var profileRepository: ProfileRepository = FakeProfileRepository()

    private val _posts = MutableStateFlow(FakeContentFixtures.posts)
    private val _feedItems =
        MutableStateFlow(
            MockFeeds.Post.allRoots.map { it.id }.associateWith { feedId ->
                FakeContentFixtures.posts.map { post ->
                    Feed.Item(
                        feedId = feedId,
                        itemId = post.id,
                        updateDate = Date(),
                    )
                }
            },
        )

    override fun getLocalItemFlow(itemId: String): Flow<Post?> =
        combine(_posts, profileRepository.getProfileFlow()) { posts, profile ->
            posts
                .find { it.id == itemId }
                ?.takeIf { it.author?.isBlocked(profile?.blockedProfiles) != true }
                ?.let { post ->
                    post.copy(featuredComment = post.featuredComment?.takeIf { it.author?.isBlocked(profile?.blockedProfiles) != true })
                }
        }

    override fun getLocalItemsFlow(feedId: String): Flow<List<Post>> =
        combine(_posts, _feedItems, profileRepository.getProfileFlow()) { posts, feeds, profile ->
            val ids = feeds[feedId].orEmpty().map { it.itemId }
            posts
                .filter { it.id in ids && it.author?.isBlocked(profile?.blockedProfiles) != true }
                .sortedBy { ids.indexOf(it.id) }
                .map { post ->
                    post.copy(featuredComment = post.featuredComment?.takeIf { it.author?.isBlocked(profile?.blockedProfiles) != true })
                }
        }

    private suspend fun postsForFeed(feedId: String): List<Post> {
        val profile = profileRepository.getProfile()
        val author =
            (listOfNotNull(profile) + MockProfiles.all).firstOrNull {
                feedId == it.postsFeeds.descendingId || feedId == it.postsFeeds.ascendingId
            }

        @Suppress("DEPRECATION")
        val group =
            MockTopics.all.firstOrNull {
                feedId == it.postsFeeds.descendingId || feedId == it.postsFeeds.ascendingId
            }
        return _posts.value.filter { post ->
            when {
                author != null -> post.author?.userId == author.userId
                group != null -> post.parentId == group.id
                else -> true
            }
        }
    }

    override suspend fun fetchItem(
        itemId: String,
        hasVideo: Boolean?,
        incrementViewCount: Boolean,
    ): OctopusResult<Post?, Nothing> {
        val post = _posts.value.find { it.id == itemId }
        if (incrementViewCount && post != null) {
            updateViewCounts(listOf(itemId))
        }
        return OctopusResult.Success(post)
    }

    override suspend fun fetchItems(
        feedId: String,
        pageSize: Int?,
        pageCursor: String?,
    ): OctopusResult<Feed<Post>, Nothing> {
        val allPosts = postsForFeed(feedId)
        val startIndex = pageCursor?.toIntOrNull() ?: 0
        val items =
            allPosts
                .drop(startIndex)
                .take(pageSize ?: allPosts.size)

        val feedItems =
            items.map { post ->
                Feed.Item(
                    feedId = feedId,
                    itemId = post.id,
                    updateDate = Date(),
                )
            }

        _feedItems.update { current ->
            current.toMutableMap().apply {
                this[feedId] =
                    if (pageCursor == null) {
                        feedItems
                    } else {
                        (current[feedId] ?: emptyList()) + feedItems
                    }
            }
        }

        return OctopusResult.Success(
            Feed(
                id = feedId,
                items = getLocalItemsFlow(feedId),
                nextPageCursor =
                    if (items.size == pageSize) {
                        (startIndex + pageSize).toString()
                    } else {
                        null
                    },
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
    ): OctopusResult<Unit, Nothing> {
        if (incrementViewCount) {
            updateViewCounts(listOf(itemId))
        }
        return OctopusResult.Success(Unit)
    }

    override suspend fun fetchCounters(
        itemIds: List<String>,
        childIds: List<String>,
        videoItemIds: List<String>?,
        incrementViewCount: Boolean,
    ): OctopusResult<Unit, Nothing> {
        if (incrementViewCount) {
            updateViewCounts(itemIds)
        }
        return OctopusResult.Success(Unit)
    }

    override suspend fun sendPost(
        draftItem: Post.Draft,
        creationSource: CreationSource,
    ): OctopusResult<Post, PostCreationError> {
        val group = draftItem.group
        if (group == null) {
            return OctopusResult.Failure.InvalidArguments(
                listOf(PostCreationError.GroupError.Missing("Group is required")),
            )
        }

        if (draftItem.text.isBlank()) {
            return OctopusResult.Failure.InvalidArguments(
                listOf(PostCreationError.TextError.Missing("Text is required")),
            )
        }

        val profile = profileRepository.getProfile() ?: return OctopusResult.Failure.UserNotAuthenticated()
        val now = Date()
        val commentsFeedId = "comments_${UUID.randomUUID()}"
        val newPost =
            MockPosts.create(
                creationDate = now,
                updateDate = now,
                author =
                    Author(
                        profile.userId,
                        profile.nickname,
                        profile.picture as? Resource.Remote,
                        profile.gamificationLevelIndex,
                        profile.tags,
                        profile.clientUserId,
                    ),
                media = draftItem.images.takeIf { it.isNotEmpty() }?.let { Medias(images = it) },
                poll =
                    draftItem.poll?.let { poll ->
                        Post.Poll(
                            answers =
                                poll.answers.map {
                                    Post.Poll.Answer(
                                        it.id,
                                        TranslatableText(originalText = it.text, originalLanguage = "en"),
                                    )
                                },
                        )
                    },
                pollVoteId = null,
                commentsFeeds = Feeds(descendingId = "${commentsFeedId}_desc", ascendingId = "${commentsFeedId}_asc"),
                viewCount = 0,
                commentCount = 0,
                reactions = emptyList(),
                reaction = null,
                featuredComment = null,
                customAction =
                    draftItem.customAction?.let {
                        Post.CustomAction(TranslatableText(originalText = it.label, originalLanguage = "en"), it.targetUrl)
                    },
                text =
                    TranslatableText(
                        original =
                            TranslatableText.Content(
                                text = draftItem.text,
                                language = "en-US",
                            ),
                    ),
                parentId = group.id,
            )

        _posts.update { currentPosts -> listOf(newPost) + currentPosts }
        @Suppress("DEPRECATION")
        val feedIds =
            (
                _feedItems.value.keys + MockFeeds.Post.allRoots.map { it.id } +
                    listOfNotNull(
                        profile.postsFeeds.descendingId,
                        profile.postsFeeds.ascendingId,
                        group.postsFeeds.descendingId,
                        group.postsFeeds.ascendingId,
                    )
            ).toSet()
        for (feedId in feedIds) {
            if (postsForFeed(feedId).any { it.id == newPost.id }) {
                _feedItems.update { feeds -> feeds + (feedId to (listOf(Feed.Item(feedId, newPost.id, now)) + feeds[feedId].orEmpty())) }
            }
        }

        return OctopusResult.Success(newPost)
    }

    override suspend fun deletePost(post: Post): OctopusResult<Unit, Nothing> {
        _posts.update { posts -> posts.filter { it.id != post.id } }
        _feedItems.update { feeds -> feeds.mapValues { (_, items) -> items.filter { it.itemId != post.id } } }
        return OctopusResult.Success(Unit)
    }

    override suspend fun setReaction(
        item: Post,
        reactionKind: Reaction.Kind?,
        isContentTranslated: Boolean?,
    ): OctopusResult<Reaction?, ServerError> {
        if (reactionKind == null) {
            // Remove reaction
            var removedReaction: Reaction? = null
            _posts.update { currentPosts ->
                currentPosts.map { existingPost ->
                    if (existingPost.id == item.id) {
                        removedReaction = existingPost.userReaction
                        val updatedReactions =
                            existingPost.reactions
                                .map { reactions ->
                                    var newCount = reactions.count
                                    if (reactions.reactionKind == removedReaction?.kind) {
                                        newCount = maxOf(0, newCount - 1)
                                    }
                                    reactions.copy(count = newCount)
                                }.filter { it.count > 0 }

                        existingPost.copy(userReaction = null, reactions = updatedReactions)
                    } else {
                        existingPost
                    }
                }
            }
            return OctopusResult.Success(removedReaction)
        }

        // Set reaction
        var newReaction: Reaction? = null
        _posts.update { currentPosts ->
            currentPosts.map { existingPost ->
                if (existingPost.id == item.id) {
                    val currentReaction = existingPost.userReaction
                    newReaction =
                        Reaction(
                            kind = reactionKind,
                            id = UUID.randomUUID().toString(),
                        )

                    val updatedReactions =
                        existingPost.reactions
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

                    existingPost.copy(userReaction = newReaction, reactions = updatedReactions)
                } else {
                    existingPost
                }
            }
        }

        return OctopusResult.Success(newReaction)
    }

    override suspend fun setReaction(
        reactionKind: OctopusReactionKind?,
        postId: String,
    ): OctopusResult<Reaction?, SetReactionError> {
        if (reactionKind is Reaction.Kind.Unknown) {
            return OctopusResult.Failure.InvalidArguments(SetReactionError.UnknownReaction)
        }
        val post =
            _posts.value.find { it.id == postId }
                ?: return OctopusResult.Failure.InvalidArguments(SetReactionError.PostNotFound)
        val result =
            setReaction(
                item = post,
                reactionKind = reactionKind as? Reaction.Kind,
                isContentTranslated = null,
            )
        return OctopusResult.Success((result as? OctopusResult.Success)?.data)
    }

    override suspend fun deleteLocalOrphanedItems() {
        // No-op for fake implementation
    }

    override suspend fun resetCache() {
        // No-op for fake implementation
    }

    override fun getClientObjectPostFlow(clientObjectId: String): Flow<Post?> =
        _posts.map { posts -> posts.find { it.clientObject?.id == clientObjectId } }

    override suspend fun fetchOrCreateClientObjectPost(
        clientPost: ClientPost,
        tokenProvider: (suspend (String) -> String?)?,
    ): OctopusResult<Post, ClientPostError> {
        if (clientPost.text.isBlank()) {
            return OctopusResult.Failure.InvalidArguments(
                listOf(ClientPostError.TextError.Missing("Text is required")),
            )
        }

        // Check if post already exists for this client object
        val existingPost = _posts.value.find { it.clientObject?.id == clientPost.objectId }
        if (existingPost != null) {
            return OctopusResult.Success(existingPost)
        }

        val newPost =
            MockPosts.create(
                text =
                    TranslatableText(
                        original =
                            TranslatableText.Content(
                                text = clientPost.text,
                                language = "en-US",
                            ),
                    ),
                parentId = clientPost.groupId ?: "",
                clientObject =
                    Post.ClientObject(
                        id = clientPost.objectId,
                        catchPhrase =
                            clientPost.catchPhrase?.let {
                                TranslatableText(
                                    original =
                                        TranslatableText.Content(
                                            text = it,
                                            language = "en-US",
                                        ),
                                )
                            },
                        ctaText =
                            clientPost.viewObjectButtonText?.let {
                                TranslatableText(
                                    original =
                                        TranslatableText.Content(
                                            text = it,
                                            language = "en-US",
                                        ),
                                )
                            },
                    ),
            )

        _posts.update { currentPosts -> currentPosts + newPost }

        return OctopusResult.Success(newPost)
    }

    override suspend fun pollVote(
        post: Post,
        answer: Post.Poll.Answer,
        isPostTranslated: Boolean,
    ): OctopusResult<Post, PollVoteError> {
        val storedPost =
            _posts.value.find { it.id == post.id }
                ?: return OctopusResult.Failure.InvalidArguments(PollVoteError.MissingPollAnswer("Post not found"))
        val poll =
            storedPost.poll ?: return OctopusResult.Failure.InvalidArguments(
                PollVoteError.MissingPollAnswer("Poll not found"),
            )

        if (poll.answers.none { it.id == answer.id }) {
            return OctopusResult.Failure.InvalidArguments(
                PollVoteError.MissingPollAnswer("Invalid poll answer"),
            )
        }

        var updatedPost: Post? = null
        _posts.update { currentPosts ->
            currentPosts.map { existingPost ->
                if (existingPost.id == post.id) {
                    val updatedPoll =
                        existingPost.poll?.let { poll ->
                            poll.copy(
                                results =
                                    poll.results
                                        .map { result ->
                                            when {
                                                existingPost.pollVoteId == answer.id -> result

                                                result.answerId == answer.id ->
                                                    result.copy(voteCount = result.voteCount + 1)

                                                result.answerId == existingPost.pollVoteId ->
                                                    result.copy(voteCount = maxOf(0, result.voteCount - 1))

                                                else -> result
                                            }
                                        }.let { results ->
                                            // Add result for new answer if it doesn't exist
                                            if (results.none { it.answerId == answer.id }) {
                                                results +
                                                    Post.Poll.Result(
                                                        answerId = answer.id,
                                                        voteCount = 1,
                                                    )
                                            } else {
                                                results
                                            }
                                        },
                            )
                        }

                    existingPost
                        .copy(
                            pollVoteId = answer.id,
                            poll = updatedPoll,
                        ).also { updatedPost = it }
                } else {
                    existingPost
                }
            }
        }

        return OctopusResult.Success(updatedPost ?: error("Updated post not found"))
    }

    private fun updateViewCounts(ids: List<String>) {
        _posts.update { currentPosts ->
            currentPosts.map { post ->
                if (post.id in ids) {
                    post.copy(
                        viewCount = post.viewCount + 1,
                    )
                } else {
                    post
                }
            }
        }
    }
}
