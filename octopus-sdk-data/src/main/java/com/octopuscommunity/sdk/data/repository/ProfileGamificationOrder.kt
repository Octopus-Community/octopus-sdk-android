package com.octopuscommunity.sdk.data.repository

import java.util.concurrent.atomic.AtomicLong

/** Process-wide request order shared by public and private profile reads. Nothing is persisted. */
internal object ProfileGamificationOrder {
    private val tickets = AtomicLong()

    fun nextTicket(): Long = tickets.incrementAndGet()
}
