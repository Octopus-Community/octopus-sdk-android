package com.octopuscommunity.sdk.domain.model

/**
 * Per-field editability status of a profile field, driven by the community configuration.
 *
 * [DISABLED] is only meaningful for the bio (the field disappears entirely); [ProfileFieldsLock.nickname]
 * and [ProfileFieldsLock.avatar] can only be [EDITABLE] or [READ_ONLY].
 */
enum class ProfileFieldLockState {
    /** Default behaviour: the field is displayed and modifiable. */
    EDITABLE,

    /** The value is displayed but no longer modifiable (edit stays inside Octopus, no redirect). */
    READ_ONLY,

    /** The field disappears entirely (bio only): no display even of an existing value, no "Add a bio". */
    DISABLED,
}

/**
 * Per-field profile lock for the current community.
 *
 * Absent config — or every field [ProfileFieldLockState.EDITABLE] — is a strict no-op: the profile
 * and edit screens behave exactly as before, so there is zero impact for communities that set no lock.
 *
 * @property nickname status of the username (only [ProfileFieldLockState.EDITABLE] / [ProfileFieldLockState.READ_ONLY])
 * @property avatar status of the profile picture (only [ProfileFieldLockState.EDITABLE] / [ProfileFieldLockState.READ_ONLY])
 * @property bio status of the bio (may additionally be [ProfileFieldLockState.DISABLED])
 */
data class ProfileFieldsLock(
    val nickname: ProfileFieldLockState = ProfileFieldLockState.EDITABLE,
    val avatar: ProfileFieldLockState = ProfileFieldLockState.EDITABLE,
    val bio: ProfileFieldLockState = ProfileFieldLockState.EDITABLE,
) {
    companion object {
        /** Default when the community sets no per-field lock: every field editable (today's behaviour). */
        val AllEditable = ProfileFieldsLock()
    }
}
