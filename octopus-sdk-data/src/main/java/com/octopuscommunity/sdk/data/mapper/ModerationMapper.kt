package com.octopuscommunity.sdk.data.mapper

import com.octopuscommunity.GrpcModelProto
import com.octopuscommunity.sdk.data.local.entity.ModerationEntity.ReasonEntity
import com.octopuscommunity.sdk.domain.model.Moderation.Reason
import com.octopuscommunity.sdk.domain.model.Moderation.ReportReason

class ModerationMapper : OctopusMapper {
    fun ReasonEntity.toDomain(): Reason? =
        when (code) {
            ReasonEntity.Code.HTE -> Reason.HatefulConduct
            ReasonEntity.Code.HRS -> Reason.BullyingOrHarassment
            ReasonEntity.Code.GV -> Reason.GraphicViolence
            ReasonEntity.Code.AGG -> Reason.Aggressive
            ReasonEntity.Code.VLC -> Reason.ViolentInstigation
            ReasonEntity.Code.SXC -> Reason.SexuallySuggestive
            ReasonEntity.Code.NUD -> Reason.AdultNudityOrSexualActivity
            ReasonEntity.Code.SOLI -> Reason.SexualSolicitationOrExplicitLanguage
            ReasonEntity.Code.AEX -> Reason.AdultSexualExploitation
            ReasonEntity.Code.CEX -> Reason.ChildSexualAbuseMaterial
            ReasonEntity.Code.SSH -> Reason.SuicideOrSelfHarm
            ReasonEntity.Code.ILL -> Reason.RegulatedGoods
            ReasonEntity.Code.HEX -> Reason.HumanExploitation
            ReasonEntity.Code.TER -> Reason.TerrorismOrganizedHateDangerousOrganizationsOrIndividual
            ReasonEntity.Code.HC -> Reason.HarmOrCrime
            ReasonEntity.Code.SPM -> Reason.SpamOrScams
            ReasonEntity.Code.IMP -> Reason.Impersonation
            ReasonEntity.Code.MIS -> Reason.Misinformation
            ReasonEntity.Code.PII -> Reason.PersonallyIdentifiableInformation
            ReasonEntity.Code.UP -> Reason.UsernamePolicy
            ReasonEntity.Code.CIP -> Reason.CopyrightOrIntellectualPropertyInfringement
            ReasonEntity.Code.OTH -> Reason.Other(message ?: "")
            null -> null
        }

    fun Reason.toEntity(): ReasonEntity =
        when (this) {
            is Reason.HatefulConduct -> ReasonEntity(ReasonEntity.Code.HTE)
            is Reason.BullyingOrHarassment -> ReasonEntity(ReasonEntity.Code.HRS)
            is Reason.GraphicViolence -> ReasonEntity(ReasonEntity.Code.GV)
            is Reason.Aggressive -> ReasonEntity(ReasonEntity.Code.AGG)
            is Reason.ViolentInstigation -> ReasonEntity(ReasonEntity.Code.VLC)
            is Reason.SexuallySuggestive -> ReasonEntity(ReasonEntity.Code.SXC)
            is Reason.AdultNudityOrSexualActivity -> ReasonEntity(ReasonEntity.Code.NUD)
            is Reason.SexualSolicitationOrExplicitLanguage -> ReasonEntity(ReasonEntity.Code.SOLI)
            is Reason.AdultSexualExploitation -> ReasonEntity(ReasonEntity.Code.AEX)
            is Reason.ChildSexualAbuseMaterial -> ReasonEntity(ReasonEntity.Code.CEX)
            is Reason.SuicideOrSelfHarm -> ReasonEntity(ReasonEntity.Code.SSH)
            is Reason.RegulatedGoods -> ReasonEntity(ReasonEntity.Code.ILL)
            is Reason.HumanExploitation -> ReasonEntity(ReasonEntity.Code.HEX)
            is Reason.TerrorismOrganizedHateDangerousOrganizationsOrIndividual ->
                ReasonEntity(
                    ReasonEntity.Code.TER,
                )

            is Reason.HarmOrCrime -> ReasonEntity(ReasonEntity.Code.HC)
            is Reason.SpamOrScams -> ReasonEntity(ReasonEntity.Code.SPM)
            is Reason.Impersonation -> ReasonEntity(ReasonEntity.Code.IMP)
            is Reason.Misinformation -> ReasonEntity(ReasonEntity.Code.MIS)
            is Reason.PersonallyIdentifiableInformation -> ReasonEntity(ReasonEntity.Code.PII)
            is Reason.UsernamePolicy -> ReasonEntity(ReasonEntity.Code.UP)
            is Reason.CopyrightOrIntellectualPropertyInfringement -> ReasonEntity(ReasonEntity.Code.CIP)
            is Reason.Other -> ReasonEntity(ReasonEntity.Code.OTH, message)
        }

