package com.octopuscommunity.sdk.ui.posts.create

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.octopuscommunity.sdk.InternalOctopusApi
import com.octopuscommunity.sdk.domain.model.Post
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.UiText.StringResource
import com.octopuscommunity.sdk.ui.common.snackbar.OctopusSnackbarHost
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModel
import com.octopuscommunity.sdk.ui.components.NavigationIconType
import com.octopuscommunity.sdk.ui.components.OctopusAlertDialog
import com.octopuscommunity.sdk.ui.components.OctopusConfirmAlertDialog
import com.octopuscommunity.sdk.ui.components.OctopusContent
import com.octopuscommunity.sdk.ui.components.OctopusScaffold
import com.octopuscommunity.sdk.ui.components.OctopusScreenContent
import com.octopuscommunity.sdk.ui.components.OctopusSmallButton
import com.octopuscommunity.sdk.ui.components.OctopusSystemBackHandler
import com.octopuscommunity.sdk.ui.components.OctopusTextField
import com.octopuscommunity.sdk.ui.consent.ConsentContribution
import com.octopuscommunity.sdk.ui.consent.ImplicitConsentNotice
import com.octopuscommunity.sdk.ui.consent.TermsConsentSheet
import com.octopuscommunity.sdk.ui.consent.resolveConsentWordings
import com.octopuscommunity.sdk.ui.consent.resolveImplicitNotice
import com.octopuscommunity.sdk.ui.groups.selection.GroupSelectionSheet
import com.octopuscommunity.sdk.ui.medium
import com.octopuscommunity.sdk.ui.posts.components.PollField
import com.octopuscommunity.sdk.ui.profile.current.avatar.CurrentUserAvatar
import com.octopuscommunity.sdk.ui.utils.rememberImagesPicker
import com.octopuscommunity.sdk.ui.utils.safeImePadding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf
import kotlin.time.Duration.Companion.seconds

/**
 * The post editor. The `prefilled…` parameters carry a Bridge Share prefill: [prefilledImageUris]
 * holds the stringified content URIs of its images, in display order. [prefilledImageUri] is the
 * deprecated single image, read only when [prefilledImageUris] is empty. A CTA label given per
 * language travels as [prefilledCustomActionLabel], the label of the default language,
 * [prefilledCustomActionLabelLanguage], the code of that language, and
 * [prefilledCustomActionLabelTranslations], the other labels by language code.
 */
