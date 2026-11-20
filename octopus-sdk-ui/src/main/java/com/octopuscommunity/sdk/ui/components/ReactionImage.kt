package com.octopuscommunity.sdk.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import com.octopuscommunity.sdk.domain.model.OctopusItem.Reaction
import com.octopuscommunity.sdk.ui.OctopusIcons
import com.octopuscommunity.sdk.ui.OctopusTheme

/**
 * Renders a reaction as an [Image] using the themed [OctopusIcons.Content.Reaction] painters.
 *
 * For the 6 known reaction kinds (Heart, Joy, MouthOpen, Clap, Cry, Rage) the SDK ships bundled
 * default images that the client can override through the theming API. For the forward-compat
 * [Reaction.Kind.Unknown] kind (reactions added server-side before an SDK update), the emoji
 * unicode character is rendered instead.
 *
 * @param kind The reaction kind to render.
 * @param modifier Layout modifier.
 * @param contentDescription Accessibility description (used only for custom images).
 */
@Composable
internal fun ReactionImage(
    kind: Reaction.Kind,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val painter = OctopusTheme.icons.content.reaction.forKind(kind)
    if (painter != null) {
        Image(
            modifier = modifier,
            painter = painter(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit
        )
    } else {
        OctopusEmoji(
            modifier = modifier,
            unicode = kind.unicode
        )
    }
}

/**
 * Returns the painter factory for a given reaction kind, or null for
 * [Reaction.Kind.Unknown] (fallback to emoji unicode).
 */
internal fun OctopusIcons.Content.Reaction.forKind(
    kind: Reaction.Kind
): (@Composable () -> Painter)? = when (kind) {
    Reaction.Kind.Heart -> heart
    Reaction.Kind.Joy -> joy
    Reaction.Kind.MouthOpen -> mouthOpen
    Reaction.Kind.Clap -> clap
    Reaction.Kind.Cry -> cry
    Reaction.Kind.Rage -> rage
    else -> null
}
