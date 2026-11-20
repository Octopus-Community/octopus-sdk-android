package com.octopuscommunity.sdk.ui.common.viewmodel

import java.util.UUID

/**
 * Storage ids of the windows that lists opened on an item keep apart from the feed they read.
 *
 * A window stored under the feed's own id would show in every other list of the feed, a poll of
 * one of them would replace it under the user's eyes, and the next list opening on the newest
 * items would show it from the cache, offline too. Each list therefore stores its window under
 * an id of its own, and deletes it once cleared.
 */
internal object WindowFeeds {
    /** Tells a window's storage id apart from a feed id. */
    const val ID_MARKER = ".window."

    /**
     * Tells this launch's windows apart from those an earlier one left behind, when it was killed
     * before its lists were cleared: [launchIdMarker] keeps the windows still on screen out of the
     * purge, whenever it runs.
     */
    private val launchId = UUID.randomUUID().toString()

    /** Marker of the storage ids of this launch's windows. */
    val launchIdMarker = "$ID_MARKER$launchId."

    /** A new storage id for a window of the feed [feedId]. */
    fun newId(feedId: String): String = "$feedId$launchIdMarker${UUID.randomUUID()}"
}
