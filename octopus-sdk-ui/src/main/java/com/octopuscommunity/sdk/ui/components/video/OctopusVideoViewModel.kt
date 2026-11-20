package com.octopuscommunity.sdk.ui.components.video

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.domain.model.Sessions
import com.octopuscommunity.sdk.domain.repository.SessionRepository
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class OctopusVideoViewModel(
    commonParameters: CommonParameters,
    private val sessionRepository: SessionRepository,
    private val itemId: String,
    private val videoId: String
) : OctopusViewModel(commonParameters) {

    @Immutable
    data class UiState(
        val isPlaying: Boolean = false,
        val isLoading: Boolean = false,
        val isMuted: Boolean = true,
        val position: Duration = 0.milliseconds,
        val showControls: Boolean = true
    ) {
        val showLoading = isLoading && !isPlaying
    }

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private var hideControlsJob: Job? = null

    init {
        viewModelScope.launch {
            sessionRepository.getVideosMutedFlow().collectLatest { muted ->
                _state.update { it.copy(isMuted = muted) }
            }
        }
        viewModelScope.launch {
            sessionRepository.getPlayedVideoFlow(videoId).collectLatest { playedVideo ->
                _state.update { it.copy(position = playedVideo?.position ?: 0.milliseconds) }
            }
        }
    }

    fun onPlayingChanged(playing: Boolean, position: Duration, duration: Duration) {
        _state.update { it.copy(isPlaying = playing) }
        viewModelScope.launch {
            sessionRepository.upsertPlayedVideo(videoId) { playedVideo ->
                playedVideo?.copy(
                    position = position,
                    duration = duration,
                    watchedTime = playedVideo.watchedTime + if (position < playedVideo.position) {
                        position
                    } else position - playedVideo.position
                ) ?: Sessions.Session.App.PlayedVideo(
                    videoId = videoId,
                    itemId = itemId,
                    position = position,
                    duration = duration,
                    watchedTime = position
                )
            }
        }
        showControls(playing)
    }

    fun onLoadingChanged(buffering: Boolean) {
        _state.update { it.copy(isLoading = buffering) }
    }

    fun onMutedChange(muted: Boolean) {
        viewModelScope.launch {
            sessionRepository.setVideosMuted(muted)
        }
    }

    fun showControls(show: Boolean) {
        _state.update { it.copy(showControls = show) }
        hideControlsJob?.cancel()
        if (show) {
            hideControlsJob = viewModelScope.launch {
                delay(2000)
                _state.update { it.copy(showControls = false) }
            }
        }
    }
}