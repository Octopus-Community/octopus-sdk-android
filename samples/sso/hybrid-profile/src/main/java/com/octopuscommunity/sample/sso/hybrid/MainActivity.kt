package com.octopuscommunity.sample.sso.hybrid

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.octopuscommunity.sample.sso.hybrid.data.AppUser
import com.octopuscommunity.sample.sso.hybrid.data.TokenProvider
import com.octopuscommunity.sample.sso.hybrid.data.clearStoredUser
import com.octopuscommunity.sample.sso.hybrid.data.getStoredUser
import com.octopuscommunity.sample.sso.hybrid.data.setStoredUser
import com.octopuscommunity.sample.sso.hybrid.screen.SSOEditUserScreen
import com.octopuscommunity.sample.sso.hybrid.screen.SSOLoginScreen
import com.octopuscommunity.sample.sso.hybrid.screen.SSOMainScreen
import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.domain.model.ClientUser
import com.octopuscommunity.sdk.ui.OctopusDestination
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.octopusDarkColorScheme
import com.octopuscommunity.sdk.ui.octopusDrawables
import com.octopuscommunity.sdk.ui.octopusLightColorScheme
import com.octopuscommunity.sdk.ui.octopusNavigation
import kotlinx.serialization.Serializable

@Serializable
data object SSOMainScreen

@Serializable
data object SSOLoginScreen

@Serializable
data class EditUserScreen(val displayAge: Boolean = false)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val navController = rememberNavController()

            var appUser by remember { mutableStateOf(getStoredUser()) }
            LaunchedEffect(appUser) {
                val user = appUser
                if (user != null) {
                    context.setStoredUser(user)
                    linkUserWithOctopus(user)
                } else {
                    context.clearStoredUser()
                    OctopusSDK.disconnectUser()
                }
            }

            MaterialTheme(
                colorScheme = if (isSystemInDarkTheme()) {
                    darkColorScheme(
                        primary = Color(0xFF3AD9B1),
                        onPrimary = Color(0xFF141414),
                        secondaryContainer = Color(0xFF3AD9B1),
                        onSecondaryContainer = Color(0xFF141414),
                        background = Color(0xFF141414)
                    )
                } else {
                    lightColorScheme(
                        primary = Color(0xFF068677),
                        onPrimary = Color(0xFFFFFFFF),
                        surfaceContainer = Color(0xFF068677),
                        onSecondaryContainer = Color(0xFFFFFFFF),
                        background = Color(0xFFFFFFFF)
                    )
                }
            ) {
                NavHost(
                    navController = navController,
                    startDestination = SSOMainScreen
                ) {
                    composable<SSOMainScreen> {
                        SSOMainScreen(
                            appUser = appUser,
                            onLogin = { navController.navigate(SSOLoginScreen) },
                            onEditUser = {
                                navController.navigate(EditUserScreen(displayAge = false))
                            },
                            onLogout = { appUser = null },
                            onOpenOctopus = {
                                navController.navigate(OctopusDestination.Home)
                            }
                        )
                    }

                    composable<SSOLoginScreen> {
                        Column {
                            SSOLoginScreen(
                                initialAppUser = context.getStoredUser(),
                                onLogin = { user ->
                                    appUser = user
                                    navController.navigateUp()
                                },
                                onBack = { navController.navigateUp() }
                            )
                        }
                    }

                    composable<EditUserScreen> { backStackEntry ->
                        val destination = backStackEntry.toRoute<EditUserScreen>()
                        SSOEditUserScreen(
                            initialUser = context.getStoredUser() ?: AppUser(),
                            appManagedFields = OctopusSDK.connectionRepository.appManagedFields,
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
                        onNavigateToLogin = { navController.navigate(SSOLoginScreen) },
                        onNavigateToProfileEdit = {
                            navController.navigate(EditUserScreen(displayAge = false))
                        },
                        container = { content ->
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

    private suspend fun linkUserWithOctopus(appUser: AppUser) {
        OctopusSDK.connectUser(
            user = ClientUser(
                userId = appUser.id ?: "",
                profile = ClientUser.Profile(
                    nickname = appUser.nickname,
                    bio = appUser.bio,
                    avatar = appUser.avatar,
                    ageInformation = appUser.ageInformation
                )
            ),
            tokenProvider = {
                // Your Octopus token provider logic here
                TokenProvider.getToken(userId = appUser.id ?: "")
            }
        )
    }
}