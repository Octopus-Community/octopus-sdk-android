package com.octopuscommunity.sdk.ui.notifications.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.octopuscommunity.sdk.domain.model.Notification
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModel
import com.octopuscommunity.sdk.ui.components.OctopusContent
import com.octopuscommunity.sdk.ui.components.OctopusPullToRefresh
import com.octopuscommunity.sdk.ui.components.OctopusScreenStateDefaults
import com.octopuscommunity.sdk.ui.components.octopusLoadingItem
import com.octopuscommunity.sdk.ui.components.octopusScreenStateFailureItem
import com.octopuscommunity.sdk.ui.components.octopusScreenStateItem
import com.octopuscommunity.sdk.ui.lineHeight125
import com.octopuscommunity.sdk.ui.medium
import com.octopuscommunity.sdk.ui.notifications.components.NotificationItem

@Composable
fun NotificationsList(
    modifier: Modifier = Modifier,
    pullToRefresh: Boolean = true,
) = OctopusContent {
    val viewModel = octopusViewModel<NotificationsListViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val inspectionMode = LocalInspectionMode.current

    val areSystemNotificationsEnabled =
        remember {
            if (inspectionMode) {
                true
            } else {
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }

    Box(
        modifier =
            modifier then
                Modifier
                    .fillMaxSize()
                    .background(OctopusTheme.colorScheme.background),
    ) {
        OctopusPullToRefresh(
            modifier =
                Modifier
                    .fillMaxSize(),
            refreshing = state.isRefreshing,
            enabled = pullToRefresh,
            onRefresh = viewModel::refresh,
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                if (areSystemNotificationsEnabled) {
                    item {
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = stringResource(R.string.notifications_list_enable_push_notifications),
                                color = OctopusTheme.colorScheme.gray900,
                                style = OctopusTheme.typography.body2.medium.lineHeight125,
                            )
                            val toggleIcons = OctopusTheme.icons.toggle
                            if (toggleIcons != null) {
                                Icon(
                                    modifier =
                                        Modifier
                                            .size(48.dp)
                                            .clickable { viewModel.togglePushNotification() },
                                    painter =
                                        if (state.isPushNotificationEnabled) {
                                            toggleIcons.on()
                                        } else {
                                            toggleIcons.off()
                                        },
                                    contentDescription = null,
                                )
                            } else {
                                Switch(
                                    modifier = Modifier.scale(0.85f),
                                    colors =
                                        SwitchDefaults.colors(
                                            checkedThumbColor = OctopusTheme.colorScheme.onPrimary,
                                            checkedTrackColor = OctopusTheme.colorScheme.primary,
                                            checkedIconColor = OctopusTheme.colorScheme.primary,
                                            uncheckedThumbColor = OctopusTheme.colorScheme.gray100,
                                            uncheckedTrackColor = OctopusTheme.colorScheme.gray300,
                                            uncheckedBorderColor = OctopusTheme.colorScheme.gray300,
                                            uncheckedIconColor = OctopusTheme.colorScheme.gray700,
                                        ),
                                    checked = state.isPushNotificationEnabled,
                                    onCheckedChange = { viewModel.togglePushNotification() },
                                )
                            }
                        }
                        HorizontalDivider(color = OctopusTheme.colorScheme.gray300)
                    }
                }
                when {
                    state.isLoading ->
                        octopusLoadingItem(
                            topPadding = OctopusScreenStateDefaults.ProfileLoaderOffset,
                        )

                    state.hasLoadFailure ->
                        octopusScreenStateFailureItem(
                            isNoNetwork = state.loadFailureIsNoNetwork,
                            verticalPadding = OctopusScreenStateDefaults.ProfilePadding,
                            onRetry = viewModel::retryFirstLoad,
                        )

                    state.notifications.isEmpty() ->
                        octopusScreenStateItem(
                            illustration = { OctopusTheme.icons.screenStates.emptyNotifications() },
                            text = R.string.profile_notifications_empty_state_title_self_formal,
                            verticalPadding = OctopusScreenStateDefaults.ProfilePadding,
                        )

                    else ->
                        items(
                            items = state.notifications,
                            key = Notification::id,
                            itemContent = { notification ->
                                // One root per entry so the row and its divider animate as a unit.
                                Column(modifier = Modifier.animateItem()) {
                                    NotificationItem(
                                        modifier = Modifier.fillMaxWidth(),
                                        notification = notification,
                                        onClick = { viewModel.navigateToNotificationItem(notification) },
                                        onProfileClick = { author ->
                                            viewModel.navigateToProfile(author)
                                        },
                                    )
                                    HorizontalDivider(color = OctopusTheme.colorScheme.gray100)
                                }
                            },
                        )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationsListPreview() {
    OctopusTheme {
        NotificationsList(modifier = Modifier.fillMaxSize(), pullToRefresh = false)
    }
}
