package com.octopuscommunity.sdk.ui.common.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.domain.model.CommunityConfig
import com.octopuscommunity.sdk.domain.repository.PreferenceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal enum class GamificationAutoPrompt { None, Home, CurrentUserProfile }

class OctopusScreenViewModel internal constructor(
    commonParameters: CommonParameters,
    private val preferenceRepository: PreferenceRepository,
    private val homeAppearanceCounter: GamificationHomeAppearanceCounter,
    private val uiOpenings: OctopusUiOpenings,
) : OctopusViewModel(commonParameters) {
    @Immutable
    data class UiState(
        val communityConfig: CommunityConfig? = null,
        val showGamificationSheet: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private var isScreenVisible = false
    private var autoPrompt = GamificationAutoPrompt.None
    private var explicitSheetRequest = false
    private var automaticSheetHasAppeared = false

    init {
        viewModelScope.launch {
            configRepository.getCommunityConfigFlow().collect { communityConfig ->
                _state.update { it.copy(communityConfig = communityConfig) }
                countHomeAppearanceIfNeeded()
                updateGamificationSheet()
            }
        }
    }

    fun onOctopusScreenStarted() {
        lifecycleEvents.onOctopusScreenStarted()
    }

    fun onOctopusScreenStopped() {
        lifecycleEvents.onOctopusScreenStopped()
    }

    internal fun onOctopusScreenVisible(registration: Any) {
        uiOpenings.onScreenStarted(registration)
    }

    internal fun onOctopusScreenHidden(
        registration: Any,
        changingConfigurations: Boolean,
    ) {
        uiOpenings.onScreenStopped(registration, changingConfigurations)
    }

    internal fun setGamificationScreenVisible(
        visible: Boolean,
        prompt: GamificationAutoPrompt,
    ) {
        isScreenVisible = visible
        autoPrompt = prompt
        countHomeAppearanceIfNeeded()
        updateGamificationSheet()
    }

    // iOS counts once per opening of the Octopus UI (the root view's onAppear), and only when
    // gamification is enabled at that moment: a return to Home from a pushed screen, a resume or
    // a configuration change does not count again. The opening is tracked by [OctopusUiOpenings],
    // not by this ViewModel, which a host tab with saved state retains across openings.
    private fun countHomeAppearanceIfNeeded() {
        if (!isScreenVisible || autoPrompt != GamificationAutoPrompt.Home) return
        // A config that has not loaded yet is unknown, not disabled: decide once it arrives.
        val communityConfig = _state.value.communityConfig ?: return
        if (!uiOpenings.handleHomeAppearance()) return
        if (communityConfig.gamification != null) homeAppearanceCounter.count++
    }

    private fun updateGamificationSheet() {
        // Visibility gates new automatic requests, never an already-open presentation.
        // Retaining that state lets rotation, theme changes and backgrounding restore the sheet.
        if (!isScreenVisible || _state.value.communityConfig?.gamification == null) return
        if (preferenceRepository.hasDisplayedGamificationSheet) {
            // Another screen may have displayed the rules while this automatic request was
            // waiting to render. Cancel that unshown request, but preserve an open sheet.
            if (!explicitSheetRequest && !automaticSheetHasAppeared) {
                _state.update { it.copy(showGamificationSheet = false) }
            }
            return
        }
        val eligible =
            when (autoPrompt) {
                GamificationAutoPrompt.Home -> homeAppearanceCounter.count >= 3
                GamificationAutoPrompt.CurrentUserProfile -> true
                GamificationAutoPrompt.None -> false
            }
        if (eligible) _state.update { it.copy(showGamificationSheet = true) }
    }

    // Badge and toast taps are explicit requests, independent of automatic prompt eligibility.
    fun showGamificationSheet(show: Boolean = true) {
        explicitSheetRequest = show
        if (!show) automaticSheetHasAppeared = false
        _state.update {
            it.copy(showGamificationSheet = show && it.communityConfig?.gamification != null)
        }
    }

    internal fun onGamificationSheetShown() {
        if (preferenceRepository.hasDisplayedGamificationSheet) return
        if (isScreenVisible && _state.value.showGamificationSheet && _state.value.communityConfig?.gamification != null) {
            automaticSheetHasAppeared = !explicitSheetRequest
            preferenceRepository.hasDisplayedGamificationSheet = true
        }
    }
}
