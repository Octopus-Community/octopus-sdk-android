package com.octopuscommunity.sdk.ui.profile.current.nickname

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Links a composer that holds a contribution back to the nickname choice screen it opened: the screen
 * reads which contribution it names, and tells the composers when it is closed without a choice so
 * the held contribution is dropped (never sent later). One per SDK container.
 */
internal class NicknameChoiceSession {
    private val _pending = MutableStateFlow<NicknameChoiceContribution?>(null)

    /** The contribution waiting for a nickname, `null` when the screen was not opened by a composer. */
    val pending = _pending.asStateFlow()

    private val _closedWithoutChoice = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits when the screen is closed without a confirmed nickname. */
    val closedWithoutChoice = _closedWithoutChoice.asSharedFlow()

    /** Records that a composer held [contribution] back and is opening the nickname choice for it. */
    fun open(contribution: NicknameChoiceContribution) {
        _pending.value = contribution
    }

    /** The nickname was chosen: nothing is pending anymore, the holding composer sends on its own. */
    fun finish() {
        _pending.value = null
    }

    /** The screen was closed without a choice: nothing is pending anymore and the held contribution is dropped. */
    fun closeWithoutChoice() {
        _pending.value = null
        _closedWithoutChoice.tryEmit(Unit)
    }
}
