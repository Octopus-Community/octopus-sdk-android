package com.octopuscommunity.sdk.sample.data

import com.octopuscommunity.sdk.sample.BuildConfig

/**
 * Initial demo content IDs for the scenario screens; availability depends on the community.
 * Explicit deep-link IDs and editable scenario inputs override these initial values without a rebuild.
 * Build inputs: -P > local.properties > octopus.properties > environment; non-blank wins over catalog.
 */
object SampleFixtures {
    val postText: String = resolveFixtureId(BuildConfig.OCTOPUS_FIXTURE_POST_ID, "SfEDTqxLavmbCEcEO6CBRp")
    val commentOnPost: String = resolveFixtureId(BuildConfig.OCTOPUS_FIXTURE_COMMENT_ID, "v_iqL4I1Scdfv3OfJeINO1")
    val userOther: String = resolveFixtureId(BuildConfig.OCTOPUS_FIXTURE_USER_ID, "FHjwxMQBGmSAD4KJxQk7uP")
    val topicDefault: String = resolveFixtureId(BuildConfig.OCTOPUS_FIXTURE_TOPIC_ID, "PMYFiz0sv6cKEcpMkq5qlT")
    val postReactionStack: String = resolveFixtureId(BuildConfig.OCTOPUS_FIXTURE_REACTION_POST_ID, "piWm_vjBa43SEOTexuz8ye")

    // Missing in the catalog: never substitute a different kind of content for a QA scenario.
    val postImage: String? = null
    val postPoll: String? = null
    val postCta: String? = null
    val commentReported: String? = null
    val topicGated: String? = null
    val deeplinkPost: String? = null

    const val postLinkPattern: String = "octopus-sample://post/{postId}"
}

/** A missing or blank build input keeps the shared catalog target. */
internal fun resolveFixtureId(
    override: String?,
    catalogDefault: String,
): String = override?.takeIf { it.isNotBlank() } ?: catalogDefault
