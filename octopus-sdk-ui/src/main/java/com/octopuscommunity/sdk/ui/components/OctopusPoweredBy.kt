package com.octopuscommunity.sdk.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.domain.model.CommunityConfig
import com.octopuscommunity.sdk.domain.model.CommunityConfig.PoweredByOctopus
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.isDark
import com.octopuscommunity.sdk.ui.medium

@Composable
internal fun OctopusPoweredBy(
    communityConfig: CommunityConfig?,
    modifier: Modifier = Modifier,
) {
    val poweredByOctopus = communityConfig?.poweredByOctopus ?: PoweredByOctopus.Default
    when (poweredByOctopus) {
        PoweredByOctopus.Hidden -> {}

        is PoweredByOctopus.Custom,
        PoweredByOctopus.Default,
        -> {
            OctopusTooltip(text = "www.octopuscommunity.com") {
                val contentDescription = stringResource(R.string.accessibility_common_powered_by)
                Row(
                    modifier =
                        modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .semantics(mergeDescendants = true) {
                                this.contentDescription = contentDescription
                            },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    when (poweredByOctopus) {
                        is PoweredByOctopus.Custom -> {
                            OctopusImage(
                                modifier =
                                    Modifier
                                        .height(24.dp)
                                        .semantics { hideFromAccessibility() },
                                data =
                                    if (OctopusTheme.colorScheme.isDark) {
                                        poweredByOctopus.darkLogoUrl
                                    } else {
                                        poweredByOctopus.lightLogoUrl
                                    },
                                contentDescription = null,
                                crossFade = false,
                            )
                        }

                        is PoweredByOctopus.Default -> {
                            Text(
                                text = stringResource(R.string.common_powered_by),
                                color = OctopusTheme.colorScheme.gray500,
                                style = OctopusTheme.typography.caption1.medium,
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                modifier =
                                    Modifier
                                        .size(width = 70.dp, height = 24.dp)
                                        .semantics { hideFromAccessibility() },
                                painter = painterResource(R.drawable.ic_octopus_logo),
                                tint = OctopusTheme.colorScheme.gray500,
                                contentDescription = null,
                            )
                        }

                        PoweredByOctopus.Hidden -> {} // Already filtered above, exhaustive match
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OctopusPoweredByPreview() {
    OctopusTheme {
        OctopusPoweredBy(communityConfig = null, modifier = Modifier.fillMaxWidth())
    }
}
