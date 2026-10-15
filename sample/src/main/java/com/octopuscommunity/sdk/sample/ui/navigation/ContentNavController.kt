package com.octopuscommunity.sdk.sample.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

/**
 * Use Navigation Compose's complete navigator registry and saver through its public API.
 * The application context prevents automatic Activity-intent handling: the host delivers links
 * explicitly after choosing the ready SDK's presentation, while child graphs receive only relays.
 */
@Composable
internal fun rememberContentNavController(): NavHostController {
    val context = LocalContext.current.applicationContext
    lateinit var controller: NavHostController
    CompositionLocalProvider(LocalContext provides context) { controller = rememberNavController() }
    return controller
}
