package com.octopuscommunity.sdk.ui.access

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.bold
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.components.OctopusModalBottomSheet
import kotlinx.coroutines.launch

/** The sheet that tells a banned member which policy was broken. Closes by its button, a swipe down or a tap outside. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BanPolicySheet(
    policy: CommunityAccessDeniedViewModel.UiState.Policy,
    onDismissRequest: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    OctopusModalBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState) {
        BanPolicySheetContent(
            policy = policy,
            // The sheet slides away before it leaves the composition.
            onClose = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() } },
        )
    }
}

/** Stateless body of [BanPolicySheet]: previews and screenshot tests render it without the sheet window. */
@Composable
internal fun BanPolicySheetContent(
    policy: CommunityAccessDeniedViewModel.UiState.Policy,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth().semantics { heading() },
            text = policy.name,
            color = OctopusTheme.colorScheme.gray900,
            style =
                OctopusTheme.typography.body1.bold
                    .copy(lineHeight = 24.sp, textDirection = TextDirection.Content),
        )
        // The text of the policy as the community wrote it, line breaks included, in the direction of its own
        // script; it scrolls, the button stays.
        Text(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
            text = policy.description,
            color = OctopusTheme.colorScheme.gray700,
            style = OctopusTheme.typography.caption1.copy(lineHeight = 20.sp, textDirection = TextDirection.Content),
        )
        AccessDeniedButton(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = stringResource(R.string.common_close),
            onClick = onClose,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BanPolicySheetContentPreview() {
    OctopusTheme {
        BanPolicySheetContent(
            policy =
                CommunityAccessDeniedViewModel.UiState.Policy(
                    name = "Job applications",
                    description = "Job applications are not allowed in the community.\n\nAsk for advice instead.",
                ),
            onClose = {},
        )
    }
}
