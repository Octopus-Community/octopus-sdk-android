package com.octopuscommunity.sdk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.OctopusDialogState
import com.octopuscommunity.sdk.ui.common.UiText
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusViewModel
import com.octopuscommunity.sdk.ui.lineHeight125
import com.octopuscommunity.sdk.ui.semiBold

@Composable
internal fun OctopusAlertDialog(viewModel: OctopusViewModel) {
    val state by viewModel.alertState.collectAsStateWithLifecycle()

    OctopusAlertDialog(state)
}

@Composable
internal fun OctopusAlertDialog(state: OctopusDialogState?) {
    when (val dialogState = state) {
        is OctopusDialogState.Alert ->
            OctopusAlertDialog(
                title = dialogState.title,
                text = dialogState.text,
                onDismiss = dialogState.onDismiss,
            )

        is OctopusDialogState.Confirm ->
            OctopusConfirmAlertDialog(
                title = dialogState.title?.asString(),
                text = dialogState.text.asString(),
                confirmButtonText = dialogState.confirmButtonText.asString(),
                dismissButtonText = dialogState.dismissButtonText.asString(),
                onConfirm = dialogState.onConfirm,
                onDismiss = dialogState.onDismiss,
            )

        is OctopusDialogState.Loading ->
            OctopusLoadingDialog(
                canDismiss = dialogState.canDismiss,
                onDismiss = dialogState.onDismiss,
            )

        null -> {}
    }
}

@Composable
internal fun OctopusAlertDialog(
    text: UiText,
    title: UiText? = null,
    onDismiss: () -> Unit = {},
) = OctopusAlertDialog(
    title = title?.let { { Text(it.asString()) } },
    text = { Text(text.asString()) },
    onDismiss = onDismiss,
)

@Composable
internal fun OctopusAlertDialog(
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    onDismiss: () -> Unit = {},
) = OctopusPreviewableAlertDialog(
    // AlertDialog creates a separate window, so we wrap content lambdas with OctopusLocaleWrapper
    title = title?.let { { OctopusLocaleWrapper { it() } } },
    text = text?.let { { OctopusLocaleWrapper { it() } } },
    confirmButton = {
        OctopusLocaleWrapper {
            OctopusTextButton(
                text = stringResource(R.string.common_ok),
                textStyle = OctopusTheme.typography.body2.semiBold.lineHeight125,
                color = OctopusTheme.colorScheme.gray900,
                onClick = onDismiss,
            )
        }
    },
    onDismissRequest = onDismiss,
)

@Composable
internal fun OctopusConfirmAlertDialog(
    text: String,
    confirmButtonText: String = stringResource(R.string.common_ok),
    dismissButtonText: String = stringResource(R.string.common_cancel),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    title: String? = null,
    error: String? = null,
    isLoading: Boolean = false,
) = OctopusPreviewableAlertDialog(
    // AlertDialog creates a separate window, so we wrap content lambdas with OctopusLocaleWrapper
    title = title?.let { { OctopusLocaleWrapper { Text(it) } } },
    text = {
        OctopusLocaleWrapper {
            Column {
                Text(
                    text = text,
                    color = OctopusTheme.colorScheme.gray900,
                    style = OctopusTheme.typography.body2.lineHeight125,
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = error,
                        color = OctopusTheme.colorScheme.alert,
                        style = OctopusTheme.typography.caption2,
                    )
                }
            }
        }
    },
    confirmButton = {
        OctopusLocaleWrapper {
            OctopusTextButton(
                text = confirmButtonText,
                enabled = !isLoading,
                loading = isLoading,
                textStyle = OctopusTheme.typography.body2.semiBold.lineHeight125,
                color = OctopusTheme.colorScheme.alert,
                horizontalSpacing = 8.dp,
                onClick = { if (!isLoading) onConfirm() },
            )
        }
    },
    dismissButton = {
        OctopusLocaleWrapper {
            OctopusTextButton(
                text = dismissButtonText,
                textStyle = OctopusTheme.typography.body2.semiBold.lineHeight125,
                color = OctopusTheme.colorScheme.gray900,
                enabled = !isLoading,
                onClick = { if (!isLoading) onDismiss() },
            )
        }
    },
    containerColor = OctopusTheme.colorScheme.onHover,
    onDismissRequest = { if (!isLoading) onDismiss() },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OctopusLoadingDialog(
    canDismiss: Boolean = false,
    onDismiss: () -> Unit = {},
) = BasicAlertDialog(
    modifier =
        Modifier
            .size(96.dp)
            .background(color = OctopusTheme.colorScheme.onHover),
    onDismissRequest = onDismiss,
    properties =
        DialogProperties(
            dismissOnBackPress = canDismiss,
            dismissOnClickOutside = canDismiss,
        ),
) {
    // BasicAlertDialog creates a separate window, so we wrap content with OctopusLocaleWrapper
    OctopusLocaleWrapper {
        OctopusProgressIndicator(
            Modifier
                .size(64.dp)
                .padding(32.dp),
        )
    }
}
