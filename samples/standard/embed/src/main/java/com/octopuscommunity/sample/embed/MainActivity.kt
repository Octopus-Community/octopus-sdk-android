package com.octopuscommunity.sample.embed

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.home.OctopusHomeContent
import com.octopuscommunity.sdk.ui.octopusDrawables
import com.octopuscommunity.sdk.ui.octopusNavigation
import kotlinx.serialization.Serializable

@Serializable
data object MainScreen

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
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
                        Scaffold(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .systemBarsPadding(),
                            topBar = {
                                TopAppBar(
                                    title = {
                                        Text("My Community")
                                    }
                                )
                            }
                        ) { innerPadding ->
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    text = "Welcome to my Community",
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                CommunityTheme {
                                    OctopusHomeContent(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        navController = navController
                                    )
                                }
                            }
                        }
                    }
                    octopusNavigation(
                        navController = navController,
                        container = { CommunityTheme(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun CommunityTheme(content: @Composable () -> Unit) = OctopusTheme(
    drawables = octopusDrawables(logo = R.drawable.ic_logo),
    content = content
)