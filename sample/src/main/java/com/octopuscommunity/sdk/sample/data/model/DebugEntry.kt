package com.octopuscommunity.sdk.sample.data.model

import java.util.concurrent.atomic.AtomicLong

/**
 * A single entry in the debug console log.
 *
 * @param timestamp Epoch millis when the entry was recorded
 * @param category The entry category for filtering
 * @param summary Short description shown in the collapsed view
 * @param details Full details shown when expanded (optional)
 */
data class DebugEntry(
    val id: Long = nextId(),
    val timestamp: Long = System.currentTimeMillis(),
    val category: Category,
    val summary: String,
    val details: String? = null
) {
    companion object {
        private val counter = AtomicLong(0)
        private fun nextId(): Long = counter.getAndIncrement()
    }

    enum class Category(val label: String) {
        EVENT("Event"),
        STATE("State"),
        API_CALL("API Call")
    }
}
