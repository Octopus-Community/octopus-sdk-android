package com.octopuscommunity.sdk.data.repository

import com.octopuscommunity.GrpcModelProto.ReportReasonCode
import com.octopuscommunity.sdk.data.mapper.ModerationMapper
import com.octopuscommunity.sdk.data.remote.service.ObjectService
import com.octopuscommunity.sdk.data.remote.service.UserService
import com.octopuscommunity.sdk.data.utils.call
import com.octopuscommunity.sdk.domain.model.Moderation
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.ModerationRepository

class ModerationRepositoryImpl(
    private val objectService: ObjectService,
    private val userService: UserService,
    private val moderationMapper: ModerationMapper
) : ModerationRepository {

    override fun getModerationReportReasons(): List<Moderation.ReportReason> =
        with(moderationMapper) {
            ReportReasonCode.entries.mapNotNull { it.toDomain() }.sortedBy { it.order }
        }

    override suspend fun reportContent(
        contentId: String,
        reasons: List<Moderation.ReportReason>,
        customReason: String?
    ): OctopusResult<Unit, Nothing> = call(moderationMapper) {
        objectService.reportContent(
            id = contentId,
            reasonCodes = reasons.map { it.toProto() },
            customReason = customReason
        )
    }

    override suspend fun reportUser(
        userId: String,
        reasons: List<Moderation.ReportReason>,
        customReason: String?
    ): OctopusResult<Unit, Nothing> = call(moderationMapper) {
        userService.reportUser(
            id = userId,
            reasonCodes = reasons.map { it.toProto() },
            customReason = customReason
        )
    }
}