package com.octopuscommunity.sdk.domain.model

/** Community access of the current user. [accessDeniedMessage] explains a denied access. */
data class UserConfig(
    val canAccessCommunity: Boolean = true,
    val accessDeniedMessage: String? = null,
    /** Why the access is denied, `null` while it is granted. Appended last so positional calls keep their meaning. */
    val accessDenial: CommunityAccessDenial? = null,
) {
    /** The constructor as it was before [accessDenial], kept so code compiled against it keeps linking. */
    @Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
    constructor(
        canAccessCommunity: Boolean = true,
        accessDeniedMessage: String? = null,
    ) : this(
        canAccessCommunity = canAccessCommunity,
        accessDeniedMessage = accessDeniedMessage,
        accessDenial = null,
    )

    /** `copy` as it was before [accessDenial], which it keeps; kept so code compiled against it keeps linking. */
    @Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
    fun copy(
        canAccessCommunity: Boolean = this.canAccessCommunity,
        accessDeniedMessage: String? = this.accessDeniedMessage,
    ): UserConfig =
        UserConfig(
            canAccessCommunity = canAccessCommunity,
            accessDeniedMessage = accessDeniedMessage,
            accessDenial = accessDenial,
        )
}
