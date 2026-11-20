package com.octopuscommunity.sdk.ui.profile.avatar

import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.domain.model.Media
import com.octopuscommunity.sdk.domain.repository.ProfileRepository
import com.octopuscommunity.sdk.ui.common.medias.MediasViewModel
import com.octopuscommunity.sdk.ui.profile.components.zoomableAvatar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfileAvatarViewModel(
    commonParameters: CommonParameters,
    private val profileRepository: ProfileRepository,
    private val userId: String,
) : MediasViewModel(commonParameters) {
    init {
        viewModelScope.launch {
            profileRepository.getProfileFlow(userId).collectLatest { profile ->
                setMedia(profile?.zoomableAvatar?.let { Media.Image(source = it) })
            }
        }

        viewModelScope.launch {
            profileRepository.fetchProfile(userId)
        }
    }
}
