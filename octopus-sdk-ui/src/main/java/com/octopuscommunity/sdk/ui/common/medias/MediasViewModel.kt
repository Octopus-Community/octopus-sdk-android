package com.octopuscommunity.sdk.ui.common.medias

import androidx.compose.runtime.Immutable
import com.octopuscommunity.sdk.domain.model.Media
import com.octopuscommunity.sdk.domain.model.Medias
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

open class MediasViewModel(commonParameters: CommonParameters) :
    OctopusViewModel(commonParameters) {

    @Immutable
    data class MediaUiState(
        val isLoading: Boolean = true,
        val medias: Medias? = null
    )

    private val _mediaState = MutableStateFlow(MediaUiState())
    val mediaState = _mediaState.asStateFlow()

    fun setMedias(medias: Medias?) {
        _mediaState.update { it.copy(isLoading = false, medias = medias) }
    }

    fun setMedia(media: Media.Image?) {
        setMedias(Medias(images = listOfNotNull(media)))
    }
}