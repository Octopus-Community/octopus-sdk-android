package com.octopuscommunity.sdk.sample.ui.console

import androidx.compose.runtime.Composable
import com.octopuscommunity.sdk.sample.data.model.DebugEntry
import com.octopuscommunity.sdk.sample.data.model.SDKConfig

@Composable
fun SendFeedbackSheet(
    where: String,
    config: SDKConfig?,
    debugLog: List<DebugEntry>,
    onDismiss: () -> Unit,
) = Unit
