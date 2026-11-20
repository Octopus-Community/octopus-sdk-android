package com.octopuscommunity.sdk.ui.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.domain.model.MinimalProfile
import com.octopuscommunity.sdk.domain.model.Profile
import com.octopuscommunity.sdk.test.mock.MockProfiles
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.components.OctopusAvatar

@Composable
internal fun ProfileAvatar(
    profile: MinimalProfile?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    colorSeed: String? = null,
) {
    val contentDescription = stringResource(R.string.accessibility_profile_picture)
    OctopusAvatar(
        modifier =
            modifier.semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
            },
        nickname = profile?.nickname,
        avatar = profile?.picture,
        onClick = onClick,
        colorSeed = colorSeed,
        defaultAvatar = profile?.defaultAvatar,
    )
}

@Preview(showBackground = true)
@Composable
private fun ProfileAvatarPreview() {
    OctopusTheme {
        ProfileAvatar(
            modifier = Modifier.size(64.dp),
            profile = MockProfiles.all.random(),
        )
    }
}

@Composable
internal fun ProfileAvatar(
    profile: Profile?,
    actionIcon: Painter?,
    onClick: (() -> Unit)?,
    onActionClick: (() -> Unit),
    modifier: Modifier = Modifier,
    colorSeed: String? = null,
) = Box {
    val contentDescription = stringResource(R.string.accessibility_profile_picture)
    ProfileAvatar(
        modifier =
            modifier.semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
            },
        profile = profile,
        onClick = onClick,
        colorSeed = colorSeed,
    )
    if (actionIcon != null) {
        IconButton(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .background(
                        color = OctopusTheme.colorScheme.primary,
                        shape = CircleShape,
                    ).size(28.dp),
            onClick = onActionClick,
        ) {
            Icon(
                painter = actionIcon,
                contentDescription = stringResource(R.string.accessibility_profile_picture_edit_formal),
                tint = OctopusTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun OctopusActionAvatarPreview() {
    OctopusTheme {
        ProfileAvatar(
            modifier = Modifier.size(64.dp),
            profile = MockProfiles.all.random(),
            actionIcon = OctopusTheme.icons.profile.editPicture(),
            onClick = {},
            onActionClick = {},
        )
    }
}
