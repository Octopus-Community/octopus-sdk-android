package com.octopuscommunity.sample.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.octopuscommunity.sample.CommunityPostRoute
import com.octopuscommunity.sample.EditUserRoute
import com.octopuscommunity.sample.LoginRoute
import com.octopuscommunity.sample.MainViewModel
import com.octopuscommunity.sample.R
import com.octopuscommunity.sample.screens.community.CommunityContent
import com.octopuscommunity.sample.screens.home.HomeContent
import com.octopuscommunity.sample.screens.settings.SettingsContent
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
    onChangeCommunityAccess: (Boolean) -> Unit
) {
    val tabsNavController = rememberNavController()

    val items = listOf(
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
            route = CommunityTabRoute,
            label = "Settings",
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings
        )
    )
    var selectedItemIndex by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(
                        start = 32.dp,
                        end = 32.dp,
                        bottom = 8.dp
                    )
                    .clip(RoundedCornerShape(32.dp)),
                containerColor = NavigationBarDefaults.containerColor.copy(
                    alpha = 0.8f
                )

            ) {
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
                .padding(top = innerPadding.calculateTopPadding()),
            navController = tabsNavController,
            startDestination = HomeTabRoute
        ) {
            // Main Screen Tab
            composable<HomeTabRoute> {
                HomeContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = innerPadding.calculateBottomPadding()),
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
                CommunityContent(
                    modifier = Modifier
                        .fillMaxSize(),
                    navController = mainNavController,
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = innerPadding.calculateBottomPadding()
                    ),
                    onLogin = { mainNavController.navigate(LoginRoute) },
                    onEditUser = { mainNavController.navigate(EditUserRoute) }
                )
            }

            composable<SettingsTabRoute> {
                SettingsContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            bottom = innerPadding.calculateBottomPadding()
                        ),
                    state = state,
                    onChangeCommunityAccess = onChangeCommunityAccess
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
        onUpdateNotificationsCount = {},
        onLogout = {},
        onChangeCommunityAccess = {}
    )
}