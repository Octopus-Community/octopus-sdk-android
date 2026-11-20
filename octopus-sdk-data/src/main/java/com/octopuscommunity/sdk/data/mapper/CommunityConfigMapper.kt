package com.octopuscommunity.sdk.data.mapper

import com.octopuscommunity.ApiKeyServiceProto
import com.octopuscommunity.ApiKeyServiceProto.GamificationConfig
import com.octopuscommunity.ApiKeyServiceProto.PoweredByOctopus.LogoCase
import com.octopuscommunity.consentTextsOrNull
import com.octopuscommunity.contentOptionsOrNull
import com.octopuscommunity.displayConfigOrNull
import com.octopuscommunity.gamificationConfigOrNull
import com.octopuscommunity.poweredByOctopusOrNull
import com.octopuscommunity.sdk.data.local.entity.CommunityConfigEntity
import com.octopuscommunity.sdk.data.local.entity.CommunityConfigEntity.ConsentTextsEntity
import com.octopuscommunity.sdk.data.local.entity.CommunityConfigEntity.GamificationEntity
import com.octopuscommunity.sdk.data.local.entity.CommunityConfigEntity.GamificationEntity.LevelEntity
import com.octopuscommunity.sdk.data.local.entity.CommunityConfigEntity.PoweredByOctopusEntity
import com.octopuscommunity.sdk.data.local.entity.CommunityConfigEntity.ProfileFieldsLockEntity
import com.octopuscommunity.sdk.domain.model.CommunityConfig
import com.octopuscommunity.sdk.domain.model.CommunityConfig.Gamification.Level
import com.octopuscommunity.sdk.domain.model.CommunityConfig.Gamification.Points
import com.octopuscommunity.sdk.domain.model.CommunityConfig.PoweredByOctopus
import com.octopuscommunity.sdk.domain.model.Gamification
import com.octopuscommunity.sdk.domain.model.ProfileFieldLockState
import com.octopuscommunity.sdk.domain.model.ProfileFieldsLock

class CommunityConfigMapper {
    fun CommunityConfigEntity.toDomain() =
        CommunityConfig(
            forceLoginOnStrongActions = forceLoginOnStrongActions,
            gamification = gamification?.toDomain(),
            displayAccountAge = displayAccountAge,
            poweredByOctopus = poweredByOctopus?.toDomain() ?: PoweredByOctopus.Default,
            profileFieldsLock = profileFieldsLock.toDomain(),
            contentOptions = contentOptions.toDomain(),
            exposeClientUserId = exposeClientUserId,
            termsAcceptanceMode = termsAcceptanceMode,
            showCommentsOnOtherProfiles = showCommentsOnOtherProfiles,
            forceNicknameChoice = forceNicknameChoice,
            consentTexts = consentTexts?.toDomain(),
            informalLanguages = informalLanguages,
        )

    fun ConsentTextsEntity.toDomain(): CommunityConfig.ConsentTexts =
        when (mode) {
            ConsentTextsEntity.Mode.IMPLICIT -> CommunityConfig.ConsentTexts.Implicit(notice = notice)
            ConsentTextsEntity.Mode.MULTI_CHECKBOX ->
                CommunityConfig.ConsentTexts.MultiCheckbox(
                    termsLabel = termsLabel,
                    privacyLabel = privacyLabel,
                    rulesLabel = rulesLabel,
                )
            ConsentTextsEntity.Mode.SINGLE_CHECKBOX ->
                CommunityConfig.ConsentTexts.SingleCheckbox(label = singleLabel, belowText = belowText)
        }

    fun ProfileFieldsLockEntity.toDomain() =
        ProfileFieldsLock(
            nickname = nickname,
            avatar = avatar,
            bio = bio,
        )

    fun CommunityConfigEntity.ContentOptionsEntity.toDomain() =
        CommunityConfig.ContentOptions(
            post =
                CommunityConfig.ContentOptions.PostOptions(
                    enablePictures = postEnablePictures,
                    enablePolls = postEnablePolls,
                    textRequired = postTextRequired,
                    imageTapOpensPost = postImageTapOpensPost,
                    groupNameTapOpensGroup = postGroupNameTapOpensGroup,
                ),
            comment =
                CommunityConfig.ContentOptions.CommentOptions(
                    enablePictures = commentEnablePictures,
                ),
            reply =
                CommunityConfig.ContentOptions.ReplyOptions(
                    enablePictures = replyEnablePictures,
                ),
        )

    fun PoweredByOctopusEntity.toDomain() =
        when (type) {
            PoweredByOctopusEntity.Type.CUSTOM ->
                PoweredByOctopus.Custom(
                    lightLogoUrl = lightLogoUrl ?: "",
                    darkLogoUrl = darkLogoUrl ?: "",
                )

            PoweredByOctopusEntity.Type.HIDDEN -> PoweredByOctopus.Hidden
            PoweredByOctopusEntity.Type.DEFAULT -> PoweredByOctopus.Default
        }

