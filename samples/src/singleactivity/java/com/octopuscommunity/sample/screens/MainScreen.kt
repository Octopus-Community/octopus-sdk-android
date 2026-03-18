package com.octopuscommunity.sample.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.octopuscommunity.sample.MainViewModel

/**
 * Unused in the singleactivity flavor — the launcher is [com.octopuscommunity.sample.CommunityActivity].
 * This stub exists only because the shared [com.octopuscommunity.sample.MainActivity] references MainScreen.
 */
@Composable
fun MainScreen(
    mainNavController: NavHostController,
    state: MainViewModel.State,
    onLogout: () -> Unit,
    onChangeCommunityAccess: (Boolean) -> Unit
) {
    // No-op: singleactivity uses CommunityActivity instead
}
