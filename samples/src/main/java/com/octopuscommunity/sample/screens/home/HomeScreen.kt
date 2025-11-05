package com.octopuscommunity.sample.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import coil3.compose.AsyncImage
import com.octopuscommunity.sample.MainViewModel
import com.octopuscommunity.sample.R
import com.octopuscommunity.sdk.ui.OctopusTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: MainViewModel.State,
    onUpdateNotificationsCount: () -> Unit,
    onLogin: () -> Unit,
    onEditUser: () -> Unit,
    onLogout: () -> Unit,
    onOpenCommunity: () -> Unit,
    onOpenBridgePost: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    LifecycleEventEffect(
        event = Lifecycle.Event.ON_RESUME,
        onEvent = onUpdateNotificationsCount
    )

    Scaffold(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        HomeContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            state = state,
            onLogin = onLogin,
            onEditUser = onEditUser,
            onLogout = onLogout,
            onOpenCommunity = onOpenCommunity,
            onOpenBridgePost = onOpenBridgePost,
            onOpenSettings = onOpenSettings
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    state: MainViewModel.State,
    onLogin: () -> Unit,
    onEditUser: () -> Unit,
    onLogout: () -> Unit,
    onOpenCommunity: () -> Unit,
    onOpenBridgePost: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenCommunity
            ) {
                Text("Open Community")
            }

            if (state.unreadNotificationsCount > 0) {
                Badge(
                    modifier = Modifier
                        .align(Alignment.TopEnd),
                    containerColor = OctopusTheme.colorScheme.alert,
                    contentColor = OctopusTheme.colorScheme.gray100,
                    content = {
                        Text(
                            modifier = Modifier.padding(2.dp),
                            text = when (state.unreadNotificationsCount) {
                                in 1..99 -> state.unreadNotificationsCount.toString()
                                else -> "+99"
                            }
                        )
                    }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 32.dp))

        val user = state.user
        if (user != null) {
            Text(
                text = "You are currently logged in as:",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "User Id: ${user.id ?: ""}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Nickname: ${user.nickname ?: ""}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Bio: ${user.bio ?: ""}",
                style = MaterialTheme.typography.bodyMedium
            )
            user.picture?.let { avatar ->
                Text(
                    text = "Avatar:",
                    style = MaterialTheme.typography.bodyMedium
                )
                AsyncImage(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape),
                    model = avatar.data,
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                )
            }
        } else {
            Text(
                text = "You are currently not logged in",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (user == null) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onLogin,
            ) {
                Text("Login")
            }
        } else {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onEditUser,
            ) {
                Text("Edit user")
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onLogout,
            ) {
                Text("Logout")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 32.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenBridgePost,
        ) {
            Text("Bridge Post")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 32.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenSettings,
        ) {
            Text("Settings")
        }
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    HomeScreen(
        state = MainViewModel.State(),
        onUpdateNotificationsCount = {},
        onLogin = {},
        onEditUser = {},
        onLogout = {},
        onOpenCommunity = {},
        onOpenBridgePost = {},
        onOpenSettings = {}
    )
}