@OptIn(InternalOctopusApi::class)
@Composable
fun CreatePostScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    type: Post.Draft.Type = Post.Draft.Type.TEXT,
    groupId: String? = null,
    prefilledText: String? = null,
    prefilledImageUri: String? = null,
    prefilledCustomActionLabel: String? = null,
    prefilledCustomActionUrl: String? = null,
    prefilledImageUris: List<String> = emptyList(),
    prefilledCustomActionLabelLanguage: String? = null,
    prefilledCustomActionLabelTranslations: Map<String, String> = emptyMap(),
) = OctopusContent(navController) {
    val focusManager = LocalFocusManager.current
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current

    val viewModel =
        octopusViewModel<CreatePostViewModel>(
            key = "createPost_${type}_$groupId",
            parameters = {
                parametersOf(
                    CreatePostParams(
                        type = type,
                        groupId = groupId,
                        prefilledText = prefilledText,
                        prefilledImageUris = prefilledImageUris.ifEmpty { listOfNotNull(prefilledImageUri) },
                        prefilledCustomActionLabel = prefilledCustomActionLabel,
                        prefilledCustomActionUrl = prefilledCustomActionUrl,
                        prefilledCustomActionLabelLanguage = prefilledCustomActionLabelLanguage,
                        prefilledCustomActionLabelTranslations = prefilledCustomActionLabelTranslations,
                    ),
                )
            },
        )
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Bridge Share: hydrate the prefilled image asynchronously. The VM
    // cannot build [Media.Image] in its constructor because it needs a
    // [Context]; the call below routes through the same validation path
    // as a user-picked image.
    LaunchedEffect(prefilledImageUris, prefilledImageUri) {
        viewModel.applyPrefilledImages(context)
    }

    // Bridge Share: a prefilled-share signer may have been wired into the SDK by the create-post
    // entry point (it can't ride the type-safe nav route, so it travels through a holder). Clear
    // it when this editor leaves composition so the provider stays scoped to this session and the
    // host's closure isn't retained past the editor's lifetime. Both entry points funnel through
    // this screen, so this covers the nav-graph and host-hosted paths alike. In the normal
    // publish-then-close flow sendPost reads the provider before the editor is dismissed (publish
    // navigates away only on success), so this clear runs after the read.
    val bridgeShareHolder = org.koin.compose.koinInject<com.octopuscommunity.sdk.domain.BridgeShareTokenProviderHolder>()
    DisposableEffect(bridgeShareHolder) {
        onDispose { bridgeShareHolder.tokenProvider = null }
    }

    OctopusScaffold(
        modifier =
            modifier then
                Modifier
                    .fillMaxSize()
                    .background(OctopusTheme.colorScheme.background),
        viewModel = viewModel,
        navigationIcon = NavigationIconType.Close,
        onBack = {
            focusManager.clearFocus()
            viewModel.onBack()
        },
        title = stringResource(R.string.post_create_title),
        actions = {
            OctopusSmallButton(
                modifier = Modifier.padding(end = 8.dp),
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.sendPost()
                },
                text = stringResource(R.string.post_create_button),
                enabled = state.canSend,
                loading = state.isSending,
            )
        },
    ) { innerPadding ->
        CreatePostContent(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
            navController = navController,
            type = type,
            groupId = groupId,
            prefilledText = prefilledText,
            prefilledCustomActionLabel = prefilledCustomActionLabel,
            prefilledCustomActionUrl = prefilledCustomActionUrl,
            prefilledImageUris = prefilledImageUris.ifEmpty { listOfNotNull(prefilledImageUri) },
            prefilledCustomActionLabelLanguage = prefilledCustomActionLabelLanguage,
            prefilledCustomActionLabelTranslations = prefilledCustomActionLabelTranslations,
        )
    }
}

/**
 * The content of [CreatePostScreen], without its scaffold. [prefilledImageUris], the deprecated
 * [prefilledImageUri] and the per-language CTA labels work as on [CreatePostScreen].
 */
