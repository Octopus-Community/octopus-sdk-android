package com.octopuscommunity.sdk.ui.responses

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.octopuscommunity.sdk.domain.model.Comment
import com.octopuscommunity.sdk.domain.model.CommunityConfig
import com.octopuscommunity.sdk.domain.model.CurrentUserProfile
import com.octopuscommunity.sdk.domain.model.OctopusItem.Reaction
import com.octopuscommunity.sdk.test.mock.MockComments
import com.octopuscommunity.sdk.test.mock.MockCurrentUserProfiles
import com.octopuscommunity.sdk.test.mock.MockReplies
import com.octopuscommunity.sdk.ui.OctopusDestination.CommentMedias
import com.octopuscommunity.sdk.ui.OctopusDestination.ReplyMedias
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.comments.menu.CommentMenuSheet
import com.octopuscommunity.sdk.ui.comments.replies.menu.ReplyMenuSheet
import com.octopuscommunity.sdk.ui.common.medias.viewer.ImageViewerKey
import com.octopuscommunity.sdk.ui.common.medias.viewer.ImageViewerSource
import com.octopuscommunity.sdk.ui.common.medias.viewer.LocalImageViewerController
import com.octopuscommunity.sdk.ui.common.medias.viewer.openOrElse
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.common.uiText
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusItemViewModel
import com.octopuscommunity.sdk.ui.lineHeight150Trimmed
import com.octopuscommunity.sdk.ui.profile.common.getUiNickname
import com.octopuscommunity.sdk.ui.profile.components.ProfileAvatar
import com.octopuscommunity.sdk.ui.responses.blocks.ResponseActionBar
import com.octopuscommunity.sdk.ui.responses.blocks.ResponseHeader
import com.octopuscommunity.sdk.ui.responses.blocks.ResponseImageContent
import com.octopuscommunity.sdk.ui.responses.blocks.ResponseSeeReplies
import com.octopuscommunity.sdk.ui.responses.blocks.ResponseTextContent
import com.octopuscommunity.sdk.ui.responses.blocks.ResponseTranslationToggle
import com.octopuscommunity.sdk.ui.utils.getShortElapsedTime

/**
 * VM wrapper for a comment. Pulls translation / profile state from the VM, owns the
 * `CommentMenuSheet` lifecycle, and wires navigation callbacks (profile, medias, reactions).
 */
@Composable
internal fun ResponseCard(
    viewModel: OctopusItemViewModel<Comment>,
    comment: Comment,
    displayMode: ResponseDisplayMode,
    onReplyClick: () -> Unit,
    onSeeRepliesClick: () -> Unit,
    modifier: Modifier = Modifier,
    showsRepliesRow: Boolean = true,
    containerColor: Color = OctopusTheme.colorScheme.gray200,
    onAccessibilityClick: (() -> Unit)? = null,
) {
    val currentUserProfile by viewModel.currentUserProfile.collectAsStateWithLifecycle()
    val communityConfig by viewModel.communityConfig.collectAsStateWithLifecycle()
    val displayTranslations by viewModel.translations.collectAsStateWithLifecycle()

    DisposableEffect(comment.id) {
        viewModel.onItemAppeared(comment)
        onDispose { viewModel.onItemDisappeared(comment) }
    }

    val uiModel =
        remember(comment, currentUserProfile) {
            comment.toUiModel(currentUserProfile)
        }

    var showMenuSheet by remember { mutableStateOf(false) }

    val imageViewer = LocalImageViewerController.current

    ResponseCard(
        modifier = modifier,
        uiModel = uiModel,
        currentUserProfile = currentUserProfile,
        communityConfig = communityConfig,
        displayMode = displayMode,
        translated = displayTranslations[comment.id] ?: true,
        showsRepliesRow = showsRepliesRow,
        containerColor = containerColor,
        onAccessibilityClick = onAccessibilityClick,
        onProfileClick = { viewModel.navigateToProfile(comment) },
        onMoreActionsClick =
            if (uiModel.canBeDeleted || uiModel.canBeReported) {
                { showMenuSheet = true }
            } else {
                null
            },
        onToggleTranslationClick = { viewModel.toggleDisplayedTranslation(comment) },
        onReactionClick = { viewModel.toggleReaction(comment, it) },
        onReplyClick = onReplyClick,
        onSeeRepliesClick = onSeeRepliesClick,
        onImageClick = { source ->
            imageViewer.openOrElse(
                key = ImageViewerKey(kind = ImageViewerKey.Kind.Comment, id = comment.id),
                media = (uiModel.content as? ResponseContent.Published)?.image,
                source = source,
            ) { viewModel.navigateTo(CommentMedias(comment.id)) }
        },
    )

    if (showMenuSheet) {
        CommentMenuSheet(
            comment = comment,
            onDismiss = { showMenuSheet = false },
        )
    }
}