    fun GamificationEntity.toDomain() =
        CommunityConfig.Gamification(
            levels = levels.map { it.toDomain() }.associateBy { it.index },
            pointsName =
                Points.Name(
                    full = pointsName,
                    shortSingular = shortenPointSingular,
                    shortPlural = shortenPointPlural,
                ),
            actionsPoints = actionsPoints,
        )

    fun LevelEntity.toDomain() =
        Level(
            index = index,
            name = name,
            nextLevelPoints = nextLevelPoints,
            colors =
                colors?.let {
                    Level.LevelColors(
                        light =
                            Level.LevelColors.LevelColor(
                                background = it.light.background,
                                text = it.light.text,
                            ),
                        dark =
                            Level.LevelColors.LevelColor(
                                background = it.dark.background,
                                text = it.dark.text,
                            ),
                    )
                },
        )

    fun ApiKeyServiceProto.ApiKeyConfig.toEntity() =
        CommunityConfigEntity(
            forceLoginOnStrongActions = clientLoginMandatory,
            gamification = gamificationConfigOrNull?.toEntity(),
            displayAccountAge = displayAccountAge,
            poweredByOctopus = displayConfigOrNull?.poweredByOctopusOrNull?.toEntity(),
            // Absent profileFieldsLock (or all EDITABLE) => default no-op.
            profileFieldsLock =
                if (hasProfileFieldsLock()) {
                    profileFieldsLock.toEntity()
                } else {
                    ProfileFieldsLockEntity()
                },
            contentOptions = contentOptionsOrNull.toEntity(),
            // ApiKeyConfig.exposeClientUserId = 8 (false when unset) (gRPC contract)
            exposeClientUserId = exposeClientUserId,
            // ApiKeyConfig.termsAcceptanceMode = 9 (IMPLICIT when unset) (gRPC contract)
            termsAcceptanceMode = termsAcceptanceMode.toDomain(),
            // ApiKeyConfig.showCommentsOnOtherProfiles = 10 (false when unset) (internal ticket)
            showCommentsOnOtherProfiles = showCommentsOnOtherProfiles,
            // ApiKeyConfig.forceNicknameChoice = 11 (false when unset)
            forceNicknameChoice = forceNicknameChoice,
            // ApiKeyConfig.consentTexts = 12 (absent when the community has set none)
            consentTexts = consentTextsOrNull.toEntity(),
            // ApiKeyConfig.informalLanguages = 15 (empty when unset: formal everywhere)
            // (gRPC contract, internal ticket). Blank entries are dropped.
            informalLanguages = informalLanguagesList.map { it.trim() }.filter { it.isNotEmpty() },
        )

    /** proto [TermsAcceptanceMode]. Unknown/unset values default to IMPLICIT (safe no-op). */
    private fun ApiKeyServiceProto.TermsAcceptanceMode.toDomain() =
        when (this) {
            ApiKeyServiceProto.TermsAcceptanceMode.TERMS_ACCEPTANCE_MODE_EXPLICIT_MULTI_CHECKBOX ->
                CommunityConfig.TermsAcceptanceMode.EXPLICIT_MULTI_CHECKBOX
            ApiKeyServiceProto.TermsAcceptanceMode.TERMS_ACCEPTANCE_MODE_EXPLICIT_SINGLE_CHECKBOX ->
                CommunityConfig.TermsAcceptanceMode.EXPLICIT_SINGLE_CHECKBOX
            else -> CommunityConfig.TermsAcceptanceMode.IMPLICIT
        }

    fun ApiKeyServiceProto.ProfileFieldsLock.toEntity() =
        ProfileFieldsLockEntity(
            nickname = nickname.toLockState(),
            avatar = avatar.toLockState(),
            bio = bio.toLockState(),
        )

    /** nickname & avatar — proto [FieldLock]. Unknown values default to EDITABLE (safe no-op). */
    private fun ApiKeyServiceProto.FieldLock.toLockState() =
        when (this) {
            ApiKeyServiceProto.FieldLock.FIELD_LOCK_READ_ONLY -> ProfileFieldLockState.READ_ONLY
            else -> ProfileFieldLockState.EDITABLE
        }

    /** bio — proto [BioFieldLock], which additionally carries HIDDEN (mapped to DISABLED). */
    private fun ApiKeyServiceProto.BioFieldLock.toLockState() =
        when (this) {
            ApiKeyServiceProto.BioFieldLock.BIO_FIELD_LOCK_READ_ONLY -> ProfileFieldLockState.READ_ONLY
            ApiKeyServiceProto.BioFieldLock.BIO_FIELD_LOCK_HIDDEN -> ProfileFieldLockState.DISABLED
            else -> ProfileFieldLockState.EDITABLE
        }

