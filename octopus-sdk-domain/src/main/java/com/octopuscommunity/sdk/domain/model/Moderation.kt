package com.octopuscommunity.sdk.domain.model

/** Namespace of the moderation types. */
class Moderation {
    /** Reasons a user can give when reporting content or a profile, sorted by [order]. */
    sealed class ReportReason(
        val order: Int,
    ) {
        /** Hate speech or discriminatory content */
        data object HateSpeechOrDiscriminatoryContent : ReportReason(0)

        /** Explicit or inappropriate content */
        data object ExplicitOrInappropriateContent : ReportReason(1)

        /** Violence and terrorism */
        data object ViolenceAndTerrorism : ReportReason(2)

        /** Spam and scams */
        data object SpamAndScams : ReportReason(3)

        /** Suicide and self-harm  */
        data object SuicideAndSelfHarm : ReportReason(4)

        /**
         * Illegal activities or products, sent as REPORT_ILL.
         * The name is kept for binary compatibility; fake profiles belong to [SpamAndScams].
         */
        data object FakeProfilesAndImpersonation : ReportReason(5)

        /** Child exploitation or abuse */
        data object ChildExploitationOrAbuse : ReportReason(6)

        /** Intellectual property violation */
        data object IntellectualPropertyViolation : ReportReason(7)

        /** Other */
        data class Other(
            val message: String? = null,
        ) : ReportReason(8)
    }

    /** Reasons given by the moderation when content is moderated, sorted by [order]. */
    sealed class Reason(
        val order: Int,
    ) {
        /** Hateful Conduct */
        data object HatefulConduct : Reason(0)

        /** Bullying or Harassment */
        data object BullyingOrHarassment : Reason(1)

        /** Graphic Violence */
        data object GraphicViolence : Reason(2)

        /** Aggressive */
        data object Aggressive : Reason(3)

        /** Violent Instigation */
        data object ViolentInstigation : Reason(4)

        /** Sexually Suggestive */
        data object SexuallySuggestive : Reason(5)

        /** Adult Nudity or Sexual Activity */
        data object AdultNudityOrSexualActivity : Reason(6)

        /** Sexual Solicitation or Explicit Language */
        data object SexualSolicitationOrExplicitLanguage : Reason(7)

        /** Adult Sexual Exploitation */
        data object AdultSexualExploitation : Reason(8)

        /** Child Sexual Abuse Material */
        data object ChildSexualAbuseMaterial : Reason(9)

        /** Suicide or Self-harm */
        data object SuicideOrSelfHarm : Reason(10)

        /** Regulated Goods */
        data object RegulatedGoods : Reason(11)

        /** Human Exploitation */
        data object HumanExploitation : Reason(12)

        /** Terrorism, Organized Hate, Dangerous Organizations or Individual */
        data object TerrorismOrganizedHateDangerousOrganizationsOrIndividual : Reason(13)

        /** Harm or Crime */
        data object HarmOrCrime : Reason(14)

        /** Spam or Scams */
        data object SpamOrScams : Reason(15)

        /** Impersonation */
        data object Impersonation : Reason(16)

        /** Misinformation */
        data object Misinformation : Reason(17)

        /** Personally Identifiable Information */
        data object PersonallyIdentifiableInformation : Reason(18)

        /** Username Policy */
        data object UsernamePolicy : Reason(19)

        /** Copyright or Intellectual Property Infringement */
        data object CopyrightOrIntellectualPropertyInfringement : Reason(20)

        /** Other */
        data class Other(
            val message: String? = null,
        ) : Reason(21)
    }
}
