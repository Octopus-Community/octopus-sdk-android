package com.octopuscommunity.sdk.domain.repository

/**
 * This class manages the gRPC services used by the Octopus SDK for messaging.
 * It handles channel creation, stub initialization, and token management.
 */
interface ExternalLinkRepository {
    val termsOfUseUrl: String
    val communityGuidelinesUrl: String
    val privacyPolicyUrl: String
}