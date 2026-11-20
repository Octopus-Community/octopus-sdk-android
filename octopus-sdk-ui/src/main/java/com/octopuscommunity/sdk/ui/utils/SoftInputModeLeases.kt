package com.octopuscommunity.sdk.ui.utils

import android.view.WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST
import java.util.WeakHashMap

/**
 * Reference-counted override of a window's soft input mode, shared by every screen that needs it.
 *
 * Each screen used to save the mode, force its own, and restore what it saved on dispose. That is
 * only right when screens leave in the reverse order they came in, and a navigation transition
 * breaks that order: the outgoing and incoming screens are composed together, so the screen being
 * left disposed last and put the host's mode back under the screen now on top. Coming back, the
 * mode flipped again while the keyboard was open, and the window re-laid out under the user.
 *
 * Here the first holder of a window saves its mode and applies the override, later holders only
 * count, and the last one to leave restores the saved mode, whatever the order they leave in.
 * A later holder still re-applies the override if something else (the host, another library)
 * changed the window's adjust mode while the lease was held; screens handing over to each other
 * write nothing. Composition runs on the main thread, so no synchronization is needed. Windows are
 * weakly held.
 */
internal class SoftInputModeLeases<W : Any>(
    private val readMode: (W) -> Int?,
    private val writeMode: (W, Int) -> Unit,
) {
    private class Lease(
        val originalMode: Int?,
        var holders: Int,
    )

    private val leases = WeakHashMap<W, Lease>()

    /**
     * Takes a hold on [window], applying [mode] if this is the first hold, or if the window's adjust
     * mode no longer matches [mode] (changed by someone else while the lease was held).
     */
    fun acquire(
        window: W,
        mode: Int,
    ) {
        val lease = leases[window]
        if (lease != null) {
            lease.holders++
            val current = readMode(window)
            if (current == null || current and SOFT_INPUT_MASK_ADJUST != mode and SOFT_INPUT_MASK_ADJUST) {
                writeMode(window, mode)
            }
        } else {
            leases[window] = Lease(originalMode = readMode(window), holders = 1)
            writeMode(window, mode)
        }
    }

    /** Releases a hold on [window], restoring its original mode once no hold is left. */
    fun release(window: W) {
        val lease = leases[window] ?: return
        lease.holders--
        if (lease.holders <= 0) {
            leases.remove(window)
            lease.originalMode?.let { writeMode(window, it) }
        }
    }
}
