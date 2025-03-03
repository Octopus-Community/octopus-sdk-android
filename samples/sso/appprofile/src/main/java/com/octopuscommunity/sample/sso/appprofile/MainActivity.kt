package com.octopuscommunity.sample.sso.appprofile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.octopuscommunity.sample.sso.appprofile.data.AppUser
import com.octopuscommunity.sample.sso.appprofile.data.TokenProvider
import com.octopuscommunity.sample.sso.appprofile.data.getStoredUser
import com.octopuscommunity.sample.sso.appprofile.data.setStoredUser
import com.octopuscommunity.sample.sso.appprofile.screens.EditUserScreen
import com.octopuscommunity.sample.sso.appprofile.screens.LoginScreen
import com.octopuscommunity.sample.sso.appprofile.screens.MainScreen
import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.domain.model.ClientUser
import com.octopuscommunity.sdk.ui.OctopusDestination
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.octopusDrawables
import com.octopuscommunity.sdk.ui.octopusNavigation
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data object MainScreen

@Serializable
data object LoginScreen

@Serializable
data class EditUserScreen(val displayAge: Boolean = false)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val navController = rememberNavController()

            MaterialTheme(
                colorScheme = if (isSystemInDarkTheme()) {
                    darkColorScheme(
                        primary = Color.White,
                        onPrimary = Color.Black,
                        background = Color.Black
                    )
                } else {
                    lightColorScheme(
                        primary = Color.Black,
                        onPrimary = Color.White,
                        background = Color.White
                    )
                }
            ) {
                NavHost(
                    navController = navController,
                    startDestination = MainScreen
                ) {
                    composable<MainScreen> {
                        MainScreen(
                            appUser = getStoredUser(),
                            onLogin = { navController.navigate(LoginScreen) },
                            onEditUser = {
                                navController.navigate(EditUserScreen(displayAge = true))
                            },
                            onLogout = {
                                scope.launch {
                                    context.setStoredUser(null)
                                    OctopusSDK.disconnectUser()
                                }
                            },
                            onOpenOctopus = {
                                navController.navigate(OctopusDestination.Home)
                            }
                        )
                    }

                    composable<LoginScreen> {
                        Column {
                            LoginScreen(
                                initialAppUser = context.getStoredUser(),
                                onLogin = { user ->
                                    scope.launch {
                                        // Your login logic here
                                        context.setStoredUser(user)
                                        linkUserWithOctopus(user)
                                        navController.navigateUp()
                                    }
                                },
                                onBack = { navController.navigateUp() }
                            )
                        }
                    }

                    composable<EditUserScreen> { backStackEntry ->
                        val destination = backStackEntry.toRoute<EditUserScreen>()
                        EditUserScreen(
                            initialUser = context.getStoredUser() ?: AppUser(),
                            displayAge = destination.displayAge,
                            onSave = { user ->
                                scope.launch {
                                    context.setStoredUser(user)
                                    linkUserWithOctopus(user)
                                    navController.navigateUp()
                                }
                            },
                            onBack = { navController.navigateUp() }
                        )
                    }
                    octopusNavigation(
                        navController = navController,
                        onNavigateToLogin = { navController.navigate(LoginScreen) },
                        onNavigateToProfileEdit = {
                            navController.navigate(EditUserScreen(displayAge = false))
                        },
                        container = { content ->
                            OctopusTheme(
                                drawables = octopusDrawables(logo = R.drawable.ic_logo)
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

private suspend fun linkUserWithOctopus(appUser: AppUser) {
    OctopusSDK.connectUser(
        user = ClientUser(
            id = appUser.userId ?: "",
            profile = ClientUser.Profile(
                nickname = appUser.nickname,
                bio = appUser.bio,
                avatar = appUser.avatar,
                ageInformation = appUser.ageInformation
            )
        ),
        tokenProvider = {
            // Your Octopus token provider logic here
            TokenProvider.getToken(appUser.userId ?: "")
        }
    )
}