@Composable
fun CreatePostContent(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    type: Post.Draft.Type = Post.Draft.Type.TEXT,
    groupId: String? = null,
    prefilledText: String? = null,
    prefilledImageUri: String? = null,
    prefilledCustomActionLabel: String? = null,
    prefilledCustomActionUrl: String? = null,
    prefilledImageUris: List<String> = emptyList(),
    prefilledCustomActionLabelLanguage: String? = null,
    prefilledCustomActionLabelTranslations: Map<String, String> = emptyMap(),
) = OctopusScreenContent(
    modifier = modifier,
    navController = navController,
) { screenViewModel ->
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    val viewModel =
        octopusViewModel<CreatePostViewModel>(
            key = "createPost_${type}_$groupId",
            parameters = {
                parametersOf(
                    CreatePostParams(
                        type = type,
                        groupId = groupId,
                        prefilledText = prefilledText,
                        prefilledImageUris = prefilledImageUris.ifEmpty { listOfNotNull(prefilledImageUri) },
                        prefilledCustomActionLabel = prefilledCustomActionLabel,
                        prefilledCustomActionUrl = prefilledCustomActionUrl,
                        prefilledCustomActionLabelLanguage = prefilledCustomActionLabelLanguage,
                        prefilledCustomActionLabelTranslations = prefilledCustomActionLabelTranslations,
                    ),
                )
            },
        )

    val state by viewModel.state.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()

    val textFocusRequester = remember { FocusRequester() }
    var textHasFocus by remember { mutableStateOf(false) }
    val bringTextIntoViewRequester = remember { BringIntoViewRequester() }
    // Scrolls to the image or the poll the member just added — armed by the buttons below only,
    // never by the Bridge Share prefill hydrated at opening.
    val addedBlockRevealer = rememberAddedBlockRevealer(scrollState)
    // Publishing is the member moving on, like a drag or a keystroke: the reveal ends there.
    LaunchedEffect(state.isSending) {
        if (state.isSending) addedBlockRevealer.stop()
    }

    val post = state.post

    // System back: the NavHost pops on its own (keeping the predictive-back preview) except
    // while a discard-post dialog must be shown first, or at the SDK back-stack root, where
    // NavigateUp falls back to the host `onBack` (bridge mode). Never enabled at the root
    // without a host `onBack` (internal change). See OctopusSystemBackHandler.
    OctopusSystemBackHandler(
        navController = navController,
        hasPendingConfirmation = viewModel.hasUnsavedChanges,
        onBack = viewModel::onBack,
    )

    var showTopicSelection by remember { mutableStateOf(false) }

    var textFieldValue by remember(state.isLoading) {
        mutableStateOf(
            TextFieldValue(
                text = post.text,
                selection = TextRange(post.text.length),
            ),
        )
    }
    // Update the text when post.text changes while preserving cursor
    LaunchedEffect(post.text) {
        if (textFieldValue.text != post.text) {
            textFieldValue = textFieldValue.copy(text = post.text)
        }
    }

    LaunchedEffect(state.showTopicSelection) {
        if (state.showTopicSelection) {
            // Clear focus first to hide keyboard
            focusManager.clearFocus()
            // Wait for keyboard hide animation (typically 300ms)
            delay(0.3.seconds)
            showTopicSelection = true
        } else {
            showTopicSelection = false
        }
    }

    val imagePicker =
        rememberImagesPicker(
            remainingSlots = state.remainingPictureSlots,
            onImagesSelected = { uris ->
                viewModel.addPictures(context = context, uris = uris)
                addedBlockRevealer.reveal(AddedBlock.Image)
            },
        )
    var showMaxPicturesAlert by remember { mutableStateOf(false) }
    // Opens the picker for the pictures the post can still take, or explains the limit once it holds
    // the maximum. Does nothing when the community disables pictures, whichever entry calls it.
    val openPictureEntry = {
        if (state.showAddPicture) {
            if (state.pictureEntryShowsMaxAlert) showMaxPicturesAlert = true else imagePicker.launch()
        }
    }

    val colorScheme = OctopusTheme.colorScheme

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .safeImePadding(),
    ) {
        Box(
            modifier = Modifier.weight(1f),
        ) {
            // The added-block reveal needs this column's size to follow the layout at once: animating
            // its size or a block's insertion would clamp the scroll to 0 on a short draft. Animate
            // inside a block instead, so the node carrying `revealWhenAdded` sees each animated size
            // (as `PollField` does).
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(ContentPadding)
                        .endsRevealOnTouch(addedBlockRevealer)
                        .verticalScroll(scrollState),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CurrentUserAvatar(
                        modifier = Modifier.size(32.dp),
                    )
                    val chooseTopicText =
                        post.group?.name
                            ?: stringResource(R.string.post_create_group_selection_button)
                    Box(
                        modifier =
                            Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(64.dp))
                                .background(OctopusTheme.colorScheme.gray200)
                                .then(
                                    if (state.isEditable) {
                                        Modifier.clickable { viewModel.showTopicSelection() }
                                    } else {
                                        Modifier
                                    },
                                ).semantics(mergeDescendants = true) {
                                    role = Role.ValuePicker
                                    contentDescription = chooseTopicText
                                }.padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = chooseTopicText,
                                color = OctopusTheme.colorScheme.gray900,
                                style = OctopusTheme.typography.body2.medium,
                            )
                            Icon(
                                painter =
                                    OctopusTheme.icons.content.post.creation
                                        .topicSelection(),
                                contentDescription = "Expand topics",
                                tint = OctopusTheme.colorScheme.gray700,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OctopusTextField(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .bringIntoViewRequester(bringTextIntoViewRequester)
//                        .verticalScroll(scrollState, enabled = true)
                            .focusRequester(textFocusRequester)
                            .onGloballyPositioned {
                                if (!textHasFocus) {
                                    textFocusRequester.requestFocus()
                                    textHasFocus = true
                                }
                            },
                    value = textFieldValue,
                    onValueChange = {
                        // Selection-only changes (focus coming back after the picker) are not typing.
                        if (it.text != textFieldValue.text) addedBlockRevealer.stop()
                        textFieldValue = it
                        viewModel.updateText(it.text)
                    },
                    enabled = state.isEditable,
                    textStyle = OctopusTheme.typography.body2,
                    placeholder = stringResource(R.string.post_create_text_placeholder_formal),
                    keyboardOptions =
                        KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                        ),
                    minLines = 2,
                    maxLines = 300,
                    contentPadding = PaddingValues(vertical = 8.dp),
                    onTextLayout = {
                        // Get Text Field cursor position
                        val cursorRect = it.getCursorRect(textFieldValue.selection.start)
                        coroutineScope.launch {
                            // Scroll to Text Field cursor position by typing
                            bringTextIntoViewRequester.bringIntoView(
                                cursorRect.copy(
                                    bottom = cursorRect.bottom + 64,
                                ),
                            )
                        }
                    },
                )

                // The images are always above the poll, whichever was added first.
                if (post.images.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CreatePostImagesStrip(
                        modifier =
                            Modifier
                                .extendToTrailingEdge(ContentPadding)
                                .revealWhenAdded(
                                    block = AddedBlock.Image,
                                    revealer = addedBlockRevealer,
                                    scrollState = scrollState,
                                ),
                        images = post.images,
                        showsAddMoreTile = state.showsAddMoreTile,
                        enabled = state.isEditable,
                        faultyImage = state.faultyImage,
                        onDelete = { index ->
                            addedBlockRevealer.stop()
                            viewModel.removePicture(index = index)
                        },
                        onAddMore = openPictureEntry,
                    )
                }

                post.poll?.let { poll ->
                    Spacer(modifier = Modifier.height(16.dp))
                    PollField(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .revealWhenAdded(
                                    block = AddedBlock.Poll,
                                    revealer = addedBlockRevealer,
                                    scrollState = scrollState,
                                ),
                        poll = poll,
                        enabled = state.isEditable,
                        focused = post.text.isNotEmpty(),
                        errors = state.pollAnswersErrors,
                        onPollChanged = {
                            addedBlockRevealer.stop()
                            viewModel.updatePoll(it)
                        },
                    )
                }

                if (post.images.isEmpty() && post.poll == null) {
                    // When there is no picture displayed, use the remaining part of the screen to
                    // catch tap
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                ) {
                                    textFocusRequester.requestFocus()
                                }.semantics { hideFromAccessibility() },
                    )
                }
                if (state.showActionBar) {
                    // Room for the floating action bar, so the last block can scroll above it.
                    Spacer(modifier = Modifier.height(CreatePostActionBarHeight))
                }
            }
            // The entries that add a photo or a poll float over the form: the content scrolls under them.
            if (state.showActionBar) {
                CreatePostActionBar(
                    modifier =
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(horizontal = ContentPadding),
                    showAddPicture = state.showAddPicture,
                    showAddPoll = state.showAddPoll,
                    enabled = !state.isSending,
                    onAddPicture = openPictureEntry,
                    onAddPoll = {
                        viewModel.updatePoll(Post.Draft.Poll())
                        addedBlockRevealer.reveal(AddedBlock.Poll)
                    },
                )
            }
            OctopusSnackbarHost(
                modifier = Modifier.align(Alignment.BottomCenter),
                screenViewModel = screenViewModel,
            )
        }

        val errors =
            listOfNotNull(
                state.topicError?.message,
                state.textError?.message,
                state.mediaError?.message,
                state.pollError?.message,
                state.otherError?.message,
            )
        if (errors.isNotEmpty()) {
            HorizontalDivider(color = OctopusTheme.colorScheme.alert)

            errors.forEach { error ->
                Text(
                    modifier =
                        Modifier
                            .padding(
                                horizontal = 16.dp,
                                vertical = 8.dp,
                            ),
                    text = error.asString(),
                    color = OctopusTheme.colorScheme.alert,
                    style = OctopusTheme.typography.caption2,
                )
            }
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(OctopusTheme.colorScheme.background)
                    .animateContentSize(),
        ) {
            if (state.showCguText) {
                HorizontalDivider(color = OctopusTheme.colorScheme.gray300)

                ImplicitConsentNotice(
                    wording = resolveImplicitNotice(mode = state.termsAcceptanceMode, consentTexts = state.consentTexts),
                    onNavigateToTermsOfUse = viewModel::navigateToTermsOfUse,
                    onNavigateToPrivacyPolicy = viewModel::navigateToPrivacyPolicy,
                    onNavigateToCommunityGuidelines = viewModel::navigateToCommunityGuidelines,
                    onNavigateToUrl = viewModel::navigateToUrl,
                )
            }
        }
    }
    GroupSelectionSheet(
        selectedGroup = post.group,
        visible = showTopicSelection,
        onGroupSelected = { viewModel.updateGroup(it) },
        onDismiss = { viewModel.showTopicSelection(false) },
    )

    TermsConsentSheet(
        visible = state.showConsentSheet,
        mode = state.termsAcceptanceMode,
        contribution = ConsentContribution.POST,
        wordings =
            resolveConsentWordings(
                mode = state.termsAcceptanceMode,
                consentTexts = state.consentTexts,
                contribution = ConsentContribution.POST,
            ),
        nicknameChoiceFollows = state.consentSheetLeadsToNicknameChoice,
        onAccept = { viewModel.acceptConsentAndSendPost() },
        onDismiss = { viewModel.dismissConsentSheet() },
        onNavigateToTerms = { viewModel.navigateToTermsOfUse() },
        onNavigateToPrivacy = { viewModel.navigateToPrivacyPolicy() },
        onNavigateToRules = { viewModel.navigateToCommunityGuidelines() },
        onNavigateToUrl = { viewModel.navigateToUrl(it) },
    )

    if (showMaxPicturesAlert) {
        OctopusAlertDialog(
            text = StringResource(R.string.content_create_picture_max_reached_formal),
            onDismiss = { showMaxPicturesAlert = false },
        )
    }

    if (state.showUnsavedChangesDialog) {
        OctopusConfirmAlertDialog(
            text = stringResource(R.string.common_cancel_modifications_formal),
            confirmButtonText = stringResource(R.string.common_yes),
            dismissButtonText = stringResource(R.string.common_no),
            onConfirm = {
                viewModel.dismissUnsavedChangesDialog(true)
                focusManager.clearFocus()
            },
            onDismiss = { viewModel.dismissUnsavedChangesDialog(false) },
        )
    }
}

