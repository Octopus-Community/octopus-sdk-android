package com.octopuscommunity.sdk.ui.profile.others.menu

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.domain.model.CurrentUserProfile
import com.octopuscommunity.sdk.domain.model.Profile
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.UiText
import com.octopuscommunity.sdk.ui.common.UiText.StringResource
import com.octopuscommunity.sdk.ui.common.errorText
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusViewModel
import com.octopuscommunity.sdk.ui.report.common.ReportTarget
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileMenuSheetViewModel internal constructor(
    commonParameters: CommonParameters,
) : OctopusViewModel(commonParameters) {
    @Immutable
    data class UiState(
        val profile: Profile? = null,
        val currentUserProfile: CurrentUserProfile? = null,
        val showConfirmBlock: Boolean = false,
        val isBlocking: Boolean = false,
        val blockError: UiText? = null,
    ) {
        val canReport = profile?.canBeReported(currentUserProfile) == true
        val canBlock = profile?.canBeBlocked(currentUserProfile) == true
    }

    private val _state: MutableStateFlow<UiState> = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    sealed class Event {
        data class OpenReport(
            val target: ReportTarget,
        ) : Event()
    }

    private val _events = Channel<Event>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            currentUserProfile.collectLatest { profile ->
                _state.update { it.copy(currentUserProfile = profile) }
            }
        }
    }

    fun setProfile(profile: Profile?) {
        _state.update { it.copy(profile = profile) }
    }

    fun reportUser() {
        val profile = state.value.profile ?: return
        viewModelScope.launch {
            _events.send(Event.OpenReport(ReportTarget.Profile(profileId = profile.userId)))
        }
    }

    fun showConfirmBlock(show: Boolean = true) {
        _state.update { it.copy(showConfirmBlock = show, blockError = null) }
    }

    fun blockProfile(onSuccess: () -> Unit) =
        checkAuthenticated(action = UserAction.BlockUser) {
            val profile = state.value.profile ?: return@checkAuthenticated
            viewModelScope.launch {
                _state.update { it.copy(isBlocking = true, blockError = null) }
                when (val result = connectionRepository.blockProfile(profile.userId)) {
                    is OctopusResult.Success -> {
                        _state.update {
                            it.copy(showConfirmBlock = false, isBlocking = false)
                        }
                        showAlertDialog(
                            text = StringResource(R.string.block_profile_done_title),
                            onDismiss = onSuccess,
                        )
                    }

                    is OctopusResult.Failure -> {
                        _state.update { it.copy(isBlocking = false, blockError = result.errorText) }
                    }
                }
            }
            _state.update { it.copy(showConfirmBlock = true) }
        }
}
