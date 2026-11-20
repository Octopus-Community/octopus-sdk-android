package com.octopuscommunity.sdk.ui.gamification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.domain.model.CommunityConfig
import com.octopuscommunity.sdk.domain.model.Gamification
import com.octopuscommunity.sdk.test.mock.MockCommunityConfig
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.LocalOctopusCommunityConfig
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.components.OctopusModalBottomSheet
import com.octopuscommunity.sdk.ui.components.OctopusPoweredBy
import com.octopuscommunity.sdk.ui.medium
import com.octopuscommunity.sdk.ui.semiBold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OctopusGamificationSheet(
    communityConfig: CommunityConfig,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) = OctopusModalBottomSheet(
    modifier = modifier,
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismissRequest = onDismissRequest,
) {
    // The sheet's texts follow this community's tone, also when a host shows it outside the SDK root.
    CompositionLocalProvider(LocalOctopusCommunityConfig provides communityConfig) {
        GamificationSheetContent(communityConfig)
    }
}

@Composable
private fun GamificationSheetContent(communityConfig: CommunityConfig) {
    with(communityConfig.gamification!!) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.gamification_sheet_title_formal),
                    color = OctopusTheme.colorScheme.gray900,
                    style = OctopusTheme.typography.title2.semiBold,
                )
                Spacer(modifier = Modifier.width(16.dp))
                Icon(
                    modifier = Modifier.size(width = 44.dp, height = 44.dp),
                    painter = OctopusTheme.icons.gamification.rulesHeader(),
                    contentDescription = "Gamification",
                    tint = OctopusTheme.colorScheme.gray900,
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                listOf(
                    R.string.gamification_sheet_reaction to Gamification.Action.REACTION,
                    R.string.gamification_sheet_vote to Gamification.Action.VOTE,
                    R.string.gamification_sheet_post to Gamification.Action.POST,
                    R.string.gamification_sheet_comment to Gamification.Action.COMMENT,
                    R.string.gamification_sheet_post_commented_formal to Gamification.Action.POST_COMMENTED,
                    R.string.gamification_sheet_daily_session to Gamification.Action.DAILY_SESSION,
                    R.string.gamification_sheet_profile_completed_formal to Gamification.Action.PROFILE_COMPLETED,
                ).forEach { (title, action) ->
                    val points = getPoints(action)
                    if (points != null) {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = stringResource(title),
                                color = OctopusTheme.colorScheme.gray900,
                                style = OctopusTheme.typography.body2.medium,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "+${points.shortText}",
                                color = OctopusTheme.colorScheme.gray900,
                                style = OctopusTheme.typography.body2.semiBold,
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            OctopusPoweredBy(communityConfig = communityConfig)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OctopusGamificationSheetPreview() {
    OctopusTheme {
        OctopusGamificationSheet(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(600.dp),
            communityConfig = MockCommunityConfig.default,
            onDismissRequest = {},
        )
    }
}
