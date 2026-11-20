package com.octopuscommunity.sdk.test.mock

import com.octopuscommunity.sdk.domain.model.CommunityConfig
import com.octopuscommunity.sdk.domain.model.CommunityConfig.Gamification.Level
import com.octopuscommunity.sdk.domain.model.Gamification
import com.octopuscommunity.sdk.domain.model.ProfileFieldsLock

/** Mock [CommunityConfig]s. */
object MockCommunityConfig {
    val default = create()

    val withoutGamification =
        create(
            gamification = null,
        )

    val all =
        listOf(
            default,
            withoutGamification,
        )

    fun create(
        forceLoginOnStrongActions: Boolean = true,
        gamification: CommunityConfig.Gamification? =
            CommunityConfig.Gamification(
                levels = MockGamificationLevels.all.associateBy { it.index },
                pointsName =
                    CommunityConfig.Gamification.Points.Name(
                        full = "points",
                        shortSingular = "pt",
                        shortPlural = "pts",
                    ),
                actionsPoints =
                    mapOf(
                        Gamification.Action.REACTION to 1,
                        Gamification.Action.VOTE to 15,
                        Gamification.Action.COMMENT to 5,
                        Gamification.Action.POST to 35,
                        Gamification.Action.POST_COMMENTED to 5,
                        Gamification.Action.DAILY_SESSION to 5,
                        Gamification.Action.PROFILE_COMPLETED to 50,
                    ),
            ),
        displayAccountAge: Boolean = true,
        profileFieldsLock: ProfileFieldsLock = ProfileFieldsLock.AllEditable,
    ) = CommunityConfig(
        forceLoginOnStrongActions = forceLoginOnStrongActions,
        gamification = gamification,
        displayAccountAge = displayAccountAge,
        profileFieldsLock = profileFieldsLock,
    )
}

/** Mock gamification levels. */
object MockGamificationLevels {
    val default =
        create(
            index = 1,
            name = "Novice",
            nextLevelPoints = 200,
            colors = null,
        )

    val all =
        listOf(
            default,
            Level(
                index = 2,
                name = "Insider",
                nextLevelPoints = 1000,
                colors =
                    Level.LevelColors(
                        Level.LevelColors.LevelColor(
                            background = "#C44003",
                            text = "#FFFFFF",
                        ),
                    ),
            ),
            Level(
                index = 3,
                name = "Expert",
                nextLevelPoints = 4000,
                colors =
                    Level.LevelColors(
                        Level.LevelColors.LevelColor(
                            background = "#828FA4",
                            text = "#FFFFFF",
                        ),
                    ),
            ),
            Level(
                index = 4,
                name = "Ambassador",
                colors =
                    Level.LevelColors(
                        Level.LevelColors.LevelColor(
                            background = "#F2BA00",
                            text = "#FFFFFF",
                        ),
                    ),
            ),
        )

    fun create(
        index: Int,
        name: String?,
        nextLevelPoints: Int?,
        colors: Level.LevelColors?,
    ) = Level(
        index = index,
        name = name,
        nextLevelPoints = nextLevelPoints,
        colors = colors,
    )
}
