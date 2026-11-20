package com.octopuscommunity.sdk.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sections")
data class SectionEntity(
    @PrimaryKey val id: String,
    val label: String
)