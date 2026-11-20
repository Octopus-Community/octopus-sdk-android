package com.octopuscommunity.sdk.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.medium

/**
 * Centered "you don't have access" message with an optional CTA button below it. Used by the
 * Post detail, Comment detail and Group detail screens when the connected user lands on a
 * non-accessible piece of content (typically reached via a deeplink or push notification).
 *
 * The CTA button is what surfaces the access-denied callback to the host app — taps trigger
 * [onCtaClick], which the calling screen forwards to its `onGroupAccessDenied` navigation
 * event with the right `groupId`. The button is hidden when [ctaText] is `null`, e.g. on a
 * locked comment whose parent group id cannot be resolved client-side.
 */
@Composable
internal fun OctopusAccessDenied(
    text: String,
    modifier: Modifier = Modifier,
    ctaText: String? = null,
    onCtaClick: () -> Unit = {}
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = text,
                textAlign = TextAlign.Center,
                style = OctopusTheme.typography.body1.medium,
                color = OctopusTheme.colorScheme.gray900
            )
            if (ctaText != null) {
                OctopusMediumOutlinedButton(
                    text = ctaText,
                    onClick = onCtaClick
                )
            }
        }
    }
}
