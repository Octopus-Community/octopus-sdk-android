package com.octopuscommunity.sdk.ui.utils

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString

/**
 * How much of a user-generated body is shown before it is cut. Iso iOS `TextTruncation`
 * (`Sources/OctopusUI/Common/TextTruncation.swift`) — same name on both platforms so the pair
 * stays greppable.
 *
 * [maxLines] counts hard `\n` characters, not rendered lines: no call site passes `maxLines` to
 * `Text`, so a single [maxLength]-character paragraph with no newline in it still wraps to as many
 * visual lines as the available width needs.
 */
@Immutable
internal data class TextTruncation(
    val maxLength: Int,
    val maxLines: Int,
) {
    init {
        require(maxLength >= 0) { "maxLength must be >= 0, was $maxLength" }
        require(maxLines >= 1) { "maxLines must be >= 1, was $maxLines" }
    }

    companion object {
        /** Post and comment bodies as shown in feeds. */
        val Content = TextTruncation(maxLength = 200, maxLines = 4)

        /** Group descriptions and profile bios (`OctopusExpandableText`). */
        val Summary = TextTruncation(maxLength = 140, maxLines = 2)
    }
}

/**
 * The result of a cut: what to draw, and whether real content was dropped.
 *
 * [isTruncated] is the "… Read more" predicate. It asks whether **non-blank** content was
 * dropped, not merely whether the string changed — trimming a leading space or a block of
 * trailing blank lines is not a truncation, and the previous call-site predicate
 * (`displayed != raw`) wrongly reported one.
 */
internal data class EllipsizedText<T>(
    val value: T,
    val isTruncated: Boolean,
)

/**
 * Line separators normalized to `\n`.
 *
 * The former `String.getEllipsizedText` normalized as a side effect of
 * `lines().joinToString("\n")`. The [AnnotatedString] cut below is `subSequence`-only — it cannot
 * rewrite characters without shifting every annotation offset — so normalization becomes an
 * explicit step performed once, before anything is parsed or annotated. Without it a `\r` would
 * survive into the drawn text and Compose could render an extra blank line.
 */
internal fun String.normalizeLineSeparators(): String = replace("\r\n", "\n").replace('\r', '\n')

/**
 * Cuts [this] to [truncation] — at most `maxLength` characters, then at most `maxLines`
 * newline-separated lines — and trims whitespace off both ends of the result.
 *
 * Implemented with [AnnotatedString.subSequence] only, which **clips** annotation ranges to the
 * kept range instead of dropping them. That is the whole point: a link sliced in half shows cut
 * text but keeps its complete URL, so tapping it opens the destination the author intended
 * (iso iOS internal change).
 *
 * Expects separators already normalized by [normalizeLineSeparators]; it splits on `\n` alone.
 */
internal fun AnnotatedString.ellipsized(truncation: TextTruncation): EllipsizedText<AnnotatedString> {
    val capped = minOf(truncation.maxLength, length)

    var cutEnd = capped
    var breaks = 0
    for (index in 0 until capped) {
        if (text[index] == '\n') {
            breaks++
            if (breaks == truncation.maxLines) {
                cutEnd = index
                break
            }
        }
    }

    var start = 0
    while (start < cutEnd && text[start].isWhitespace()) start++
    var end = cutEnd

    // Drop an orphaned high surrogate BEFORE trimming, not after. A cut landing between a
    // surrogate pair would leave a lone high surrogate, which Compose draws as tofu — but doing
    // this after the trim loop re-exposes whitespace the trim had just removed, so `"abc " + 😀`
    // cut at 5 kept its trailing space and put a double space before the "… Read more" suffix.
    //
    // It is a pragmatic guard for the common case (emoji); full grapheme correctness — ZWJ
    // sequences, flag pairs, combining marks — would need a `BreakIterator` and is out of
    // proportion for a "… Read more" preview.
    if (end > start && text[end - 1].isHighSurrogate()) end--
    while (end > start && text[end - 1].isWhitespace()) end--

    return EllipsizedText(
        value = subSequence(start, end),
        // Scanned rather than `text.substring(cutEnd).isNotBlank()`: the substring copies the whole
        // dropped tail (~9.8 kB on a 10 kB body cut to 200) just to answer a yes/no question.
        isTruncated = (cutEnd until text.length).any { !text[it].isWhitespace() },
    )
}

/**
 * Plain-string cut, **derived** from the [AnnotatedString] overload so the two cannot drift.
 *
 * iOS keeps two implementations and a 392-case matrix test to prove they agree; deriving makes
 * that agreement structural, so no test asserts it. What the tests pin instead is agreement with
 * the algorithm this replaces.
 */
internal fun String.ellipsized(truncation: TextTruncation): EllipsizedText<String> =
    AnnotatedString(normalizeLineSeparators())
        .ellipsized(truncation)
        .let { EllipsizedText(value = it.value.text, isTruncated = it.isTruncated) }
