package com.octopuscommunity.sdk.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.medium
import com.octopuscommunity.sdk.ui.profile.current.avatar.CurrentUserProfileAvatar

/**
 * Floating action bar combining the current-user avatar and a "Create post" FAB.
 *
 * When [displayCreateButton] is `false` (internal ticket — no topic in scope allows post creation by
 * the current user), the FAB is omitted and only the avatar is rendered. Mirrors iOS
 * `AuthorActionView` (internal change) — the bubble stays so the user can still navigate to their own
 * profile / log in.
 */
@Composable
internal fun OctopusCreatePostBar(
    isScrollingDown: Boolean,
    onCreatePost: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    displayCreateButton: Boolean = true,
    onHeightChanged: (Int) -> Unit = {},
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .onSizeChanged { onHeightChanged(it.height) },
    ) {
        Box(modifier = Modifier.padding(contentPadding)) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                CurrentUserProfileAvatar(
                    modifier =
                        Modifier
                            .size(56.dp)
                            .shadow(elevation = 6.dp, shape = CircleShape),
                    showNotificationBadge = true,
                )
                if (displayCreateButton) {
                    val createPostText = stringResource(R.string.post_list_open_create_post)
                    ExtendedFloatingActionButton(
                        modifier =
                            Modifier
                                .padding(start = 8.dp)
                                .semantics(mergeDescendants = true) {
                                    contentDescription = createPostText
                                },
                        text = {
                            // Let the label wrap its content so the expanded button hugs its text
                            // instead of stretching to fill the remaining row width.
                            Text(
                                text = createPostText,
                                style = OctopusTheme.typography.body2.medium,
                            )
                        },
                        icon = {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                painter =
                                    OctopusTheme.icons.content.post.creation
                                        .create(),
                                contentDescription = null,
                            )
                        },
                        containerColor = OctopusTheme.colorScheme.primary,
                        contentColor = OctopusTheme.colorScheme.onPrimary,
                        onClick = onCreatePost,
                        expanded = !isScrollingDown,
                        shape = RoundedCornerShape(32.dp),
                    )
                }
            }
        }
    }
}
