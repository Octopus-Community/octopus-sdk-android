package com.octopuscommunity.sdk.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.medium

/**
 * In-screen empty state shown when a piece of content is not available — the post or
 * comment was deleted server-side, was moderated, or the user landed on it through a
 * deeplink targeting a group they cannot access (PRD Rule 5 / internal ticket `lockedOther`
 * case). Mirrors the iOS `postNotAvailable` / `commentNotAvailable` empty state surfaced
 * in `PostDetailView.swift` and `CommentDetailView.swift` (internal change commits d655011 and
 * 95bb3b6).
 */
@Composable
internal fun OctopusContentNotAvailable(
    icon: Painter,
    text: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 60.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            modifier = Modifier.size(80.dp),
            painter = icon,
            contentDescription = null
        )
        Text(
            modifier = Modifier.padding(top = 16.dp),
            text = text,
            textAlign = TextAlign.Center,
            style = OctopusTheme.typography.body1.medium,
            color = OctopusTheme.colorScheme.gray700
        )
    }
}
