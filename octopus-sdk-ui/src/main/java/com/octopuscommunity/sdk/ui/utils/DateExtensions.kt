package com.octopuscommunity.sdk.ui.utils

import android.content.Context
import android.content.res.Configuration
import android.icu.number.NumberFormatter
import android.icu.text.DisplayContext
import android.icu.text.MeasureFormat
import android.icu.text.RelativeDateTimeFormatter
import android.icu.text.RelativeDateTimeFormatter.RelativeUnit
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import android.icu.util.ULocale
import android.os.Build
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.octopuscommunity.sdk.ui.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

/**
 * Gets the current locale from Configuration, respecting any overrides.
 * Uses locales[0] on Android N+ or the deprecated locale property on older versions.
 */
internal val Configuration.currentLocale: Locale
    @Suppress("DEPRECATION")
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) locales[0] else locale

/**
 * Returns the elapsed time string for this date, using the locale from LocalConfiguration.
 */
@Composable
internal fun Date.getElapsedTime() = getElapsedTime(LocalConfiguration.current.currentLocale)

internal fun Date.getElapsedTime(locale: Locale): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val seniority = getSeniority()
        when (seniority.unit) {
            MeasureUnit.SECOND, MeasureUnit.MINUTE, MeasureUnit.HOUR, MeasureUnit.DAY -> {
                RelativeDateTimeFormatter
                    .getInstance(
                        ULocale.forLocale(locale),
                        null,
                        RelativeDateTimeFormatter.Style.SHORT,
                        DisplayContext.CAPITALIZATION_NONE,
                    ).format(
                        seniority.value.toDouble(),
                        RelativeDateTimeFormatter.Direction.LAST,
                        seniority.relativeUnit,
                    )
            }

            else ->
                SimpleDateFormat
                    .getDateInstance(
                        SimpleDateFormat.MEDIUM,
                        locale,
                    ).format(this)
        }
    } else {
        // Fallback for older Android versions - uses Locale.getDefault()
        DateUtils
            .getRelativeTimeSpanString(
                this.time,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE,
            ).toString()
    }

/**
 * The age of a post, a comment or a reply as their header displays it.
 *
 * @property text the short form shown on screen, like "8m" or "Oct 12".
 * @property accessibilityText the long form TalkBack reads, like "8 minutes ago" or "2 months ago".
 */
internal data class ElapsedTime(
    val text: String,
    val accessibilityText: String,
)

/**
 * Returns the short elapsed time for this date, using the locale from LocalConfiguration.
 */
@Composable
internal fun Date.getShortElapsedTime() = getShortElapsedTime(LocalConfiguration.current.currentLocale)

/**
 * Formats the age of a post, a comment or a reply in the short form social apps use: "8m", "3h", "2d", "3w", then
 * the date once it is 5 weeks old or more, with the year when it is not the current one. Every form comes from the
 * system formatters, so all languages follow without any wording of ours. Iso iOS `ContentDateFormatter`.
 *
 * TalkBack always gets a relative phrase ("8 minutes ago", "2 months ago"), even when the date is shown: the card
 * summaries insert it in sentences written around a relative phrase.
 */
internal fun Date.getShortElapsedTime(
    locale: Locale,
    now: Date = Date(),
): ElapsedTime {
    // A date slightly in the future (clock skew with the server) reads as the youngest age we display.
    val seconds = maxOf((now.time - time) / 1000, 0L)
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    val showsDate = days >= 35
    val seniority =
        when {
            hours < 1 -> Seniority.Minutes(maxOf(minutes, 1L).toInt())
            days < 1 -> Seniority.Hours(hours.toInt())
            days < 7 -> Seniority.Days(days.toInt())
            !showsDate -> Seniority.Weeks((days / 7).toInt())
            else -> getSeniority(to = now)
        }

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
        // No ICU unit formatter below API 24: the abbreviated relative span is the shortest system form.
        val relative =
            DateUtils
                .getRelativeTimeSpanString(time, now.time, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE)
                .toString()
        return ElapsedTime(text = if (showsDate) getShortDate(locale, now) else relative, accessibilityText = relative)
    }
    val uLocale = ULocale.forLocale(locale)
    return ElapsedTime(
        text =
            if (showsDate) {
                getShortDate(locale, now)
            } else {
                MeasureFormat
                    .getInstance(uLocale, MeasureFormat.FormatWidth.NARROW)
                    .format(Measure(seniority.value, seniority.unit))
            },
        accessibilityText =
            RelativeDateTimeFormatter
                .getInstance(uLocale, null, RelativeDateTimeFormatter.Style.LONG, DisplayContext.CAPITALIZATION_NONE)
                .format(seniority.value.toDouble(), RelativeDateTimeFormatter.Direction.LAST, seniority.relativeUnit),
    )
}

private fun Date.getShortDate(
    locale: Locale,
    now: Date,
): String {
    val year = Calendar.getInstance().apply { time = this@getShortDate }[Calendar.YEAR]
    val currentYear = Calendar.getInstance().apply { time = now }[Calendar.YEAR]
    val pattern = DateFormat.getBestDateTimePattern(locale, if (year == currentYear) "dMMM" else "dMMMy")
    return SimpleDateFormat(pattern, locale).format(this)
}

/**
 * Returns the localized age string for this date, using the locale from LocalConfiguration.
 */
@Composable
fun Date.getLocalizedAge() = getLocalizedAge(LocalContext.current, LocalConfiguration.current.currentLocale)

