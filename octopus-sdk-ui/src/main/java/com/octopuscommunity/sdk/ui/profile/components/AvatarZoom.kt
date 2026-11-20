package com.octopuscommunity.sdk.ui.profile.components

import com.octopuscommunity.sdk.domain.model.CurrentUserProfile
import com.octopuscommunity.sdk.domain.model.DefaultAvatar
import com.octopuscommunity.sdk.domain.model.MinimalProfile
import com.octopuscommunity.sdk.domain.model.Profile
import com.octopuscommunity.sdk.domain.model.Resource

/** The image zooming this member's avatar shows: their picture, else their community's default image. */
internal val MinimalProfile.zoomableAvatar: Resource?
    get() = picture ?: (defaultAvatar as? DefaultAvatar.Image)?.image

/**
 * Whether tapping this profile's header avatar zooms it. A picture always does; a default image only on another
 * member's profile — on their own, members get the "add a picture" button instead.
 */
internal val Profile.isAvatarZoomable: Boolean
    get() = picture != null || (this !is CurrentUserProfile && zoomableAvatar != null)
