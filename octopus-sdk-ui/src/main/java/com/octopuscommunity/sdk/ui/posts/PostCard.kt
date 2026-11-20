package com.octopuscommunity.sdk.ui.posts

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.octopuscommunity.sdk.domain.model.CommunityConfig
import com.octopuscommunity.sdk.domain.model.CurrentUserProfile
import com.octopuscommunity.sdk.domain.model.OctopusItem.Reaction
import com.octopuscommunity.sdk.domain.model.Post
import com.octopuscommunity.sdk.test.mock.MockCommunityConfig
import com.octopuscommunity.sdk.test.mock.MockCurrentUserProfiles
import com.octopuscommunity.sdk.test.mock.MockPosts
import com.octopuscommunity.sdk.ui.OctopusDestination.GroupDetails
import com.octopuscommunity.sdk.ui.OctopusDestination.PostMedias
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.medias.viewer.ImageViewerKey
import com.octopuscommunity.sdk.ui.common.medias.viewer.ImageViewerSource
import com.octopuscommunity.sdk.ui.common.medias.viewer.LocalImageViewerController
import com.octopuscommunity.sdk.ui.common.medias.viewer.openOrElse
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.common.uiText
import com.octopuscommunity.sdk.ui.lineHeight150Trimmed
import com.octopuscommunity.sdk.ui.posts.blocks.PostCTAContent
import com.octopuscommunity.sdk.ui.posts.blocks.PostCatchPhrase
import com.octopuscommunity.sdk.ui.posts.blocks.PostFooter
import com.octopuscommunity.sdk.ui.posts.blocks.PostHeader
import com.octopuscommunity.sdk.ui.posts.blocks.PostMediaContent
import com.octopuscommunity.sdk.ui.posts.blocks.PostPollContent
import com.octopuscommunity.sdk.ui.posts.blocks.PostTags
import com.octopuscommunity.sdk.ui.posts.blocks.PostTextContent
import com.octopuscommunity.sdk.ui.posts.blocks.PostTranslationToggle
import com.octopuscommunity.sdk.ui.posts.blocks.postFooterState
import com.octopuscommunity.sdk.ui.posts.blocks.postHeaderGroupTouchArea
import com.octopuscommunity.sdk.ui.posts.blocks.rememberPostHeaderGroupTouchTarget
import com.octopuscommunity.sdk.ui.posts.common.PostViewModel
import com.octopuscommunity.sdk.ui.posts.components.PollVoteOutcome
import com.octopuscommunity.sdk.ui.posts.menu.PostMenuSheet
import com.octopuscommunity.sdk.ui.profile.common.getUiNickname
import com.octopuscommunity.sdk.ui.semiBold
import com.octopuscommunity.sdk.ui.utils.getShortElapsedTime
import kotlinx.coroutines.Deferred

/**
 * Shared post renderer used in both feed (summary) and detail contexts — iso iOS `PostView`.
 *
 * Pulls VM state (current user profile, community config, translation preferences) and wires
 * the [uiModel] rendering to the VM's navigation / mutation calls. Owns the `PostMenuSheet`
 * lifecycle state.
 */
