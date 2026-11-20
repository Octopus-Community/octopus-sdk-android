package com.octopuscommunity.sdk.ui.posts.create

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.core.net.toUri
import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.domain.model.CommunityConfig.ConsentTexts
import com.octopuscommunity.sdk.domain.model.CommunityConfig.TermsAcceptanceMode
import com.octopuscommunity.sdk.domain.model.CreationSource
import com.octopuscommunity.sdk.domain.model.Media
import com.octopuscommunity.sdk.domain.model.OctopusEvent.ScreenDisplayed
import com.octopuscommunity.sdk.domain.model.OctopusGroup
import com.octopuscommunity.sdk.domain.model.Post
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.GroupRepository
import com.octopuscommunity.sdk.domain.repository.PostCreationError
import com.octopuscommunity.sdk.domain.usecase.SendPostUseCase
import com.octopuscommunity.sdk.ui.OctopusDestination.ValidateNickname
import com.octopuscommunity.sdk.ui.common.validation.ValidateImage
import com.octopuscommunity.sdk.ui.common.validation.ValidateText
import com.octopuscommunity.sdk.ui.common.validation.ValidationError
import com.octopuscommunity.sdk.ui.common.validation.findError
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusViewModel
import com.octopuscommunity.sdk.ui.consent.shouldPresentConsentSheet
import com.octopuscommunity.sdk.ui.profile.current.nickname.NicknameChoiceContribution
import com.octopuscommunity.sdk.ui.profile.current.nickname.NicknameChoiceHold
import com.octopuscommunity.sdk.ui.profile.current.nickname.NicknameChoiceSession
import com.octopuscommunity.sdk.ui.profile.current.nickname.shouldPresentNicknameChoice
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Single bag-of-primitives Koin parameter for [CreatePostViewModel].
 *
 * Wrapping the constructor primitives in a typed data class keeps the
 * Koin call sites and the VM reader in lockstep: adding or reordering
 * a prefill field is a compile error rather than a silent positional
 * mismatch. Internal to the UI module — host apps never construct it.
 */
internal data class CreatePostParams(
    val type: Post.Draft.Type,
    val groupId: String? = null,
    val prefilledText: String? = null,
    val prefilledImageUris: List<String> = emptyList(),
    val prefilledCustomActionLabel: String? = null,
    val prefilledCustomActionUrl: String? = null,
    /** Code of the language of [prefilledCustomActionLabel]; `null` when the label is auto-translated. */
    val prefilledCustomActionLabelLanguage: String? = null,
    /** The CTA labels of the other languages by language code. */
    val prefilledCustomActionLabelTranslations: Map<String, String> = emptyMap(),
)

