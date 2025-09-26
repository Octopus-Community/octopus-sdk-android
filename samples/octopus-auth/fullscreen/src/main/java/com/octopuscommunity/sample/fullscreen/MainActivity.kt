package com.octopuscommunity.sample.fullscreen

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.octopuscommunity.sdk.ui.OctopusDrawablesDefaults
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.OctopusTypographyDefaults
import com.octopuscommunity.sdk.ui.navigateToOctopusHome
import com.octopuscommunity.sdk.ui.navigateToOctopusPost
import com.octopuscommunity.sdk.ui.octopusComposables
import com.octopuscommunity.sdk.ui.octopusDarkColorScheme
import com.octopuscommunity.sdk.ui.octopusLightColorScheme
import kotlinx.serialization.Serializable

@Serializable
data object MainScreen

class MainActivity : ComponentActivity() {

    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val navController = rememberNavController()
            val state by viewModel.state.collectAsStateWithLifecycle()
            MaterialTheme(
                colorScheme = if (isSystemInDarkTheme()) {
                    darkColorScheme(primary = Color(0xFF3AD9B1))
                } else {
                    lightColorScheme(primary = Color(0xFF068677))
                }
            ) {
                NavHost(
                    navController = navController,
                    startDestination = MainScreen
                ) {
                    composable<MainScreen> {
                        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                            viewModel.updateNotificationsCount()
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { navController.navigateToOctopusHome() },
                                ) {
                                    Text("Open Octopus")
                                }
                                if (state.unreadNotificationsCount > 0) {
                                    Badge(
                                        modifier = Modifier.align(Alignment.TopEnd),
                                        content = { Text(state.unreadNotificationsCount.toString()) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    state.bridgePost?.let { bridgePost ->
                                        navController.navigateToOctopusPost(postId = bridgePost.id)
                                    } ?: viewModel.createBridgePost()
                                }
                            ) {
                                when {
                                    state.isLoading -> CircularProgressIndicator(
                                        modifier = Modifier.height(16.dp)
                                    )

                                    else -> Text(text = "Open Bridge Post")
                                }
                            }
                            state.error?.let {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = it.asString(),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    octopusComposables(
                        navController = navController,
                        onNavigateToClientObject = {
                            Toast.makeText(
                                this@MainActivity,
                                "Opening client object $it",
                                Toast.LENGTH_SHORT
                            ).show()
                        }) { backStackEntry, content ->
                        OctopusTheme(
                            colorScheme = if (isSystemInDarkTheme()) {
                                octopusDarkColorScheme(
                                    primary = Color(0xFF3AD9B1),
                                    primaryLow = Color(0xFF083B2F),
                                    primaryHigh = Color(0xFFD8F4F1),
                                    onPrimary = Color(0xFF141414),
                                    background = Color(0xFF141414),
                                    onHover = Color(0xFF242526)
                                )
                            } else {
                                octopusLightColorScheme(
                                    primary = Color(0xFF068677),
                                    primaryLow = Color(0xFFD8F4F1),
                                    primaryHigh = Color(0xFF15D1A2),
                                    onPrimary = Color(0xFFFFFFFF),
                                    background = Color(0xFFFFFFFF),
                                    onHover = Color(0xFFFFFFFF)
                                )
                            },
                            typography = OctopusTypographyDefaults.typography(),
                            drawables = OctopusDrawablesDefaults.drawables(
                                logo = R.drawable.ic_logo
                            ),
                        ) {
                            content()
                        }
                    }
                }
            }
        }
    }
}