@Composable
internal fun PostCard(
    viewModel: PostViewModel,
    post: Post,
    displayMode: PostDisplayMode,
    playVideo: Boolean,
    onAccessibilityClick: (() -> Unit)?,
    onCommentButtonTap: () -> Unit,
    modifier: Modifier = Modifier,
    enableProfileClick: Boolean = true,
    showMoreActions: Boolean = displayMode is PostDisplayMode.Summary,
    /**
     * Forces the post text to render fully (no ellipsis) even in [PostDisplayMode.Summary].
     * Wired by the caller to the local inline-expand state when the post has
     * `canCreateChildren = false` and zero comments — iso iOS `PostSummaryView.isTextExpanded`
     * (internal change).
     */
    forceFullText: Boolean = false,
) {
    val currentUserProfile by viewModel.currentUserProfile.collectAsStateWithLifecycle()
    val communityConfig by viewModel.communityConfig.collectAsStateWithLifecycle()
    val displayTranslations by viewModel.translations.collectAsStateWithLifecycle()

    DisposableEffect(post.id) {
        viewModel.onItemAppeared(post)
        onDispose { viewModel.onItemDisappeared(post) }
    }

    val uiModel = remember(post, currentUserProfile) { post.toUiModel(currentUserProfile) }

    var showMenuSheet by remember { mutableStateOf(false) }

    // "View group" is shown wherever the group name is shown (every surface except the group feed,
    // which sets displayGroupName = false). Navigates to the post's group.
    val displayGroupName = (displayMode as? PostDisplayMode.Summary)?.displayGroupName ?: true

    val imageViewer = LocalImageViewerController.current

    PostCard(
        modifier = modifier,
        uiModel = uiModel,
        currentUserProfile = currentUserProfile,
        communityConfig = communityConfig,
        displayMode = displayMode,
        translated = displayTranslations[post.id] ?: true,
        playVideo = playVideo,
        forceFullText = forceFullText,
        onAccessibilityClick = onAccessibilityClick,
        onProfileClick =
            if (enableProfileClick) {
                { viewModel.navigateToProfile(post) }
            } else {
                null
            },
        onMoreActionsClick =
            if (showMoreActions &&
                (uiModel.canBeDeleted || uiModel.canBeReported || displayGroupName)
            ) {
                { showMenuSheet = true }
            } else {
                null
            },
        onToggleTranslationClick = { viewModel.toggleDisplayedTranslation(post) },
        onReactionClick = { viewModel.toggleReaction(post, it) },
        onCommentButtonTap = onCommentButtonTap,
        onOpenGroup = { viewModel.navigateToGroup(post) },
        onVoteClick = { viewModel.pollVote(post, it) },
        onVoteAttempt = { viewModel.pollVoteAttempt(post, it) },
        onImageClick = { index, source ->
            val images =
                ((uiModel.content as? PostContent.Published)?.media)
                    ?.imageMedias
                    .orEmpty()
            imageViewer.openOrElse(
                key = ImageViewerKey(kind = ImageViewerKey.Kind.Post, id = post.id, index = index),
                medias = images,
                source = source,
            ) { viewModel.navigateTo(PostMedias(postId = post.id, selectedIndex = index)) }
        },
        onCtaClick = {
            val cta = (uiModel.content as? PostContent.Published)?.cta
            when (cta?.action) {
                is PostCta.Action.Bridge -> viewModel.navigateToClientObject(post)
                is PostCta.Action.OpenUrl -> viewModel.navigateToCustomAction(post)
                null -> {}
            }
        },
    )

    if (showMenuSheet) {
        PostMenuSheet(
            post = post,
            onDismiss = { showMenuSheet = false },
            onViewGroup =
                if (displayGroupName) {
                    { viewModel.navigateTo(GroupDetails(groupId = post.parentId)) }
                } else {
                    null
                },
        )
    }
}

