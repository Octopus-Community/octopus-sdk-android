package com.octopuscommunity.sdk.data.repository

import com.octopuscommunity.sdk.domain.model.CommunityAccessDenial
import com.octopuscommunity.sdk.domain.model.UserConfig

/**
 * The message of a banned account: the server's, else the sentence of the SDK. `null` for any other access.
 *
 * The sentence is in English: it ends up in an error message read by the host app, and this module has no
 * localized text.
 */
internal fun UserConfig.bannedMessageOrNull(): String? {
    val ban = accessDenial as? CommunityAccessDenial.AccountBanned ?: return null
    return accessDeniedMessage?.ifEmpty { null }
        ?: ban.nickname?.let { "Your account @$it was banned for breaking the \"${ban.policyName}\" policy." }
        ?: "Your account was banned for breaking the \"${ban.policyName}\" policy."
}
