package com.octopuscommunity.sdk.sample.data

import com.octopuscommunity.sdk.sample.BuildConfig

/**
 * Named demo API keys shown as Switch-Community presets in the lifecycle scenario,
 * and used as the label source (matched by slot id) for the Config screen's
 * named-key picker (see `ApiKeyPresets.kt`).
 *
 * PUBLIC-SAFE and committed on purpose: this source declares only a generic
 * parser and reads ONE build-time field (`BuildConfig.OCTOPUS_NAMED_API_KEYS`).
 * It enumerates NO internal demo community, carries NO human label, and
 * embeds NO key value — those live exclusively in the launcher / private
 * `octopus.properties` (gitignored) that fills the field at build time from
 * the local build configuration.
 *
 * A public / keyless build (cold clone, CI, the public OSS mirror) passes no
 * value, so [injectedApiKeys] is empty and the Switch-Community scenario
 * falls back to its plain free-text input. No secret, and no list of internal
 * QA communities, ever ships in this repo's source.
 *
 * Wire format (one field, parsed at runtime): entries separated by `;`,
 * fields by `~`, in `id~label~key` order:
 *
 *   slotA~Human label A~AbC123;slotB~Human label B~XyZ789
 *
 * Keys are base64url-shaped (`[A-Za-z0-9_-]`) and labels are plain text, so
 * neither ever contains the `;`/`~` delimiters.
 */
data class InjectedApiKey(
    /** Stable identifier (used for test ids and preset display). */
    val id: String,
    /** Human-readable description of the community configuration. */
    val label: String,
    /** The key value handed to the SDK when this preset is picked. */
    val key: String,
)

/**
 * The named keys present in this build, parsed from the injected field.
 * Empty on a public / keyless build → the Switch-Community scenario shows
 * its free-text fallback only.
 */
val injectedApiKeys: List<InjectedApiKey> = parseNamedApiKeys(BuildConfig.OCTOPUS_NAMED_API_KEYS)

/**
 * Parses the `OCTOPUS_NAMED_API_KEYS` wire string into typed slots.
 *
 * Public (rather than private) so a unit test can exercise it without a real
 * build-time field. Defensive against the realities of a shell-built value:
 * tolerates empty input, surrounding whitespace, and trailing / empty
 * entries, and silently drops malformed entries (wrong field count or an
 * empty id / key) rather than throwing into the screen's composition.
 */
fun parseNamedApiKeys(raw: String): List<InjectedApiKey> {
    val keys = mutableListOf<InjectedApiKey>()
    for (entry in raw.split(';')) {
        val trimmed = entry.trim()
        if (trimmed.isEmpty()) continue
        val fields = trimmed.split('~')
        if (fields.size != 3) continue
        val id = fields[0].trim()
        val label = fields[1].trim()
        val key = fields[2].trim()
        if (id.isEmpty() || key.isEmpty()) continue
        keys.add(InjectedApiKey(id = id, label = label, key = key))
    }
    return keys
}