    fun GrpcModelProto.StatusReason.toEntity() =
        ReasonEntity(
            code = code.toEntity(),
            message = message,
        )

    fun ReportReason.toProto(): GrpcModelProto.ReportReasonCode =
        when (this) {
            is ReportReason.HateSpeechOrDiscriminatoryContent -> GrpcModelProto.ReportReasonCode.REPORT_HTE_HRS
            is ReportReason.ExplicitOrInappropriateContent -> GrpcModelProto.ReportReasonCode.REPORT_SXC
            is ReportReason.ViolenceAndTerrorism -> GrpcModelProto.ReportReasonCode.REPORT_VLC_TER
            is ReportReason.SpamAndScams -> GrpcModelProto.ReportReasonCode.REPORT_SPM
            is ReportReason.SuicideAndSelfHarm -> GrpcModelProto.ReportReasonCode.REPORT_SSH
            is ReportReason.FakeProfilesAndImpersonation -> GrpcModelProto.ReportReasonCode.REPORT_ILL
            is ReportReason.ChildExploitationOrAbuse -> GrpcModelProto.ReportReasonCode.REPORT_CEX
            is ReportReason.IntellectualPropertyViolation -> GrpcModelProto.ReportReasonCode.REPORT_IPV
            is ReportReason.Other -> GrpcModelProto.ReportReasonCode.REPORT_OTH
        }

    fun GrpcModelProto.StatusReasonCode.toEntity(): ReasonEntity.Code? =
        when (this) {
            GrpcModelProto.StatusReasonCode.HTE -> ReasonEntity.Code.HTE
            GrpcModelProto.StatusReasonCode.HRS -> ReasonEntity.Code.HRS
            GrpcModelProto.StatusReasonCode.VLC -> ReasonEntity.Code.VLC
            GrpcModelProto.StatusReasonCode.SXC -> ReasonEntity.Code.SXC
            GrpcModelProto.StatusReasonCode.SPM -> ReasonEntity.Code.SPM
            GrpcModelProto.StatusReasonCode.PII -> ReasonEntity.Code.PII
            GrpcModelProto.StatusReasonCode.ILL -> ReasonEntity.Code.ILL
            GrpcModelProto.StatusReasonCode.TER -> ReasonEntity.Code.TER
            GrpcModelProto.StatusReasonCode.CEX -> ReasonEntity.Code.CEX
            GrpcModelProto.StatusReasonCode.IMP -> ReasonEntity.Code.IMP
            GrpcModelProto.StatusReasonCode.SSH -> ReasonEntity.Code.SSH
            GrpcModelProto.StatusReasonCode.OTH -> ReasonEntity.Code.OTH
            GrpcModelProto.StatusReasonCode.GV -> ReasonEntity.Code.GV
            GrpcModelProto.StatusReasonCode.AGG -> ReasonEntity.Code.AGG
            GrpcModelProto.StatusReasonCode.NUD -> ReasonEntity.Code.NUD
            GrpcModelProto.StatusReasonCode.SOLI -> ReasonEntity.Code.SOLI
            GrpcModelProto.StatusReasonCode.AEX -> ReasonEntity.Code.AEX
            GrpcModelProto.StatusReasonCode.HEX -> ReasonEntity.Code.HEX
            GrpcModelProto.StatusReasonCode.HC -> ReasonEntity.Code.HC
            GrpcModelProto.StatusReasonCode.MIS -> ReasonEntity.Code.MIS
            GrpcModelProto.StatusReasonCode.UP -> ReasonEntity.Code.UP
            GrpcModelProto.StatusReasonCode.CIP -> ReasonEntity.Code.CIP
            GrpcModelProto.StatusReasonCode.UNRECOGNIZED,
            GrpcModelProto.StatusReasonCode.UNSPECIFIED_REASON,
            -> null
        }

