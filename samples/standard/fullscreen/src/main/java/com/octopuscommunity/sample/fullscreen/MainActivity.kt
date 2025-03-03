package com.octopuscommunity.sample.fullscreen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.octopuscommunity.sdk.ui.OctopusDestination
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.octopusDrawables
import com.octopuscommunity.sdk.ui.octopusNavigation
import kotlinx.serialization.Serializable

@Serializable
data object MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
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
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = MainScreen
                ) {
                    composable<MainScreen> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp)
                        ) {
                            Button(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.Center),
                                onClick = { navController.navigate(OctopusDestination.Home) },
                            ) {
                                Text("Open Octopus")
                            }
                        }
                    }
                    octopusNavigation(navController = navController) { content ->
                        OctopusTheme(
                            drawables = octopusDrawables(logo = R.drawable.ic_logo)
                        ) {
                            content()
                        }
                    }
                }
            }
        }
    }
}