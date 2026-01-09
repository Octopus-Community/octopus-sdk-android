package com.octopuscommunity.sample.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sample.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: MainViewModel.State,
    backButton: Boolean,
    onChangeCommunityAccess: (hasAccess: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    Scaffold(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
        SettingsContent(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            state = state,
            onChangeCommunityAccess = onChangeCommunityAccess
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    state: MainViewModel.State,
    onChangeCommunityAccess: (hasAccess: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "A/B Tests",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold
            )
        )

        Text(
            text = "Override Community Access",
            style = MaterialTheme.typography.titleSmall
        )

        Text("The following switch will permanently override the internal cohort attribution for the current user.")

        Spacer(modifier = Modifier.height(16.dp))
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth(),
        ) {
            SegmentedButton(
                selected = !state.hasAccessToCommunity,
                shape = SegmentedButtonDefaults.itemShape(
                    index = 0,
                    count = 2
                ),
                enabled = !state.isUpdatingCommunityAccess,
                onClick = { onChangeCommunityAccess(false) }
            ) {
                Text("Disabled")
            }
            SegmentedButton(
                selected = state.hasAccessToCommunity,
                shape = SegmentedButtonDefaults.itemShape(
                    index = 1,
                    count = 2
                ),
                enabled = !state.isUpdatingCommunityAccess,
                onClick = { onChangeCommunityAccess(true) }
            ) {
                Text("Enabled")
            }
        }

        HorizontalDivider(Modifier.padding(top = 16.dp, bottom = 8.dp))
    }
}

@Preview
@Composable
private fun SettingsScreenPreview() {
    SettingsScreen(
        state = MainViewModel.State(),
        backButton = false,
        onChangeCommunityAccess = {}
    )
}