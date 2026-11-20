package com.octopuscommunity.sdk.domain.model

import com.octopuscommunity.sdk.domain.model.Gamification.Action
import com.octopuscommunity.sdk.domain.model.Gamification.Toast
import java.text.NumberFormat

/**
 * The configuration of the current community (identified by the API key).
 */
data class CommunityConfig(
    /**
     * Whether any strong actions (post, comment, reply, open profile) should open the login flow
     */
    val forceLoginOnStrongActions: Boolean,
    val gamification: Gamification?,
    val displayAccountAge: Boolean,
    val poweredByOctopus: PoweredByOctopus = PoweredByOctopus.Default,
    /**
     * Per-field profile editability lock. Absent config defaults to [ProfileFieldsLock.AllEditable],
     * i.e. today's behaviour (every field editable).
     */
    val profileFieldsLock: ProfileFieldsLock = ProfileFieldsLock.AllEditable,
    val contentOptions: ContentOptions = ContentOptions(),
    /**
     * Whether the community exposes members' client user ids. Unified Profile is active only when
     * this flag is on AND the host wired `onNavigateToProfile`. When active, taps on members with a
     * [MinimalProfile.clientUserId] call the host's callback, and members without a client id
     * (guest / BO / admin) open the Octopus activity screen. With the flag off or the callback
     * absent, profile taps open the native Octopus profile. Defaults to `false`.
     */
    val exposeClientUserId: Boolean = false,
    /**
     * How users must accept the legal documents at their first contribution. Absent / unseeded
     * defaults to [TermsAcceptanceMode.IMPLICIT], i.e. today's behaviour (implicit acceptance, no
     * consent sheet).
     */
    val termsAcceptanceMode: TermsAcceptanceMode = TermsAcceptanceMode.IMPLICIT,
    /**
     * Whether the member "Comments" tab is shown on *other* members' profiles too, not only the
     * connected user's own profile. Absent / unseeded defaults to `false` (comments tab only on the
     * connected user's own profile). Mirrors iOS `CommunityConfig.showCommentsOnOtherProfiles`.
     */
    val showCommentsOnOtherProfiles: Boolean = false,
    /**
     * Whether a member who has not confirmed a nickname yet must type one before their first post,
     * comment or reply (empty field), instead of being offered the generated one (prefilled field).
     * Absent / unseeded defaults to `false` (prefilled field). Mirrors iOS
     * `CommunityConfig.forceNicknameChoice`.
     */
    val forceNicknameChoice: Boolean = false,
    /**
     * The community's own consent wordings, in markdown and in the member's language, for the mode
     * set on its dashboard. `null` when the community has set none: every consent wording is the
     * SDK's. They only apply when their case matches [termsAcceptanceMode], and a `null` field keeps
     * the SDK wording of that field.
     */
    val consentTexts: ConsentTexts? = null,
    /**
     * The community languages whose SDK texts address members informally ("tu" rather than "vous"),
     * as BCP 47 tags (`it`, `fr`, `zh-TW`…). Empty, the default, means the formal tone in every
     * language, i.e. today's texts. Only languages with a polite form are ever listed; see
     * [isInformalTone] for how a member's language is matched against it.
     *
     * No default here on purpose: a call that leaves it out resolves to the constructor without it,
     * which keeps the 1.15 signature callable from Java source and sets it to an empty list.
     */
    val informalLanguages: List<String>,
) {
    /**
     * Whether the SDK texts shown in [languageTag] (a BCP 47 tag such as `it-IT`, the language the
     * texts are displayed in) use the informal tone.
     *
     * Only the language is compared, case-insensitively: `it` and `it-CH` match `it-IT`, and
     * `pt-BR` matches `pt-PT`. Chinese also compares the script: an explicit `Hans` / `Hant` wins,
     * otherwise `TW`, `HK` and `MO` mean traditional and any other region simplified; a `zh` entry
     * or [languageTag] that tells neither covers both, as on iOS. The legacy Indonesian code `in`
     * and `id` are the same language. A blank [languageTag] never matches.
     */
    fun isInformalTone(languageTag: String): Boolean = informalLanguages.matchesLanguage(languageTag)

    /**
     * Every setting but [informalLanguages], left empty: the formal tone in every language. This is
     * the 1.15 constructor, kept visible so code built or written against it, Java included, keeps
     * working.
     */
    constructor(
        forceLoginOnStrongActions: Boolean,
        gamification: Gamification?,
        displayAccountAge: Boolean,
        poweredByOctopus: PoweredByOctopus = PoweredByOctopus.Default,
        profileFieldsLock: ProfileFieldsLock = ProfileFieldsLock.AllEditable,
        contentOptions: ContentOptions = ContentOptions(),
        exposeClientUserId: Boolean = false,
        termsAcceptanceMode: TermsAcceptanceMode = TermsAcceptanceMode.IMPLICIT,
        showCommentsOnOtherProfiles: Boolean = false,
        forceNicknameChoice: Boolean = false,
        consentTexts: ConsentTexts? = null,
    ) : this(
        forceLoginOnStrongActions = forceLoginOnStrongActions,
        gamification = gamification,
        displayAccountAge = displayAccountAge,
        poweredByOctopus = poweredByOctopus,
        profileFieldsLock = profileFieldsLock,
        contentOptions = contentOptions,
        exposeClientUserId = exposeClientUserId,
        termsAcceptanceMode = termsAcceptanceMode,
        showCommentsOnOtherProfiles = showCommentsOnOtherProfiles,
        forceNicknameChoice = forceNicknameChoice,
        consentTexts = consentTexts,
        informalLanguages = emptyList(),
    )

    /** The `copy` before [informalLanguages], kept so binaries built against 1.15 still link. */
    @Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
    fun copy(
        forceLoginOnStrongActions: Boolean = this.forceLoginOnStrongActions,
        gamification: Gamification? = this.gamification,
        displayAccountAge: Boolean = this.displayAccountAge,
        poweredByOctopus: PoweredByOctopus = this.poweredByOctopus,
        profileFieldsLock: ProfileFieldsLock = this.profileFieldsLock,
        contentOptions: ContentOptions = this.contentOptions,
        exposeClientUserId: Boolean = this.exposeClientUserId,
        termsAcceptanceMode: TermsAcceptanceMode = this.termsAcceptanceMode,
        showCommentsOnOtherProfiles: Boolean = this.showCommentsOnOtherProfiles,
        forceNicknameChoice: Boolean = this.forceNicknameChoice,
        consentTexts: ConsentTexts? = this.consentTexts,
    ): CommunityConfig =
        copy(
            forceLoginOnStrongActions = forceLoginOnStrongActions,
            gamification = gamification,
            displayAccountAge = displayAccountAge,
            poweredByOctopus = poweredByOctopus,
            profileFieldsLock = profileFieldsLock,
            contentOptions = contentOptions,
            exposeClientUserId = exposeClientUserId,
            termsAcceptanceMode = termsAcceptanceMode,
            showCommentsOnOtherProfiles = showCommentsOnOtherProfiles,
            forceNicknameChoice = forceNicknameChoice,
            consentTexts = consentTexts,
            informalLanguages = informalLanguages,
        )

    /**
     * How the community requires users to accept its legal documents (terms, privacy policy,
     * community rules) at their first contribution.
     *
     * - [IMPLICIT]: today's default — a legal disclaimer is shown at the bottom of the editor and
     *   acceptance is implicit when the user publishes. Unchanged for every existing community.
     * - [EXPLICIT_MULTI_CHECKBOX]: a bottom sheet with **one checkbox per legal document** (all
     *   required) is shown at the first contribution; publishing is gated until every box is checked.
     * - [EXPLICIT_SINGLE_CHECKBOX]: same bottom sheet with **a single combined checkbox** (plus a
     *   passive privacy-policy acknowledgement); publishing is gated until that box is checked.
     *
     * The two explicit modes share the same behaviour (trigger, memorization, dismissal) and differ
     * only in the sheet's content (number of checkboxes + wordings).
     */
    enum class TermsAcceptanceMode {
        IMPLICIT,
        EXPLICIT_MULTI_CHECKBOX,
        EXPLICIT_SINGLE_CHECKBOX,
        ;

        /** Whether this mode presents the explicit-consent bottom sheet (i.e. anything but [IMPLICIT]). */
        val isExplicit: Boolean
            get() = this != IMPLICIT
    }

    /**
     * Consent wordings written by the community, in markdown (links, bold, italic, line breaks).
     * One case per [TermsAcceptanceMode]; a `null` field means "the SDK wording" for that field.
     */
    sealed interface ConsentTexts {
        /**
         * Wordings of [TermsAcceptanceMode.IMPLICIT].
         *
         * @property notice the notice under the post, comment and reply editors.
         */
        data class Implicit(
            val notice: String? = null,
        ) : ConsentTexts

        /**
         * Wordings of [TermsAcceptanceMode.EXPLICIT_MULTI_CHECKBOX].
         *
         * @property termsLabel label of the terms checkbox.
         * @property privacyLabel label of the privacy policy checkbox.
         * @property rulesLabel label of the community guidelines checkbox.
         */
        data class MultiCheckbox(
            val termsLabel: String? = null,
            val privacyLabel: String? = null,
            val rulesLabel: String? = null,
        ) : ConsentTexts

        /**
         * Wordings of [TermsAcceptanceMode.EXPLICIT_SINGLE_CHECKBOX].
         *
         * @property label label of the single checkbox.
         * @property belowText text under the checkbox, the same for posts, comments and replies.
         */
        data class SingleCheckbox(
            val label: String? = null,
            val belowText: String? = null,
        ) : ConsentTexts
    }

    sealed class PoweredByOctopus {
        /** Show the default "Powered by Octopus" badge */
        data object Default : PoweredByOctopus()

        /** Replace the badge with a custom image provided by Octopus */
        data class Custom(
            val lightLogoUrl: String,
            val darkLogoUrl: String,
        ) : PoweredByOctopus()

        /** Do not display the badge */
        data object Hidden : PoweredByOctopus()
    }

    /**
     * Per-content-type creation and post navigation options.
     *
     * Creation flags default to `true` (pictures and polls enabled, text required). Post tap
     * options default to `false`, preserving existing navigation for unconfigured communities.
     */
    data class ContentOptions(
        val post: PostOptions = PostOptions(),
        val comment: CommentOptions = CommentOptions(),
        val reply: ReplyOptions = ReplyOptions(),
    ) {
        /**
         * Post creation and tap behaviour.
         *
         * @property enablePictures Whether the composer offers to attach a picture.
         * @property enablePolls Whether the composer offers to add a poll.
         * @property textRequired Whether a post must carry text. When `false`, a post that carries an
         *   image, with or without a poll, may be published without text (blank text counts as none
         *   and is not sent). A post without an image still needs text, and as soon as there is text
         *   the usual length rules apply. Defaults to `true`, which keeps text required.
         * @property imageTapOpensPost Whether tapping a feed image invokes the post card's action
         *   instead of opening the fullscreen photo, when the card can open post detail. Otherwise,
         *   and in post detail, images still open fullscreen.
         *   Defaults to `false` in the existing constructors.
         * @property groupNameTapOpensGroup Whether tapping a post's group name opens that group.
         *   Defaults to `false` in the existing constructors.
         *
         * Not a `data class` on purpose: it started with two flags, and the two-flag constructor and
         * `copy` must keep compiling and linking for existing callers, Java included. A data class
         * can only offer them as hidden (synthetic) members, which Java source cannot call, or as
         * visible ones with defaults, which make Kotlin calls such as `PostOptions()` ambiguous.
         * Leaving [textRequired] without a default in the three-flag constructor and [copy] keeps
         * each call resolving to exactly one overload.
         *
         * The tap flags are supplied together so the existing constructors and `copy` overloads
         * keep their Kotlin, Java and JVM signatures without ambiguous default arguments.
         */
        class PostOptions(
            val enablePictures: Boolean = true,
            val enablePolls: Boolean = true,
            val textRequired: Boolean = true,
            val imageTapOpensPost: Boolean,
            val groupNameTapOpensGroup: Boolean,
        ) {
            /** Creation enabled, text required, and the original tap behaviour. */
            constructor() : this(enablePictures = true, enablePolls = true, textRequired = true)

            /** The two original flags, with text required and the original tap behaviour. */
            constructor(
                enablePictures: Boolean = true,
                enablePolls: Boolean = true,
            ) : this(enablePictures, enablePolls, textRequired = true)

            /** Creation flags with the original tap behaviour. */
            constructor(
                enablePictures: Boolean = true,
                enablePolls: Boolean = true,
                textRequired: Boolean,
            ) : this(enablePictures, enablePolls, textRequired, imageTapOpensPost = false, groupNameTapOpensGroup = false)

            /** A copy with the given picture and poll flags, keeping text and tap options. */
            fun copy(
                enablePictures: Boolean = this.enablePictures,
                enablePolls: Boolean = this.enablePolls,
            ): PostOptions = PostOptions(enablePictures, enablePolls, textRequired, imageTapOpensPost, groupNameTapOpensGroup)

            /** A copy with the given creation flags, keeping tap options. */
            fun copy(
                enablePictures: Boolean = this.enablePictures,
                enablePolls: Boolean = this.enablePolls,
                textRequired: Boolean,
            ): PostOptions = PostOptions(enablePictures, enablePolls, textRequired, imageTapOpensPost, groupNameTapOpensGroup)

            /** A copy with both tap flags, keeping unspecified creation flags. */
            fun copy(
                enablePictures: Boolean = this.enablePictures,
                enablePolls: Boolean = this.enablePolls,
                textRequired: Boolean = this.textRequired,
                imageTapOpensPost: Boolean,
                groupNameTapOpensGroup: Boolean,
            ): PostOptions = PostOptions(enablePictures, enablePolls, textRequired, imageTapOpensPost, groupNameTapOpensGroup)

            /** [enablePictures], for destructuring. */
            operator fun component1(): Boolean = enablePictures

            /** [enablePolls], for destructuring. */
            operator fun component2(): Boolean = enablePolls

            /** [textRequired], for destructuring. */
            operator fun component3(): Boolean = textRequired

            override fun equals(other: Any?): Boolean =
                other is PostOptions &&
                    other.enablePictures == enablePictures &&
                    other.enablePolls == enablePolls &&
                    other.textRequired == textRequired &&
                    other.imageTapOpensPost == imageTapOpensPost &&
                    other.groupNameTapOpensGroup == groupNameTapOpensGroup

            override fun hashCode(): Int {
                var result = enablePictures.hashCode()
                result = result * 31 + enablePolls.hashCode()
                result = result * 31 + textRequired.hashCode()
                result = result * 31 + imageTapOpensPost.hashCode()
                return result * 31 + groupNameTapOpensGroup.hashCode()
            }

            override fun toString(): String =
                "PostOptions(enablePictures=$enablePictures, enablePolls=$enablePolls, textRequired=$textRequired, " +
                    "imageTapOpensPost=$imageTapOpensPost, groupNameTapOpensGroup=$groupNameTapOpensGroup)"
        }

        data class CommentOptions(
            val enablePictures: Boolean = true,
        )

        data class ReplyOptions(
            val enablePictures: Boolean = true,
        )
    }

    class Gamification(
        val levels: Map<Int, Level>,
        val pointsName: Points.Name,
        val actionsPoints: Map<Action, Int>,
    ) {
        data class Level(
            val index: Int,
            val name: String?,
            val nextLevelPoints: Int? = null,
            val colors: LevelColors?,
        ) {
            data class LevelColors(
                val light: LevelColor,
                val dark: LevelColor,
            ) {
                constructor(color: LevelColor) : this(color, color)

                data class LevelColor(
                    val background: String,
                    val text: String,
                )
            }
        }

        data class Points(
            val value: Int,
            val name: Name,
        ) {
            data class Name(
                val full: String,
                val shortSingular: String,
                val shortPlural: String,
            )

            val shortText
                get() = "${value.numberText} ${
                    when (value) {
                        1 -> name.shortSingular
                        else -> name.shortPlural
                    }
                }"
        }

        val MinimalProfile.gamificationLevel get() = gamificationLevelIndex?.let { levels[it] }

        val Profile.gamificationScoreProgress
            get() =
                gamificationScore?.let { score ->
                    gamificationLevel?.let { level ->
                        level.nextLevelPoints?.let { nextLevelPoints ->
                            val levelStartPoints = levels[level.index - 1]?.nextLevelPoints ?: 0
                            (score - levelStartPoints).toFloat() / (nextLevelPoints - levelStartPoints).toFloat()
                        }
                    }
                }

        val Profile.gamificationScoreProgressText
            get() =
                gamificationScore?.let { score ->
                    "${score.numberText} ${
                        gamificationLevel?.nextLevelPoints?.let { "/ ${it.numberText} " } ?: " "
                    }${pointsName.full}"
                }

        fun getToast(action: Action) =
            getPoints(action)
                ?.takeIf {
                    it.value > 0
                }?.let { Toast(action = action, points = it) }

        fun getPoints(action: Action) =
            actionsPoints[action]?.let {
                Points(value = it, name = pointsName)
            }

        companion object {
            internal val Int.numberText: String
                get() = NumberFormat.getNumberInstance().format(this)
        }
    }
}

