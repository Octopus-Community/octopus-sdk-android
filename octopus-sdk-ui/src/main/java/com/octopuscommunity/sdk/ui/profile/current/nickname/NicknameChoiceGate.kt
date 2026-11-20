package com.octopuscommunity.sdk.ui.profile.current.nickname

import com.octopuscommunity.sdk.domain.model.CommunityConfig
import com.octopuscommunity.sdk.domain.model.CurrentUserProfile
import com.octopuscommunity.sdk.domain.model.ProfileField
import com.octopuscommunity.sdk.domain.model.ProfileFieldLockState
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusViewModel.UserAction

/**
 * Whether [action] must go through the nickname choice first: a post, comment or reply of a member
 * whose nickname is not confirmed yet. Neither a nickname the community locks (read-only / disabled)
 * nor a nickname the host app manages ([appManagedFields] of the SSO connection mode) ever asks: the
 * member could not change the first, and the host owns the second. The SSO sync does not always
 * confirm an app-managed nickname (only when the host's nickname differs from the stored one), so the
 * gate cannot rely on [CurrentUserProfile.hasConfirmedNickname] for it. As everywhere else in the SDK,
 * app-managed fields do not apply to a guest.
 */
internal fun shouldPresentNicknameChoice(
    action: UserAction,
    profile: CurrentUserProfile,
    config: CommunityConfig?,
    appManagedFields: Set<ProfileField>,
): Boolean =
    action.needNicknameValidation &&
        !profile.hasConfirmedNickname &&
        (profile.isGuest || ProfileField.NICKNAME !in appManagedFields) &&
        (config?.profileFieldsLock?.nickname ?: ProfileFieldLockState.EDITABLE) == ProfileFieldLockState.EDITABLE

/**
 * A contribution held back by the nickname choice, for the member [profileId]. It goes once that
 * member's nickname is confirmed; it is dropped as soon as another member, or none, is connected, so
 * it never goes out under someone else.
 */
internal data class NicknameChoiceHold(
    val profileId: String,
) {
    /** What to do with the held contribution for the currently connected profile. */
    enum class Resolution { KEEP_WAITING, SEND, DROP }

    /** The [Resolution] of this hold when [profile] is the connected member (`null`: none). */
    fun resolution(profile: CurrentUserProfile?): Resolution =
        when {
            profile == null || profile.userId != profileId -> Resolution.DROP
            profile.hasConfirmedNickname -> Resolution.SEND
            else -> Resolution.KEEP_WAITING
        }
}
