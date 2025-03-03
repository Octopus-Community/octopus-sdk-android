package com.octopuscommunity.sample.sso.octopusprofile

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.octopuscommunity.sample.sso.octopusprofile.data.TokenProvider
import com.octopuscommunity.sample.sso.octopusprofile.screens.LoginScreen
import com.octopuscommunity.sample.sso.octopusprofile.screens.MainScreen
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val navController = rememberNavController()

            var userId by remember { mutableStateOf(getStoredUserId()) }

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
                            storedUserId = userId,
                            onLogin = { navController.navigate(LoginScreen) },
                            onLogout = {
                                scope.launch {
                                    userId = null
                                    context.setStoredUserId(null)
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
                                storedUserId = userId,
                                onLogin = { newUserId ->
                                    scope.launch {
                                        // Your login logic here
                                        userId = newUserId
                                        setStoredUserId(newUserId)
                                        linkUserWithOctopus(newUserId)
                                        navController.navigateUp()
                                    }
                                },
                                onBack = { navController.navigateUp() }
                            )
                        }
                    }

                    octopusNavigation(
                        navController = navController,
                        onNavigateToLogin = { navController.navigate(LoginScreen) },
                        container = { content ->
                            OctopusTheme(
                                drawables = octopusDrawables(logo = R.drawable.ic_logo),
                                content = content
                            )
                        }
                    )
                }
            }
        }
    }
}

private suspend fun linkUserWithOctopus(appUserId: String) {
    OctopusSDK.connectUser(
        user = ClientUser(
            id = appUserId,
            // Optional : Prefill the Octopus Profile with your user's data
            profile = ClientUser.Profile(
                nickname = "UserNickname",
                avatar = null,
                bio = "User's Bio",
                // Optional : Prefill the Legal Age checking
                ageInformation = ClientUser.Profile.AgeInformation.LegalAgeReached
            )
        ),
        tokenProvider = {
            // Your Octopus token provider logic here
            TokenProvider.getToken(appUserId)
        }
    )
}

/**
 * User store
 * Only used to keep the logged in app user across app restart.
 * You probably do not need this class as you already have your own way to store your users.
 */
private fun Context.getUserPreferences() = getSharedPreferences("app_user", Context.MODE_PRIVATE)

fun Context.getStoredUserId(): String? = getUserPreferences().getString("userId", null)

private fun Context.setStoredUserId(userId: String?) = getUserPreferences().edit {
    if (userId != null) {
        putString("userId", userId)
    } else {
        remove("userId")
    }
}