package com.octopuscommunity.sdk.data.remote.service

import com.octopuscommunity.GrpcModelProto
import com.octopuscommunity.OctoObjectServiceGrpcKt.OctoObjectServiceCoroutineStub
import com.octopuscommunity.OctoObjectServiceProto.CreateImageUploadsRequest
import com.octopuscommunity.OctoObjectServiceProto.GetRequest
import com.octopuscommunity.OctoObjectServiceProto.PutRequest
import com.octopuscommunity.OctoObjectServiceProto.RwOctoObject
import com.octopuscommunity.content
import com.octopuscommunity.deleteCommentRequest
import com.octopuscommunity.deletePostRequest
import com.octopuscommunity.deleteReplyRequest
import com.octopuscommunity.deleteRequest
import com.octopuscommunity.getBatchRequest
import com.octopuscommunity.getBridgePostRequest
import com.octopuscommunity.getOrCreateBridgePostRequest
import com.octopuscommunity.getReactionsPageRequest
import com.octopuscommunity.getTopicsRequest
import com.octopuscommunity.pollVote
import com.octopuscommunity.putRequest
import com.octopuscommunity.reaction
import com.octopuscommunity.reportContentRequest
import com.octopuscommunity.rwOctoObject
import com.octopuscommunity.sdk.data.remote.client.OctopusGrpcClient

class ObjectService(
    private val grpcClient: OctopusGrpcClient,
) {
    private val serviceStub by lazy {
        OctoObjectServiceCoroutineStub(grpcClient.unaryChannel)
    }

    suspend fun getBatch(requests: List<GetRequest>) =
        serviceStub
            .getBatch(
                getBatchRequest {
                    this.requests += requests
                },
            ).responsesList

    suspend fun getGroups() =
        serviceStub.getTopics(
            getTopicsRequest {},
        )

    suspend fun putPost(
        octopusObject: RwOctoObject,
        creationSource: PutRequest.CreationSource = PutRequest.CreationSource.USER,
        clientToken: String? = null,
    ) = serviceStub.putPost(
        putRequest {
            this.octoObject = octopusObject
            this.creationSource = creationSource
            clientToken?.let { this.clientToken = it }
        },
    )

    /** Asks for one signed upload URL per photo of a post, in order, before the post is put. */
    suspend fun createImageUploads(request: CreateImageUploadsRequest) = serviceStub.createImageUploads(request)

    suspend fun getOrCreateBridgePost(
        postBridge: GrpcModelProto.Post,
        groupId: String? = null,
        clientToken: String? = null,
    ) = serviceStub.getOrCreateBridgePost(
        getOrCreateBridgePostRequest {
            this.postBridge = postBridge
            groupId?.let { this.topicId = it }
            clientToken?.let { this.clientToken = it }
        },
    )

    suspend fun getBridgePost(clientObjectId: String) =
        serviceStub.getBridgePost(
            getBridgePostRequest {
                this.clientObjectId = clientObjectId
            },
        )

    suspend fun createBridgePost(
        postBridge: GrpcModelProto.Post,
        groupId: String? = null,
        clientToken: String? = null,
    ) = serviceStub.createBridgePost(
        getOrCreateBridgePostRequest {
            this.postBridge = postBridge
            groupId?.let { this.topicId = it }
            clientToken?.let { this.clientToken = it }
        },
    )

    suspend fun deletePost(id: String) =
        serviceStub.deletePost(
            deletePostRequest {
                this.octoObjectId = id
            },
        )

    suspend fun putComment(
        comment: RwOctoObject,
        parentIsTranslated: Boolean,
    ) = serviceStub.putComment(
        putRequest {
            this.octoObject = comment
            this.translatedContent = parentIsTranslated
        },
    )

    suspend fun deleteComment(id: String) =
        serviceStub.deleteComment(
            deleteCommentRequest {
                this.octoObjectId = id
            },
        )

    suspend fun putReply(
        reply: RwOctoObject,
        isParentTranslated: Boolean,
    ) = serviceStub.putReply(
        putRequest {
            this.octoObject = reply
            this.translatedContent = isParentTranslated
        },
    )

    suspend fun deleteReply(id: String) =
        serviceStub.deleteReply(
            deleteReplyRequest {
                this.octoObjectId = id
            },
        )

    suspend fun get(request: GetRequest) = serviceStub.get(request)

    suspend fun putReaction(
        parentId: String,
        unicode: String,
        isContentTranslated: Boolean?,
    ) = serviceStub.putReaction(
        putRequest {
            this.octoObject =
                rwOctoObject {
                    this.parentId = parentId
                    this.content =
                        content {
                            this.reaction =
                                reaction {
                                    this.unicode = unicode
                                }
                        }
                }
            if (isContentTranslated != null) {
                this.translatedContent = isContentTranslated
            }
        },
    )

    suspend fun deleteReaction(reactionId: String) =
        serviceStub.deleteReaction(
            deleteRequest {
                this.octoObjectId = reactionId
            },
        )

    /**
     * Lists who reacted on [parentId], most recent reaction first.
     *
     * [unicode] and [pageCursor] are left unset rather than empty when null: unset unicode means "every
     * reaction kind" (the All tab), and unset cursor means "first page".
     */
    suspend fun getReactionsPage(
        parentId: String,
        unicode: String?,
        pageCursor: String?,
        pageSize: Int,
    ) = serviceStub.getReactionsPage(
        getReactionsPageRequest {
            this.parentId = parentId
            this.pageSize = pageSize.coerceAtLeast(0)
            if (unicode != null) this.unicode = unicode
            if (pageCursor != null) this.pageCursor = pageCursor
        },
    )

    suspend fun pollVote(
        parentId: String,
        answerId: String,
        isParentTranslated: Boolean,
    ) = serviceStub.putPollVote(
        putRequest {
            this.octoObject =
                rwOctoObject {
                    this.parentId = parentId
                    this.content =
                        content {
                            this.vote =
                                pollVote {
                                    this.pollAnswerId = answerId
                                }
                        }
                }
            this.translatedContent = isParentTranslated
        },
    )

    suspend fun reportContent(
        id: String,
        reasonCodes: List<GrpcModelProto.ReportReasonCode>,
        customReason: String? = null,
    ) = serviceStub.reportContent(
        reportContentRequest {
            this.octoObjectId = id
            this.reasonCodes += reasonCodes
            customReason?.let {
                this.customReason = it
            }
        },
    )
}