    // Absent creation flags default to true; absent tap options default to false, preserving
    // existing behaviour when contentOptions, the per-type config, or the flag is missing.
    fun ApiKeyServiceProto.ContentOptions?.toEntity(): CommunityConfigEntity.ContentOptionsEntity {
        if (this == null) return CommunityConfigEntity.ContentOptionsEntity()
        return CommunityConfigEntity.ContentOptionsEntity(
            postEnablePictures = if (hasPost() && post.hasEnablePictures()) post.enablePictures else true,
            postEnablePolls = if (hasPost() && post.hasEnablePolls()) post.enablePolls else true,
            postTextRequired = if (hasPost() && post.hasTextRequired()) post.textRequired else true,
            postImageTapOpensPost = hasPost() && post.imageTapOpensPost,
            postGroupNameTapOpensGroup = hasPost() && post.groupNameTapOpensGroup,
            commentEnablePictures = if (hasComment() && comment.hasEnablePictures()) comment.enablePictures else true,
            replyEnablePictures = if (hasReply() && reply.hasEnablePictures()) reply.enablePictures else true,
        )
    }

    // An absent message or an unset `oneof` => no texts (the SDK wordings). A blank wording => null
    // for that field alone, so it falls back on its own SDK wording; non-blank wordings are kept as
    // written. A field this SDK does not mirror is skipped by the parser and never reaches here.
    fun ApiKeyServiceProto.ConsentTexts?.toEntity(): ConsentTextsEntity? {
        if (this == null) return null
        return when (modeCase) {
            ApiKeyServiceProto.ConsentTexts.ModeCase.IMPLICIT ->
                ConsentTextsEntity(
                    mode = ConsentTextsEntity.Mode.IMPLICIT,
                    notice = implicit.notice.nullIfBlank(),
                )
            ApiKeyServiceProto.ConsentTexts.ModeCase.MULTICHECKBOX ->
                ConsentTextsEntity(
                    mode = ConsentTextsEntity.Mode.MULTI_CHECKBOX,
                    termsLabel = multiCheckbox.termsLabel.nullIfBlank(),
                    privacyLabel = multiCheckbox.privacyLabel.nullIfBlank(),
                    rulesLabel = multiCheckbox.rulesLabel.nullIfBlank(),
                )
            ApiKeyServiceProto.ConsentTexts.ModeCase.SINGLECHECKBOX ->
                ConsentTextsEntity(
                    mode = ConsentTextsEntity.Mode.SINGLE_CHECKBOX,
                    singleLabel = singleCheckbox.label.nullIfBlank(),
                    belowText = singleCheckbox.belowText.nullIfBlank(),
                )
            else -> null
        }
    }

    private fun String.nullIfBlank(): String? = takeUnless { it.isBlank() }

    fun ApiKeyServiceProto.PoweredByOctopus.toEntity() =
        when (logoCase) {
            LogoCase.CUSTOM ->
                PoweredByOctopusEntity(
                    type = PoweredByOctopusEntity.Type.CUSTOM,
                    lightLogoUrl = custom.lightLogoUrl,
                    darkLogoUrl = custom.darkLogoUrl,
                )

            LogoCase.HIDDEN -> PoweredByOctopusEntity(type = PoweredByOctopusEntity.Type.HIDDEN)
            else -> PoweredByOctopusEntity(type = PoweredByOctopusEntity.Type.DEFAULT)
        }

    fun GamificationConfig.toEntity() =
        GamificationEntity(
            levels = gamificationLevelsList.map { it.toEntity() },
            pointsName = pointsName,
            shortenPointSingular = shortenPointSingular,
            shortenPointPlural = shortenPointPlural,
            actionsPoints =
                mapOf(
                    Gamification.Action.REACTION to if (hasReactPts()) reactPts else 0,
                    Gamification.Action.VOTE to if (hasVotePts()) votePts else 0,
                    Gamification.Action.COMMENT to if (hasCommentPts()) commentPts else 0,
                    Gamification.Action.REPLY to if (hasReplyPts()) replyPts else 0,
                    Gamification.Action.POST to if (hasPostPts()) postPts else 0,
                    Gamification.Action.POST_COMMENTED to if (hasCommentOnYourPostPts()) commentOnYourPostPts else 0,
                    Gamification.Action.DAILY_SESSION to if (hasLoginPts()) loginPts else 0,
                    Gamification.Action.PROFILE_COMPLETED to if (hasCompleteProfilePts()) completeProfilePts else 0,
                ),
        )

    fun ApiKeyServiceProto.GamificationLevel.toEntity() =
        LevelEntity(
            index = level,
            name = name,
            nextLevelPoints = nextLevelAt.takeIf { hasNextLevelAt() },
            colors =
                if (hasBadgeLightColorHex()) {
                    LevelEntity.LevelColorsEntity(
                        light =
                            LevelEntity.LevelColorsEntity.LevelColorEntity(
                                background = badgeLightColorHex,
                                text = badgeTextLightColorHex,
                            ),
                        dark =
                            LevelEntity.LevelColorsEntity.LevelColorEntity(
                                background =
                                    if (hasBadgeDarkColorHex()) {
                                        badgeDarkColorHex
                                    } else {
                                        badgeLightColorHex
                                    },
                                text =
                                    if (hasBadgeTextDarkColorHex()) {
                                        badgeTextDarkColorHex
                                    } else {
                                        badgeTextLightColorHex
                                    },
                            ),
                    )
                } else {
                    null
                },
        )
}
