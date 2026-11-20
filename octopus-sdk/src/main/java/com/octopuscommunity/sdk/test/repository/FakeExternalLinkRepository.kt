package com.octopuscommunity.sdk.test.repository

import com.octopuscommunity.sdk.domain.repository.ExternalLinkRepository

/** [ExternalLinkRepository] returning the given urls, empty by default. */
class FakeExternalLinkRepository(
    override val termsOfUseUrl: String = "",
    override val communityGuidelinesUrl: String = "",
    override val privacyPolicyUrl: String = "",
) : ExternalLinkRepository
