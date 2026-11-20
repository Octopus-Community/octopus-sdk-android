package com.octopuscommunity.sdk.ui.profile.components

import com.octopuscommunity.sdk.domain.model.CurrentUserProfile

/**
 * This profile as its avatar is previewed while the member edits it: their default avatar, else the one they
 * would get if they removed their picture. An avatar draws the picture before any default, so the fallback
 * only shows once the picture is removed, before it is saved. For display only: edit and save the profile
 * itself, which this leaves untouched.
 */
internal fun CurrentUserProfile.withEditPreviewDefaultAvatar(): CurrentUserProfile =
    copy(defaultAvatar = defaultAvatar ?: defaultAvatarIfPictureRemoved)
