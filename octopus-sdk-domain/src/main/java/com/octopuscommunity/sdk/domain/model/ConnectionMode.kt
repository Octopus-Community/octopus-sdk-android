package com.octopuscommunity.sdk.domain.model

/** The kind of connection */
sealed class ConnectionMode {
    /**
     * You provide to OctopusSDK the user and their lifecycle are tight to their lifecycle in your
     * app
     *
     * @param appManagedFields Set of fields that are "owned" by your app. An associated field
     * cannot be updated by Octopus and is only updated by your app.
     * For example: if `appManagedFields` contains NICKNAME, only your app will be able to
     * update the user's nickname and the field won't be editable in Octopus.
     */
    data class SSO(
        val appManagedFields: Set<ProfileField> = emptySet(),
    ) : ConnectionMode()
}
