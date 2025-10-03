package com.octopuscommunity.sample.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.octopuscommunity.sample.EditUserRoute
import com.octopuscommunity.sample.LoginRoute
import com.octopuscommunity.sample.MainViewModel
import com.octopuscommunity.sample.R
import com.octopuscommunity.sample.SettingsRoute
import com.octopuscommunity.sample.screens.community.CommunityContent
import com.octopuscommunity.sample.screens.home.HomeContent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    mainNavController: NavHostController,
    state: MainViewModel.State,
    onLogout: () -> Unit,
    onUpdateNotificationsCount: () -> Unit,
    onChangeCommunityAccess: (Boolean) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true // Start fully expanded
    )
    var showCommunitySheet by rememberSaveable { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        onUpdateNotificationsCount()
    }

    val hideModal = NavController.OnDestinationChangedListener { _, _, _ ->
        coroutineScope.launch {
            sheetState.hide()
        }
    }

    DisposableEffect(mainNavController) {
        mainNavController.addOnDestinationChangedListener(hideModal)
        onDispose {
            mainNavController.removeOnDestinationChangedListener(hideModal)
        }
    }

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
            onOpenCommunity = {
                showCommunitySheet = true
            },
            onOpenSettings = { mainNavController.navigate(SettingsRoute) }
        )

        // Community Modal Bottom Sheet
        if (showCommunitySheet) {
            ModalBottomSheet(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.background,
                onDismissRequest = {
                    showCommunitySheet = false
                }
            ) {
                CommunityContent(
                    modifier = Modifier.fillMaxSize(),
                    navController = mainNavController,
                    onLogin = {
                        mainNavController.navigate(LoginRoute)
                    },
                    onEditUser = {
                        mainNavController.navigate(EditUserRoute)
                    }
                )
            }
        }
    }
}

@Preview
@Composable
private fun MainScreenPreview() {
    MainScreen(
        mainNavController = rememberNavController(),
        state = MainViewModel.State(),
        onLogout = {},
        onUpdateNotificationsCount = {},
        onChangeCommunityAccess = {}
    )
}