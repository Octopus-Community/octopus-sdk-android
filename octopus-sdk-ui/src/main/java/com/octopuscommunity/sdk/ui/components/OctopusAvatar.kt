package com.octopuscommunity.sdk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.octopuscommunity.sdk.domain.model.DefaultAvatar
import com.octopuscommunity.sdk.domain.model.Resource
import com.octopuscommunity.sdk.test.mock.MockProfiles
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.semiBold
import com.octopuscommunity.sdk.ui.utils.avatarColorSource
import com.octopuscommunity.sdk.ui.utils.toInitials
import com.octopuscommunity.sdk.ui.utils.toUserColor

@Composable
internal fun OctopusAvatar(
    nickname: String?,
    avatar: Resource?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    colorSeed: String? = null,
    defaultAvatar: DefaultAvatar? = null,
) = OctopusAvatar(
    modifier = modifier,
    nickname = nickname,
    data = avatar?.data,
    onClick = onClick,
    colorSeed = colorSeed,
    defaultAvatar = defaultAvatar,
)

@Composable
internal fun OctopusAvatar(
    nickname: String?,
    data: Any?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    // Where the initials' background takes its color from, when it should not follow [nickname] (see avatarColorSource).
    colorSeed: String? = null,
    // What the member's community draws when they have no picture; ignored when [data] is set.
    defaultAvatar: DefaultAvatar? = null,
) {
    BoxWithConstraints(
        modifier =
            modifier
                .then(
                    if (onClick != null) {
                        Modifier.minimumInteractiveComponentSize()
                    } else {
                        Modifier
                    },
                ).clip(CircleShape)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            role = Role.Button,
                            onClick = onClick,
                        )
                    } else {
                        Modifier
                    },
                ),
    ) {
        AvatarLayer(
            content = resolveAvatarContent(data = data, nickname = nickname, defaultAvatar = defaultAvatar, colorSeed = colorSeed),
            size = minOf(maxWidth, maxHeight),
        )
    }
}

/** What an avatar draws, as decided by [resolveAvatarContent]. */
internal sealed interface AvatarContent {
    /** The member's own picture, [data] being the image model. */
    data class Picture(
        val data: Any,
    ) : AvatarContent

    /** The community's default image [data], drawn over [fallback], which shows while it loads and if it fails. */
    data class DefaultImage(
        val data: Any,
        val fallback: AvatarContent,
    ) : AvatarContent

    /** Initials [text] in [textColor] on [background]. */
    data class Initials(
        val text: String,
        val background: Color,
        val textColor: Color,
    ) : AvatarContent

    /** Nothing to take initials from: the generic avatar icon. */
    data object Icon : AvatarContent
}

/**
 * The member's own picture first; then the community's default avatar; then initials on a color computed from
 * [nickname]; then the generic icon. Community colors only recolor initials, so without a nickname they give
 * the icon, like the computed color does.
 */
internal fun resolveAvatarContent(
    data: Any?,
    nickname: String?,
    defaultAvatar: DefaultAvatar?,
    colorSeed: String? = null,
): AvatarContent {
    val computed = computedAvatarContent(nickname, colorSeed)
    return when {
        data != null -> AvatarContent.Picture(data)

        defaultAvatar is DefaultAvatar.Image -> AvatarContent.DefaultImage(data = defaultAvatar.image.data, fallback = computed)

        defaultAvatar is DefaultAvatar.Color && computed is AvatarContent.Initials ->
            computed.copy(background = Color(defaultAvatar.background), textColor = Color(defaultAvatar.initials))

        else -> computed
    }
}

private fun computedAvatarContent(
    nickname: String?,
    colorSeed: String?,
): AvatarContent =
    nickname?.ifEmpty { null }?.let {
        AvatarContent.Initials(
            text = it.toInitials(count = 1),
            background = avatarColorSource(it, colorSeed).toUserColor(),
            textColor = Color.Black,
        )
    } ?: AvatarContent.Icon

@Composable
private fun AvatarLayer(
    content: AvatarContent,
    size: Dp,
) {
    when (content) {
        is AvatarContent.Picture ->
            OctopusImage(
                modifier =
                    Modifier
                        .size(size)
                        .background(OctopusTheme.colorScheme.primaryLow),
                data = content.data,
                contentScale = ContentScale.Crop,
                crossFade = false,
            )

        is AvatarContent.DefaultImage ->
            Box {
                AvatarLayer(content = content.fallback, size = size)
                // No background of its own, so the fallback underneath shows until the image is drawn.
                OctopusImage(
                    modifier = Modifier.size(size),
                    data = content.data,
                    contentScale = ContentScale.Crop,
                    crossFade = false,
                )
            }

        is AvatarContent.Initials ->
            Text(
                modifier =
                    Modifier
                        .size(size)
                        .background(content.background)
                        .border(
                            width = 1.dp,
                            color = OctopusTheme.colorScheme.gray300,
                            shape = CircleShape,
                        ).padding(size * 0.25f),
                text = content.text,
                textAlign = TextAlign.Center,
                color = content.textColor,
                style = OctopusTheme.typography.body1.semiBold,
                autoSize =
                    TextAutoSize.StepBased(
                        minFontSize = 9.sp,
                    ),
            )

        AvatarContent.Icon ->
            Icon(
                modifier =
                    Modifier
                        .size(size)
                        .background(OctopusTheme.colorScheme.primaryLow)
                        .border(
                            width = 1.dp,
                            color = OctopusTheme.colorScheme.gray300,
                            shape = CircleShape,
                        ).padding(8.dp),
                painter = OctopusTheme.icons.profile.defaultAvatar(),
                contentDescription = null,
                tint = OctopusTheme.colorScheme.primary,
            )
    }
}

@Preview(showBackground = true)
@Composable
private fun OctopusAvatarPreview() {
    OctopusTheme {
        val profile = MockProfiles.all.random()
        OctopusAvatar(
            modifier = Modifier.size(64.dp),
            nickname = profile.nickname,
            avatar = profile.picture,
        )
    }
}
