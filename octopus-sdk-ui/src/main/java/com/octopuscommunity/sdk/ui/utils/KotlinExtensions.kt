package com.octopuscommunity.sdk.ui.utils

import java.util.Locale
import kotlin.math.floor

/**
 * Formats a count as a compact string (e.g. `1.2K`, `12K`, `1.2M`, `999M`, `1.2B`).
 *
 * Below 1 000 the raw integer string is returned. Above 1 000 the value is divided
 * by 1 000 / 1 000 000 / 1 000 000 000, floored, and rendered with one decimal when
 * the truncated value is < 10 (e.g. `1.2K`, `9.9K`), or as an integer otherwise
 * (e.g. `12K`, `999K`). Trailing `.0` is stripped (`2K`, not `2.0K`).
 *
 * The decimal separator follows the provided [locale] (`1,2K` in `fr-FR`, `1.2K` in `en-US`).
 * Always pass `LocalConfiguration.current.currentLocale` from the calling Composable so the
 * format stays in sync with the SDK locale override (see `OctopusLocaleWrapper`) rather than
 * the JVM-wide default — `OctopusSDK.overrideDefaultLocale(...)` only updates
 * `LocalConfiguration`, never `Locale.getDefault()`.
 */
internal fun Int.toCompactString(locale: Locale): String =
    when {
        this >= 1_000_000_000 -> formatCompact(1_000_000_000.0, "B", locale)
        this >= 1_000_000 -> formatCompact(1_000_000.0, "M", locale)
        this >= 1_000 -> formatCompact(1_000.0, "K", locale)
        else -> toString()
    }

private fun Int.formatCompact(
    divisor: Double,
    suffix: String,
    locale: Locale,
): String {
    val value = this / divisor
    return if (value < 10) {
        val truncated = floor(value * 10) / 10
        if (truncated.rem(1.0) == 0.0) {
            "${truncated.toInt()}$suffix"
        } else {
            String.format(locale, "%.1f", truncated) + suffix
        }
    } else {
        "${floor(value).toInt()}$suffix"
    }
}
