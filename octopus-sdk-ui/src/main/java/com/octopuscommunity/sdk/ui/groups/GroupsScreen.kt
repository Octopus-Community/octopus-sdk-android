package com.octopuscommunity.sdk.ui.groups

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.octopuscommunity.sdk.ui.OctopusDestination
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.snackbar.OctopusSnackbarHost
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModel
import com.octopuscommunity.sdk.ui.components.OctopusContent
import com.octopuscommunity.sdk.ui.components.OctopusPoweredBy
import com.octopuscommunity.sdk.ui.components.OctopusScaffold
import com.octopuscommunity.sdk.ui.components.OctopusScreenContent
import com.octopuscommunity.sdk.ui.components.OctopusTopAppBar
import com.octopuscommunity.sdk.ui.groups.list.GroupsList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) = OctopusContent(navController) {
    val viewModel = octopusViewModel<GroupsScreenViewModel>()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    OctopusScaffold(
        modifier = modifier,
        viewModel = viewModel,
        topBar = { contentVisible ->
            OctopusTopAppBar(
                title = stringResource(R.string.groups_title),
                onNavigationIconClick = { navController.navigateUp() },
                contentVisible = contentVisible,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        GroupsContent(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
            navController = navController,
        )
    }
}

@Composable
internal fun GroupsContent(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) = OctopusScreenContent(
    modifier = modifier,
    navController = navController,
) { screenViewModel ->
    val screenState by screenViewModel.state.collectAsStateWithLifecycle()

    GroupsList(
        modifier = Modifier.fillMaxSize(),
        footer = {
            OctopusPoweredBy(
                modifier = Modifier.padding(top = 20.dp, bottom = 30.dp),
                communityConfig = screenState.communityConfig,
            )
        },
        onGroupClick = { topic ->
            navController.navigate(OctopusDestination.GroupDetails(groupId = topic.id))
        },
    )

    OctopusSnackbarHost(
        modifier = Modifier.align(Alignment.BottomCenter),
        screenViewModel = screenViewModel,
    )
}

@Preview(showBackground = true)
@Composable
private fun GroupsScreenPreview() {
    OctopusTheme {
        GroupsScreen(navController = rememberNavController())
    }
}
