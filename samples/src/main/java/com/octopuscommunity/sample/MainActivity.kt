package com.octopuscommunity.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.octopuscommunity.sample.data.datastore.UserDataStore
import com.octopuscommunity.sample.screens.MainScreen
import com.octopuscommunity.sample.screens.community.CommunityScreen
import com.octopuscommunity.sample.screens.login.LoginScreen
import com.octopuscommunity.sample.screens.profile.EditProfileScreen
import com.octopuscommunity.sample.screens.settings.SettingsScreen
import com.octopuscommunity.sample.theme.AppTheme
import com.octopuscommunity.sample.theme.CommunityTheme
import com.octopuscommunity.sdk.ui.OctopusTransitionsDefaults
import com.octopuscommunity.sdk.ui.octopusComposables
import kotlinx.serialization.Serializable

/**
 * Route representing the main screen of the sample app in SSO mode.
 */
@Serializable
data object MainRoute

/**
 * Route representing the login screen for SSO authentication.
 */
@Serializable
data object LoginRoute

/**
 * Route representing the user profile editing screen.
 */
@Serializable
data object EditUserRoute

/**
 * Route representing the community.
 */
@Serializable
data object CommunityRoute

@Serializable
data object SettingsRoute

/**
 * Main activity for the Octopus SDK sample application.
 *
 * This activity demonstrates how to integrate the Octopus SDK into an Android application,
 * including user authentication, navigation, theming, and push notification handling.
 * It serves as a reference implementation for common SDK usage patterns.
 */
class MainActivity : ComponentActivity() {

    private val userDataStore by lazy { UserDataStore(applicationContext) }
    private val viewModelFactory = viewModelFactory {
        initializer {
            MainViewModel(userDataStore)
        }
    }
    private val viewModel by viewModels<MainViewModel> { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val navController = rememberNavController()
            val state by viewModel.state.collectAsStateWithLifecycle()

            AppTheme {
                NavHost(
                    navController = navController,
                    startDestination = MainRoute
                ) {
                    composable<MainRoute> {
                        MainScreen(
                            mainNavController = navController,
                            state = state,
                            onLogout = { viewModel.updateUser(null) },
                            onUpdateNotificationsCount = viewModel::updateNotificationsCount,
                            onChangeCommunityAccess = viewModel::updateCommunityAccess
                        )
                    }

                    composable<LoginRoute> {
                        LoginScreen(
                            onLogin = { user ->
                                viewModel.updateUser(user)
                                navController.navigateUp()
                            },
                            onBack = { navController.navigateUp() }
                        )
                    }

                    composable<EditUserRoute> {
                        val user = state.user
                        if (user == null) {
                            LaunchedEffect(Unit) {
                                navController.navigate(LoginRoute) {
                                    popUpTo<EditUserRoute> { inclusive = true }
                                }
                            }
                        } else {
                            EditProfileScreen(
                                user = user,
                                onSave = { user ->
                                    viewModel.updateUser(user)
                                    navController.navigateUp()
                                },
                                onBack = { navController.navigateUp() }
                            )
                        }
                    }

                    composable<CommunityRoute> {
                        CommunityScreen(
                            navController = navController,
                            onLogin = { navController.navigate(LoginRoute) },
                            onEditUser = { navController.navigate(EditUserRoute) },
                            onBack = { navController.navigateUp() }
                        )
                    }

                    composable<SettingsRoute> {
                        SettingsScreen(
                            state = state,
                            onChangeCommunityAccess = viewModel::updateCommunityAccess,
                            onBack = { navController.navigateUp() }
                        )
                    }

                    octopusComposables(
                        navController = navController,
                        transitions = OctopusTransitionsDefaults(),
                        onNavigateToLogin = { navController.navigate(LoginRoute) },
                        onNavigateToProfileEdit = { fieldToEdit ->
                            navController.navigate(EditUserRoute)
                        },
                        container = { backStackEntry, content ->
                            CommunityTheme(content = content)
                        }
                    )
                }
            }
        }
    }
}