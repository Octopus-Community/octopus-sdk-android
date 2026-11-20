package com.octopuscommunity.sdk.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.octopuscommunity.sdk.domain.model.CommunityConfig
import com.octopuscommunity.sdk.domain.model.Gamification
import com.octopuscommunity.sdk.domain.model.ProfileFieldLockState
import kotlinx.serialization.Serializable

@Entity(tableName = "community_configs")
data class CommunityConfigEntity(
    @PrimaryKey val id: Int = 0,
    val forceLoginOnStrongActions: Boolean,
    @Embedded(prefix = "gamification") val gamification: GamificationEntity?,
    val displayAccountAge: Boolean,
    val exposeClientUserId: Boolean = false,
    // Room persists the enum as its constant name; new/migrated rows default to IMPLICIT,
    // preserving today's behaviour (implicit acceptance, no consent sheet).
    val termsAcceptanceMode: CommunityConfig.TermsAcceptanceMode = CommunityConfig.TermsAcceptanceMode.IMPLICIT,
    // ApiKeyConfig.showCommentsOnOtherProfiles (field 10); new/migrated rows default to false,
    // preserving today's behaviour (comments tab only on the connected user's own profile).
    val showCommentsOnOtherProfiles: Boolean = false,
    // ApiKeyConfig.forceNicknameChoice (field 11); new/migrated rows default to false, preserving
    // today's behaviour (the nickname choice screen is prefilled with the generated nickname).
    val forceNicknameChoice: Boolean = false,
    @Embedded(prefix = "poweredBy") val poweredByOctopus: PoweredByOctopusEntity? = null,
    @Embedded(prefix = "profileLock") val profileFieldsLock: ProfileFieldsLockEntity =
        ProfileFieldsLockEntity(),
    @Embedded(prefix = "contentOptions") val contentOptions: ContentOptionsEntity = ContentOptionsEntity(),
    // ApiKeyConfig.consentTexts (field 12). Null when the community has set none, and for rows
    // migrated from an older version (all seven columns NULL), so the SDK wordings apply.
    @Embedded(prefix = "consentTexts") val consentTexts: ConsentTextsEntity? = null,
    // ApiKeyConfig.informalLanguages (field 15), stored as a JSON array. New/migrated rows default to
    // `[]`: formal tone in every language, i.e. today's texts.
    val informalLanguages: List<String> = emptyList(),
) {
    /**
     * The community's own consent wordings. [mode] records which proto `oneof` case was served; only
     * that mode's columns are filled, the others stay null. Blank wordings are already stored as null
     * by the mapper.
     */
    data class ConsentTextsEntity(
        val mode: Mode,
        val notice: String? = null,
        val termsLabel: String? = null,
        val privacyLabel: String? = null,
        val rulesLabel: String? = null,
        val singleLabel: String? = null,
        val belowText: String? = null,
    ) {
        /**
         * Room persists this enum as its constant name: never rename or remove a constant, a stored
         * name that no longer exists makes Room throw when the row is read.
         */
        enum class Mode { IMPLICIT, MULTI_CHECKBOX, SINGLE_CHECKBOX }
    }

    /**
     * Per-field profile lock columns. Room persists the enums as their constant name; new/migrated
     * rows default to [ProfileFieldLockState.EDITABLE], preserving today's behaviour.
     */
    data class ProfileFieldsLockEntity(
        val nickname: ProfileFieldLockState = ProfileFieldLockState.EDITABLE,
        val avatar: ProfileFieldLockState = ProfileFieldLockState.EDITABLE,
        val bio: ProfileFieldLockState = ProfileFieldLockState.EDITABLE,
    )

    /**
     * Resolved content creation and navigation flags. Creation defaults to enabled; the
     * optional tap behaviours default to disabled, including for previously cached rows.
     */
    data class ContentOptionsEntity(
        val postEnablePictures: Boolean = true,
        val postEnablePolls: Boolean = true,
        val commentEnablePictures: Boolean = true,
        val replyEnablePictures: Boolean = true,
        // `ContentOptions.post.textRequired`; rows cached before the column existed migrate to 1
        // (text required), today's behaviour.
        val postTextRequired: Boolean = true,
        @ColumnInfo(defaultValue = "0") val postImageTapOpensPost: Boolean = false,
        @ColumnInfo(defaultValue = "0") val postGroupNameTapOpensGroup: Boolean = false,
    )

    data class PoweredByOctopusEntity(
        val type: Type,
        val lightLogoUrl: String? = null,
        val darkLogoUrl: String? = null,
    ) {
        enum class Type { DEFAULT, CUSTOM, HIDDEN }
    }

    @Serializable
    data class GamificationEntity(
        val pointsName: String,
        val shortenPointSingular: String,
        val shortenPointPlural: String,
        val actionsPoints: Map<Gamification.Action, Int>,
        val levels: List<LevelEntity>,
    ) {
        @Serializable
        data class LevelEntity(
            val index: Int,
            val name: String?,
            val nextLevelPoints: Int?,
            val colors: LevelColorsEntity?,
        ) {
            @Serializable
            data class LevelColorsEntity(
                val light: LevelColorEntity,
                val dark: LevelColorEntity,
            ) {
                @Serializable
                data class LevelColorEntity(
                    val background: String,
                    val text: String,
                )
            }
        }
    }
}
