package com.octopuscommunity.sdk.ui.groups.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.components.OctopusButtonDefaults
import com.octopuscommunity.sdk.ui.components.OctopusMediumButton
import com.octopuscommunity.sdk.ui.components.OctopusMediumOutlinedButton

@Composable
internal fun FollowButton(
    isFollowed: Boolean,
    onFollowChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
) {
    val hapticFeedback = LocalHapticFeedback.current
    if (isFollowed) {
        OctopusMediumOutlinedButton(
            modifier = modifier,
            text = stringResource(R.string.group_action_unfollow),
            loading = loading,
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onFollowChanged(false)
            },
        )
    } else {
        OctopusMediumButton(
            modifier = modifier,
            text = stringResource(R.string.group_action_follow),
            loading = loading,
            colors = OctopusButtonDefaults.buttonColors(),
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onFollowChanged(true)
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FollowButtonNotFollowedPreview() {
    OctopusTheme {
        FollowButton(isFollowed = false, onFollowChanged = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun FollowButtonFollowedPreview() {
    OctopusTheme {
        FollowButton(isFollowed = true, onFollowChanged = {})
    }
}
