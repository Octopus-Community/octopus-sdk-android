package com.octopuscommunity.sdk.domain.model

/** Namespace of the gamification types. */
class Gamification {
    /** User actions that may earn gamification points. */
    enum class Action {
        REACTION,
        COMMENT,
        REPLY,
        POST,
        VOTE,
        POST_COMMENTED,
        PROFILE_COMPLETED,
        DAILY_SESSION,
    }

    /** Toast telling the user they earned [points] for [action]. */
    data class Toast(
        val action: Action,
        val points: CommunityConfig.Gamification.Points,
    )
}