/**
 * VM wrapper for a reply. Reply responses cannot themselves be replied to, so `onReplyClick`
 * and `onSeeRepliesClick` never fire from inside the card — they're still required by the pure
 * overload but wired to no-ops here.
 */
@Composable
internal fun ResponseCard(
    viewModel: OctopusItemViewModel<Comment.Reply>,
    reply: Comment.Reply,
    displayMode: ResponseDisplayMode,
    modifier: Modifier = Modifier,
    containerColor: Color = OctopusTheme.colorScheme.gray200,
    onAccessibilityClick: (() -> Unit)? = null,
) {
    val currentUserProfile by viewModel.currentUserProfile.collectAsStateWithLifecycle()
    val communityConfig by viewModel.communityConfig.collectAsStateWithLifecycle()
    val displayTranslations by viewModel.translations.collectAsStateWithLifecycle()

    DisposableEffect(reply.id) {
        viewModel.onItemAppeared(reply)
        onDispose { viewModel.onItemDisappeared(reply) }
    }

    val uiModel =
        remember(reply, currentUserProfile) {
            reply.toUiModel(currentUserProfile)
        }

    var showMenuSheet by remember { mutableStateOf(false) }

    val imageViewer = LocalImageViewerController.current

    ResponseCard(
        modifier = modifier,
        uiModel = uiModel,
        currentUserProfile = currentUserProfile,
        communityConfig = communityConfig,
        displayMode = displayMode,
        translated = displayTranslations[reply.id] ?: true,
        showsRepliesRow = false,
        containerColor = containerColor,
        onAccessibilityClick = onAccessibilityClick,
        onProfileClick = { viewModel.navigateToProfile(reply) },
        onMoreActionsClick =
            if (uiModel.canBeDeleted || uiModel.canBeReported) {
                { showMenuSheet = true }
            } else {
                null
            },
        onToggleTranslationClick = { viewModel.toggleDisplayedTranslation(reply) },
        onReactionClick = { viewModel.toggleReaction(reply, it) },
        onReplyClick = {},
        onSeeRepliesClick = {},
        onImageClick = { source ->
            imageViewer.openOrElse(
                key = ImageViewerKey(kind = ImageViewerKey.Kind.Reply, id = reply.id),
                media = (uiModel.content as? ResponseContent.Published)?.image,
                source = source,
            ) { viewModel.navigateTo(ReplyMedias(replyId = reply.id)) }
        },
    )

    if (showMenuSheet) {
        ReplyMenuSheet(
            reply = reply,
            onDismiss = { showMenuSheet = false },
        )
    }
}

/**
 * Pure renderer — iso iOS `ResponseContentView`. Composes the seven blocks inside a
 * row: `avatar · (gray card [header, translation, text, image] + action bar + see-replies)`.
 *
 * The gray rounded card is applied inline via Modifier (no separate container composable).
 * The "See N replies" row is suppressed in detail mode, for replies, or when the comment has
 * no replies.
 */
