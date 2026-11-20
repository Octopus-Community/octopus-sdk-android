package com.octopuscommunity.sdk.data.remote.service

import com.octopuscommunity.FeedServiceGrpcKt
import com.octopuscommunity.FeedServiceProto.GetFeedPageResponse
import com.octopuscommunity.FeedServiceProto.GetFeedWithOctoObjectPageResponse
import com.octopuscommunity.getFeedPageRequest
import com.octopuscommunity.getFeedWithOctoObjectPageRequest
import com.octopuscommunity.getRootFeedsInfoRequest
import com.octopuscommunity.initializeFeedRequest
import com.octopuscommunity.initializeFeedWithOctoObjectRequest
import com.octopuscommunity.sdk.data.remote.client.OctopusGrpcClient
import com.octopuscommunity.sdk.data.repository.RootFeedSession
import com.octopuscommunity.sdk.data.repository.UserRepositoryImpl
import io.grpc.Channel

open class FeedService(
    private val channelProvider: () -> Channel,
) {
    // Kept for the production DI call site (`FeedService(grpcClient = get())`); tests inject a
    // channelProvider directly (e.g. an in-process channel bound to a fake stub) without needing
    // a real OctopusGrpcClient (which requires an Android Context to construct).
    constructor(grpcClient: OctopusGrpcClient) : this({ grpcClient.unaryChannel }) {
        sessionProvider = { (grpcClient.userRepository as? UserRepositoryImpl)?.rootFeedSession }
    }

    private var sessionProvider: () -> RootFeedSession? = { null }
    internal val rootFeedSession: RootFeedSession? get() = sessionProvider()

    private val serviceStub by lazy {
        FeedServiceGrpcKt.FeedServiceCoroutineStub(channelProvider())
    }

    suspend fun getRootFeedsInfo() = serviceStub.getRootFeedsInfo(getRootFeedsInfoRequest {})

    suspend fun initializeFeed(
        feedId: String,
        pageSize: Int?,
        anchorItemId: String? = null,
    ): GetFeedPageResponse =
        serviceStub.initializeFeed(
            initializeFeedRequest {
                this.feedId = feedId
                pageSize?.let { this.pageSize = it }
                anchorItemId?.let { this.anchorItemId = it }
            },
        )

    suspend fun getFeedPage(
        pageCursor: String,
        pageSize: Int?,
    ): GetFeedPageResponse =
        serviceStub.getFeedPage(
            getFeedPageRequest {
                this.pageCursor = pageCursor
                pageSize?.let { this.pageSize = it }
            },
        )

    // WithOctoObject variants: same RPCs as above but the response ships parent objects
    // (relatedObjects/relatedAggregates) alongside the feed, needed by feeds that lack their own
    // parent context (e.g. the profile "comments" feed - internal ticket).
    suspend fun initializeFeedWithOctoObject(
        feedId: String,
        pageSize: Int,
    ): GetFeedWithOctoObjectPageResponse =
        serviceStub.initializeFeedWithOctoObject(
            initializeFeedWithOctoObjectRequest {
                this.feedId = feedId
                this.pageSize = pageSize
            },
        )

    suspend fun getFeedWithOctoObjectPage(
        pageCursor: String,
        pageSize: Int,
        fetchAggregates: Boolean = true,
    ): GetFeedWithOctoObjectPageResponse =
        serviceStub.getFeedWithOctoObjectPage(
            getFeedWithOctoObjectPageRequest {
                this.pageCursor = pageCursor
                this.pageSize = pageSize
                this.fetchAggregates = fetchAggregates
            },
        )
}
