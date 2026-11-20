package com.octopuscommunity.sdk.ui.notifications.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.material3.RichText
import com.octopuscommunity.sdk.domain.model.Author
import com.octopuscommunity.sdk.domain.model.Notification
import com.octopuscommunity.sdk.test.mock.MockNotifications
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.lineHeight125
import com.octopuscommunity.sdk.ui.profile.components.ProfileAvatar
import com.octopuscommunity.sdk.ui.utils.getElapsedTime

@Composable
fun NotificationItem(
    notification: Notification,
    onProfileClick: ((Author) -> Unit),
    onClick: (() -> Unit),
    modifier: Modifier = Modifier,
) = with(notification) {
    Column(
        modifier =
            modifier
                .background(
                    if (notification.isRead) {
                        Color.Transparent
                    } else {
                        OctopusTheme.colorScheme.primaryLow
                    },
                ).clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (notification.authors.size) {
                0 -> {}

                1 ->
                    ProfileAvatar(
                        modifier = Modifier.size(48.dp),
                        profile = notification.authors[0],
                        onClick = { onProfileClick(notification.authors[0]) },
                    )

                else ->
                    Box(modifier = Modifier.size(48.dp)) {
                        ProfileAvatar(
                            modifier =
                                Modifier
                                    .size(32.dp)
                                    .align(Alignment.TopStart),
                            profile = notification.authors[0],
                            onClick = { onProfileClick(notification.authors[0]) },
                        )
                        ProfileAvatar(
                            modifier =
                                Modifier
                                    .size(32.dp)
                                    .align(Alignment.BottomEnd),
                            profile = notification.authors[1],
                            onClick = { onProfileClick(notification.authors[1]) },
                        )
                    }
            }

            Column(modifier = Modifier.weight(1f)) {
                ProvideTextStyle(
                    OctopusTheme.typography.body2.lineHeight125.copy(
                        color = OctopusTheme.colorScheme.gray900,
                    ),
                ) {
                    RichText(modifier = Modifier.fillMaxWidth()) {
                        Markdown(text)
                    }
                }
                Text(
                    text = updateDate.getElapsedTime(),
                    color = OctopusTheme.colorScheme.gray500,
                    style = OctopusTheme.typography.caption1,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationItemPreview() {
    OctopusTheme {
        NotificationItem(
            notification = MockNotifications.all.random(),
            onClick = {},
            onProfileClick = {},
        )
    }
}
