package com.octopuscommunity.sample.sso.hybrid.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.octopuscommunity.sample.sso.hybrid.R
import com.octopuscommunity.sample.sso.hybrid.data.AppUser
import com.octopuscommunity.sdk.domain.model.ClientUser.Profile.AgeInformation
import com.octopuscommunity.sdk.domain.model.Image
import com.octopuscommunity.sdk.domain.model.ProfileField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SSOMainScreen(
    appUser: AppUser?,
    appManagedFields: Set<ProfileField>,
    onLogin: () -> Unit,
    onEditUser: () -> Unit,
    onLogout: () -> Unit,
    onOpenOctopus: () -> Unit
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
        topBar = {
            TopAppBar(
                title = {
                    Image(
                        modifier = Modifier.height(32.dp),
                        painter = painterResource(R.drawable.ic_logo),
                        contentDescription = "Logo"
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "SSO Mode - Hybrid Profile",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Some profile fields are managed by your app. This means these fields are the ones " +
                            "that will be used in the community. It also means that Octopus Community won't moderate those  " +
                            "fields (nickname, bio, or picture profile). It also means that, if the nickname is part of " +
                            "these fields, you have to ensure that it is unique.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "In order to test this scenario:\n" +
                            "- your community should be configured to use SSO authentication\n" +
                            "- your community should be configured to have some app managed fields\n" +
                            "- you must set those fields in the OctopusSDK.initialize() function",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Red
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (appUser != null) {
                Text(
                    text = "You are currently logged in as:",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "User Id: ${appUser.id ?: ""}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Nickname: ${appUser.nickname ?: ""}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Bio: ${appUser.bio ?: ""}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Age Information: ${
                        when (appUser.ageInformation) {
                            AgeInformation.LegalAgeReached -> ">= 16"
                            AgeInformation.Underage -> "< 16>"
                            null -> "Not checked"
                        }
                    }",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Avatar:",
                    style = MaterialTheme.typography.bodyMedium
                )
                AsyncImage(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape),
                    model = when (val avatar = appUser.avatar) {
                        is Image.Remote -> avatar.url
                        is Image.Local -> avatar.uri
                        else -> null
                    },
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = "You are currently not logged in",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (appUser == null) {
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
                    onClick = {
                        onLogout()
                    },
                ) {
                    Text("Logout")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenOctopus
            ) {
                Text("Open Octopus")
            }
        }
    }
}

@Preview
@Composable
private fun SSOMainScreenPreview() {
    SSOMainScreen(
        appManagedFields = emptySet(),
        appUser = null,
        onLogin = {},
        onEditUser = {},
        onLogout = {},
        onOpenOctopus = {}
    )
}