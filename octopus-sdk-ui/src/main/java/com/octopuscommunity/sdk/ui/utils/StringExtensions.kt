package com.octopuscommunity.sdk.ui.utils

import android.text.SpannableString
import android.text.style.URLSpan
import android.text.util.Linkify
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.graphics.toColorInt
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource
import kotlin.math.abs

internal fun String.toColor() = Color(toColorInt())

/**
 * The string a default avatar takes its color from: [colorSeed] when there is one, so that a nickname being typed
 * keeps the color of the stored one instead of changing with every character, the nickname otherwise.
 */
internal fun avatarColorSource(
    nickname: String,
    colorSeed: String?,
): String = colorSeed?.ifEmpty { null } ?: nickname

/**
 * Used to generate a color based on the user's name
 */
internal fun String.toUserColor(): Color {
    val hash = this.hashCode()
    val r = abs(hash and 0xFF) % 128 + 128 // 128-255
    val g = abs((hash shr 8) and 0xFF) % 128 + 128 // 128-255
    val b = abs((hash shr 16) and 0xFF) % 128 + 128 // 128-255
    return Color(r / 255f, g / 255f, b / 255f, 1f)
}

/**
 * to get initials from a name (unchanged)
 */
internal fun String.toInitials(count: Int = 1): String =
    this
        .split(" ")
        .take(count)
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .joinToString("")

/**
 * Renders user-generated text into an [AnnotatedString], iso-iOS `RichText`.
 *
 * Inline markdown set (ported from iOS `RichText.swift` `MarkdownText.parse(text:)`):
 * - `[label](url)` → underlined link styled with [linkStyle]
 * - `**bold**` / `__bold__` → bold
 * - `*italic*` / `_italic_` → italic
 *
 * The content of a bold or italic run is rendered the same way, so `**Read [the terms](url)**`
 * is a bold link and `**bold _and italic_**` nests. Emphasis inside a link label is not parsed.
 *
 * In addition, when [linkify] is true, plain segments (text outside markdown) are scanned for
 * bare URLs and emails using Android `Linkify`. This matches the iOS<15 fallback in
 * `RichText.swift`; iOS 15+ uses Apple's `AttributedString(markdown:)` which only renders
 * explicit markdown links — keeping `Linkify` here gives a strictly better UX.
 *
 * When [markdown] is false the text is rendered verbatim (no bold/italic/link parsing) —
 * matches iOS `Text(verbatim:)` used in the ellipsized branch of `PostTextContentView` and
 * `ResponseTextContentView`.
 *
 * Links open through the composition's `UriHandler` ([LinkAnnotation.Url]) by default. When
 * [onLinkClick] is given, they become [LinkAnnotation.Clickable] annotations tagged with their URL
 * that hand that URL to [onLinkClick] instead — for texts whose taps must go through the view
 * model's navigation events, like the community's own consent wordings.
 *
 * Whitespace (newlines, multiple spaces) is preserved verbatim, matching iOS
 * `inlineOnlyPreservingWhitespace`. Headings and lists are not markdown here: they stay as typed.
 */
internal fun AnnotatedString.Builder.appendRichText(
    text: String,
    linkify: Boolean = true,
    markdown: Boolean = true,
    linkStyle: SpanStyle,
    onLinkClick: ((url: String) -> Unit)? = null,
) {
    if (!markdown) {
        appendLinkifiedSegment(text, linkify, linkStyle, onLinkClick)
        return
    }
    var remaining = text
    while (remaining.isNotEmpty()) {
        var earliest: Pair<MatchResult, MarkdownPattern>? = null
        for ((kind, regex) in MARKDOWN_PATTERNS) {
            val match = regex.find(remaining) ?: continue
            if (earliest == null || match.range.first < earliest.first.range.first) {
                earliest = match to kind
            }
        }
        if (earliest == null) {
            appendLinkifiedSegment(remaining, linkify, linkStyle, onLinkClick)
            return
        }
        val (match, kind) = earliest
        if (match.range.first > 0) {
            appendLinkifiedSegment(remaining.substring(0, match.range.first), linkify, linkStyle, onLinkClick)
        }
        when (kind) {
            MarkdownPattern.Link -> {
                val label = match.groupValues[1]
                val url = match.groupValues[2]
                val start = length
                append(label)
                addRoutedLink(url = url, linkStyle = linkStyle, onLinkClick = onLinkClick, start = start, end = length)
            }
            // The emphasized content is rendered like the rest of the text, so a link or a nested
            // emphasis inside it keeps its own markup instead of showing as raw markdown.
            MarkdownPattern.BoldStar, MarkdownPattern.BoldUnderscore -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    appendRichText(match.groupValues[1], linkify, markdown, linkStyle, onLinkClick)
                }
            }
            MarkdownPattern.ItalicStar, MarkdownPattern.ItalicUnderscore -> {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    appendRichText(match.groupValues[1], linkify, markdown, linkStyle, onLinkClick)
                }
            }
        }
        remaining = remaining.substring(match.range.last + 1)
    }
}

