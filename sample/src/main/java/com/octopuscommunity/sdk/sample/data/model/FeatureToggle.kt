package com.octopuscommunity.sdk.sample.data.model

/**
 * A boolean SDK option a tester can flip at runtime, from the header of the Scenarios section it
 * belongs to — and from nowhere else.
 *
 * The shared design contract states the rule twice, because it is the one that keeps the sample
 * readable: *"a feature toggle lives in the section header — and nowhere else"*, and *"a toggle
 * without an observable effect is a bug, not a setting"*. Two consequences are encoded here rather
 * than left to each screen:
 *
 * - **Every toggle owns its effect sentence**, in product words, for both positions. The Scenarios
 *   header renders it under the switch, the scenario screen renders [stateLine] read-only, Debug
 *   info renders [readOnlyValueLine]. A single source means the three surfaces cannot drift
 *   into describing the same switch differently.
 * - **The wording never names a type or an SDK symbol.** That vocabulary belongs to the
 *   scenario screen's API pill (design card 07); a toggle line is what the tester and the PO read.
 *
 * The effect sentences describe what a *tester* observes, not what the code does. Both toggles are
 * host-integration decisions rather than live SDK switches: flipping one rewrites the persisted
 * sample config, which is what a host app would ship.
 */
enum class FeatureToggle(
    val id: String,
    val label: String,
    val effectWhenOn: String,
    val effectWhenOff: String,
    val testTag: String,
) {
    ForceLogin(
        id = "force-login",
        label = "Force login",
        effectWhenOn = "Visitors must sign in before they can read or post.",
        effectWhenOff = "Visitors can browse the community before signing in.",
        testTag = "scenarios-toggle-force-login",
    ),

    /**
     * Whether the sample hands new device tokens to Octopus.
     *
     * Deliberately worded as "new tokens": the SDK entry point takes a token to register and has no
     * public counterpart to unregister one, so switching this off stops the *next* registration —
     * it does not revoke a token the backend already holds. Claiming otherwise in the effect line
     * would be the "toggle without an observable effect" the design contract calls a bug, in
     * reverse: an effect claimed but not delivered.
     */
    PushRegistration(
        id = "push-registration",
        label = "Push registration",
        effectWhenOn = "New device tokens go to Octopus, so this device can be notified.",
        effectWhenOff = "Device tokens stay in the app; already-registered ones live until a reset.",
        testTag = "scenarios-toggle-push-registration",
    ),
    ;

    /** The line rendered under the switch on the Scenarios section header. */
    fun effectLine(enabled: Boolean): String = if (enabled) effectWhenOn else effectWhenOff

    /** Read-only state, as the scenario screen shows it: "Force login is on". */
    fun stateLine(enabled: Boolean): String = "$label is ${onOff(enabled)}"

    /** Read-only state plus where to change it, as Debug info shows it. */
    fun readOnlyValueLine(enabled: Boolean): String = "${onOff(enabled)} · set in Scenarios"

    private fun onOff(enabled: Boolean) = if (enabled) "on" else "off"
}

/**
 * The live position of every [FeatureToggle], derived from the persisted [SDKConfig].
 *
 * Derived rather than stored as its own record: the toggles *are* SDK config fields, and a second
 * copy of a boolean is a second thing to keep in sync. This type exists so the read-only
 * surfaces (Scenarios header, scenario screen, Debug info) depend on one small value instead of each
 * reaching into a different corner of [SDKConfig].
 */
data class FeatureToggles(
    val forceLogin: Boolean = false,
    val pushRegistration: Boolean = true,
) {
    operator fun get(toggle: FeatureToggle): Boolean =
        when (toggle) {
            FeatureToggle.ForceLogin -> forceLogin
            FeatureToggle.PushRegistration -> pushRegistration
        }

    /** "Force login on · Push registration off" — the Debug info subtitle, and the feedback body. */
    val summaryLine: String
        get() =
            FeatureToggle.entries.joinToString(" · ") {
                "${it.label} ${if (get(it)) "on" else "off"}"
            }

    companion object {
        val Default = FeatureToggles()
    }
}
