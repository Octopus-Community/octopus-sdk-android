package com.octopuscommunity.sdk.ui.profile.current.avatar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModel
import com.octopuscommunity.sdk.ui.common.viewmodel.unifiedProfileActive
import com.octopuscommunity.sdk.ui.components.LocalNavigationHandler
import com.octopuscommunity.sdk.ui.components.OctopusContent
import com.octopuscommunity.sdk.ui.medium
import com.octopuscommunity.sdk.ui.profile.components.ProfileAvatar

@Composable
fun CurrentUserProfileAvatar(
    modifier: Modifier = Modifier,
    showNotificationBadge: Boolean = false,
) = OctopusContent {
    val viewModel = octopusViewModel<CurrentUserProfileAvatarViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val profile = state.profile
    val communityConfig by viewModel.communityConfig.collectAsStateWithLifecycle()
    // Use the same flag + callback gate as profile taps: active opens Activity,
    // inactive opens the native profile summary.
    val unifiedProfileEnabled =
        unifiedProfileActive(
            exposeClientUserId = communityConfig?.exposeClientUserId == true,
            onNavigateToProfileWired = LocalNavigationHandler.current?.onNavigateToProfile != null,
        )

    Box {
        if (unifiedProfileEnabled) {
            // Unified Profile (internal ticket): the home floating button opens the community Activity
            // screen, so it shows a community-activity bell (client-overridable via
            // OctopusIcons.activityButton) instead of the connected user's avatar.
            Box(
                modifier =
                    modifier
                        .clip(CircleShape)
                        .background(OctopusTheme.colorScheme.primary)
                        .clickable(onClick = { viewModel.viewCurrentUserProfile(unifiedProfileEnabled = true) }),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    modifier = Modifier.fillMaxSize(0.6f),
                    painter = OctopusTheme.icons.activityButton(),
                    contentDescription = stringResource(R.string.activity_screen_title),
                    tint = OctopusTheme.colorScheme.onPrimary,
                )
            }
        } else {
            ProfileAvatar(
                modifier = modifier,
                profile = profile,
                onClick = { viewModel.viewCurrentUserProfile(unifiedProfileEnabled = false) },
            )
        }

        val unreadNotificationsCount = profile?.notSeenNotificationsCount ?: 0
        if (showNotificationBadge && unreadNotificationsCount > 0) {
            Badge(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd),
                containerColor = OctopusTheme.colorScheme.alert,
                contentColor = OctopusTheme.colorScheme.gray100,
                content = {
                    Text(
                        modifier = Modifier.padding(2.dp),
                        text =
                            when (unreadNotificationsCount) {
                                in 1..99 -> unreadNotificationsCount.toString()
                                else -> "+99"
                            },
                        style = OctopusTheme.typography.caption2.medium,
                    )
                },
            )
        }
    }
}

@Preview
@Composable
private fun CurrentUserProfileAvatarPreview() {
    OctopusTheme {
        CurrentUserProfileAvatar(
            modifier = Modifier.size(56.dp),
        )
    }
}
