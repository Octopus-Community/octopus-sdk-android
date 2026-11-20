package com.octopuscommunity.sdk.ui.report.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.common.uiText
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModel
import com.octopuscommunity.sdk.ui.components.OctopusAlertDialog
import com.octopuscommunity.sdk.ui.components.OctopusButton
import com.octopuscommunity.sdk.ui.components.OctopusConfirmAlertDialog
import com.octopuscommunity.sdk.ui.components.OctopusModalBottomSheet
import com.octopuscommunity.sdk.ui.lineHeight125
import com.octopuscommunity.sdk.ui.lineHeight150Trimmed
import com.octopuscommunity.sdk.ui.report.content.ReportContentViewModel
import com.octopuscommunity.sdk.ui.report.user.ReportUserViewModel
import com.octopuscommunity.sdk.ui.semiBold
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf

/**
 * Full-height moderation report sheet, mirroring the iOS report sheet.
 *
 * Hosted by the menu sheets (post / comment / reply / profile) when their VM emits
 * `Event.OpenReport(target)` — closes via system back, swipe-down, or after the success alert.
 * Modeled after `GroupSelectionSheet`: a `ModalBottomSheet` at full height, with a centered
 * heading bar at the top and no explicit cancel button.
 *
 * When [blockUserId] is set, the "Report sent" alert also offers to block that user (internal ticket):
 * **Block** blocks right away, closes this sheet and shows the existing "This user is blocked"
 * confirmation at screen level (it must outlive the blocked author's card, which hosts this sheet);
 * [onBlocked] runs when that confirmation is dismissed. A failed block also closes the sheet and
 * shows its error at screen level. **Not now** closes as OK used to. `null` keeps the single OK button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReportSheet(
    target: ReportTarget,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    blockUserId: String? = null,
    onBlocked: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    OctopusModalBottomSheet(
        modifier = modifier.fillMaxHeight(),
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
    ) {
        // Play the sheet hide animation before notifying the host so the success / OK path
        // doesn't snap-close. Swipe-down and system back already go through Material 3's native
        // dismissal animation via `onDismissRequest`, so they're handled directly.
        ReportSheetContent(
            target = target,
            blockUserId = blockUserId,
            onBlocked = onBlocked,
            onDone = {
                scope.launch {
                    sheetState.hide()
                    onDismissRequest()
                }
            },
        )
    }
}

@Composable
private fun ReportSheetContent(
    target: ReportTarget,
    blockUserId: String?,
    onBlocked: () -> Unit,
    onDone: () -> Unit,
) {
    val viewModel: ReportViewModel =
        when (target) {
            is ReportTarget.Content ->
                octopusViewModel<ReportContentViewModel>(
                    key = "reportContent_${target.contentId}_${target.contentKind}",
                    parameters = { parametersOf(target.contentId, target.contentKind) },
                )

            is ReportTarget.Profile ->
                octopusViewModel<ReportUserViewModel>(
                    key = "reportProfile_${target.profileId}",
                    parameters = { parametersOf(target.profileId) },
                )
        }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showSentDialog by rememberSaveable { mutableStateOf(false) }
    val currentOnDone by rememberUpdatedState(onDone)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ReportViewModel.Event.Sent -> showSentDialog = true

                ReportViewModel.Event.Blocked, is ReportViewModel.Event.BlockFailed -> {
                    // Both outcome dialogs already live on the navigator and survive this sheet.
                    showSentDialog = false
                    currentOnDone()
                }
            }
        }
    }

    val explanationResId =
        when (target) {
            is ReportTarget.Content -> R.string.moderation_content_explanation_formal
            is ReportTarget.Profile -> R.string.moderation_profile_explanation_formal
        }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Heading bar (iso GroupSelectionSheet)
        Text(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(OctopusTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 13.dp),
            text = stringResource(R.string.moderation_common_title),
            textAlign = TextAlign.Center,
            color = OctopusTheme.colorScheme.gray900,
            style = OctopusTheme.typography.body2.semiBold,
        )

        // Heading + caption block (Figma frame 15003:1614, height 113dp, padding x16 y16/12)
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(explanationResId),
                color = OctopusTheme.colorScheme.gray900,
                style = OctopusTheme.typography.body1.semiBold.lineHeight150Trimmed,
            )
            Text(
                text = stringResource(R.string.moderation_common_caption_formal),
                color = OctopusTheme.colorScheme.gray500,
                style = OctopusTheme.typography.caption1,
            )
        }

        // Reasons list (Figma frame 15003:1616) — flat, scrollable, fills the available space
        // between the caption and the pinned CTA so the button stays docked at the bottom even
        // when the reasons list is shorter than the screen (iso iOS internal change ContentView: the
        // ScrollView is the weighted sibling of the Continue Button inside the outer VStack).
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
        ) {
            state.reasons.forEach { reason ->
                ReasonRow(
                    text = reason.uiText.asString(),
                    selected = state.selectedReasons.contains(reason),
                    onClick = { viewModel.toggleSelectedReason(reason) },
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // CTA pinned bottom
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            OctopusButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.common_continue),
                enabled = state.canSend,
                loading = state.isSending,
                onClick = viewModel::send,
            )
        }
    }

    if (showSentDialog) {
        ReportSentDialog(
            blockUserId = blockUserId,
            isBlocking = state.isBlocking,
            onBlock = { userId -> viewModel.blockUser(userId, onBlocked = onBlocked) },
            onDismiss = {
                // Not now is ignored until the block request settles.
                if (!state.isBlocking) {
                    showSentDialog = false
                    onDone()
                }
            },
        )
    }

    state.error?.let { error ->
        ReportBlockErrorDialog(text = error.asString(), onDismiss = viewModel::dismissError)
    }
}

@Composable
internal fun ReportSentDialog(
    blockUserId: String?,
    isBlocking: Boolean,
    onBlock: (userId: String) -> Unit,
    onDismiss: () -> Unit,
) {
    if (blockUserId == null) {
        OctopusAlertDialog(
            title = { Text(stringResource(R.string.moderation_common_done_title)) },
            text = {
                Text(
                    text = stringResource(R.string.moderation_common_done_message_formal),
                    color = OctopusTheme.colorScheme.gray900,
                    style = OctopusTheme.typography.body2.lineHeight125,
                )
            },
            onDismiss = onDismiss,
        )
    } else {
        // Block is the destructive (alert-colored) confirm button; no "Block this user" alert
        // follows, the question below already states the consequence.
        OctopusConfirmAlertDialog(
            title = stringResource(R.string.moderation_common_done_title),
            text =
                stringResource(R.string.moderation_common_done_message_formal) + "\n\n" +
                    stringResource(R.string.moderation_common_done_block_question_formal),
            confirmButtonText = stringResource(R.string.block_profile_button),
            dismissButtonText = stringResource(R.string.common_not_now),
            isLoading = isBlocking,
            onConfirm = { onBlock(blockUserId) },
            onDismiss = onDismiss,
        )
    }
}

/** Shared by the report error call site and its screenshot fixture. */
@Composable
internal fun ReportBlockErrorDialog(
    text: String,
    onDismiss: () -> Unit,
) {
    OctopusAlertDialog(text = { Text(text) }, onDismiss = onDismiss)
}

@Composable
private fun ReasonRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    // Figma frame 15003:1617 — gap 10, padding x16 y12, icon 24, text body2.lineHeight150Trimmed
    // selectable(role = RadioButton) exposes the selected state via the a11y tree so TalkBack
    // announces "selected"/"not selected" — clickable alone would not propagate state.
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            painter =
                if (selected) {
                    painterResource(R.drawable.ic_octopus_report_choice_on)
                } else {
                    painterResource(R.drawable.ic_octopus_report_choice_off)
                },
            tint =
                if (selected) {
                    OctopusTheme.colorScheme.primary
                } else {
                    OctopusTheme.colorScheme.gray300
                },
            contentDescription = null,
        )
        Text(
            modifier = Modifier.weight(1f),
            text = text,
            color = OctopusTheme.colorScheme.gray900,
            style = OctopusTheme.typography.body2.lineHeight150Trimmed,
        )
    }
}
