package com.octopuscommunity.sample.sso.octopus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
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
import com.octopuscommunity.sample.sso.octopus.data.AppUser
import com.octopuscommunity.sample.sso.octopus.data.TokenProvider
import com.octopuscommunity.sample.sso.octopus.data.clearStoredUser
import com.octopuscommunity.sample.sso.octopus.data.getStoredUser
import com.octopuscommunity.sample.sso.octopus.data.setStoredUser
import com.octopuscommunity.sample.sso.octopus.screen.SSOLoginScreen
import com.octopuscommunity.sample.sso.octopus.screen.SSOMainScreen
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
            NavHost(
                navController = navController,
                startDestination = SSOMainScreen
            ) {
                composable<SSOMainScreen> {
                    SSOMainScreen(
                        appUser = appUser,
                        onLogin = { navController.navigate(SSOLoginScreen) },
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

                octopusNavigation(
                    navController = navController,
                    onNavigateToLogin = { navController.navigate(SSOLoginScreen) },
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