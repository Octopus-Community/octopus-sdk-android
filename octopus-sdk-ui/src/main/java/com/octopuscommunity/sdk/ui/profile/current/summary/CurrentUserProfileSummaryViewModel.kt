package com.octopuscommunity.sdk.ui.profile.current.summary

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.domain.model.CommunityConfig
import com.octopuscommunity.sdk.domain.model.CurrentUserProfile
import com.octopuscommunity.sdk.domain.model.OctopusEvent.CommentCreated
import com.octopuscommunity.sdk.domain.model.OctopusEvent.CommentDeleted
import com.octopuscommunity.sdk.domain.model.OctopusEvent.PostCreated
import com.octopuscommunity.sdk.domain.model.OctopusEvent.PostDeleted
import com.octopuscommunity.sdk.domain.model.OctopusEvent.ReactionModified
import com.octopuscommunity.sdk.domain.model.OctopusEvent.ReplyCreated
import com.octopuscommunity.sdk.domain.model.OctopusEvent.ReplyDeleted
import com.octopuscommunity.sdk.domain.model.OctopusEvent.ScreenDisplayed
import com.octopuscommunity.sdk.domain.model.ProfileFieldsLock
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.domain.repository.GroupRepository
import com.octopuscommunity.sdk.domain.repository.PreferenceRepository
import com.octopuscommunity.sdk.domain.repository.canCreateAnyPostFlow
import com.octopuscommunity.sdk.domain.usecase.DisconnectUseCase
import com.octopuscommunity.sdk.ui.common.ErrorChannel
import com.octopuscommunity.sdk.ui.common.loadFailureChannel
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusViewModel
import com.octopuscommunity.sdk.ui.profile.current.ProfileFieldsEditability
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CurrentUserProfileSummaryViewModel(
    commonParameters: CommonParameters,
    groupRepository: GroupRepository,
) : OctopusViewModel(commonParameters) {
    /**
     * Legacy constructor retained for source and binary compatibility. [preferenceRepository] is
     * unused since the gamification rules sheet moved to the screen level. This constructor will
     * be removed in the next major release.
     */
    @Deprecated(
        message = "Use the constructor without preferences; rules are managed at the screen level. Removed in the next major.",
        level = DeprecationLevel.WARNING,
    )
    @Suppress("UNUSED_PARAMETER")
    constructor(
        commonParameters: CommonParameters,
        preferenceRepository: PreferenceRepository,
        groupRepository: GroupRepository,
        disconnectUseCase: DisconnectUseCase,
    ) : this(commonParameters, groupRepository)

    /** Legacy constructor; account logout is now owned by the host app. */
    @Deprecated("Use the constructor without disconnectUseCase.", level = DeprecationLevel.WARNING)
    @Suppress("UNUSED_PARAMETER")
    constructor(
        commonParameters: CommonParameters,
        groupRepository: GroupRepository,
        disconnectUseCase: DisconnectUseCase,
    ) : this(commonParameters, groupRepository)

    @Immutable
    data class UiState(
        val profile: CurrentUserProfile? = null,
        val communityConfig: CommunityConfig? = null,
        /** Per-field editability derived from the community profile lock (internal ticket). */
        val editability: ProfileFieldsEditability = ProfileFieldsEditability.AllEditable,
        val isLoading: Boolean = true,
        /**
         * `true` iff at least one accessible topic allows post creation. Wired to gate the
         * create-post incentive chips on the empty-posts placeholder. Mirrors iOS
         * `CurrentUserProfileSummaryViewModel.canCreatePost` (internal change). Defaults to `true`
         * so existing behavior is preserved until the BE-served topic permissions resolve.
         */
        val canCreatePost: Boolean = true,
    )

    /**
     * Legacy events, unused since the gamification rules sheet moved to the screen level.
     * Retained for compatibility and scheduled for removal in the next major release.
     */
    @Deprecated(
        message = "Gamification rules are managed at the screen level. Event will be removed in the next major release.",
        level = DeprecationLevel.WARNING,
    )
    sealed interface Event {
        /**
         * Legacy event, unused since the gamification rules sheet moved to the screen level.
         * Retained for compatibility and scheduled for removal in the next major release.
         */
        @Deprecated(
            message = "Gamification rules are managed at the screen level. This event will be removed in the next major release.",
            level = DeprecationLevel.WARNING,
        )
        @Suppress("DEPRECATION")
        data object ShowGamificationSheet : Event
    }

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    /**
     * Legacy event stream that never emits, unused since the gamification rules sheet moved to
     * the screen level. Retained for compatibility and removed in the next major release.
     */
    @Deprecated(
        message = "Gamification rules are managed at the screen level. This stream will be removed in the next major release.",
        level = DeprecationLevel.WARNING,
    )
    @Suppress("DEPRECATION")
    val events = MutableSharedFlow<Event>().asSharedFlow()

    init {
        viewModelScope.launch {
            currentUserProfile.collectLatest { profile ->
                _state.update { it.copy(profile = profile) }
            }
        }
        viewModelScope.launch {
            configRepository.getCommunityConfigFlow().collectLatest { communityConfig ->
                _state.update {
                    it.copy(
                        communityConfig = communityConfig,
                        editability =
                            ProfileFieldsEditability.from(
                                communityConfig?.profileFieldsLock ?: ProfileFieldsLock.AllEditable,
                            ),
                    )
                }
            }
        }
        viewModelScope.launch {
            eventRepository.getEventsFlow().collect { event ->
                when (event) {
                    is PostCreated, is CommentCreated, is ReplyCreated,
                    is PostDeleted, is CommentDeleted, is ReplyDeleted,
                    is ReactionModified,
                    -> refresh()

                    else -> {}
                }
            }
        }
        viewModelScope.launch {
            groupRepository.canCreateAnyPostFlow().collect { canCreate ->
                _state.update { it.copy(canCreatePost = canCreate) }
            }
        }
        viewModelScope.launch { eventRepository.emitEvent(ScreenDisplayed.Profile) }
    }

    fun onResume() {
        refresh()
    }

    fun refresh() =
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = connectionRepository.fetchProfile()) {
                is OctopusResult.Success -> {}
                // The tabs below carry a screen state for the same outage: reporting it here too
                // would say it twice. Only speak up over a profile already on screen.
                is OctopusResult.Failure ->
                    if (loadFailureChannel(hasVisibleContent = _state.value.profile != null) ==
                        ErrorChannel.Transient
                    ) {
                        showError(result)
                    }
            }
            _state.update { it.copy(isLoading = false) }
        }
}
