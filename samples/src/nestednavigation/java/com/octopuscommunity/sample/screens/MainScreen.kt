package com.octopuscommunity.sample.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.octopuscommunity.sample.CommunityPostRoute
import com.octopuscommunity.sample.EditUserRoute
import com.octopuscommunity.sample.LoginRoute
import com.octopuscommunity.sample.MainViewModel
import com.octopuscommunity.sample.screens.community.CommunityScreen
import com.octopuscommunity.sample.screens.home.HomeScreen
import com.octopuscommunity.sample.screens.settings.SettingsScreen
import com.octopuscommunity.sample.theme.CommunityTheme
import com.octopuscommunity.sdk.ui.OctopusTransitionsDefaults
import com.octopuscommunity.sdk.ui.octopusComposables
import kotlinx.serialization.Serializable

@Serializable
data object HomeTabRoute

@Serializable
data object CommunityTabRoute

@Serializable
data object SettingsTabRoute

data class BottomNavigationItem(
    val route: Any,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    var badgeCount: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    mainNavController: NavHostController,
    state: MainViewModel.State,
    onLogout: () -> Unit,
    onUpdateNotificationsCount: () -> Unit,
    onChangeCommunityAccess: (Boolean) -> Unit
) {
    val tabsNavController = rememberNavController()

    LifecycleEventEffect(
        event = Lifecycle.Event.ON_RESUME,
        onEvent = onUpdateNotificationsCount
    )

    val items = remember(state.unreadNotificationsCount) {
        listOf(
            BottomNavigationItem(
                route = HomeTabRoute,
                label = "Home",
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home
            ),
            BottomNavigationItem(
                route = CommunityTabRoute,
                label = "Community",
                selectedIcon = Icons.Filled.Person,
                unselectedIcon = Icons.Outlined.Person,
                badgeCount = state.unreadNotificationsCount
            ),
            BottomNavigationItem(
                route = SettingsTabRoute,
                label = "Settings",
                selectedIcon = Icons.Filled.Settings,
                unselectedIcon = Icons.Outlined.Settings
            )
        )
    }
    var selectedItemIndex by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {},
//        topBar = {
//            TopAppBar(
//                title = { Text(stringResource(R.string.app_name)) },
//                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = MaterialTheme.colorScheme.background
//                )
//            )
//        },
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedItemIndex == index,
                        onClick = {
                            tabsNavController.navigate(item.route)
                            selectedItemIndex = index
                        },
                        icon = {
                            BadgedBox(
                                badge = {
                                    if (item.badgeCount > 0) {
                                        Badge {
                                            Text(
                                                text = when {
                                                    item.badgeCount > 99 -> "99+"
                                                    else -> item.badgeCount.toString()
                                                }
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (selectedItemIndex == index) {
                                        item.selectedIcon
                                    } else {
                                        item.unselectedIcon
                                    },
                                    contentDescription = item.label
                                )
                            }
                        },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            navController = tabsNavController,
            startDestination = HomeTabRoute
        ) {
            // Main Screen Tab
            composable<HomeTabRoute> {
                HomeScreen(
                    modifier = Modifier
                        .fillMaxSize(),
                    state = state,
                    onLogin = { mainNavController.navigate(LoginRoute) },
                    onEditUser = { mainNavController.navigate(EditUserRoute) },
                    onLogout = onLogout,
                    onOpenCommunity = {
                        selectedItemIndex = 1
                        tabsNavController.navigate(CommunityTabRoute) {
                            popUpTo(CommunityTabRoute) { saveState = true }
                            launchSingleTop = true
                        }
                    },
                    onOpenBridgePost = {
                        state.octopusPost?.let { octopusPost ->
                            mainNavController.navigate(CommunityPostRoute(octopusPost.id))
                        }
                    },
                    onOpenSettings = {
                        selectedItemIndex = 2
                        tabsNavController.navigate(SettingsTabRoute) {
                            popUpTo(SettingsTabRoute) { saveState = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            // Community Screen Tab
            composable<CommunityTabRoute> {
                CommunityScreen(
                    modifier = Modifier
                        .fillMaxSize(),
                    navController = tabsNavController,
                    backButton = false,
                    onLogin = { mainNavController.navigate(LoginRoute) },
                    onEditUser = { mainNavController.navigate(EditUserRoute) },
                    onBack = tabsNavController::navigateUp
                )
            }

            composable<SettingsTabRoute> {
                SettingsScreen(
                    modifier = Modifier
                        .fillMaxSize(),
                    backButton = false,
                    state = state,
                    onChangeCommunityAccess = onChangeCommunityAccess
                )
            }

            octopusComposables(
                navController = tabsNavController,
                transitions = OctopusTransitionsDefaults(),
                onNavigateToLogin = { mainNavController.navigate(LoginRoute) },
                onNavigateToProfileEdit = { fieldToEdit ->
                    mainNavController.navigate(EditUserRoute)
                },
                container = { backStackEntry, content ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .consumeWindowInsets(WindowInsets.systemBars)
                    ) {
                        CommunityTheme(content = content)
                    }
                }
            )
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