class CreatePostViewModel internal constructor(
    commonParameters: CommonParameters,
    private val sendPostUseCase: SendPostUseCase,
    private val validateText: ValidateText,
    private val validateImage: ValidateImage,
    private val groupRepository: GroupRepository,
    private val nicknameChoiceSession: NicknameChoiceSession,
    params: CreatePostParams,
    /** Where the picked and prefilled images are read: each costs content resolver queries and a decode. */
    private val imageReadDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : OctopusViewModel(commonParameters) {
    private val type: Post.Draft.Type = params.type
    private val groupId: String? = params.groupId
    private val prefilledText: String? = params.prefilledText
    private val prefilledImageUris: List<String> = params.prefilledImageUris
    private val prefilledCustomActionLabel: String? = params.prefilledCustomActionLabel
    private val prefilledCustomActionUrl: String? = params.prefilledCustomActionUrl
    private val prefilledCustomActionLabelLanguage: String? = params.prefilledCustomActionLabelLanguage
    private val prefilledCustomActionLabelTranslations: Map<String, String> = params.prefilledCustomActionLabelTranslations

    @Immutable
    data class UiState(
        val post: Post.Draft,
        val isLoading: Boolean = false,
        val isSending: Boolean = false,
        val hasAcceptedCgu: Boolean = false,
        val showTopicSelection: Boolean = false,
        val topicError: ValidationError? = null,
        val textError: ValidationError? = null,
        val mediaError: ValidationError? = null,
        val pollError: ValidationError? = null,
        val pollAnswersErrors: Map<Post.Draft.Poll.Answer, ValidationError?> = emptyMap(),
        val otherError: ValidationError? = null,
        val showUnsavedChangesDialog: Boolean = false,
        // Community config gating. Default `true` keeps today's behaviour until the config resolves
        // (and for communities that have not been seeded). `false` hides the matching affordance.
        val enablePictures: Boolean = true,
        val enablePolls: Boolean = true,
        // Whether a post must carry text. `true` (default until the config resolves, and for
        // communities that have not been seeded) keeps today's behaviour; `false` lets a draft that
        // carries an image be published without text.
        val textRequired: Boolean = true,
        // Explicit terms-acceptance. `IMPLICIT` (default until the config resolves) keeps today's
        // behaviour; the explicit modes present the consent sheet at the first contribution.
        val termsAcceptanceMode: TermsAcceptanceMode = TermsAcceptanceMode.IMPLICIT,
        val showConsentSheet: Boolean = false,
        // The community's own consent wordings (null = the SDK's), resolved against
        // [termsAcceptanceMode] by the screen.
        val consentTexts: ConsentTexts? = null,
        // Whether the nickname choice comes right after the consent sheet: its button then reads
        // "Accept", the contribution only going out once the nickname is confirmed.
        val consentSheetLeadsToNicknameChoice: Boolean = false,
        // Whether the editor was opened with a host prefill (Bridge Share), whose images are signed.
        val isPrefilled: Boolean = false,
        // Whether the community config has resolved [enablePictures] and [enablePolls].
        val communityOptionsResolved: Boolean = false,
        // The photo the last refusal was about, for the strip to scroll to; `null` when it named none.
        val faultyImage: FaultyImage? = null,
        // Whether picked or prefilled photos are still being read: the draft does not hold them yet.
        val isReadingImages: Boolean = false,
    ) {
        /** The 1.15.x constructor, retained for Kotlin, Java and already compiled hosts. */
        constructor(
            post: Post.Draft,
            isLoading: Boolean = false,
            isSending: Boolean = false,
            hasAcceptedCgu: Boolean = false,
            showTopicSelection: Boolean = false,
            topicError: ValidationError? = null,
            textError: ValidationError? = null,
            mediaError: ValidationError? = null,
            pollError: ValidationError? = null,
            pollAnswersErrors: Map<Post.Draft.Poll.Answer, ValidationError?> = emptyMap(),
            otherError: ValidationError? = null,
            showUnsavedChangesDialog: Boolean = false,
            enablePictures: Boolean = true,
            enablePolls: Boolean = true,
            textRequired: Boolean = true,
            termsAcceptanceMode: TermsAcceptanceMode = TermsAcceptanceMode.IMPLICIT,
            showConsentSheet: Boolean = false,
            consentTexts: ConsentTexts? = null,
        ) : this(
            post = post,
            isLoading = isLoading,
            isSending = isSending,
            hasAcceptedCgu = hasAcceptedCgu,
            showTopicSelection = showTopicSelection,
            topicError = topicError,
            textError = textError,
            mediaError = mediaError,
            pollError = pollError,
            pollAnswersErrors = pollAnswersErrors,
            otherError = otherError,
            showUnsavedChangesDialog = showUnsavedChangesDialog,
            enablePictures = enablePictures,
            enablePolls = enablePolls,
            textRequired = textRequired,
            termsAcceptanceMode = termsAcceptanceMode,
            showConsentSheet = showConsentSheet,
            consentTexts = consentTexts,
            consentSheetLeadsToNicknameChoice = false,
            isPrefilled = false,
            communityOptionsResolved = false,
            faultyImage = null,
            isReadingImages = false,
        )

        /** The 1.15.x copy signature; preserves the multi-photo state fields. */
        fun copy(
            post: Post.Draft = this.post,
            isLoading: Boolean = this.isLoading,
            isSending: Boolean = this.isSending,
            hasAcceptedCgu: Boolean = this.hasAcceptedCgu,
            showTopicSelection: Boolean = this.showTopicSelection,
            topicError: ValidationError? = this.topicError,
            textError: ValidationError? = this.textError,
            mediaError: ValidationError? = this.mediaError,
            pollError: ValidationError? = this.pollError,
            pollAnswersErrors: Map<Post.Draft.Poll.Answer, ValidationError?> = this.pollAnswersErrors,
            otherError: ValidationError? = this.otherError,
            showUnsavedChangesDialog: Boolean = this.showUnsavedChangesDialog,
            enablePictures: Boolean = this.enablePictures,
            enablePolls: Boolean = this.enablePolls,
            textRequired: Boolean = this.textRequired,
            termsAcceptanceMode: TermsAcceptanceMode = this.termsAcceptanceMode,
            showConsentSheet: Boolean = this.showConsentSheet,
            consentTexts: ConsentTexts? = this.consentTexts,
        ): UiState =
            copy(
                post = post,
                isLoading = isLoading,
                isSending = isSending,
                hasAcceptedCgu = hasAcceptedCgu,
                showTopicSelection = showTopicSelection,
                topicError = topicError,
                textError = textError,
                mediaError = mediaError,
                pollError = pollError,
                pollAnswersErrors = pollAnswersErrors,
                otherError = otherError,
                showUnsavedChangesDialog = showUnsavedChangesDialog,
                enablePictures = enablePictures,
                enablePolls = enablePolls,
                textRequired = textRequired,
                termsAcceptanceMode = termsAcceptanceMode,
                showConsentSheet = showConsentSheet,
                consentTexts = consentTexts,
                consentSheetLeadsToNicknameChoice = this.consentSheetLeadsToNicknameChoice,
                isPrefilled = this.isPrefilled,
                communityOptionsResolved = this.communityOptionsResolved,
                faultyImage = this.faultyImage,
                isReadingImages = this.isReadingImages,
            )

        /** The pre-multi-photo constructor, retained for Kotlin, Java and already compiled hosts. */
        constructor(
            post: Post.Draft,
            isLoading: Boolean = false,
            isSending: Boolean = false,
            hasAcceptedCgu: Boolean = false,
            showTopicSelection: Boolean = false,
            topicError: ValidationError? = null,
            textError: ValidationError? = null,
            mediaError: ValidationError? = null,
            pollError: ValidationError? = null,
            pollAnswersErrors: Map<Post.Draft.Poll.Answer, ValidationError?> = emptyMap(),
            otherError: ValidationError? = null,
            showUnsavedChangesDialog: Boolean = false,
            enablePictures: Boolean = true,
            enablePolls: Boolean = true,
            textRequired: Boolean = true,
            termsAcceptanceMode: TermsAcceptanceMode = TermsAcceptanceMode.IMPLICIT,
            showConsentSheet: Boolean = false,
            consentTexts: ConsentTexts? = null,
            consentSheetLeadsToNicknameChoice: Boolean = false,
        ) : this(
            post = post,
            isLoading = isLoading,
            isSending = isSending,
            hasAcceptedCgu = hasAcceptedCgu,
            showTopicSelection = showTopicSelection,
            topicError = topicError,
            textError = textError,
            mediaError = mediaError,
            pollError = pollError,
            pollAnswersErrors = pollAnswersErrors,
            otherError = otherError,
            showUnsavedChangesDialog = showUnsavedChangesDialog,
            enablePictures = enablePictures,
            enablePolls = enablePolls,
            textRequired = textRequired,
            termsAcceptanceMode = termsAcceptanceMode,
            showConsentSheet = showConsentSheet,
            consentTexts = consentTexts,
            consentSheetLeadsToNicknameChoice = consentSheetLeadsToNicknameChoice,
            isPrefilled = false,
            communityOptionsResolved = false,
            faultyImage = null,
            isReadingImages = false,
        )

        /** The pre-multi-photo copy signature; preserves the multi-photo state fields. */
        fun copy(
            post: Post.Draft = this.post,
            isLoading: Boolean = this.isLoading,
            isSending: Boolean = this.isSending,
            hasAcceptedCgu: Boolean = this.hasAcceptedCgu,
            showTopicSelection: Boolean = this.showTopicSelection,
            topicError: ValidationError? = this.topicError,
            textError: ValidationError? = this.textError,
            mediaError: ValidationError? = this.mediaError,
            pollError: ValidationError? = this.pollError,
            pollAnswersErrors: Map<Post.Draft.Poll.Answer, ValidationError?> = this.pollAnswersErrors,
            otherError: ValidationError? = this.otherError,
            showUnsavedChangesDialog: Boolean = this.showUnsavedChangesDialog,
            enablePictures: Boolean = this.enablePictures,
            enablePolls: Boolean = this.enablePolls,
            textRequired: Boolean = this.textRequired,
            termsAcceptanceMode: TermsAcceptanceMode = this.termsAcceptanceMode,
            showConsentSheet: Boolean = this.showConsentSheet,
            consentTexts: ConsentTexts? = this.consentTexts,
            consentSheetLeadsToNicknameChoice: Boolean = this.consentSheetLeadsToNicknameChoice,
        ): UiState =
            copy(
                post = post,
                isLoading = isLoading,
                isSending = isSending,
                hasAcceptedCgu = hasAcceptedCgu,
                showTopicSelection = showTopicSelection,
                topicError = topicError,
                textError = textError,
                mediaError = mediaError,
                pollError = pollError,
                pollAnswersErrors = pollAnswersErrors,
                otherError = otherError,
                showUnsavedChangesDialog = showUnsavedChangesDialog,
                enablePictures = enablePictures,
                enablePolls = enablePolls,
                textRequired = textRequired,
                termsAcceptanceMode = termsAcceptanceMode,
                showConsentSheet = showConsentSheet,
                consentTexts = consentTexts,
                consentSheetLeadsToNicknameChoice = consentSheetLeadsToNicknameChoice,
                isPrefilled = this.isPrefilled,
                communityOptionsResolved = this.communityOptionsResolved,
                faultyImage = this.faultyImage,
                isReadingImages = this.isReadingImages,
            )

        /**
         * Whether the member may add pictures. A prefilled draft waits for the community options:
         * until then [enablePictures] is only its default, and a photo added in a community that
         * disables pictures would be signed and accepted with the shared ones.
         */
        private val canAddPictures get() = enablePictures && (communityOptionsResolved || !isPrefilled)

        val isEditable = !isLoading && !isSending
        val canSend =
            isEditable &&
                !isReadingImages &&
                (
                    post.text.isNotBlank() ||
                        canOmitPostText(
                            text = post.text,
                            hasImage = imageCount > 0,
                            textRequired = textRequired,
                        )
                ) &&
                topicError == null &&
                textError == null &&
                mediaError == null &&
                pollError == null &&
                pollAnswersErrors.values.all { it == null }

        /** Number of images in the draft, up to [Post.Draft.MAX_IMAGE_COUNT]. */
        val imageCount get() = post.images.size

        /** How many more images the draft can take, which caps the selection of the next pick. */
        val remainingPictureSlots get() = (Post.Draft.MAX_IMAGE_COUNT - imageCount).coerceAtLeast(0)

        /**
         * Whether the last thumbnail carries the tile that adds more images: only while the community
         * allows pictures and the draft holds at least one image and can still take another.
         * Prefilled images can exist with pictures disabled, and the member must not add their own
         * to them: they would be signed and accepted with the shared ones.
         */
        val showsAddMoreTile get() = canAddPictures && imageCount in 1 until Post.Draft.MAX_IMAGE_COUNT

        /** Whether the Photo button explains the limit instead of opening the picker. */
        val pictureEntryShowsMaxAlert get() = imageCount >= Post.Draft.MAX_IMAGE_COUNT

        /**
         * Whether the action bar shows the Photo button. A draft carries 0..10 images and 0..1 poll
         * independently, so it only follows the community option: the button stays once the draft
         * holds the maximum, and then explains the limit ([pictureEntryShowsMaxAlert]).
         */
        val showAddPicture get() = canAddPictures

        /**
         * Whether the action bar shows the Poll button: the community allows polls and none is open
         * yet. An attached image changes nothing, a Bridge Share prefilled one included.
         */
        val showAddPoll = post.poll == null && enablePolls

        /** Whether the action bar shows at all: it hides when it has no button to show. */
        val showActionBar = showAddPicture || showAddPoll

        // Only the implicit mode shows the inline legal disclaimer. The explicit modes use the
        // consent sheet instead, so the inline banner must be suppressed there (otherwise it would
        // sit under the sheet — matches iOS `CreatePostView` `displayCguText` `&& !isExplicit`).
        val showCguText = !termsAcceptanceMode.isExplicit && !hasAcceptedCgu && canSend
    }

    /**
     * Initial draft including any host-provided prefill (text + CTA). The
     * image-prefill cannot be materialised here because [Media.Image] needs
     * a [Context]; the composable calls [applyPrefilledImages] from a
     * `LaunchedEffect` to hydrate it, and the baseline is re-snapshot at
     * that point so [onBack] does not flag silent hydration as a change.
     *
     * Mutable because the image baseline is patched in by
     * [applyPrefilledImages].
     */
    private var initialDraft: Post.Draft =
        Post.Draft(type = type).let { base ->
            val withText = if (prefilledText != null) base.copy(text = prefilledText) else base
            val withCta =
                if (prefilledCustomActionLabel != null && prefilledCustomActionUrl != null) {
                    withText.copy(
                        customAction =
                            Post.Draft.CustomAction(
                                label = prefilledCustomActionLabel,
                                targetUrl = prefilledCustomActionUrl,
                                labelLanguage = prefilledCustomActionLabelLanguage,
                                labelTranslations = prefilledCustomActionLabelTranslations,
                            ),
                    )
                } else {
                    withText
                }
            withCta
        }

    /**
     * One-shot guard: ensures the prefilled images are hydrated exactly
     * once. Without this, navigating away after manually removing a
     * prefilled image and coming back would silently re-hydrate it and
     * overwrite the user's removal.
     */
    private var imagePrefillConsumed = false

    /**
     * Whether the editor was opened via Bridge Share (host-provided
     * prefill). Drives the `creation_source` analytics value at publish
     * time.
     */
    private val isPrefilledFromClient: Boolean =
        prefilledText != null ||
            prefilledImageUris.isNotEmpty() ||
            (prefilledCustomActionLabel != null && prefilledCustomActionUrl != null)

    private val creationSource: CreationSource =
        if (isPrefilledFromClient) {
            CreationSource.PREFILLED_FROM_CLIENT
        } else {
            CreationSource.USER
        }

    private val _state = MutableStateFlow(UiState(post = initialDraft, isPrefilled = isPrefilledFromClient))
    val state = _state.asStateFlow()

    private var onGroupSelected: ((OctopusGroup) -> Unit)? = null

    // Set once the user has explicitly accepted via the consent sheet in this session, so the gate
    // is not re-triggered between the sheet's confirm and the profile's `hasAcceptedCgu` flip.
    private var consentGivenThisSession = false

    // A post held back by the nickname choice, for the member it was held for (see NicknameChoiceHold).
    private var nicknameChoiceHold: NicknameChoiceHold? = null

    /** One-shot guard so the polls-disabled poll-strip runs only on first config resolution. */
    private var pollGateApplied = false

    /** How many image reads are running; [UiState.isReadingImages] while there is one. */
    private var imageReads = 0

    /** The whole send attempt, including image reads, authentication and first-contribution gates. */
    private var sendAttempt: Job? = null

    init {
        viewModelScope.launch {
            currentUserProfile.collectLatest { profile ->
                _state.update {
                    it.copy(hasAcceptedCgu = profile?.hasAcceptedCgu != false)
                }
                val hold = nicknameChoiceHold ?: return@collectLatest
                when (hold.resolution(profile)) {
                    NicknameChoiceHold.Resolution.SEND -> {
                        nicknameChoiceHold = null
                        sendPost()
                    }

                    NicknameChoiceHold.Resolution.DROP -> {
                        nicknameChoiceHold = null
                        nicknameChoiceSession.finish()
                    }

                    NicknameChoiceHold.Resolution.KEEP_WAITING -> Unit
                }
            }
        }
        viewModelScope.launch {
            // Closed without a choice: the post stays a draft and is never sent on its own later.
            nicknameChoiceSession.closedWithoutChoice.collect { nicknameChoiceHold = null }
        }
        viewModelScope.launch { eventRepository.emitEvent(ScreenDisplayed.CreatePost) }
        viewModelScope.launch {
            communityConfig.collectLatest { config ->
                config ?: return@collectLatest
                _state.update {
                    it.copy(
                        termsAcceptanceMode = config.termsAcceptanceMode,
                        consentTexts = config.consentTexts,
                    )
                }
                val postOptions = config.contentOptions.post
                _state.update {
                    it.copy(
                        enablePictures = postOptions.enablePictures,
                        enablePolls = postOptions.enablePolls,
                        communityOptionsResolved = true,
                        textRequired = postOptions.textRequired,
                        // Re-check the text only when the rule itself changes, so a config refresh
                        // does not clear an error the server returned on the last send.
                        textError =
                            if (it.textRequired != postOptions.textRequired) {
                                validateText(draft = it.post, textRequired = postOptions.textRequired)
                            } else {
                                it.textError
                            },
                    )
                }
                // When the community disables polls, drop a pre-seeded poll coming from a
                // POLL-type launch so the composer never opens in the poll editor. One-shot,
                // applied on first config resolution, so a later re-seed never clobbers an
                // in-progress edit.
                if (!pollGateApplied) {
                    pollGateApplied = true
                    if (!postOptions.enablePolls && _state.value.post.poll != null) {
                        initialDraft = initialDraft.copy(poll = null)
                        _state.update { it.copy(post = it.post.copy(poll = null)) }
                    }
                }
            }
        }
        if (groupId != null) {
            viewModelScope.launch {
                groupRepository.getGroupFlow(groupId).collect { group ->
                    // internal ticket: drop a preselected group that the user cannot post in
                    // (admin-only topic, locked group). Mirrors iOS CreatePostViewModel
                    // (internal change) — the user is then forced to pick a creatable topic.
                    if (group != null &&
                        _state.value.post.group == null &&
                        group.canAccess &&
                        group.canCreateChildren
                    ) {
                        updateGroup(group)
                    }
                }
            }
        }
    }

    /**
     * Hydrates the prefilled images into the editor state, in order, up to
     * [Post.Draft.MAX_IMAGE_COUNT]. Must be invoked from the composable (which holds the [Context]
     * required to build [Media.Image]). Strictly one-shot: subsequent invocations no-op even if the
     * user has removed images, so the host-supplied prefill does not silently override a manual
     * removal on the next recomposition.
     *
     * The images [validateImage] rejects are dropped and explained in an alert, as for a pick. The
     * prefill is shown whatever the community's pictures option: the host signs it. [initialDraft]
     * is patched with the hydrated images so [onBack] does not register the silent hydration as a
     * user change.
     */
    fun applyPrefilledImages(context: Context) {
        if (!_state.value.isEditable || imagePrefillConsumed) return
        imagePrefillConsumed = true
        if (prefilledImageUris.isEmpty()) return
        if (_state.value.post.images
                .isNotEmpty()
        ) {
            return
        }

        readPictures(read = { prefilledImageUris.map { Media.Image(context, it.toUri()) } }) { kept ->
            // The prefill goes first: the photos the member picked during its read follow it.
            updateImages { current -> appendCapped(current = kept, new = current) }
            initialDraft = initialDraft.copy(medias = appendCapped(current = emptyList(), new = kept))
        }
    }

    /** Former single-image prefill, now [applyPrefilledImages]. */
    @Deprecated("Use applyPrefilledImages.", ReplaceWith("applyPrefilledImages(context)"))
    fun applyPrefilledImage(context: Context) = applyPrefilledImages(context)

    /**
     * Adds the pictures the member picked after the current ones, in order, up to
     * [Post.Draft.MAX_IMAGE_COUNT]: the picks beyond the limit are dropped. The ones [validateImage]
     * rejects are dropped too, the others and the poll kept, and an alert explains why. Does nothing
     * when the community disables pictures, whichever entry calls it.
     */
    fun addPictures(
        context: Context,
        uris: List<Uri>,
    ) {
        if (!_state.value.isEditable || !_state.value.showAddPicture || uris.isEmpty()) return
        readPictures(read = { uris.map { Media.Image(context, it) } }) { kept ->
            updateImages { current -> appendCapped(current = current, new = kept) }
        }
    }

    /**
     * Builds the images [read] returns on [imageReadDispatcher], drops those [validateImage] rejects
     * and hands the others to [apply], then explains the first rejection. The draft cannot be sent
     * until then ([UiState.isReadingImages]): it would go without these images.
     */
    private fun readPictures(
        read: () -> List<Media.Image>,
        apply: (kept: List<Media.Image>) -> Unit,
    ) {
        setImageReads(imageReads + 1)
        viewModelScope.launch {
            try {
                val (kept, error) = dropInvalidPictures(withContext(imageReadDispatcher) { read() }, validateImage)
                apply(kept)
                error?.message?.let { showAlertDialog(it) }
            } finally {
                setImageReads(imageReads - 1)
            }
        }
    }

    private fun setImageReads(count: Int) {
        imageReads = count
        _state.update { it.copy(isReadingImages = count > 0) }
    }

    /** Removes the image at [index], keeping the others in order. */
    fun removePicture(index: Int) {
        if (!_state.value.isEditable) return
        updateImages { current -> current.filterIndexed { i, _ -> i != index } }
    }

    /**
     * Former single-picture entry: a `null` [localUri] removes every image, a picked one replaces
     * them with it, unless the community disables pictures.
     */
    @Deprecated("Use addPictures and removePicture.")
    fun updatePicture(
        context: Context,
        localUri: Uri?,
    ) {
        if (!_state.value.isEditable) return
        if (localUri == null) {
            updateImages { emptyList() }
            return
        }
        if (!_state.value.showAddPicture) return
        readPictures(read = { listOf(Media.Image(context, localUri)) }) { kept -> updateImages { kept } }
    }

    /**
     * Sets the draft's images to [transform] of the current ones. A change of images clears the
     * media error the server returned on the last send. The images decide whether the text may be
     * left out: the text is re-checked only when that answer changes, so an image change does not
     * clear an error the server returned on the text.
     */
    private fun updateImages(transform: (List<Media.Image>) -> List<Media.Image>) {
        _state.update { state ->
            val post = state.post.copy(medias = transform(state.post.images))
            val canOmitTextChanged =
                canOmitPostText(state.post.text, state.post.images.isNotEmpty(), state.textRequired) !=
                    canOmitPostText(post.text, post.images.isNotEmpty(), state.textRequired)
            state.copy(
                post = post,
                mediaError = null,
                faultyImage = null,
                textError =
                    if (canOmitTextChanged) {
                        validateText(draft = post, textRequired = state.textRequired)
                    } else {
                        state.textError
                    },
            )
        }
    }

    fun updateText(text: String) {
        if (!_state.value.isEditable) return
        _state.update { state ->
            val post = state.post.copy(text = text)
            state.copy(
                post = post,
                textError = validateText(draft = post, textRequired = state.textRequired),
            )
        }
    }

    fun updateGroup(group: OctopusGroup) {
        if (!_state.value.isEditable) return
        _state.update { state ->
            state.copy(
                post = state.post.copy(group = group),
                topicError = null,
                showTopicSelection = false,
            )
        }
        onGroupSelected?.invoke(group)
    }

    fun updatePoll(poll: Post.Draft.Poll?) {
        if (!_state.value.isEditable) return
        _state.update { state ->
            val post = state.post.copy(poll = poll)
            state.copy(
                post = post,
                textError = validateText(draft = post, textRequired = state.textRequired),
                pollError = null,
                pollAnswersErrors =
                    poll?.answers?.associateWith {
                        validateText(
                            text = it.text.trim(),
                            lengthRange = Post.Draft.Poll.Answer.TEXT_LENGTH_RANGE,
                        )
                    } ?: emptyMap(),
            )
        }
    }

    fun sendPost() {
        if (sendAttempt?.isActive == true || state.value.isSending) return
        // Assign before starting: Main.immediate can run the attempt up to its first suspension.
        sendAttempt = viewModelScope.launch(start = CoroutineStart.LAZY) { sendPostAttempt() }
        sendAttempt?.start()
    }

    private suspend fun sendPostAttempt() {
        state.first { !it.isReadingImages }
        val post = state.value.post
        val textError = validateText(draft = post, textRequired = state.value.textRequired)
        if (textError != null) {
            _state.update { state -> state.copy(textError = textError) }
            return
        }

        if (post.group == null) {
            showTopicSelection(show = true, onSelected = { sendPost() })
            return
        }
        launchCheckAuthenticated(action = UserAction.Post) { profile ->
            // First-contribution gates, in this order: consent → nickname choice → publish.
            // Explicit terms-acceptance gate: on the user's first contribution, present the consent
            // sheet BEFORE anything else, the nickname choice included; the send resumes from
            // [acceptConsentAndSendPost].
            if (shouldPresentConsentSheet(
                    mode = state.value.termsAcceptanceMode,
                    hasAcceptedCgu = profile.hasAcceptedCgu,
                    consentGivenThisSession = consentGivenThisSession,
                )
            ) {
                // The nickname choice gate below, evaluated ahead: it labels the sheet's button.
                val nicknameChoiceFollows =
                    shouldPresentNicknameChoice(
                        action = UserAction.Post,
                        profile = profile,
                        config = configRepository.getCommunityConfig(),
                        appManagedFields = appManagedFields,
                    )
                _state.update {
                    it.copy(
                        showConsentSheet = true,
                        consentSheetLeadsToNicknameChoice = nicknameChoiceFollows,
                    )
                }
                return@launchCheckAuthenticated
            }

            // Nickname choice gate: a member whose nickname is not confirmed yet chooses it before the
            // post goes. The post is held for this member and sent again once their nickname is
            // confirmed (see the profile collector in init).
            if (shouldPresentNicknameChoice(
                    action = UserAction.Post,
                    profile = profile,
                    config = configRepository.getCommunityConfig(),
                    appManagedFields = appManagedFields,
                )
            ) {
                // Already holding one (a second tap during the check or the transition): the screen
                // is open or opening, so do not open it again. The hold is cleared on send, drop and
                // close, so this never sticks.
                if (nicknameChoiceHold != null) return@launchCheckAuthenticated
                nicknameChoiceHold = NicknameChoiceHold(profileId = profile.userId)
                nicknameChoiceSession.open(NicknameChoiceContribution.POST)
                navigateTo(ValidateNickname) { launchSingleTop = true }
                return@launchCheckAuthenticated
            }

            // Authentication/configuration can suspend while another picker read starts. Settle
            // every read and validate the final draft before locking it, with no suspension between.
            state.first { !it.isReadingImages }
            val draft = state.value.post
            val finalTextError = validateText(draft = draft, textRequired = state.value.textRequired)
            if (finalTextError != null) {
                _state.update { it.copy(textError = finalTextError) }
                return@launchCheckAuthenticated
            }
            if (draft.group == null) {
                showTopicSelection(show = true, onSelected = { sendPost() })
                return@launchCheckAuthenticated
            }
            _state.update { it.copy(isSending = true) }

            if (!profile.hasAcceptedCgu) {
                when (val result = acceptCgu(profile)) {
                    is OctopusResult.Success -> {}
                    is OctopusResult.Failure -> {
                        _state.update { state -> state.copy(isSending = false) }
                        showError(result)
                        return@launchCheckAuthenticated
                    }
                }
            }

            when (
                val result =
                    sendPostUseCase(
                        draftItem = draft,
                        creationSource = creationSource,
                    )
            ) {
                is OctopusResult.Success -> navigateUp()

                is OctopusResult.Failure.InvalidArguments -> {
                    _state.update { state ->
                        state.copy(
                            isSending = false,
                            topicError = result.findError<PostCreationError.GroupError>(),
                            textError = result.findError<PostCreationError.TextError>(),
                            mediaError = result.findError<PostCreationError.FileError>(),
                            // mediaError lists every file error: point at the first photo one of them names.
                            faultyImage =
                                result.errors
                                    .filterIsInstance<PostCreationError.FileError>()
                                    .firstNotNullOfOrNull { it.imageIndex }
                                    ?.let { FaultyImage(index = it) },
                            pollError = result.findError<PostCreationError.PollError>(),
                            otherError = result.findError<PostCreationError.OtherError>(),
                        )
                    }
                    result.findError<PostCreationError.PublicationRateLimitExceeded>()?.message?.let { error ->
                        showAlertDialog(error)
                    }
                }

                is OctopusResult.Failure -> {
                    _state.update { state -> state.copy(isSending = false) }
                    showError(result)
                }
            }
        }.join()
    }

    /**
     * Called when the user confirms the explicit-consent sheet. Records that consent was given this
     * session, dismisses the sheet, and resumes [sendPost] — which now passes the consent gate, goes
     * through the nickname choice when the nickname is not confirmed yet, and publishes (the publish
     * path still calls [acceptCgu] since the profile has not accepted yet).
     */
    fun acceptConsentAndSendPost() {
        consentGivenThisSession = true
        _state.update { it.copy(showConsentSheet = false) }
        sendPost()
    }

    /** Dismisses the consent sheet without publishing (swipe-down / tap outside). */
    fun dismissConsentSheet() {
        _state.update { it.copy(showConsentSheet = false) }
    }

    // Compare against the prefill-aware baseline so that a Bridge Share session whose user did
    // not touch anything closes silently. For a regular (non-prefilled) session, [initialDraft]
    // reduces to `Post.Draft(type = type)` and the legacy behaviour is preserved. Exposed to the
    // module (not private) so the Composable's system-back `BackHandler` can gate on it directly.
    internal val hasUnsavedChanges get() = _state.value.post != initialDraft

    fun onBack() {
        if (!forcedBack && hasUnsavedChanges) {
            _state.update { it.copy(showUnsavedChangesDialog = true) }
        } else {
            navigateUp()
        }
    }

    fun showTopicSelection(
        show: Boolean = true,
        onSelected: ((OctopusGroup) -> Unit)? = null,
    ) {
        onGroupSelected = onSelected
        _state.update {
            it.copy(showTopicSelection = show)
        }
    }

    fun dismissUnsavedChangesDialog(navigateUp: Boolean) {
        _state.update { it.copy(showUnsavedChangesDialog = false) }
        if (navigateUp) {
            navigateUp()
        }
    }

    private fun validateText(
        draft: Post.Draft,
        textRequired: Boolean,
    ): ValidationError? {
        if (canOmitPostText(text = draft.text, hasImage = draft.images.isNotEmpty(), textRequired = textRequired)) {
            return null
        }
        // One visible character is enough, so the only too-short text is a blank one. It carries no
        // message: the publish button is already disabled for it (see UiState.canSend).
        return validateText(
            text = draft.text.trim(),
            lengthRange = Post.Draft.TEXT_LENGTH_RANGE,
        )
    }
}

/**
 * Whether a post draft may be published without text: the community does not require text
 * ([textRequired] `false`) and the draft carries an image, with or without a poll next to it. Blank
 * text counts as no text (it is not sent). As soon as the text is not blank this is `false`, and
 * [Post.Draft.TEXT_LENGTH_RANGE] applies as usual. Without an image, text stays required, even with
 * a poll.
 */
internal fun canOmitPostText(
    text: String,
    hasImage: Boolean,
    textRequired: Boolean,
): Boolean = !textRequired && hasImage && text.isBlank()

/**
 * The images a selection keeps once [validateImage] has rejected the ones with a side too small or a
 * ratio too big, in order, with the first rejection to explain to the member (`null` when none was
 * rejected). An image whose dimensions cannot be read is kept, as before.
 */
internal fun dropInvalidPictures(
    images: List<Media.Image>,
    validateImage: ValidateImage,
): Pair<List<Media.Image>, ValidationError?> {
    var firstError: ValidationError? = null
    val kept =
        images.filter { image ->
            val error = image.dimensions?.let { validateImage(it) }
            if (error != null && firstError == null) firstError = error
            error == null
        }
    return kept to firstError
}

/** [current] followed by [new], up to [Post.Draft.MAX_IMAGE_COUNT]: the items beyond are dropped. */
internal fun <T> appendCapped(
    current: List<T>,
    new: List<T>,
): List<T> = (current + new).take(Post.Draft.MAX_IMAGE_COUNT)

/**
 * A request to show the photo at [index], which the server refused. Compared by identity, so each
 * refusal scrolls the strip again, even to the same photo.
 */
@Immutable
class FaultyImage(
    val index: Int,
)
