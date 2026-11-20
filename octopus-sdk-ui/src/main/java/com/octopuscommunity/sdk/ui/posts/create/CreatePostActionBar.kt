package com.octopuscommunity.sdk.ui.posts.create

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource

/** Height of a capsule of the composer's action bar. */
private val ActionCapsuleHeight = 44.dp

/** Space above and below the action bar. */
private val ActionBarVerticalPadding = 8.dp

/** Height the floating action bar takes over the form, for the form to leave room under its end. */
internal val CreatePostActionBarHeight = ActionCapsuleHeight + ActionBarVerticalPadding * 2

/** Opacity of a capsule's content while the bar is disabled (a post is being sent). */
private const val DisabledContentAlpha = 0.38f

/**
 * The composer's action bar: a « Photo » capsule while [showAddPicture], a « Poll » capsule while
 * [showAddPoll], each with its icon and label. Disabled, they dim and ignore taps.
 */
@Composable
internal fun CreatePostActionBar(
    showAddPicture: Boolean,
    showAddPoll: Boolean,
    enabled: Boolean,
    onAddPicture: () -> Unit,
    onAddPoll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(vertical = ActionBarVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showAddPicture) {
            ActionCapsule(
                icon =
                    OctopusTheme.icons.content.post.creation
                        .addPicture(),
                label = stringResource(R.string.content_create_add_picture),
                enabled = enabled,
                onClick = onAddPicture,
            )
        }
        if (showAddPoll) {
            ActionCapsule(
                icon =
                    OctopusTheme.icons.content.post.creation
                        .addPoll(),
                label = stringResource(R.string.content_create_add_poll),
                enabled = enabled,
                onClick = onAddPoll,
            )
        }
    }
}

/** A 44dp gray capsule holding [icon] and [label], in a 48dp tap target. */
@Composable
private fun ActionCapsule(
    icon: Painter,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val contentColor =
        OctopusTheme.colorScheme.gray700.let { if (enabled) it else it.copy(alpha = DisabledContentAlpha) }
    Row(
        modifier =
            Modifier
                .minimumInteractiveComponentSize()
                .heightIn(min = ActionCapsuleHeight)
                .clip(CircleShape)
                .background(OctopusTheme.colorScheme.gray200)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            painter = icon,
            contentDescription = null,
            tint = contentColor,
        )
        Text(
            text = label,
            color = contentColor,
            style = OctopusTheme.typography.body2,
        )
    }
}
