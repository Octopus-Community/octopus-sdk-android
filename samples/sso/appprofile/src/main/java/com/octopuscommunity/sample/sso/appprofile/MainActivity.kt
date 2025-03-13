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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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

            var appUser by remember { mutableStateOf(getStoredUser()) }
            LaunchedEffect(appUser) {
                val user = appUser
                setStoredUser(user)
                if (user != null) {
                    linkUserWithOctopus(user)
                } else {
                    OctopusSDK.disconnectUser()
                }
            }

            // Material3 values used by Octopus SDK - You can also instead customize them in
            // OctopusTheme
            MaterialTheme(
                colorScheme = if (isSystemInDarkTheme()) {
                    darkColorScheme(
                        primary = Color(0xFF3AD9B1),
                        primaryContainer = Color(0xFF083B2F),
                        inversePrimary = Color(0xFFD8F4F1),
                        onPrimary = Color(0xFF141414),
                        background = Color(0xFF141414),
                        surfaceContainerHigh = Color(0xFF242526)
                    )
                } else {
                    lightColorScheme(
                        primary = Color(0xFF068677),
                        primaryContainer = Color(0xFFD8F4F1),
                        inversePrimary = Color(0xFF15D1A2),
                        onPrimary = Color(0xFFFFFFFF),
                        background = Color(0xFFFFFFFF),
                        surfaceContainerHigh = Color(0xFFFFFFFF)
                    )
                }
            ) {
                NavHost(
                    navController = navController,
                    startDestination = MainScreen
                ) {
                    composable<MainScreen> {
                        MainScreen(
                            appUser = appUser,
                            onLogin = { navController.navigate(LoginScreen) },
                            onEditUser = {
                                navController.navigate(EditUserScreen(displayAge = true))
                            },
                            onLogout = {
                                scope.launch {
                                    appUser = null
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
                                storedUser = appUser,
                                onLogin = { user ->
                                    scope.launch {
                                        // Your login logic here
                                        appUser = user
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
                            initialUser = appUser ?: AppUser(),
                            displayAge = destination.displayAge,
                            onSave = { user ->
                                appUser = user
                                navController.navigateUp()
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