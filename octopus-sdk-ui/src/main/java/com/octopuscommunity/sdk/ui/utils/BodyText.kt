package com.octopuscommunity.sdk.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource

/**
 * Turns a user-generated body into what gets drawn: parse the **complete** text, then cut.
 *
 * The order is the fix for internal ticket. Cutting first — as the call sites used to — meant the
 * shortened branch could not be linkified without producing links to sliced URLs, so
 * linkification was simply switched off in the feed and the same body behaved differently on two
 * screens. Parsing first and cutting the `AnnotatedString` keeps every link, with its full URL,
 * on both. Iso iOS `RichText(_:truncation:)` (internal change).
 *
 * Non-`@Composable` on purpose: this is the whole behaviour, and it stays unit-testable. The
 * composable wrapper only resolves the string resource and the theme colours.
 *
 * The truncation result also governs selection, so the UI suffix can never be copied.
 *
 * @param truncation how to shorten the body, or `null` to render it in full.
 * @param readMore the resolved `common_read_more` string, appended after `"... "` when the body
 * was actually cut.
 */
internal fun buildBody(
    text: String,
    truncation: TextTruncation?,
    readMore: String,
    ellipsisStyle: SpanStyle,
    linkStyle: SpanStyle,
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
): EllipsizedText<AnnotatedString> {
    val parsed =
        buildAnnotatedString {
            appendRichText(
                text = text.normalizeLineSeparators(),
                linkify = true,
                markdown = true,
                linkStyle = linkStyle,
            )
        }
    val displayed =
        truncation?.let { parsed.ellipsized(it) }
            ?: EllipsizedText(value = parsed, isTruncated = false)

    if (!displayed.isTruncated) return displayed

    return displayed.copy(
        value =
            buildAnnotatedString {
                withStyle(
                    ParagraphStyle(
                        textDirection =
                            if (layoutDirection ==
                                LayoutDirection.Rtl
                            ) {
                                TextDirection.Rtl
                            } else {
                                TextDirection.Ltr
                            },
                    ),
                ) {
                    val content =
                        buildAnnotatedString {
                            append(displayed.value)
                            withStyle(ellipsisStyle) { append("...") }
                        }
                    append(isolateBodyParagraphs(content))
                    withStyle(ellipsisStyle) { append(" $readMore") }
                }
            },
    )
}

// The equivalent loop in buildExpandableText is not exposed separately. Keep this local until
// StringExtensions can offer a shared AnnotatedString helper; never reparse the truncated links.
private fun isolateBodyParagraphs(content: AnnotatedString): AnnotatedString =
    buildAnnotatedString {
        var start = 0
        for (index in content.text.indices) {
            if (index < start) continue
            if (Character.getDirectionality(content.text[index]) == Character.DIRECTIONALITY_PARAGRAPH_SEPARATOR) {
                append("\u2068")
                append(content.subSequence(start, index))
                append("\u2069")
                val end = if (content.text[index] == '\r' && content.text.getOrNull(index + 1) == '\n') index + 2 else index + 1
                append(content.subSequence(index, end))
                start = end
            }
        }
        append("\u2068")
        append(content.subSequence(start, content.length))
        append("\u2069")
    }

/**
 * Composable front for [buildBody]: resolves `common_read_more` and the theme colours, and
 * memoizes the parse.
 *
 * The `remember` really does memoize: every key has value equality ([TextTruncation] is a data
 * class, `SpanStyle` hand-writes `equals`/`hashCode`, `Color` is an inline value class, and the
 * theme colour is constant), so rebuilding the default `SpanStyle`s inline on each recomposition
 * still hits the memo.
 *
 * It does not make the cost disappear, though, and the honest picture is worse than it looks. A
 * feed row used to parse *nothing* — the shortened branch appended plain text — and now parses its
 * complete body. The memo covers recompositions, not the first composition, and `LazyColumn`
 * disposes off-screen rows, so scrolling back re-parses. Worse, [appendRichText] copies the
 * remainder with `substring` per match and re-runs all five patterns from the start of that
 * remainder, which is quadratic. Warmed best-of-7 on a desktop JVM at the 5000-character body cap:
 * ~67 ms for a body of `*a*` pairs, ~51 ms for `**a**`, ~45 ms for `_a_`, ~22 ms for one made of
 * `[` characters, and ~6.6 ms for a realistic body mixing a link and some bold. Plain prose is
 * ~0.13 ms. So the worst case is a body dense in emphasis pairs, not one dense in brackets — each
 * pair is a match, and every match copies the remainder. At ~67 ms on a desktop that is plausibly
 * 200-400 ms per row on a mid-range device, which makes a hostile body a jank vector rather than
 * merely slow. `Linkify` then runs once more per plain segment between matches, on top.
 *
 * Do NOT "fix" this by pre-cutting the source before parsing — that is exactly the bug internal ticket
 * removed, and it would destroy the complete-URL guarantee. The fix is inside [appendRichText]:
 * scan with `Regex.find(input, startIndex)` instead of copying the remainder, or replace the
 * five-pattern sweep with a single-pass scanner. Tracked separately.
 */
@Composable
internal fun rememberBody(
    text: String,
    truncation: TextTruncation?,
    ellipsisStyle: SpanStyle =
        SpanStyle(
            color = OctopusTheme.colorScheme.gray500,
            fontWeight = FontWeight.Medium,
        ),
    linkStyle: SpanStyle =
        SpanStyle(
            color = OctopusTheme.colorScheme.link,
            textDecoration = TextDecoration.Underline,
        ),
): EllipsizedText<AnnotatedString> {
    val readMore = stringResource(R.string.common_read_more)
    val layoutDirection = LocalLayoutDirection.current
    return remember(text, truncation, readMore, ellipsisStyle, linkStyle, layoutDirection) {
        buildBody(
            text = text,
            truncation = truncation,
            readMore = readMore,
            ellipsisStyle = ellipsisStyle,
            linkStyle = linkStyle,
            layoutDirection = layoutDirection,
        )
    }
}
