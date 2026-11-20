package com.octopuscommunity.sdk.ui.profile.current

import androidx.compose.runtime.Immutable
import com.octopuscommunity.sdk.domain.model.ProfileFieldLockState
import com.octopuscommunity.sdk.domain.model.ProfileFieldsLock

/**
 * Derived, per-field editability for the current-user profile UI, computed from the community
 * per-field lock. Drives which affordances the profile / edit screens display.
 *
 * This is the community-config lock only; the SSO `appManagedFields` redirect is handled separately
 * (and wins per field). With [ProfileFieldsLock.AllEditable] every flag is `true`, so the UI is
 * strictly identical to today for every community that sets no lock.
 */
@Immutable
data class ProfileFieldsEditability(
    val nicknameEditable: Boolean,
    val avatarEditable: Boolean,
    val bioEditable: Boolean,
    /** Bio is DISABLED: no bio section at all (no display even of an existing value, no "Add a bio"). */
    val bioHidden: Boolean,
) {
    /** The "Edit profile" button is shown as soon as at least one of the three fields is editable. */
    val showEditButton: Boolean get() = nicknameEditable || avatarEditable || bioEditable

    companion object {
        fun from(lock: ProfileFieldsLock) =
            ProfileFieldsEditability(
                nicknameEditable = lock.nickname == ProfileFieldLockState.EDITABLE,
                avatarEditable = lock.avatar == ProfileFieldLockState.EDITABLE,
                bioEditable = lock.bio == ProfileFieldLockState.EDITABLE,
                bioHidden = lock.bio == ProfileFieldLockState.DISABLED,
            )

        val AllEditable = from(ProfileFieldsLock.AllEditable)
    }
}
