package com.octopuscommunity.sdk.ui.common

import com.octopuscommunity.sdk.domain.model.Moderation.Reason
import com.octopuscommunity.sdk.domain.model.Moderation.ReportReason
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.UiText.JoinedResource
import com.octopuscommunity.sdk.ui.common.UiText.StringResource

val List<Reason>.uiText
    get() = StringResource(
        R.string.post_list_moderated_post_reason_reasons,
        listOf(JoinedResource(map { it.uiText }, separator = ", "))
    )

val Reason.uiText
    get() = when (this) {
        is Reason.HatefulConduct -> StringResource(R.string.moderation_reason_hateful_conduct)
        is Reason.BullyingOrHarassment -> StringResource(R.string.moderation_reason_bullying_or_harassment)
        is Reason.GraphicViolence -> StringResource(R.string.moderation_reason_graphic_violence)
        is Reason.Aggressive -> StringResource(R.string.moderation_reason_aggressive)
        is Reason.ViolentInstigation -> StringResource(R.string.moderation_reason_violent_instigation)
        is Reason.SexuallySuggestive -> StringResource(R.string.moderation_reason_sexually_suggestive)
        is Reason.AdultNudityOrSexualActivity -> StringResource(R.string.moderation_reason_adult_nudity_or_sexual_activity)
        is Reason.SexualSolicitationOrExplicitLanguage -> StringResource(R.string.moderation_reason_sexual_solicitation_or_explicit_language)
        is Reason.AdultSexualExploitation -> StringResource(R.string.moderation_reason_adult_sexual_exploitation)
        is Reason.ChildSexualAbuseMaterial -> StringResource(R.string.moderation_reason_child_sexual_abuse_material)
        is Reason.SuicideOrSelfHarm -> StringResource(R.string.moderation_reason_suicide_or_self_harm)
        is Reason.RegulatedGoods -> StringResource(R.string.moderation_reason_regulated_goods)
        is Reason.HumanExploitation -> StringResource(R.string.moderation_reason_human_exploitation)
        is Reason.TerrorismOrganizedHateDangerousOrganizationsOrIndividual -> StringResource(R.string.moderation_reason_terrorism_organized_hate_dangerous_organizations_or_individual)
        is Reason.HarmOrCrime -> StringResource(R.string.moderation_reason_harm_or_crime)
        is Reason.SpamOrScams -> StringResource(R.string.moderation_reason_spam_or_scams)
        is Reason.Impersonation -> StringResource(R.string.moderation_reason_impersonation)
        is Reason.Misinformation -> StringResource(R.string.moderation_reason_misinformation)
        is Reason.PersonallyIdentifiableInformation -> StringResource(R.string.moderation_reason_personally_identifiable_information)
        is Reason.UsernamePolicy -> StringResource(R.string.moderation_reason_username_policy)
        is Reason.CopyrightOrIntellectualPropertyInfringement -> StringResource(R.string.moderation_reason_copyright_or_intellectual_property_infringement)
        is Reason.Other -> message?.let { UiText.DynamicString(it) }
            ?: StringResource(R.string.moderation_reason_other)
    }

val ReportReason.uiText
    get() = when (this) {
        ReportReason.HateSpeechOrDiscriminatoryContent -> StringResource(R.string.report_reason_hate_speech_or_discrimination_or_harassment)
        ReportReason.ExplicitOrInappropriateContent -> StringResource(R.string.report_reason_explicit_or_inappropriate_content)
        ReportReason.ViolenceAndTerrorism -> StringResource(R.string.report_reason_violence_and_terrorism)
        ReportReason.SpamAndScams -> StringResource(R.string.report_reason_spam_and_scams)
        ReportReason.SuicideAndSelfHarm -> StringResource(R.string.report_reason_suicide_and_self_harm)
        ReportReason.FakeProfilesAndImpersonation -> StringResource(R.string.report_reason_fake_profiles_and_impersonation)
        ReportReason.ChildExploitationOrAbuse -> StringResource(R.string.report_reason_child_exploitation_or_abuse)
        ReportReason.IntellectualPropertyViolation -> StringResource(R.string.report_reason_intellectual_property_violation)
        is ReportReason.Other -> message?.let { UiText.DynamicString(it) }
            ?: StringResource(R.string.report_reason_other)
    }