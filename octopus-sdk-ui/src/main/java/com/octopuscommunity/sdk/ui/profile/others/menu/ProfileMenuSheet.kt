package com.octopuscommunity.sdk.ui.profile.others.menu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.octopuscommunity.sdk.domain.model.Profile
import com.octopuscommunity.sdk.test.mock.MockProfiles
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModel
import com.octopuscommunity.sdk.ui.components.MenuItem
import com.octopuscommunity.sdk.ui.components.OctopusConfirmAlertDialog
import com.octopuscommunity.sdk.ui.components.OctopusContent
import com.octopuscommunity.sdk.ui.components.OctopusMenuSheet
import com.octopuscommunity.sdk.ui.profile.others.menu.ProfileMenuSheetViewModel.Event.OpenReport
import com.octopuscommunity.sdk.ui.report.common.ReportSheet
import com.octopuscommunity.sdk.ui.report.common.ReportTarget
import com.octopuscommunity.sdk.ui.report.common.userIdToBlockAfterReport

@Composable
fun ProfileMenuSheet(
    profile: Profile?,
    onDismiss: () -> Unit,
    onBlocked: () -> Unit,
    modifier: Modifier = Modifier,
) = OctopusContent {
    val viewModel = octopusViewModel<ProfileMenuSheetViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var reportTarget by remember { mutableStateOf<ReportTarget?>(null) }
    // Captured when the report opens (see OctopusItemMenuSheet): stable while the block runs.
    var reportBlockUserId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(profile) {
        viewModel.setProfile(profile)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is OpenReport -> {
                    reportBlockUserId = state.profile?.userIdToBlockAfterReport(state.currentUserProfile)
                    reportTarget = event.target
                }
            }
        }
    }

    val items =
        mutableListOf<MenuItem>().apply {
            state.profile?.let { profile ->
                if (state.canReport) {
                    this +=
                        MenuItem(
                            icon = OctopusTheme.icons.profile.report(),
                            title = stringResource(R.string.moderation_profile_button),
                            isDestructive = true,
                            onClick = { viewModel.reportUser() },
                        )
                }
                if (state.canBlock) {
                    this +=
                        MenuItem(
                            icon = OctopusTheme.icons.profile.blockUser(),
                            title = stringResource(R.string.block_profile_button),
                            isDestructive = true,
                            onClick = { viewModel.showConfirmBlock() },
                        )
                }
            }
        }

    if (reportTarget == null && items.isNotEmpty()) {
        OctopusMenuSheet(
            modifier = modifier,
            items = items,
            onDismissRequest = onDismiss,
        )
    }

    reportTarget?.let { target ->
        ReportSheet(
            target = target,
            blockUserId = reportBlockUserId,
            onBlocked = onBlocked,
            onDismissRequest = {
                reportTarget = null
                onDismiss()
            },
        )
    }

    if (state.showConfirmBlock) {
        OctopusConfirmAlertDialog(
            title = stringResource(R.string.block_profile_alert_title),
            text = stringResource(R.string.block_profile_alert_message_formal),
            confirmButtonText = stringResource(R.string.common_continue),
            dismissButtonText = stringResource(R.string.common_cancel),
            isLoading = state.isBlocking,
            error = state.blockError?.asString(),
            onConfirm = { viewModel.blockProfile(onSuccess = onBlocked) },
            onDismiss = { viewModel.showConfirmBlock(false) },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileMenuSheetPreview() {
    OctopusTheme {
        ProfileMenuSheet(
            profile = MockProfiles.default,
            onDismiss = {},
            onBlocked = {},
        )
    }
}
