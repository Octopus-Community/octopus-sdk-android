package com.octopuscommunity.sdk.sample.data

import com.octopuscommunity.sdk.sample.BuildConfig

/**
 * Predefined SSO user ids surfaced by the LoginScreen quick-pick chips.
 *
 * Hardcoded on purpose — these are stable test identities, not secrets, and
 * they ship in the public OSS mirror. Each value is the literal `sub` the
 * host-side SSO signer embeds in the user JWT handed to
 * [com.octopuscommunity.sdk.OctopusSDK.connectUser].
 *
 * The build-time `OCTOPUS_USER_ID` (forwarded by a private launcher or
 * `octopus.properties`) still acts as the initial seed for fresh installs /
 * CI, but at runtime the LoginScreen lets QA pick a different identity (or
 * paste a custom one) so two instances of the sample running side-by-side can
 * hold distinct user JWTs — required for end-to-end push-notification testing
 * between, e.g., an Android emulator and an iOS simulator.
 *
 * Adding a new id: just append to [defaultPredefinedUserIds]. The picker
 * rebuilds from this list + the build-time seed (deduped).
 */
val defaultPredefinedUserIds: List<String> =
    listOf(
        "android-sample-user",
        "qa-tester-ios",
        "qa-tester-android",
    )

/**
 * The final picker list: the build-time seed first (so the default chip is
 * the id the launcher signed the bundled `OCTOPUS_USER_TOKEN` against — if
 * any), then the hardcoded predefined ids, deduped while preserving insertion
 * order. The custom-text fallback is just the TextField itself, not an entry
 * in this list.
 */
val pickerUserIds: List<String> =
    buildPickerUserIds(
        seed = BuildConfig.OCTOPUS_USER_ID,
        predefined = defaultPredefinedUserIds,
    )

/**
 * Pure helper exposed for tests: builds the picker list from a seed and a
 * predefined-id list. Trims each value and drops empties; deduplicates while
 * preserving first-seen order (the seed is first when non-empty).
 */
fun buildPickerUserIds(
    seed: String,
    predefined: Iterable<String>,
): List<String> {
    val out = mutableListOf<String>()
    val seen = mutableSetOf<String>()

    fun addIfNew(value: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) return
        if (seen.add(trimmed)) out.add(trimmed)
    }
    addIfNew(seed)
    predefined.forEach { addIfNew(it) }
    return out
}
