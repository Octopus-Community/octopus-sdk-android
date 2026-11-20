//
//  Copyright © 2026 Octopus Community. All rights reserved.
//

package com.octopuscommunity.sdk.data.local.data

/**
 * Applies an `AppAlive` heartbeat to the stored sessions, returning the records to persist.
 *
 * Extracted from [com.octopuscommunity.sdk.data.repository.SessionRepositoryImpl] so the rule can
 * be exercised from a JVM test: the store it is applied to needs a `Context`, which put this
 * decision out of reach of the unit tests (internal ticket).
 * As on iOS, wall-clock ordering does not determine ownership; legacy reused ids may be misattributed once.
 */
internal fun SessionsData.keptAliveAt(now: Long): SessionsData {
    val liveAppSession = appSession?.takeIf { it.stopTime == null }
    return copy(
        appSession = appSession?.copy(lastAliveTime = now),
        // Refresh an open visit only while its named app session is live. App starts create new
        // ids, and stops accept rollback bounds, so an older run is identified by its parent id.
        // Do not compare start times: a clock rollback can put a current visit before its parent.
        // Keep inactive records unchanged; recovery uses their last heartbeat to close them.
        octopusSession =
            octopusSession?.let { octopus ->
                val insideLiveAppSession =
                    liveAppSession != null &&
                        octopus.appSessionId == liveAppSession.id
                if (octopus.stopTime == null && insideLiveAppSession) {
                    octopus.copy(lastAliveTime = now)
                } else {
                    octopus
                }
            },
    )
}
