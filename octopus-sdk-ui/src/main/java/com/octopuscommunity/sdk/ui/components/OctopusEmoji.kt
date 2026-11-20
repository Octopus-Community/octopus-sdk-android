package com.octopuscommunity.sdk.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.octopuscommunity.sdk.domain.model.OctopusItem.Reaction.Kind.Heart
import com.octopuscommunity.sdk.ui.OctopusTheme

@Composable
fun OctopusEmoji(
    unicode: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(3.dp)
    ) {
        BasicText(
            modifier = Modifier
                .fillMaxSize(),
            text = unicode,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 2.sp,
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OctopusEmojiPreview() {
    OctopusTheme {
        OctopusEmoji(unicode = Heart.unicode)
    }
}