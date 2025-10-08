package com.octopuscommunity.sample.screens

import androidx.navigation.NavHostController
import com.octopuscommunity.sample.MainViewModel

fun MainScreen(
    mainNavController: NavHostController,
    state: MainViewModel.State,
    onLogout: () -> Unit,
    onUpdateNotificationsCount: () -> Unit,
    onChangeCommunityAccess: (Boolean) -> Unit
) {
}