/** See [CommunityConfig.isInformalTone]. */
internal fun List<String>.matchesLanguage(languageTag: String): Boolean {
    if (isEmpty()) return false
    val member = languageTag.toToneLanguage() ?: return false
    return any { entry -> entry.toToneLanguage()?.covers(member) == true }
}

/**
 * A language as the tone setting sees it: the language code, plus, for Chinese only, the script
 * (`hans` / `hant`) since simplified and traditional Chinese carry different texts. Any other region
 * or script is ignored. A `null` [chineseScript], on either side, covers both scripts (same rule as iOS).
 */
private data class ToneLanguage(
    val language: String,
    val chineseScript: String?,
) {
    fun covers(member: ToneLanguage): Boolean =
        language == member.language &&
            (chineseScript == null || member.chineseScript == null || chineseScript == member.chineseScript)
}

/**
 * Parses a BCP 47 tag, lower-cased, with `_` accepted as a separator and the legacy `in` / `iw` /
 * `ji` codes Android still reports mapped to their current form. For Chinese, an explicit script
 * wins; otherwise TW, HK and MO mean traditional and any other region simplified.
 */
private fun String.toToneLanguage(): ToneLanguage? {
    val parts =
        trim()
            .lowercase()
            .replace('_', '-')
            .split('-')
            .filter { it.isNotEmpty() }
    val language =
        when (val code = parts.firstOrNull() ?: return null) {
            "in" -> "id"
            "iw" -> "he"
            "ji" -> "yi"
            else -> code
        }
    if (language != "zh") return ToneLanguage(language, null)
    val subtags = parts.drop(1)
    val script =
        subtags.firstOrNull { it == "hans" || it == "hant" }
            ?: subtags.firstOrNull { it.length == 2 }?.let {
                if (it in TRADITIONAL_CHINESE_REGIONS) "hant" else "hans"
            }
    return ToneLanguage(language, script)
}

private val TRADITIONAL_CHINESE_REGIONS = setOf("tw", "hk", "mo")
