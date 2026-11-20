package com.octopuscommunity.sdk.ui.common.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import com.octopuscommunity.sdk.domain.model.CurrentUserProfile
import com.octopuscommunity.sdk.domain.model.OctopusItem
import com.octopuscommunity.sdk.domain.network.OctopusResult
import com.octopuscommunity.sdk.ui.common.UiText
import com.octopuscommunity.sdk.ui.common.errorText
import com.octopuscommunity.sdk.ui.report.common.ReportTarget
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class OctopusItemMenuSheetViewModel<Item : OctopusItem> internal constructor(
    commonParameters: CommonParameters,
    private val item: Item,
    private val deleteItem: suspend (Item) -> OctopusResult<Unit, Nothing>,
) : OctopusViewModel(commonParameters) {
    @Immutable
    data class UiState<Item : OctopusItem>(
        val item: Item,
        val currentUserProfile: CurrentUserProfile? = null,
        val showConfirmDelete: Boolean = false,
        val isDeleting: Boolean = false,
        val deleteError: UiText? = null,
        val showConfirmBlock: Boolean = false,
        val isBlocking: Boolean = false,
        val blockError: UiText? = null,
    ) {
        val canDelete = item.canBeDeleted(currentUserProfile)
        val canReport = item.canBeReported(currentUserProfile)
        val canBlock = item.canAuthorBeBlocked(currentUserProfile)
    }

    sealed class Event {
        object Dismiss : Event()

        data class OpenReport(
            val target: ReportTarget,
        ) : Event()
    }

    private val _state = MutableStateFlow(UiState(item = item))
    val state = _state.asStateFlow()

    private val _events = Channel<Event>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            currentUserProfile.collect { profile ->
                _state.update { it.copy(currentUserProfile = profile) }
            }
        }
    }

    fun showConfirmDelete(show: Boolean = true) {
        _state.update { it.copy(showConfirmDelete = show, deleteError = null) }
    }

    fun deleteItem() {
        val item = state.value.item
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true, deleteError = null) }
            when (val result = deleteItem(item)) {
                is OctopusResult.Success -> {
                    _state.update { it.copy(showConfirmDelete = false, isDeleting = false) }
                    _events.send(Event.Dismiss)
                }

                is OctopusResult.Failure ->
                    _state.update {
                        it.copy(isDeleting = false, deleteError = result.errorText)
                    }
            }
        }
    }

    fun reportContent() {
        val item = state.value.item
        viewModelScope.launch {
            _events.send(
                Event.OpenReport(
                    ReportTarget.Content(
                        contentId = item.id,
                        contentKind = item.contentKind,
                    ),
                ),
            )
        }
    }

    fun showConfirmBlock(show: Boolean = true) {
        _state.update { it.copy(showConfirmBlock = show, blockError = null) }
    }

    fun blockAuthor() =
        checkAuthenticated(action = UserAction.BlockUser) {
            val authorId =
                state.value.item.author
                    ?.userId ?: return@checkAuthenticated
            viewModelScope.launch {
                _state.update { it.copy(isBlocking = true, blockError = null) }
                when (val result = connectionRepository.blockProfile(authorId)) {
                    is OctopusResult.Success -> {
                        _state.update { it.copy(showConfirmBlock = false, isBlocking = false) }
                        _events.send(Event.Dismiss)
                    }

                    is OctopusResult.Failure ->
                        _state.update {
                            it.copy(isBlocking = false, blockError = result.errorText)
                        }
                }
            }
            _state.update { it.copy(showConfirmBlock = true) }
        }
}