/** [CreatePostScreen] as it was before the per-language CTA labels, kept so code compiled against it keeps linking. */
@Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
@Composable
fun CreatePostScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    type: Post.Draft.Type = Post.Draft.Type.TEXT,
    groupId: String? = null,
    prefilledText: String? = null,
    prefilledImageUri: String? = null,
    prefilledCustomActionLabel: String? = null,
    prefilledCustomActionUrl: String? = null,
    prefilledImageUris: List<String> = emptyList(),
) = CreatePostScreen(
    navController = navController,
    modifier = modifier,
    type = type,
    groupId = groupId,
    prefilledText = prefilledText,
    prefilledImageUri = prefilledImageUri,
    prefilledCustomActionLabel = prefilledCustomActionLabel,
    prefilledCustomActionUrl = prefilledCustomActionUrl,
    prefilledImageUris = prefilledImageUris,
    prefilledCustomActionLabelLanguage = null,
    prefilledCustomActionLabelTranslations = emptyMap(),
)

/** [CreatePostContent] as it was before the per-language CTA labels, kept so code compiled against it keeps linking. */
@Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
@Composable
fun CreatePostContent(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    type: Post.Draft.Type = Post.Draft.Type.TEXT,
    groupId: String? = null,
    prefilledText: String? = null,
    prefilledImageUri: String? = null,
    prefilledCustomActionLabel: String? = null,
    prefilledCustomActionUrl: String? = null,
    prefilledImageUris: List<String> = emptyList(),
) = CreatePostContent(
    navController = navController,
    modifier = modifier,
    type = type,
    groupId = groupId,
    prefilledText = prefilledText,
    prefilledImageUri = prefilledImageUri,
    prefilledCustomActionLabel = prefilledCustomActionLabel,
    prefilledCustomActionUrl = prefilledCustomActionUrl,
    prefilledImageUris = prefilledImageUris,
    prefilledCustomActionLabelLanguage = null,
    prefilledCustomActionLabelTranslations = emptyMap(),
)

