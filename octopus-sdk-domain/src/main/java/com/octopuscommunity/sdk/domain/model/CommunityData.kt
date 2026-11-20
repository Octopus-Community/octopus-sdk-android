package com.octopuscommunity.sdk.domain.model

/**
 * Read-only snapshot of a user's public activity inside the Octopus community.
 *
 * This is a dedicated, additive type — **not** an extension of [OctopusProfile] — so a host app
 * can surface community stats (message count, gamification) inside its own profile screen without
 * adopting the Octopus profile UI. It is consumed à la carte: render whichever fields you need.
 *
 * Obtain it via [com.octopuscommunity.sdk.OctopusSDK.communityDataFlow] (reactive) or
 * [com.octopuscommunity.sdk.OctopusSDK.fetchCommunityData] (one-shot refresh).
 *
 * @property userId The id of the user this data describes.
 * @property messageCount The user's aggregate message count in the community, or `null` when the
 * backend did not provide it. Today this is a single aggregate; a posts/comments split may be added
 * later (additive, non-breaking).
 * @property gamification The user's gamification standing, or `null` when gamification is disabled
 * for the community or the profile is tagged admin.
 */
data class OctopusCommunityData(
    val userId: String,
    val messageCount: Int?,
    val gamification: OctopusGamification?,
)

/**
 * A user's gamification standing in the community.
 *
 * Present only when gamification is enabled for the community and the profile is not tagged admin
 * (see [OctopusCommunityData.gamification]).
 *
 * @property level The user's gamification level index.
 * @property score The user's total gamification points, for the connected member as for any other
 * member. It is read again on every refresh, so it includes points earned since the last one,
 * including points your app awarded. `null` when the total is not known yet: data cached before the
 * SDK read the total, or a server that does not send it. The Octopus screens keep showing only
 * the connected member's own points.
 */
data class OctopusGamification(
    val level: Int,
    val score: Int? = null,
)
