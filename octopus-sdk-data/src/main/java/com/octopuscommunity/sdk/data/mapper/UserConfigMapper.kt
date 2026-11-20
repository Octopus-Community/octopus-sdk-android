package com.octopuscommunity.sdk.data.mapper

import com.octopuscommunity.UserServiceProto
import com.octopuscommunity.UserServiceProto.CanAccessCommunityResponse.NoAccessReasonCase
import com.octopuscommunity.sdk.data.local.entity.UserConfigEntity
import com.octopuscommunity.sdk.domain.model.CommunityAccessDenial
import com.octopuscommunity.sdk.domain.model.UserConfig
import com.octopuscommunity.sdk.domain.repository.CommunityConfigRepository

class UserConfigMapper {
    fun UserConfigEntity.toDomain() =
        UserConfig(
            canAccessCommunity = canAccessCommunity,
            accessDeniedMessage = accessDeniedMessage,
            accessDenial = if (canAccessCommunity) null else accessDenial(),
        )

    /** A row without reason (written before the reason existed) or with one this SDK does not know is unknown. */
    private fun UserConfigEntity.accessDenial(): CommunityAccessDenial =
        when (accessDeniedReason) {
            REASON_COHORT -> CommunityAccessDenial.NotInCohort
            REASON_MEMBER_EXCLUDED -> CommunityAccessDenial.MemberExcluded
            REASON_ACCOUNT_BANNED ->
                CommunityAccessDenial.AccountBanned(
                    nickname = banNickname,
                    policyName = banPolicyName.orEmpty(),
                    policyDescription = banPolicyDescription.orEmpty(),
                    contactEmail = banContactEmail,
                )
            REASON_ACCOUNT_DELETED -> CommunityAccessDenial.AccountDeleted
            else -> CommunityAccessDenial.Unknown
        }

    fun UserServiceProto.CanAccessCommunityResponse.toEntity(): UserConfigEntity {
        val message = if (hasCommunityDisabledMessage()) communityDisabledMessage.ifEmpty { null } else null
        // A reason only means something while the access is denied.
        if (canAccessCommunity) return UserConfigEntity(canAccessCommunity = true, accessDeniedMessage = message)
        val ban = if (hasAccountBanned()) accountBanned else null
        return UserConfigEntity(
            canAccessCommunity = false,
            accessDeniedMessage = message,
            accessDeniedReason =
                when (noAccessReasonCase) {
                    NoAccessReasonCase.NOACCESSCOHORT -> REASON_COHORT
                    NoAccessReasonCase.MEMBEREXCLUDED -> REASON_MEMBER_EXCLUDED
                    NoAccessReasonCase.ACCOUNTBANNED -> REASON_ACCOUNT_BANNED
                    NoAccessReasonCase.ACCOUNTDELETED -> REASON_ACCOUNT_DELETED
                    // An ended session is replaced, never stored as an access: the access read stops before this.
                    NoAccessReasonCase.SESSIONENDED,
                    // An older server sends no reason; a newer one may send a case this SDK cannot read.
                    NoAccessReasonCase.NOACCESSREASON_NOT_SET, null,
                    -> REASON_UNKNOWN
                },
            // The server may send an empty text where it has none.
            banNickname = ban?.takeIf { it.hasNickname() }?.nickname?.ifEmpty { null },
            banPolicyName = ban?.policyName,
            banPolicyDescription = ban?.policyDescription,
            banContactEmail = ban?.takeIf { it.hasContactEmail() }?.contactEmail?.ifEmpty { null },
        )
    }

    fun UserServiceProto.ByPassAbTestingResponse.Error.toDomain() =
        when (errorCode) {
            UserServiceProto.ByPassAbTestingResponse.ErrorCode.UNRECOGNIZED,
            UserServiceProto.ByPassAbTestingResponse.ErrorCode.UNKNOWN_ERROR,
            -> {
                CommunityConfigRepository.OverrideCommunityAccessError.Unknown(message)
            }
        }

    private companion object {
        const val REASON_COHORT = "COHORT"
        const val REASON_MEMBER_EXCLUDED = "MEMBER_EXCLUDED"
        const val REASON_ACCOUNT_BANNED = "ACCOUNT_BANNED"
        const val REASON_ACCOUNT_DELETED = "ACCOUNT_DELETED"
        const val REASON_UNKNOWN = "UNKNOWN"
    }
}
