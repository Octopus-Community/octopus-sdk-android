package com.octopuscommunity.sample.sso.appprofile.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sample.sso.appprofile.data.AppUser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    appUser: AppUser?,
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
                title = { Text("My Application") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
private fun MainScreenPreview() {
    MainScreen(
        appUser = null,
        onLogin = {},
        onEditUser = {},
        onLogout = {},
        onOpenOctopus = {}
    )
}