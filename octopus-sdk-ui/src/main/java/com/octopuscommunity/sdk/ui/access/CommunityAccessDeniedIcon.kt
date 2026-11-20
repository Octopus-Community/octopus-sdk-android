package com.octopuscommunity.sdk.ui.access

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R

/**
 * The two speech bubbles shown above a denied access. Three layers of one drawing, each in a colour of the theme,
 * so that it follows the host's primary colour in light and dark. Decorative, and not one of the icons a host
 * can replace.
 */
@Composable
internal fun CommunityAccessDeniedIcon(modifier: Modifier = Modifier) {
    Box(modifier = modifier.width(60.dp).aspectRatio(ICON_RATIO)) {
        // The outline of the left bubble, under its inside.
        Icon(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(R.drawable.ic_octopus_access_denied_outline),
            contentDescription = null,
            tint = OctopusTheme.colorScheme.gray900,
        )
        // The right bubble, and the three dots of the left one.
        Icon(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(R.drawable.ic_octopus_access_denied_primary),
            contentDescription = null,
            tint = OctopusTheme.colorScheme.primary,
        )
        // The inside of the left bubble, and the two dots of the right one.
        Icon(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(R.drawable.ic_octopus_access_denied_fill),
            contentDescription = null,
            tint = OctopusTheme.colorScheme.background,
        )
    }
}

/** Width over height of the drawing. */
private const val ICON_RATIO = 355f / 221f

@Preview(showBackground = true)
@Composable
private fun CommunityAccessDeniedIconPreview() {
    OctopusTheme { CommunityAccessDeniedIcon() }
}
