package com.octopuscommunity.sample.screens.community

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.octopuscommunity.sample.theme.CommunityTheme
import com.octopuscommunity.sdk.ui.home.OctopusHomeContent
import com.octopuscommunity.sdk.ui.home.OctopusHomeDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    navController: NavHostController,
    backButton: Boolean,
    onLogin: () -> Unit,
    onEditUser: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    Scaffold(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background),
        contentWindowInsets = WindowInsets(),
        topBar = {
            TopAppBar(
                title = { Text("Community") },
                navigationIcon = {
                    if (backButton) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        CommunityContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            navController = navController,
            onLogin = onLogin,
            onEditUser = onEditUser
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityContent(
    navController: NavHostController,
    onLogin: () -> Unit,
    onEditUser: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = OctopusHomeDefaults.contentPadding(),
) {
    CommunityTheme {
        OctopusHomeContent(
            modifier = modifier,
            navController = navController,
            contentPadding = contentPadding,
            onNavigateToLogin = onLogin,
            onNavigateToProfileEdit = { profileField ->
                onEditUser()
            }
        )
    }
}

@Preview
@Composable
private fun CommunityScreenPreview() {
    CommunityScreen(
        navController = rememberNavController(),
        backButton = true,
        onLogin = {},
        onEditUser = {},
        onBack = {}
    )
}