private enum class MarkdownPattern {
    Link,
    BoldStar,
    BoldUnderscore,
    ItalicStar,
    ItalicUnderscore,
}

// ORDER IS LOAD-BEARING: on a tie at `range.first`, `appendRichText` keeps the first match seen
// (strict `<` comparison). Bold patterns must come before their italic counterparts so `**bold**`
// is taken as bold (not as italic-italic with empty inner) and `__bold__` likewise. Link comes
// first so `[**label**](url)` is taken as a link (not as bold containing brackets). Do NOT sort
// or refactor this list without re-checking the `appendRichText` semantics.
//
// The two underscore patterns are guarded against intra-word emphasis, `*` deliberately is not.
// CommonMark forbids emphasis on an underscore run flanked by word characters and allows it for a
// star run, and iOS parses with a real CommonMark implementation — so guarding `*` too would
// diverge from iOS on bodies like `2*3*4`.
//
// The guard uses `\p{L}\p{N}` rather than `[A-Za-z0-9]` because this module ships in 24 locales:
// an ASCII class leaves `имя_переменной_ность`, `كلمة_كلمة_كلمة` and `変数_名前_値` emphasized, since
// breaking the guard needs a non-ASCII character on *both* flanks. It is still an approximation of
// CommonMark, which flanks on Unicode whitespace or punctuation rather than on word characters —
// `😀_😀_😀` is emphasized here and is not in CommonMark, because emoji are `\p{So}`.
//
// The guard is not a nicety. Unguarded, `_(.+?)_` matched from the first underscore of a URL's
// `utm_source=…` to the next one, ate both underscores and split the URL across two segments, so
// `Linkify` only ever saw the prefix: the rendered link targeted `…?utm` while the rest sat beside
// it in italic. Every shared link carrying two tracking parameters opened the wrong page. Found on
// a real feed post during internal ticket; pinned by `BodyAnnotatedStringTest`.
private val MARKDOWN_PATTERNS =
    listOf(
        MarkdownPattern.Link to Regex("""\[([^]]+)]\(([^)]+)\)"""),
        MarkdownPattern.BoldStar to Regex("""\*\*(.+?)\*\*"""),
        MarkdownPattern.BoldUnderscore to Regex("""(?<![\p{L}\p{N}])__(.+?)__(?![\p{L}\p{N}])"""),
        MarkdownPattern.ItalicStar to Regex("""\*(.+?)\*"""),
        MarkdownPattern.ItalicUnderscore to Regex("""(?<![\p{L}\p{N}])_(.+?)_(?![\p{L}\p{N}])"""),
    )

private fun AnnotatedString.Builder.appendLinkifiedSegment(
    text: String,
    linkify: Boolean,
    linkStyle: SpanStyle,
    onLinkClick: ((url: String) -> Unit)?,
) {
    if (!linkify) {
        append(text)
        return
    }
    val startIndex = length
    append(text)
    val spannable =
        SpannableString(text).apply {
            Linkify.addLinks(this, Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES)
        }
    spannable.getSpans(0, spannable.length, URLSpan::class.java).forEach { span ->
        addRoutedLink(
            url = span.url,
            linkStyle = linkStyle,
            onLinkClick = onLinkClick,
            start = startIndex + spannable.getSpanStart(span),
            end = startIndex + spannable.getSpanEnd(span),
        )
    }
}

