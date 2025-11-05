package com.octopuscommunity.sample.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.octopuscommunity.sample.CommunityPostRoute
import com.octopuscommunity.sample.CommunityRoute
import com.octopuscommunity.sample.EditUserRoute
import com.octopuscommunity.sample.LoginRoute
import com.octopuscommunity.sample.MainViewModel
import com.octopuscommunity.sample.R
import com.octopuscommunity.sample.SettingsRoute
import com.octopuscommunity.sample.screens.home.HomeContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    mainNavController: NavHostController,
    state: MainViewModel.State,
    onLogout: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        HomeContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            state = state,
            onLogin = { mainNavController.navigate(LoginRoute) },
            onEditUser = { mainNavController.navigate(EditUserRoute) },
            onLogout = onLogout,
            onOpenCommunity = { mainNavController.navigate(CommunityRoute) },
            onOpenBridgePost = {
                state.octopusPost?.let { octopusPost ->
                    mainNavController.navigate(CommunityPostRoute(octopusPost.id))
                }
            },
            onOpenSettings = { mainNavController.navigate(SettingsRoute) }
        )
    }
}

@Preview
@Composable
private fun MainScreenPreview() {
    MainScreen(
        mainNavController = rememberNavController(),
        state = MainViewModel.State(),
        onLogout = {}
    )
}