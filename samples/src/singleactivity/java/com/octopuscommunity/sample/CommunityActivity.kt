package com.octopuscommunity.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.octopuscommunity.sample.theme.CommunityTheme
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.OctopusTransitionsDefaults
import com.octopuscommunity.sdk.ui.home.OctopusHomeScreen
import com.octopuscommunity.sdk.ui.octopusComposables

/**
 * Main activity for the Octopus SDK sample application.
 *
 * This activity demonstrates how to integrate the Octopus SDK into an Android application.
 * It serves as a reference implementation for common SDK usage patterns.
 */
class CommunityActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val navController = rememberNavController()

            OctopusTheme(
                // Configure the community theme here
            ) {
                NavHost(
                    navController = navController,
                    startDestination = "Home"
                ) {
                    composable("Home") {
                        OctopusHomeScreen(
                            modifier = Modifier.fillMaxSize(),
                            navController = navController,
                            onNavigateToLogin = {
                                // Start your Login Activity
                            },
                            onNavigateToProfileEdit = {
                                // Start your Profile Edit Activity
                            },
                            onBack = {
                                finish()
                            }
                        )
                    }

                    octopusComposables(
                        navController = navController,
                        transitions = OctopusTransitionsDefaults(),
                        onNavigateToLogin = {
                            // Start your Login Activity
                        },
                        onNavigateToProfileEdit = { fieldToEdit ->
                            // Start your Profile Edit Activity
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