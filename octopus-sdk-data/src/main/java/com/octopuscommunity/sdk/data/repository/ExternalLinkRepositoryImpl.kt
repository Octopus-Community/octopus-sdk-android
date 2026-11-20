package com.octopuscommunity.sdk.data.repository

import com.octopuscommunity.sdk.data.utils.md5
import com.octopuscommunity.sdk.domain.repository.ExternalLinkRepository

/**
 * This class manages the gRPC services used by the Octopus SDK for messaging.
 * It handles channel creation, stub initialization, and token management.
 */
class ExternalLinkRepositoryImpl(
    private val linksHost: String,
    private val apiKey: String
) : ExternalLinkRepository {
    override val termsOfUseUrl by lazy { getUrl("tos") }
    override val communityGuidelinesUrl by lazy { getUrl("guidelines") }
    override val privacyPolicyUrl by lazy { getUrl("privacy") }

    private fun getUrl(category: String) = "https://${linksHost}/$category/${apiKey.md5()}"
}