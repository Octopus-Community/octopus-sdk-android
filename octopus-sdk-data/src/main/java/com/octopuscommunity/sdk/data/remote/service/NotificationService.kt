package com.octopuscommunity.sdk.data.remote.service

import com.octopuscommunity.NotificationServiceGrpcKt.NotificationServiceCoroutineStub
import com.octopuscommunity.NotificationServiceProto.NotificationSettings
import com.octopuscommunity.getNotificationSettingsRequest
import com.octopuscommunity.getUserNotificationsRequest
import com.octopuscommunity.markNotificationsAsReadRequest
import com.octopuscommunity.registerPushTokenRequest
import com.octopuscommunity.sdk.data.remote.client.OctopusGrpcClient
import com.octopuscommunity.setNotificationSettingsRequest

class NotificationService(
    private val grpcClient: OctopusGrpcClient
) {
    private val serviceStub by lazy {
        NotificationServiceCoroutineStub(grpcClient.unaryChannel)
    }

    suspend fun registerPushToken(token: String?) = serviceStub.registerPushToken(
        registerPushTokenRequest {
            if (token != null) {
                this.pushToken = token
            }
        }
    )

    suspend fun getNotificationSettings() = serviceStub.getNotificationSettings(
        getNotificationSettingsRequest { }
    )

    suspend fun setNotificationSettings(
        settings: NotificationSettings
    ) = serviceStub.setNotificationSettings(
        setNotificationSettingsRequest {
            this.settings = settings
        }
    )

    suspend fun getUserNotifications(
        userId: String,
        markdownText: Boolean = true
    ) = serviceStub.getUserNotifications(
        getUserNotificationsRequest {
            this.userId = userId
            this.markdownText = markdownText
        }
    )

    suspend fun markNotificationsAsRead(
        notificationIds: List<String>,
        isRead: Boolean = true
    ) = serviceStub.markNotificationsAsRead(
        markNotificationsAsReadRequest {
            this.notificationIds += notificationIds
            this.read = isRead
        }
    )
}