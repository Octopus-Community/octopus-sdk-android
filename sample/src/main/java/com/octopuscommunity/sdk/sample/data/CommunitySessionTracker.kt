package com.octopuscommunity.sdk.sample.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Counts the community sessions the sample has started — one per `initialize` / `switchCommunity`
 * the app performs.
 *
 * `switchCommunity` builds a **new** Koin container: every repository the SDK hands out afterwards
 * is a new instance, over a cleared database and a new gRPC channel. Anything the host already
 * built from the previous container — the ViewModels of an embedded Octopus surface, for one —
 * keeps resolving against the closed one and can never show the new community. `OctopusSDK` offers
 * no signal a host could observe for that: its own Flows re-bind internally, and
 * `isInitialisedFlow` conflates the `false` → `true` of the internal stop/re-initialize into no
 * emission at all (which is deliberate — it is what stops the embedded UI from blanking mid-switch).
 *
 * So the host counts its own re-targets. [onCommunityTargeted] is called by the two places that
 * re-target the SDK — `SampleApplication`'s config collector and the lifecycle scenario's
 * **Switch community** — **after** the call returns, so a collector reacting to a bump is
 * guaranteed to resolve against the container that is now live rather than the one being torn down.
 *
 * A counter rather than the API key: two different configs can resolve to the same key (a server or
 * auth-mode change re-targets the SDK without touching it), and the key is a secret that has no
 * business being a UI key.
 */
class CommunitySessionTracker {
    private val _generation = MutableStateFlow(0)

    /** Increments once per completed (re)initialization. */
    val generation: StateFlow<Int> = _generation.asStateFlow()

    /** Records that the SDK now targets a (possibly different) community. */
    fun onCommunityTargeted() {
        _generation.update { it + 1 }
    }
}
