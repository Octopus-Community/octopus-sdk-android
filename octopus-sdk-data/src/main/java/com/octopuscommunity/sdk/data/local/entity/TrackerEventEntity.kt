package com.octopuscommunity.sdk.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.max

@Entity(tableName = "tracker_events")
@TypeConverters(TrackerEventEntity.Converters::class)
data class TrackerEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val appSessionId: String?,
    val octopusSessionId: String?,
    val data: Data,
) {
    @Serializable
    sealed class Data {
        @Serializable
        data class EnteringApp(
            val isFirstSession: Boolean,
        ) : Data()

        @Serializable
        data class LeavingApp(
            val sessionStartTime: Long,
            val sessionEndTime: Long,
            val isFirstSession: Boolean,
        ) : Data() {
            val sessionDuration: Long get() = max(0, sessionEndTime - sessionStartTime)
        }

        @Serializable
        data class EnteringOctopus(
            val isFirstSession: Boolean,
        ) : Data()

        @Serializable
        data class LeavingOctopus(
            val sessionStartTime: Long,
            val sessionEndTime: Long,
            val isFirstSession: Boolean,
        ) : Data() {
            val sessionDuration: Long get() = max(0, sessionEndTime - sessionStartTime)
        }

        @Serializable
        data class PostOpened(
            val success: Boolean,
            val origin: Origin,
        ) : Data() {
            @Serializable
            sealed class Origin {
                @Serializable
                data object ClientApp : Origin()

                @Serializable
                data class Sdk(
                    val hasFeaturedComment: Boolean,
                ) : Origin()
            }
        }

        @Serializable
        data object ClientObjectOpened : Data()

        @Serializable
        data class TranslationButtonClicked(
            val translationDisplayed: Boolean,
        ) : Data()

        @Serializable
        data class VideoPlayed(
            val contentId: String,
            val videoId: String,
            val watchedTime: Long,
            val videoDuration: Long,
        ) : Data()

        @Serializable
        data class CtaButtonClicked(
            val postId: String,
        ) : Data()

        @Serializable
        data class TopicCtaButtonClicked(
            val topicId: String,
        ) : Data()

        @Serializable
        data class ClientSetABTest(
            val hasAccessToCommunity: Boolean,
        ) : Data()

        @Serializable
        data class Custom(
            val name: String,
            val properties: Map<String, String>,
        ) : Data()

        @Serializable
        data class OctopusDrivenLogin(
            /** The triggering action, stored as `OctopusDrivenLoginAction.name`. */
            val action: String,
        ) : Data()
    }

    class Converters {
        private val json = Json { ignoreUnknownKeys = true }

        @TypeConverter
        fun fromEventData(data: Data): String = json.encodeToString(data)

        @TypeConverter
        fun toEventData(eventDataString: String): Data = json.decodeFromString(eventDataString)
    }
}
