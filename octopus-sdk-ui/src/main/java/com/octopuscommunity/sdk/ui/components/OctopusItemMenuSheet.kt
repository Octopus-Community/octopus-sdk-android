package com.octopuscommunity.sdk.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.octopuscommunity.sdk.domain.model.OctopusItem
import com.octopuscommunity.sdk.test.mock.MockPosts
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusItemMenuSheetViewModel
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusItemMenuSheetViewModel.Event.Dismiss
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusItemMenuSheetViewModel.Event.OpenReport
import com.octopuscommunity.sdk.ui.report.common.ReportSheet
import com.octopuscommunity.sdk.ui.report.common.ReportTarget
import com.octopuscommunity.sdk.ui.report.common.userIdToBlockAfterReport

@Composable
internal inline fun <reified T : OctopusItem> OctopusItemMenuSheet(
    viewModel: OctopusItemMenuSheetViewModel<T>,
    deleteText: String,
    reportText: String,
    confirmDeleteText: String,
    noinline onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    leadingItems: List<MenuItem> = emptyList(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var reportTarget by remember { mutableStateOf<ReportTarget?>(null) }
    // Captured when the report opens, so a profile refresh during the block cannot flip the
    // "Report sent" alert back to its OK-only form mid-request.
    var reportBlockUserId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                // Swallow Dismiss while the report sheet is open — the wrapper composable must
                // stay mounted so the report sheet survives; the caller's onDismiss runs later
                // when the report sheet dismisses (see `ReportSheet.onDismissRequest` below).
                Dismiss -> if (reportTarget == null) onDismiss()
                is OpenReport -> {
                    reportBlockUserId = state.item.userIdToBlockAfterReport(state.currentUserProfile)
                    reportTarget = event.target
                }
            }
        }
    }

    if (reportTarget == null) {
        OctopusItemMenuSheet(
            modifier = modifier,
            state = state,
            deleteText = deleteText,
            reportText = reportText,
            confirmDeleteText = confirmDeleteText,
            onShowConfirmDelete = { viewModel.showConfirmDelete(true) },
            onDismissConfirmDelete = { viewModel.showConfirmDelete(false) },
            onDelete = viewModel::deleteItem,
            onReportContent = viewModel::reportContent,
            onShowConfirmBlock = { viewModel.showConfirmBlock(true) },
            onDismissConfirmBlock = { viewModel.showConfirmBlock(false) },
            onBlockAuthor = viewModel::blockAuthor,
            onDismiss = onDismiss,
            leadingItems = leadingItems,
        )
    }

    reportTarget?.let { target ->
        ReportSheet(
            target = target,
            blockUserId = reportBlockUserId,
            onDismissRequest = {
                reportTarget = null
                onDismiss()
            },
        )
    }
}

@Composable
internal inline fun <reified T : OctopusItem> OctopusItemMenuSheet(
    state: OctopusItemMenuSheetViewModel.UiState<T>,
    deleteText: String,
    reportText: String,
    confirmDeleteText: String,
    noinline onShowConfirmDelete: () -> Unit,
    noinline onDismissConfirmDelete: () -> Unit,
    noinline onDelete: () -> Unit,
    noinline onReportContent: () -> Unit,
    noinline onShowConfirmBlock: () -> Unit,
    noinline onDismissConfirmBlock: () -> Unit,
    noinline onBlockAuthor: () -> Unit,
    noinline onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    // Non-destructive, item-specific entries prepended above Delete / Report / Block
    // (e.g. a post's "View group"). Empty for items that don't add any.
    leadingItems: List<MenuItem> = emptyList(),
) = with(state) {
    val items =
        mutableListOf<MenuItem>().apply {
            addAll(leadingItems)
            if (canDelete) {
                this +=
                    MenuItem(
                        icon = OctopusTheme.icons.content.delete(),
                        title = deleteText,
                        isDestructive = true,
                        onClick = onShowConfirmDelete,
                    )
            }
            if (canReport) {
                this +=
                    MenuItem(
                        icon = OctopusTheme.icons.content.report(),
                        title = reportText,
                        isDestructive = true,
                        onClick = onReportContent,
                    )
            }
            if (canBlock) {
                this +=
                    MenuItem(
                        icon = OctopusTheme.icons.profile.blockUser(),
                        title = stringResource(R.string.block_profile_button),
                        isDestructive = true,
                        onClick = onShowConfirmBlock,
                    )
            }
        }

    if (items.isNotEmpty()) {
        OctopusMenuSheet(
            modifier = modifier,
            items = items,
            onDismissRequest = onDismiss,
        )
    }

    if (showConfirmDelete) {
        OctopusConfirmAlertDialog(
            text = confirmDeleteText,
            confirmButtonText = stringResource(R.string.common_delete),
            dismissButtonText = stringResource(R.string.common_cancel),
            isLoading = isDeleting,
            error = deleteError?.asString(),
            onConfirm = onDelete,
            onDismiss = onDismissConfirmDelete,
        )
    }

    if (showConfirmBlock) {
        OctopusConfirmAlertDialog(
            title = stringResource(R.string.block_profile_alert_title),
            text = stringResource(R.string.block_profile_alert_message_formal),
            confirmButtonText = stringResource(R.string.common_continue),
            dismissButtonText = stringResource(R.string.common_cancel),
            isLoading = isBlocking,
            error = blockError?.asString(),
            onConfirm = onBlockAuthor,
            onDismiss = onDismissConfirmBlock,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OctopusItemMenuSheetPreview() {
    OctopusTheme {
        OctopusItemMenuSheet(
            state =
                OctopusItemMenuSheetViewModel.UiState(
                    item = MockPosts.default,
                ),
            deleteText = "Delete",
            reportText = "Report",
            confirmDeleteText = "Are you sure?",
            onShowConfirmDelete = {},
            onDismissConfirmDelete = {},
            onDelete = {},
            onReportContent = {},
            onShowConfirmBlock = {},
            onDismissConfirmBlock = {},
            onBlockAuthor = {},
            onDismiss = {},
        )
    }
}
