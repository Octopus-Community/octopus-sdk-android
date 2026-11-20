package com.octopuscommunity.sdk.data.local.entity

data class StatusEntity(
    val value: Value,
    val reasons: List<ModerationEntity.ReasonEntity> = listOf()
) {
    enum class Value {
        UNKNOWN,
        PUBLISHED,
        MODERATED
    }
}