/**
 * Adds a link over `[start, end)`: a [LinkAnnotation.Url] opened by the `UriHandler` when
 * [onLinkClick] is null, otherwise a [LinkAnnotation.Clickable] tagged with [url] that calls
 * [onLinkClick] with it. Both carry [linkStyle] and the same link semantics for accessibility.
 */
private fun AnnotatedString.Builder.addRoutedLink(
    url: String,
    linkStyle: SpanStyle,
    onLinkClick: ((url: String) -> Unit)?,
    start: Int,
    end: Int,
) {
    val styles = TextLinkStyles(style = linkStyle)
    if (onLinkClick == null) {
        addLink(url = LinkAnnotation.Url(url = url, styles = styles), start = start, end = end)
    } else {
        addLink(
            clickable =
                LinkAnnotation.Clickable(
                    tag = url,
                    styles = styles,
                    linkInteractionListener = { onLinkClick(url) },
                ),
            start = start,
            end = end,
        )
    }
}

sealed interface Ellipsis {
    data object More : Ellipsis

    data object Less : Ellipsis
}

@Composable
internal fun buildExpandableText(
    text: String,
    ellipsis: Ellipsis?,
    ellipsisStyle: SpanStyle =
        SpanStyle(
            color = OctopusTheme.colorScheme.gray500,
            fontWeight = FontWeight.Medium,
        ),
    linkify: Boolean,
    markdown: Boolean = linkify,
    linkStyle: SpanStyle =
        SpanStyle(
            color = OctopusTheme.colorScheme.link,
            textDecoration = TextDecoration.Underline,
        ),
) = buildExpandableText(
    text = text,
    ellipsis = ellipsis,
    suffix =
        when (ellipsis) {
            Ellipsis.More -> stringResource(R.string.common_read_more)
            Ellipsis.Less -> stringResource(R.string.common_read_less)
            null -> ""
        },
    layoutDirection = LocalLayoutDirection.current,
    ellipsisStyle = ellipsisStyle,
    linkify = linkify,
    markdown = markdown,
    linkStyle = linkStyle,
)

/** Truncation happens before this builder; isolation never participates in the character budget. */
internal fun buildExpandableText(
    text: String,
    ellipsis: Ellipsis?,
    suffix: String,
    layoutDirection: LayoutDirection,
    ellipsisStyle: SpanStyle,
    linkify: Boolean,
    markdown: Boolean = linkify,
    linkStyle: SpanStyle,
): AnnotatedString {
    val content =
        buildAnnotatedString {
            appendRichText(text, linkify, markdown, linkStyle)
            if (ellipsis == Ellipsis.More) {
                withStyle(ellipsisStyle) { append("...") }
            }
        }
    if (ellipsis == null) return content

    val label = buildAnnotatedString { withStyle(ellipsisStyle) { append(" $suffix") } }
    return buildAnnotatedString {
        // FSI hides the content's first strong character from paragraph-direction detection.
        // Set the surrounding paragraph explicitly so the suffix follows the UI direction.
        withStyle(
            ParagraphStyle(
                textDirection = if (layoutDirection == LayoutDirection.Rtl) TextDirection.Rtl else TextDirection.Ltr,
            ),
        ) {
            var start = 0
            for (index in content.text.indices) {
                if (index < start) continue
                // Isolates end at paragraph separators. Reopen after each one, preserving
                // the existing rich-text annotations through AnnotatedString.subSequence.
                if (Character.getDirectionality(content.text[index]) == Character.DIRECTIONALITY_PARAGRAPH_SEPARATOR) {
                    append("\u2068")
                    append(content.subSequence(start, index))
                    append("\u2069")
                    // Keep CRLF together: inserting controls between the two would add a line.
                    val separatorEnd =
                        if (content.text[index] == '\r' && content.text.getOrNull(index + 1) == '\n') index + 2 else index + 1
                    append(content.subSequence(index, separatorEnd))
                    start = separatorEnd
                }
            }
            append("\u2068")
            append(content.subSequence(start, content.length))
            append("\u2069")
            append(label)
        }
    }
}