/**
 * Pure renderer — no VM, no Koin. Composes the nine content blocks, owns the shared reactions
 * tooltip state + accessibility grouping toggle, and handles moderated vs published dispatch.
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
)
@Composable
internal fun PostCard(
    uiModel: PostUiModel,
    currentUserProfile: CurrentUserProfile?,
    communityConfig: CommunityConfig?,
    displayMode: PostDisplayMode,
    translated: Boolean,
    playVideo: Boolean,
    onAccessibilityClick: (() -> Unit)?,
    onProfileClick: (() -> Unit)?,
    onMoreActionsClick: (() -> Unit)?,
    onToggleTranslationClick: () -> Unit,
    onReactionClick: (Reaction.Kind) -> Unit,
    onCommentButtonTap: () -> Unit,
    onVoteClick: (Post.Poll.Answer) -> Unit,
    onImageClick: (index: Int, source: ImageViewerSource) -> Unit,
    onCtaClick: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Forces the post text to render fully even in summary mode. See the VM-aware overload
     * for the full rationale (iOS `PostSummaryView.isTextExpanded`, internal change).
     */
    forceFullText: Boolean = false,
    onVoteAttempt: ((Post.Poll.Answer) -> Deferred<PollVoteOutcome>)? = null,
    onOpenGroup: () -> Unit = {},
) = with(uiModel) {
    var groupedForAccessibility by remember { mutableStateOf(true) }
    val reactionsTooltipState = rememberTooltipState(isPersistent = true)

    val authorName = author.getUiNickname(currentUserProfile).asString()
    val elapsedTime = creationDate.getShortElapsedTime()
    val isSummary = displayMode is PostDisplayMode.Summary
    val displayGroupName = (displayMode as? PostDisplayMode.Summary)?.displayGroupName ?: true
    val headerGroupName = if (displayGroupName) group?.name else null

    val gamificationLevelDisplay =
        communityConfig?.gamification?.let { gamif ->
            with(gamif) { author?.gamificationLevel }
        }

    val accessibilitySummary =
        buildAccessibilitySummary(
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
            if (onProfileClick != null) {
                add(
                    CustomAccessibilityAction(
                        label = stringResource(R.string.accessibility_content_action_view_author),
                        action = {
                            onProfileClick()
                            true
                        },
                    ),
                )
            }
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
    val openDetailHint = stringResource(R.string.accessibility_post_list_open_detail_hint_formal)

    val postOptions = communityConfig?.contentOptions?.post
    val onGroupClick = displayMode.groupNameTapAction(postOptions, onOpenGroup)
    val groupTouchTarget =
        rememberPostHeaderGroupTouchTarget(
            onClick = onGroupClick,
            extendToHeaderBottom = postOptions?.groupNameTapOpensGroup == true,
            groupName = headerGroupName,
        )
    val extraGroupInput =
        if (groupTouchTarget.enabled) {
            Modifier.onGloballyPositioned { groupTouchTarget.area.input = it }.postHeaderGroupTouchArea(groupTouchTarget)
        } else {
            Modifier
        }
    // Figma expects tight layout (e.g. Post Footer = 44+1+44=89dp). Material3's default
    // 48dp minimum touch target bloats clickable blocks (counts, like, comment, translation
    // toggle) to 48dp each, adding ~8dp excess to every footer. Disable the enforcement here
    // so blocks render at Figma heights — iso iOS which has no equivalent min-touch guard.
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Column(
            modifier =
                modifier
                    .then(
                        if (groupedForAccessibility) {
                            Modifier.semantics(mergeDescendants = true) {
                                this.contentDescription = accessibilitySummary
                                this.customActions = accessibilityCustomActions
                                if (onAccessibilityClick != null) {
                                    this.onClick(
                                        label = openDetailHint,
                                        action = {
                                            onAccessibilityClick()
                                            true
                                        },
                                    )
                                }
                            }
                        } else {
                            Modifier.semantics {
                                this.customActions = accessibilityCustomActions
                                if (onAccessibilityClick != null) {
                                    this.onClick(
                                        label = openDetailHint,
                                        action = {
                                            onAccessibilityClick()
                                            true
                                        },
                                    )
                                }
                            }
                        },
                    ).then(extraGroupInput),
        ) {
            PostHeader(
                profile = author,
                authorName = authorName,
                relativeDate = elapsedTime,
                gamificationLevel = gamificationLevelDisplay,
                groupName = headerGroupName,
                onProfileClick = onProfileClick,
                onGroupClick = onGroupClick,
                sharedGroupTouchTarget = groupTouchTarget,
                onMoreActionsClick = onMoreActionsClick,
            )
            // The translation toggle opens the content, right under the header, so a member sees
            // the post is translated before reading it — iso iOS. Any translated part the toggle
            // switches (text, poll options, catch phrase) shows it; the call to action does not, as
            // it is always shown translated.
            val publishedContent = content as? PostContent.Published
            if (publishedContent != null && hasTranslation) {
                PostTranslationToggle(
                    touchTargetModifier = Modifier.onGloballyPositioned { groupTouchTarget.area.translationToggle = it },
                    translated = translated,
                    originalLanguage = publishedContent.text.original.language,
                    onClick = onToggleTranslationClick,
                )
            } else {
                groupTouchTarget.area.translationToggle = null
            }
            PostTags(tags = tags)

            when (val content = content) {
                is PostContent.Moderated -> {
                    // iso iOS `PostView.swift:144-153`: moderated branch renders a semibold catch-
                    // phrase (Post.List.ModeratedPost.MainText) followed by a body2 reason line,
                    // mirroring the published `PostCatchPhrase` + `PostTextContent` block geometry
                    // (horizontal 16 + vertical 4) so paddings line up regardless of branch.
                    Text(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        text = stringResource(R.string.post_list_moderated_post_main_text),
                        color = OctopusTheme.colorScheme.gray900,
                        // Single-line moderated headline. Default lineHeight matches Figma
                        // visually without the regression observed when forcing 24sp interacts
                        // with the surrounding column rhythm.
                        style = OctopusTheme.typography.body2.semiBold,
                    )
                    Text(
                        // Extra 8.dp bottom under the reason text, iso iOS small-fixes commit
                        // 804ac41 (`PostView.swift:174 .padding(.bottom, 8)`).
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
                        text = content.reasons.uiText.asString(),
                        color = OctopusTheme.colorScheme.gray900,
                        // Mirrors `PostTextContent` typography iso Figma (16/24 regular).
                        style = OctopusTheme.typography.body2.lineHeight150Trimmed,
                    )
                }

                is PostContent.Published -> {
                    content.catchPhrase?.let { catchPhrase ->
                        PostCatchPhrase(text = catchPhrase, translated = translated)
                    }
                    PostTextContent(
                        text = content.text,
                        translated = translated,
                        ellipsize = isSummary && !forceFullText,
                    )
                    // Design order, iso iOS: text → media → poll → CTA, the media keeping 8dp under
                    // it when a poll follows (the poll adds its own 8dp top padding). A video takes
                    // the same slot as an image.
                    content.media?.let { media ->
                        PostMediaBlock(
                            media = media,
                            postId = id,
                            hasText = content.text.getText(translated = translated).isNotBlank(),
                            playVideo = playVideo,
                            onImageClick = { index, source ->
                                displayMode.imageTapAction(
                                    options = postOptions,
                                    canOpenDetail = canCreateChildren || commentCount > 0,
                                ) { onImageClick(index, source) }()
                            },
                        )
                        if (content.poll != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    content.poll?.let { poll ->
                        PostPollContent(
                            poll = poll.poll,
                            currentUserVote = poll.currentUserVote,
                            translated = translated,
                            onVote = onVoteClick,
                            onVoteAttempt = onVoteAttempt,
                        )
                    }
                    content.cta?.let { cta ->
                        PostCTAContent(
                            text = cta.text.getText(translated = true),
                            onClick = onCtaClick,
                        )
                    }
                    // One-line footer (internal ticket): like · comment, then the top reactions on the
                    // right. The view count is no longer shown.
                    PostFooter(
                        contentId = id,
                        state =
                            postFooterState(
                                reactions = reactions,
                                userReaction = userReaction,
                                commentCount = commentCount,
                                canCreateChildren = canCreateChildren,
                            ),
                        reactions = reactions,
                        onReactionClick = onReactionClick,
                        onCommentClick = onCommentButtonTap,
                        tooltipState = reactionsTooltipState,
                    )
                }
            }
        }
    }
}

@Composable
private fun buildAccessibilitySummary(
    uiModel: PostUiModel,
    authorName: String,
    dateText: String,
    translated: Boolean,
): String =
    with(uiModel) {
        when (val content = content) {
            is PostContent.Moderated ->
                stringResource(
                    R.string.accessibility_post_summary_moderated_author,
                    authorName,
                    dateText,
                    content.reasons.uiText.asString(),
                )
            is PostContent.Published -> {
                val text = content.text.getText(translated = translated)
                val template =
                    accessibilitySummaryTemplate(
                        media = content.media,
                        hasText = text.isNotBlank(),
                        hasPoll = content.poll != null,
                    )
                val quotedText =
                    if (template.quotesText) {
                        listOf(
                            accessibilitySummaryText(
                                text = text,
                                poll = content.poll,
                                translated = translated,
                                selectedSuffix = stringResource(R.string.accessibility_common_selected),
                            ),
                        )
                    } else {
                        emptyList()
                    }
                val formatArgs = listOf(authorName, dateText, group?.name ?: "") + quotedText + listOfNotNull(template.imageCount)
                stringResource(template.resId, *formatArgs.toTypedArray())
            }
        }
    }

/** The post's image or video, between the text and the poll. */
@Composable
private fun PostMediaBlock(
    media: PostMedia,
    postId: String,
    hasText: Boolean,
    playVideo: Boolean,
    onImageClick: (index: Int, source: ImageViewerSource) -> Unit,
) {
    @Suppress("DEPRECATION")
    when (media) {
        is PostMedia.Image ->
            PostMediaContent(
                images = listOf(media.media),
                postId = postId,
                hasText = hasText,
                onClick = onImageClick,
            )
        is PostMedia.Images ->
            PostMediaContent(images = media.medias, postId = postId, hasText = hasText, onClick = onImageClick)
        is PostMedia.Video -> PostMediaContent(video = media.media, postId = postId, autoPlay = playVideo)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Preview(showBackground = true)
@Composable
private fun PostCardSummaryPreview() {
    OctopusTheme {
        PostCard(
            uiModel =
                MockPosts.default
                    .copy(
                        userReaction = Reaction(Reaction.Kind.Joy),
                    ).toUiModel(MockCurrentUserProfiles.default),
            currentUserProfile = MockCurrentUserProfiles.default,
            communityConfig = MockCommunityConfig.default,
            displayMode =
                PostDisplayMode.Summary(
                    onCardTap = {},
                    onChildrenTap = {},
                    displayGroupName = true,
                ),
            translated = true,
            playVideo = false,
            onAccessibilityClick = {},
            onProfileClick = {},
            onMoreActionsClick = {},
            onToggleTranslationClick = {},
            onReactionClick = {},
            onCommentButtonTap = {},
            onVoteClick = {},
            onImageClick = { _, _ -> },
            onCtaClick = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Preview(showBackground = true)
@Composable
private fun PostCardPollPreview() {
    OctopusTheme {
        PostCard(
            uiModel = MockPosts.withPoll.toUiModel(MockCurrentUserProfiles.default),
            currentUserProfile = MockCurrentUserProfiles.default,
            communityConfig = MockCommunityConfig.default,
            displayMode =
                PostDisplayMode.Summary(
                    onCardTap = {},
                    onChildrenTap = {},
                    displayGroupName = true,
                ),
            translated = false,
            playVideo = false,
            onAccessibilityClick = null,
            onProfileClick = null,
            onMoreActionsClick = null,
            onToggleTranslationClick = {},
            onReactionClick = {},
            onCommentButtonTap = {},
            onVoteClick = {},
            onImageClick = { _, _ -> },
            onCtaClick = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Preview(showBackground = true)
@Composable
private fun PostCardClientObjectPreview() {
    OctopusTheme {
        PostCard(
            uiModel = MockPosts.withClientObject.toUiModel(MockCurrentUserProfiles.default),
            currentUserProfile = MockCurrentUserProfiles.default,
            communityConfig = MockCommunityConfig.default,
            displayMode =
                PostDisplayMode.Summary(
                    onCardTap = {},
                    onChildrenTap = {},
                    displayGroupName = false,
                ),
            translated = false,
            playVideo = false,
            onAccessibilityClick = null,
            onProfileClick = {},
            onMoreActionsClick = null,
            onToggleTranslationClick = {},
            onReactionClick = {},
            onCommentButtonTap = {},
            onVoteClick = {},
            onImageClick = { _, _ -> },
            onCtaClick = {},
        )
    }
}
