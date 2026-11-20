package com.octopuscommunity.sdk.ui.profile.current.avatar

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModel
import com.octopuscommunity.sdk.ui.components.OctopusContent
import com.octopuscommunity.sdk.ui.profile.components.ProfileAvatar

/**
 * The connected user's avatar (picture, or initials/default fallback), rendered as a plain,
 * **non-interactive** image — no tap navigation and no Unified Profile activity bell.
 *
 * This is the affordance shown next to the composer author (see the New Post screen), matching iOS
 * `CreatePostView` where the author avatar is display-only. For the tappable home floating button
 * (which shows the activity bell in Unified Profile mode), use [CurrentUserProfileAvatar] instead.
 */
@Composable
internal fun CurrentUserAvatar(modifier: Modifier = Modifier) =
    OctopusContent {
        val viewModel = octopusViewModel<CurrentUserProfileAvatarViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()

        ProfileAvatar(
            modifier = modifier,
            profile = state.profile,
            onClick = null,
        )
    }

@Preview
@Composable
private fun CurrentUserAvatarPreview() {
    OctopusTheme {
        CurrentUserAvatar(
            modifier = Modifier.size(32.dp),
        )
    }
}
