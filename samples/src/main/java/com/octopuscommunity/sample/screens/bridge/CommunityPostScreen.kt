package com.octopuscommunity.sample.screens.bridge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
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
import com.octopuscommunity.sdk.test.mock.MockPosts
import com.octopuscommunity.sdk.ui.posts.details.OctopusPostDetailsContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityPostScreen(
    navController: NavHostController,
    octopusPostId: String,
    onLogin: () -> Unit,
    onEditUser: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text("Community Post") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        CommunityPostContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            navController = navController,
            postId = octopusPostId,
            onLogin = onLogin,
            onEditUser = onEditUser
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityPostContent(
    navController: NavHostController,
    postId: String,
    onLogin: () -> Unit,
    onEditUser: () -> Unit,
    modifier: Modifier = Modifier
) {
    CommunityTheme {
        OctopusPostDetailsContent(
            modifier = modifier,
            navController = navController,
            postId = postId,
            onNavigateToLogin = onLogin,
            onNavigateToProfileEdit = { profileField ->
                onEditUser()
            }
        )
    }
}

@Preview
@Composable
private fun CommunityPostScreenPreview() {
    CommunityPostScreen(
        navController = rememberNavController(),
        octopusPostId = MockPosts.default.id,
        onLogin = {},
        onEditUser = {},
        onBack = {}
    )
}