    fun GrpcModelProto.StatusReasonCode.toDomain(): Reason? =
        when (this) {
            GrpcModelProto.StatusReasonCode.HTE -> Reason.HatefulConduct
            GrpcModelProto.StatusReasonCode.HRS -> Reason.BullyingOrHarassment
            GrpcModelProto.StatusReasonCode.GV -> Reason.GraphicViolence
            GrpcModelProto.StatusReasonCode.AGG -> Reason.Aggressive
            GrpcModelProto.StatusReasonCode.VLC -> Reason.ViolentInstigation
            GrpcModelProto.StatusReasonCode.SXC -> Reason.SexuallySuggestive
            GrpcModelProto.StatusReasonCode.NUD -> Reason.AdultNudityOrSexualActivity
            GrpcModelProto.StatusReasonCode.SOLI -> Reason.SexualSolicitationOrExplicitLanguage
            GrpcModelProto.StatusReasonCode.AEX -> Reason.AdultSexualExploitation
            GrpcModelProto.StatusReasonCode.CEX -> Reason.ChildSexualAbuseMaterial
            GrpcModelProto.StatusReasonCode.SSH -> Reason.SuicideOrSelfHarm
            GrpcModelProto.StatusReasonCode.ILL -> Reason.RegulatedGoods
            GrpcModelProto.StatusReasonCode.HEX -> Reason.HumanExploitation
            GrpcModelProto.StatusReasonCode.TER -> Reason.TerrorismOrganizedHateDangerousOrganizationsOrIndividual
            GrpcModelProto.StatusReasonCode.HC -> Reason.HarmOrCrime
            GrpcModelProto.StatusReasonCode.SPM -> Reason.SpamOrScams
            GrpcModelProto.StatusReasonCode.IMP -> Reason.Impersonation
            GrpcModelProto.StatusReasonCode.MIS -> Reason.Misinformation
            GrpcModelProto.StatusReasonCode.PII -> Reason.PersonallyIdentifiableInformation
            GrpcModelProto.StatusReasonCode.UP -> Reason.UsernamePolicy
            GrpcModelProto.StatusReasonCode.CIP -> Reason.CopyrightOrIntellectualPropertyInfringement
            GrpcModelProto.StatusReasonCode.OTH -> Reason.Other()
            GrpcModelProto.StatusReasonCode.UNSPECIFIED_REASON,
            GrpcModelProto.StatusReasonCode.UNRECOGNIZED,
            -> null
        }

    fun GrpcModelProto.ReportReasonCode.toDomain(): ReportReason? =
        when (this) {
            GrpcModelProto.ReportReasonCode.REPORT_HTE_HRS -> ReportReason.HateSpeechOrDiscriminatoryContent
            GrpcModelProto.ReportReasonCode.REPORT_SXC -> ReportReason.ExplicitOrInappropriateContent
            GrpcModelProto.ReportReasonCode.REPORT_VLC_TER -> ReportReason.ViolenceAndTerrorism
            GrpcModelProto.ReportReasonCode.REPORT_SPM -> ReportReason.SpamAndScams
            GrpcModelProto.ReportReasonCode.REPORT_SSH -> ReportReason.SuicideAndSelfHarm
            GrpcModelProto.ReportReasonCode.REPORT_ILL -> ReportReason.FakeProfilesAndImpersonation
            GrpcModelProto.ReportReasonCode.REPORT_CEX -> ReportReason.ChildExploitationOrAbuse
            GrpcModelProto.ReportReasonCode.REPORT_IPV -> ReportReason.IntellectualPropertyViolation
            GrpcModelProto.ReportReasonCode.REPORT_OTH -> ReportReason.Other()
            GrpcModelProto.ReportReasonCode.REPORT_IMP,
            GrpcModelProto.ReportReasonCode.REPORT_UNSPECIFIED_REASON,
            GrpcModelProto.ReportReasonCode.UNRECOGNIZED,
            -> null
        }
}