internal fun Date.getLocalizedAge(
    context: Context,
    locale: Locale,
): String {
    val seniority = getSeniority(minSeniority = Seniority.Days(0))

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        NumberFormatter
            .withLocale(locale)
            .unit(seniority.unit)
            .unitWidth(NumberFormatter.UnitWidth.FULL_NAME)
            .format(seniority.value.toLong())
            .toString()
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        "${seniority.value} " +
            MeasureFormat
                .getInstance(
                    locale,
                    MeasureFormat.FormatWidth.WIDE,
                ).getUnitDisplayName(seniority.unit)
    } else {
        context.resources.getQuantityString(
            when {
                seniority <= Seniority.Days(0) -> R.plurals.common_duration_day_value
                seniority is Seniority.Weeks -> R.plurals.common_duration_week_value
                seniority is Seniority.Months -> R.plurals.common_duration_month_value
                else -> R.plurals.common_duration_year_value
            },
            seniority.value,
            seniority.value,
        )
    }
}

internal sealed interface Seniority : Comparable<Seniority> {
    data class Seconds(
        override val value: Int,
    ) : Seniority {
        override val unit: MeasureUnit
            @RequiresApi(Build.VERSION_CODES.N)
            get() = MeasureUnit.SECOND
        override val ordinal: Int = 0
    }

    data class Minutes(
        override val value: Int,
    ) : Seniority {
        override val unit: MeasureUnit
            @RequiresApi(Build.VERSION_CODES.N)
            get() = MeasureUnit.MINUTE
        override val ordinal: Int = 1
    }

    data class Hours(
        override val value: Int,
    ) : Seniority {
        override val unit: MeasureUnit
            @RequiresApi(Build.VERSION_CODES.N)
            get() = MeasureUnit.HOUR
        override val ordinal: Int = 2
    }

    data class Days(
        override val value: Int,
    ) : Seniority {
        override val unit: MeasureUnit
            @RequiresApi(Build.VERSION_CODES.N)
            get() = MeasureUnit.DAY
        override val ordinal: Int = 3
    }

    data class Weeks(
        override val value: Int,
    ) : Seniority {
        override val unit: MeasureUnit
            @RequiresApi(Build.VERSION_CODES.N)
            get() = MeasureUnit.WEEK
        override val ordinal: Int = 4
    }

    data class Months(
        override val value: Int,
    ) : Seniority {
        override val unit: MeasureUnit
            @RequiresApi(Build.VERSION_CODES.N)
            get() = MeasureUnit.MONTH
        override val ordinal: Int = 5
    }

    data class Years(
        override val value: Int,
    ) : Seniority {
        override val unit: MeasureUnit
            @RequiresApi(Build.VERSION_CODES.N)
            get() = MeasureUnit.YEAR
        override val ordinal: Int = 6
    }

    val value: Int
    val unit: MeasureUnit
    val ordinal: Int

    override fun compareTo(other: Seniority): Int = ordinal.compareTo(other.ordinal)

    val relativeUnit
        @RequiresApi(Build.VERSION_CODES.N)
        get() =
            when (unit) {
                MeasureUnit.SECOND -> RelativeUnit.SECONDS
                MeasureUnit.MINUTE -> RelativeUnit.MINUTES
                MeasureUnit.HOUR -> RelativeUnit.HOURS
                MeasureUnit.DAY -> RelativeUnit.DAYS
                MeasureUnit.WEEK -> RelativeUnit.WEEKS
                MeasureUnit.MONTH -> RelativeUnit.MONTHS
                MeasureUnit.YEAR -> RelativeUnit.YEARS
                else -> RelativeUnit.SECONDS
            }
}

internal fun Date.getSeniority(
    to: Date = Date(),
    minSeniority: Seniority? = null,
): Seniority {
    val duration = (to.time - time).milliseconds
    val seconds = duration.inWholeSeconds.toInt()
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = duration.inWholeDays.toInt()
    val weeks = days / 7

    val start = Calendar.getInstance().apply { time = this@getSeniority }
    val end = Calendar.getInstance().apply { time = to }

    val months =
        (end[Calendar.YEAR] - start[Calendar.YEAR]) * 12 +
            end[Calendar.MONTH] - start[Calendar.MONTH] -
            if (end[Calendar.DAY_OF_MONTH] < start[Calendar.DAY_OF_MONTH]) 1 else 0

    val years =
        end[Calendar.YEAR] - start[Calendar.YEAR] -
            if (end[Calendar.MONTH] < start[Calendar.MONTH] ||
                (
                    end[Calendar.MONTH] == start[Calendar.MONTH] &&
                        end[Calendar.DAY_OF_MONTH] < start[Calendar.DAY_OF_MONTH]
                )
            ) {
                1
            } else {
                0
            }

    val seniority =
        when {
            seconds < 60 -> Seniority.Seconds(maxOf(seconds, 1))
            minutes < 60 -> Seniority.Minutes(maxOf(minutes, 1))
            hours < 24 -> Seniority.Hours(maxOf(hours, 1))
            days < 7 -> Seniority.Days(maxOf(days, 1))
            weeks < 4 -> Seniority.Weeks(maxOf(weeks, 1))
            months < 12 -> Seniority.Months(maxOf(months, 1))
            else -> Seniority.Years(maxOf(years, 1))
        }

    return if (minSeniority == null || seniority >= minSeniority) {
        seniority
    } else {
        when (minSeniority) {
            is Seniority.Seconds -> Seniority.Seconds(1)
            is Seniority.Minutes -> Seniority.Minutes(1)
            is Seniority.Hours -> Seniority.Hours(1)
            is Seniority.Days -> Seniority.Days(1)
            is Seniority.Weeks -> Seniority.Weeks(1)
            is Seniority.Months -> Seniority.Months(1)
            is Seniority.Years -> Seniority.Years(1)
        }
    }
}
