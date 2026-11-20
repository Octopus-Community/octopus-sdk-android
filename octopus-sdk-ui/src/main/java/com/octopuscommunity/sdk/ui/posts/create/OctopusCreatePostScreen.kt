package com.octopuscommunity.sdk.ui.posts.create

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.octopuscommunity.sdk.InternalOctopusApi
import com.octopuscommunity.sdk.domain.model.CreatePostScreenInfo
import com.octopuscommunity.sdk.domain.model.Post
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.components.OctopusContent

/**
 * Top-level entry point for the Octopus post editor (Bridge Share).
 *
 * Opens the standard SDK post-creation screen, optionally prefilled
 * with content supplied by the host app via [CreatePostScreenInfo].
 * When [info] is omitted (or its `prefilledPost` is `null`), the
 * editor opens empty — equivalent to today's in-SDK new-post flow.
 *
 * Mirrors the [com.octopuscommunity.sdk.ui.posts.details.OctopusPostDetailsScreen]
 * pattern: a thin public wrapper around the internal screen
 * implementation, suitable for hosting under the consumer's own
 * `NavHost` without going through `octopusComposables()`.
 *
 * The CTA carried by `info.prefilledPost.cta` is **not displayed in
 * the editor** — it travels invisibly through and is attached to the
 * published post.
 *
 * The `info.bridgeShareTokenProvider`, when set, is wired for this
 * editor session so a prefilled share carrying an image can be signed
 * at publish time (see
 * [com.octopuscommunity.sdk.domain.model.CreatePostScreenInfo]).
 *
 * @param navController Host's [NavHostController]. Used by the screen
 * for back-stack navigation (close, group picker, etc.).
 * @param info Optional payload prefilling the editor.
 * @param modifier Modifier applied to the root container.
 */
@OptIn(InternalOctopusApi::class)
@Composable
fun OctopusCreatePostScreen(
    navController: NavHostController,
    info: CreatePostScreenInfo = CreatePostScreenInfo(),
    modifier: Modifier = Modifier,
) = OctopusContent(navController) {
    // Hand the (non-Serializable) bridge-share signer to the SDK for this editor session — keyed
    // on the provider so a recomposition doesn't re-wire needlessly. Mirrors the nav-graph entry
    // [com.octopuscommunity.sdk.ui.navigateToOctopusCreatePost]; `null` for a non-share editor.
    val holder = org.koin.compose.koinInject<com.octopuscommunity.sdk.domain.BridgeShareTokenProviderHolder>()
    LaunchedEffect(holder, info.bridgeShareTokenProvider) {
        holder.tokenProvider = info.bridgeShareTokenProvider
    }
    CreatePostScreen(
        modifier = modifier,
        navController = navController,
        type = Post.Draft.Type.TEXT,
        groupId = info.prefilledPost?.topicId,
        prefilledText = info.prefilledPost?.text,
        prefilledImageUris =
            info.prefilledPost
                ?.images
                ?.map { it.toString() }
                .orEmpty(),
        prefilledCustomActionLabel = info.prefilledPost?.cta?.label,
        prefilledCustomActionUrl =
            info.prefilledPost
                ?.cta
                ?.url
                ?.toString(),
        prefilledCustomActionLabelLanguage = info.prefilledPost?.cta?.labelLanguage,
        prefilledCustomActionLabelTranslations =
            info.prefilledPost
                ?.cta
                ?.labelTranslations
                .orEmpty(),
    )
}

@Preview(showBackground = true)
@Composable
private fun OctopusCreatePostScreenPreview() {
    OctopusTheme {
        OctopusCreatePostScreen(rememberNavController())
    }
}
