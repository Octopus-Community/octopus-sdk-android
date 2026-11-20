package com.octopuscommunity.sdk.ui.settings.report

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.snackbar.OctopusSnackbarHost
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModel
import com.octopuscommunity.sdk.ui.components.OctopusContent
import com.octopuscommunity.sdk.ui.components.OctopusLinkedText
import com.octopuscommunity.sdk.ui.components.OctopusPoweredBy
import com.octopuscommunity.sdk.ui.components.OctopusScaffold
import com.octopuscommunity.sdk.ui.components.OctopusScreenContent
import com.octopuscommunity.sdk.ui.components.TextLink
import com.octopuscommunity.sdk.ui.lineHeight150
import com.octopuscommunity.sdk.ui.medium

@Composable
fun HowToReportContentScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) = OctopusContent(navController) {
    val viewModel = octopusViewModel<HowToReportContentViewModel>()

    OctopusScaffold(
        modifier = modifier,
        viewModel = viewModel,
        title = stringResource(R.string.settings_report_content),
        onBack = navController::navigateUp,
    ) { innerPadding ->
        HowToReportContentContent(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            navController = navController,
        )
    }
}

@Composable
fun HowToReportContentContent(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) = OctopusScreenContent(
    modifier = modifier,
    navController = navController,
) { screenViewModel ->
    val viewModel = octopusViewModel<HowToReportContentViewModel>()
    val screenState by screenViewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1.0f)
                    .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
            ) {
                Icon(
                    painter = OctopusTheme.icons.settings.info(),
                    contentDescription = null,
                    tint = OctopusTheme.colorScheme.gray900,
                )
                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.settings_report_main_text_formal),
                        color = OctopusTheme.colorScheme.gray900,
                        style = OctopusTheme.typography.body2.medium,
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    OctopusLinkedText(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.settings_report_explanation_formal),
                        links =
                            listOf(
                                TextLink(
                                    tag = "community_guidelines",
                                    text = stringResource(R.string.settings_community_guidelines),
                                    onClick = viewModel::navigateToCommunityGuidelines,
                                ),
                            ),
                        color = OctopusTheme.colorScheme.gray500,
                        style = OctopusTheme.typography.body2.lineHeight150,
                    )
                }
            }
        }
        OctopusPoweredBy(communityConfig = screenState.communityConfig)
    }

    OctopusSnackbarHost(
        modifier = Modifier.align(Alignment.BottomCenter),
        screenViewModel = screenViewModel,
    )
}

@Preview
@Composable
private fun HowToReportContentScreenPreview() {
    OctopusTheme {
        HowToReportContentScreen(navController = rememberNavController())
    }
}
