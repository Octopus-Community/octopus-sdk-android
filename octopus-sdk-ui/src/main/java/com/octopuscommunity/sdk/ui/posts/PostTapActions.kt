package com.octopuscommunity.sdk.ui.posts

import com.octopuscommunity.sdk.domain.model.CommunityConfig.ContentOptions.PostOptions

/** Keep the override above media rendering, so image gestures and the viewer stay unchanged. */
internal fun PostDisplayMode.imageTapAction(
    options: PostOptions?,
    canOpenDetail: Boolean,
    onFullscreen: () -> Unit,
): () -> Unit = if (this is PostDisplayMode.Summary && canOpenDetail && options?.imageTapOpensPost == true) onCardTap else onFullscreen

/** An unconfigured group label keeps its card action in feeds and stays inert in detail. */
internal fun PostDisplayMode.groupNameTapAction(
    options: PostOptions?,
    onOpenGroup: () -> Unit,
): (() -> Unit)? = if (options?.groupNameTapOpensGroup == true) onOpenGroup else (this as? PostDisplayMode.Summary)?.onCardTap
