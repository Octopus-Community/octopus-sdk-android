package com.octopuscommunity.sdk.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.ui.OctopusTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OctopusModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable ColumnScope.() -> Unit
) = ModalBottomSheet(
    modifier = modifier,
    onDismissRequest = onDismissRequest,
    sheetState = sheetState,
    dragHandle = {
        BottomSheetDefaults.DragHandle(
            color = OctopusTheme.colorScheme.gray300
        )
    },
    containerColor = OctopusTheme.colorScheme.background,
    contentColor = OctopusTheme.colorScheme.gray900,
) {
    OctopusLocaleWrapper {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun OctopusModalBottomSheetPreview() {
    OctopusTheme {
        OctopusModalBottomSheet(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            onDismissRequest = {}
        ) {}
    }
}