@Composable
internal fun ResponseCard(
    uiModel: ResponseUiModel,
    currentUserProfile: CurrentUserProfile?,
    communityConfig: CommunityConfig?,
    displayMode: ResponseDisplayMode,
    translated: Boolean,
    showsRepliesRow: Boolean,
    onAccessibilityClick: (() -> Unit)?,
    onProfileClick: () -> Unit,
    onMoreActionsClick: (() -> Unit)?,
    onToggleTranslationClick: () -> Unit,
    onReactionClick: (Reaction.Kind) -> Unit,
    onReplyClick: () -> Unit,
    onSeeRepliesClick: () -> Unit,
    onImageClick: (ImageViewerSource) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = OctopusTheme.colorScheme.gray200,
) = with(uiModel) {
    var groupedForAccessibility by remember { mutableStateOf(true) }

    val authorName = author.getUiNickname(currentUserProfile).asString()
    val elapsedTime = creationDate.getShortElapsedTime()
    val isSummary = displayMode is ResponseDisplayMode.Summary
    val cardTap = (displayMode as? ResponseDisplayMode.Summary)?.onCardTap
    // Iso iOS: only the featured comment in a post summary is ellipsized
    // (`DisplayablePost.featuredComment` is built with `ellipsizeText: true`). Comments inside
    // the post detail's comment list, replies inside the comment detail, and the focal comment
    // on the comment detail screen all render the full body.
    val ellipsizeText = (displayMode as? ResponseDisplayMode.Summary)?.ellipsizeText == true

    val accessibilitySummary =
        buildResponseAccessibilitySummary(
            uiModel = uiModel,
            authorName = authorName,
            dateText = elapsedTime.accessibilityText,
            translated = translated,
        )

    val accessibilityCustomActions =
        buildList {
            add(
                CustomAccessibilityAction(
                    label =
                        stringResource(
                            if (groupedForAccessibility) {
                                R.string.accessibility_content_action_read_elements
                            } else {
                                R.string.accessibility_content_action_read_summary
                            },
                        ),
                    action = {
                        groupedForAccessibility = !groupedForAccessibility
                        true
                    },
                ),
            )
            add(
                CustomAccessibilityAction(
                    label = stringResource(R.string.accessibility_content_action_view_author),
                    action = {
                        onProfileClick()
                        true
                    },
                ),
            )
            if (onAccessibilityClick != null) {
                add(
                    CustomAccessibilityAction(
                        label = stringResource(R.string.accessibility_content_action_open_detail),
                        action = {
                            onAccessibilityClick()
                            true
                        },
                    ),
                )
            }
        }

    // Disable Material3's 48dp min-touch enforcement so clickable blocks (translation toggle,
    // counts, like/reply) render at Figma heights — iso iOS which has no global min-touch
    // guard (individual elements opt in via `.frame(minHeight: 44)` where needed).
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Row(
            modifier =
                modifier
                    .fillMaxWidth()
                    // Figma `Comment Card/With Image` (node 33:4919): outer wrapper has `pt-[6px]`.
                    // Avatar AND bubble are both children of this flex container with `items-start`,
                    // so they're top-aligned at y=6 from the Comment Card frame. We apply the 6dp top
                    // padding here so the avatar and the bubble sit at the same Y — the previous
                    // `drawBehind` inset trick made the bubble's visible BG start 6dp below the row
                    // top while keeping the avatar at y=0, which produced an avatar-overflow-above-
                    // bubble effect that does NOT exist in the Figma comp (Thomas P, DM 2026-04-29).
                    .padding(top = 6.dp)
                    .then(
                        if (cardTap != null) {
                            Modifier.clickable(onClick = cardTap)
                        } else {
                            Modifier
                        },
                    ).then(
                        if (groupedForAccessibility) {
                            Modifier.semantics(mergeDescendants = true) {
                                this.contentDescription = accessibilitySummary
                                this.customActions = accessibilityCustomActions
                            }
                        } else {
                            Modifier.semantics {
                                this.customActions = accessibilityCustomActions
                            }
                        },
                    ),
            verticalAlignment = Alignment.Top,
        ) {
            ProfileAvatar(
                modifier = Modifier.size(32.dp),
                profile = author,
                onClick = onProfileClick,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                // Gray rounded bubble wrapping header + translation + text + image. Plain
                // background + clip — no drawBehind offset trick; the 6dp gap above the avatar /
                // bubble row is owned by the outer Row's `padding(top = 6.dp)` (iso Figma's
                // `pt-[6px]` on the Comment Card wrapper).
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(containerColor),
                ) {
                    val gamificationLevelDisplay =
                        communityConfig?.gamification?.let { gamif ->
                            with(gamif) { author?.gamificationLevel }
                        }
                    ResponseHeader(
                        authorName = authorName,
                        authorTags = author?.tags ?: emptyList(),
                        gamificationLevel = gamificationLevelDisplay,
                        relativeDate = elapsedTime,
                        onProfileClick = onProfileClick,
                        onMoreActionsClick = onMoreActionsClick,
                    )
                    // The translation toggle opens the content, right under the header, so a member
                    // sees the response is translated before reading it — iso iOS.
                    val publishedContent = content as? ResponseContent.Published
                    if (publishedContent != null && hasTranslation) {
                        ResponseTranslationToggle(
                            translated = translated,
                            originalLanguage = publishedContent.text.original.language,
                            onClick = onToggleTranslationClick,
                        )
                    }
                    when (val content = content) {
                        is ResponseContent.Moderated -> {
                            Text(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, end = 8.dp, top = 4.dp, bottom = 8.dp),
                                text =
                                    stringResource(R.string.post_list_moderated_post_main_text) +
                                        "\n" + content.reasons.uiText.asString(),
                                color = OctopusTheme.colorScheme.gray900,
                                // Mirrors `ResponseTextContent` typography iso Figma (16/24 regular).
                                style = OctopusTheme.typography.body2.lineHeight150Trimmed,
                            )
                        }
                        is ResponseContent.Published -> {
                            ResponseTextContent(
                                text = content.text,
                                translated = translated,
                                ellipsize = ellipsizeText,
                            )
                            // 8dp bottom whitespace inside the bubble, skipped when the image sits
                            // flush to its bottom. A translated response ends exactly like an
                            // untranslated one: its toggle sits above the text.
                            if (content.image == null) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            content.image?.let { image ->
                                ResponseImageContent(image = image, onClick = onImageClick)
                            }
                        }
                    }
                }
                // Action bar (outside the gray card)
                ResponseActionBar(
                    contentId = uiModel.id,
                    kind = kind,
                    displayReplyButton = canCreateChildren,
                    userReaction = userReaction,
                    reactions = reactions,
                    onReactionClick = onReactionClick,
                    onReplyClick = onReplyClick,
                )
                // "See N replies" — only for summary + comment + has replies + caller opted in
                if (isSummary && showsRepliesRow && kind == ResponseKind.Comment && replyCount > 0) {
                    ResponseSeeReplies(
                        replyCount = replyCount,
                        onClick = onSeeRepliesClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun buildResponseAccessibilitySummary(
    uiModel: ResponseUiModel,
    authorName: String,
    dateText: String,
    translated: Boolean,
): String =
    with(uiModel) {
        when (val content = content) {
            is ResponseContent.Moderated ->
                stringResource(
                    R.string.accessibility_response_summary_text_only_author,
                    authorName,
                    dateText,
                    content.reasons.uiText.asString(),
                )
            is ResponseContent.Published -> {
                val textContent = content.text.getText(translated = translated).trim()
                val hasImage = content.image != null
                when {
                    hasImage && textContent.isNotEmpty() ->
                        stringResource(
                            R.string.accessibility_response_summary_text_and_image_author,
                            authorName,
                            dateText,
                            textContent,
                        )
                    hasImage ->
                        stringResource(
                            R.string.accessibility_response_summary_image_author,
                            authorName,
                            dateText,
                        )
                    else ->
                        stringResource(
                            R.string.accessibility_response_summary_text_only_author,
                            authorName,
                            dateText,
                            textContent,
                        )
                }
            }
        }
    }

@Preview(showBackground = true)
@Composable
private fun ResponseCardCommentPreview() {
    OctopusTheme {
        ResponseCard(
            uiModel = MockComments.all.random().toUiModel(MockCurrentUserProfiles.default),
            currentUserProfile = MockCurrentUserProfiles.default,
            communityConfig = null,
            displayMode = ResponseDisplayMode.Summary(onCardTap = null),
            translated = false,
            showsRepliesRow = true,
            onAccessibilityClick = null,
            onProfileClick = {},
            onMoreActionsClick = {},
            onToggleTranslationClick = {},
            onReactionClick = {},
            onReplyClick = {},
            onSeeRepliesClick = {},
            onImageClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ResponseCardReplyPreview() {
    OctopusTheme {
        ResponseCard(
            uiModel = MockReplies.all.random().toUiModel(MockCurrentUserProfiles.default),
            currentUserProfile = MockCurrentUserProfiles.default,
            communityConfig = null,
            displayMode = ResponseDisplayMode.Summary(onCardTap = null),
            translated = false,
            showsRepliesRow = false,
            onAccessibilityClick = null,
            onProfileClick = {},
            onMoreActionsClick = null,
            onToggleTranslationClick = {},
            onReactionClick = {},
            onReplyClick = {},
            onSeeRepliesClick = {},
            onImageClick = {},
        )
    }
}
