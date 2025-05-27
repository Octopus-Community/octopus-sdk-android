package com.octopuscommunity.sample.sso.hybrid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.octopuscommunity.sample.sso.hybrid.data.datastore.UserDataStore
import com.octopuscommunity.sample.sso.hybrid.data.model.User
import com.octopuscommunity.sample.sso.hybrid.screen.EditUserScreen
import com.octopuscommunity.sample.sso.hybrid.screen.LoginScreen
import com.octopuscommunity.sample.sso.hybrid.screen.MainScreen
import com.octopuscommunity.sdk.ui.OctopusDestination
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.octopusDarkColorScheme
import com.octopuscommunity.sdk.ui.octopusDrawables
import com.octopuscommunity.sdk.ui.octopusLightColorScheme
import com.octopuscommunity.sdk.ui.octopusNavigation
import kotlinx.serialization.Serializable

/**
 * Route representing the main screen of the sample app in SSO mode.
 */
@Serializable
data object MainScreen

/**
 * Route representing the login screen for SSO authentication.
 */
@Serializable
data object LoginScreen

/**
 * Route representing the user profile editing screen.
 */
@Serializable
data object EditUserScreen

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
            val context = LocalContext.current
            val navController = rememberNavController()
            val state by viewModel.state.collectAsStateWithLifecycle()

            // Set up Material theme with custom colors
            MaterialTheme(
                colorScheme = if (isSystemInDarkTheme()) {
                    darkColorScheme(
                        primary = Color(0xFF3AD9B1),
                        onPrimary = Color(0xFF141414),
                        secondaryContainer = Color(0xFF3AD9B1),
                        onSecondaryContainer = Color(0xFF141414),
                        surface = Color(0xFF141414),
                        background = Color(0xFF141414)
                    )
                } else {
                    lightColorScheme(
                        primary = Color(0xFF068677),
                        onPrimary = Color(0xFFFFFFFF),
                        secondaryContainer = Color(0xFF068677),
                        onSecondaryContainer = Color(0xFFFFFFFF),
                        surface = Color(0xFFFFFFFF),
                        background = Color(0xFFFFFFFF)
                    )
                }
            ) {
                // Set up navigation with both app screens and Octopus SDK screens
                NavHost(
                    navController = navController,
                    startDestination = MainScreen
                ) {
                    // SSO Main Screen - initial landing page when SSO is enabled
                    composable<MainScreen> {
                        MainScreen(
                            user = state.user,
                            unreadNotificationsCount = state.unreadNotificationsCount,
                            onResume = viewModel::updateNotificationsCount,
                            onLogin = { navController.navigate(LoginScreen) },
                            onEditUser = {
                                navController.navigate(EditUserScreen)
                            },
                            onLogout = { viewModel.updateUser(null) },
                            onOpenOctopus = {
                                navController.navigate(OctopusDestination.Home)
                            }
                        )
                    }

                    // SSO Login Screen - handles user authentication
                    composable<LoginScreen> {
                        Column {
                            LoginScreen(
                                onLogin = { user ->
                                    viewModel.updateUser(user)
                                    navController.navigateUp()
                                },
                                onBack = { navController.navigateUp() }
                            )
                        }
                    }

                    // SSO Edit User Screen - handles user profile editing
                    composable<EditUserScreen> {
                        EditUserScreen(
                            user = state.user ?: User(),
                            onSave = { user ->
                                viewModel.updateUser(user)
                                navController.navigateUp()
                            },
                            onBack = { navController.navigateUp() }
                        )
                    }

                    // Octopus SDK Navigation - integrates all Octopus screens
                    octopusNavigation(
                        navController = navController,
                        onNavigateToLogin = { navController.navigate(LoginScreen) },
                        onNavigateToProfileEdit = { fieldToEdit ->
                            navController.navigate(EditUserScreen)
                        },
                        container = { content ->
                            // Apply Octopus theming to match the rest of the app
                            OctopusTheme(
                                colorScheme = if (isSystemInDarkTheme()) {
                                    octopusDarkColorScheme(
                                        // Green
                                        primary = Color(0xFF3AD9B1),
                                        primaryLow = Color(0xFF083B2F),
                                        primaryHigh = Color(0xFFD8F4F1)
                                    )
                                } else {
                                    octopusLightColorScheme(
                                        // Green
                                        primary = Color(0xFF068677),
                                        primaryLow = Color(0xFFD8F4F1),
                                        primaryHigh = Color(0xFF15D1A2)
                                    )
                                },
                                drawables = octopusDrawables(
                                    logo = R.drawable.ic_logo
                                )
                            ) {
                                content()
                            }
                        }
                    )
                }
            }
        }
    }
}