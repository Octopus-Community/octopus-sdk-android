package com.octopuscommunity.sdk.ui.groups.selection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.domain.model.OctopusGroup
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.components.OctopusModalBottomSheet
import com.octopuscommunity.sdk.ui.groups.list.GroupsList
import com.octopuscommunity.sdk.ui.semiBold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GroupSelectionSheet(
    selectedGroup: OctopusGroup?,
    visible: Boolean,
    onGroupSelected: (OctopusGroup) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )
    var showSheet by remember { mutableStateOf(visible) }

    LaunchedEffect(visible) {
        if (visible) {
            showSheet = true
        } else {
            if (sheetState.isVisible) sheetState.hide()
            showSheet = false
        }
    }

    if (showSheet) {
        OctopusModalBottomSheet(
            modifier = modifier.fillMaxHeight(),
            sheetState = sheetState,
            onDismissRequest = onDismiss,
        ) {
            Text(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(OctopusTheme.colorScheme.background)
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                text = stringResource(R.string.create_post_groups_picker_title),
                textAlign = TextAlign.Center,
                color = OctopusTheme.colorScheme.gray900,
                style = OctopusTheme.typography.body2.semiBold,
            )
            GroupsList(
                modifier = Modifier.fillMaxSize(),
                selectedGroupId = selectedGroup?.id,
                pullToRefresh = false,
                selectable = true,
                onGroupClick = onGroupSelected,
            )
        }
    }
}
