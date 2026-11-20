package com.octopuscommunity.sdk.data.local.entity

import kotlinx.serialization.Serializable

interface OctopusEntity {
    val id: String
    val parentId: String
    val createdAt: Long
    val updatedAt: Long
    val localeLanguage: String
    val status: StatusEntity
}

@Serializable
data class ReactionCountEntity(val unicode: String, val count: Int)

data class ReactionEntity(val unicode: String, val id: String)

data class FeedsEntity(val descendingId: String?, val ascendingId: String?)