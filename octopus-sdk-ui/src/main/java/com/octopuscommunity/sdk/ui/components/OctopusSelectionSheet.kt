package com.octopuscommunity.sdk.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.domain.model.CommunityConfig
import com.octopuscommunity.sdk.test.mock.MockTopics
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.semiBold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> OctopusSelectionSheet(
    title: String,
    items: List<T>,
    itemText: (T) -> String,
    selectedItem: T?,
    communityConfig: CommunityConfig?,
    onItemSelected: (T) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = false
) {
    val sheetState = rememberModalBottomSheetState()
    var showSheet by remember { mutableStateOf(visible) }

    LaunchedEffect(visible) {
        if (visible) {
            showSheet = true
            sheetState.show()
        } else {
            showSheet = false
            sheetState.hide()
        }
    }

    if (showSheet) {
        OctopusModalBottomSheet(
            modifier = modifier,
            sheetState = sheetState,
            onDismissRequest = onDismissRequest
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = title,
                    textAlign = TextAlign.Center,
                    color = OctopusTheme.colorScheme.gray900,
                    style = OctopusTheme.typography.body2.semiBold
                )
                Spacer(modifier = Modifier.height(16.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        space = 8.dp,
                        alignment = Alignment.CenterHorizontally
                    ),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items.forEach { item ->
                        OctopusBadge(
                            text = itemText(item),
                            selected = item == selectedItem,
                            onClick = { onItemSelected(item) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OctopusPoweredBy(communityConfig = communityConfig)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OctopusSelectionSheetPreview() {
    OctopusTheme {
        OctopusSelectionSheet(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            items = MockTopics.all,
            selectedItem = MockTopics.default,
            title = "Choose a topic",
            itemText = { it.name },
            visible = true,
            communityConfig = null,
            onItemSelected = {},
            onDismissRequest = {}
        )
    }
}