/** [CreatePostScreen] as it was before `prefilledImageUris`, kept so code compiled against it keeps linking. */
@Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
@Composable
fun CreatePostScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    type: Post.Draft.Type = Post.Draft.Type.TEXT,
    groupId: String? = null,
    prefilledText: String? = null,
    prefilledImageUri: String? = null,
    prefilledCustomActionLabel: String? = null,
    prefilledCustomActionUrl: String? = null,
) = CreatePostScreen(
    navController = navController,
    modifier = modifier,
    type = type,
    groupId = groupId,
    prefilledText = prefilledText,
    prefilledImageUri = prefilledImageUri,
    prefilledCustomActionLabel = prefilledCustomActionLabel,
    prefilledCustomActionUrl = prefilledCustomActionUrl,
    prefilledImageUris = emptyList(),
)

/** [CreatePostContent] as it was before `prefilledImageUris`, kept so code compiled against it keeps linking. */
@Deprecated("Kept for binary compatibility.", level = DeprecationLevel.HIDDEN)
@Composable
fun CreatePostContent(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    type: Post.Draft.Type = Post.Draft.Type.TEXT,
    groupId: String? = null,
    prefilledText: String? = null,
    prefilledImageUri: String? = null,
    prefilledCustomActionLabel: String? = null,
    prefilledCustomActionUrl: String? = null,
) = CreatePostContent(
    navController = navController,
    modifier = modifier,
    type = type,
    groupId = groupId,
    prefilledText = prefilledText,
    prefilledImageUri = prefilledImageUri,
    prefilledCustomActionLabel = prefilledCustomActionLabel,
    prefilledCustomActionUrl = prefilledCustomActionUrl,
    prefilledImageUris = emptyList(),
)

@Preview(showBackground = true)
@Composable
private fun CreatePostScreenPreview() {
    OctopusTheme {
        CreatePostScreen(rememberNavController())
    }
}

@Preview(showBackground = true)
@Composable
private fun CreatePostScreenPollPreview() {
    OctopusTheme {
        CreatePostScreen(
            navController = rememberNavController(),
            type = Post.Draft.Type.POLL,
        )
    }
}

/** Horizontal margin of the composer's content. */
private val ContentPadding = 16.dp

/**
 * Lets the element run past the end of the [padding] its parent applies on its trailing side, so it
 * reaches the screen edge: it is measured [padding] wider and placed at its usual start.
 */
private fun Modifier.extendToTrailingEdge(padding: Dp): Modifier =
    layout { measurable, constraints ->
        val extra = padding.roundToPx()
        val placeable =
            measurable.measure(
                constraints.copy(
                    minWidth = constraints.minWidth + extra,
                    maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth + extra else constraints.maxWidth,
                ),
            )
        layout(width = (placeable.width - extra).coerceAtLeast(0), height = placeable.height) {
            placeable.placeRelative(0, 0)
        }
    }
