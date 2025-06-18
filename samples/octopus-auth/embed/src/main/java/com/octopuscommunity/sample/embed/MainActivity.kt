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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.octopuscommunity.sdk.ui.OctopusDrawablesDefaults
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.home.OctopusHomeContent
import com.octopuscommunity.sdk.ui.octopusComposables
import com.octopuscommunity.sdk.ui.octopusDarkColorScheme
import com.octopuscommunity.sdk.ui.octopusLightColorScheme
import kotlinx.serialization.Serializable

@Serializable
data object MainScreen

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
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
                            OctopusContainer {
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
                octopusComposables(navController) { backStackEntry, content ->
                    OctopusContainer {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
fun OctopusContainer(content: @Composable () -> Unit) {
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
        drawables = OctopusDrawablesDefaults.drawables(
            logo = painterResource(id = R.drawable.ic_logo)
        )
    ) {
        content()
    }
}
