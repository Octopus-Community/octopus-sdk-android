package com.octopuscommunity.sdk.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A durable enqueue receipt, retained after upload so replaying the journal cannot queue twice. */
@Entity(tableName = "app_session_end_receipts")
data class AppSessionEndReceiptEntity(
    @PrimaryKey val appSessionId: String,
)
