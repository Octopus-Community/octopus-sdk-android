package com.octopuscommunity.sdk.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.utils.appendRichText
import com.octopuscommunity.sdk.ui.utils.normalizeLineSeparators

/**
 * Builds the [AnnotatedString] of a markdown text written outside the SDK (links, bold, italic,
 * line breaks kept; headings and lists left as typed). Line separators are normalised to `\n` like
 * post bodies, since the text comes from a dashboard and may carry `\r\n`. Every `[label](url)`
 * link keeps the URL written in the markdown and hands it to [onLinkClick] when tapped. Bare URLs
 * stay plain text: only explicit links are links.
 */
internal fun buildRoutedMarkdown(
    text: String,
    linkStyle: SpanStyle,
    onLinkClick: (url: String) -> Unit,
): AnnotatedString =
    buildAnnotatedString {
        appendRichText(
            text = text.normalizeLineSeparators(),
            linkify = false,
            markdown = true,
            linkStyle = linkStyle,
            onLinkClick = onLinkClick,
        )
    }

/**
 * Displays a markdown text written outside the SDK (see [buildRoutedMarkdown]), links in the
 * theme's link color. A tap on a link calls [onLinkClick] with the link's URL — callers route it to
 * the view model's `navigateToUrl`, so the host's `onNavigateToUrl` decides how it opens.
 */
@Composable
internal fun OctopusMarkdownText(
    text: String,
    onLinkClick: (url: String) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = OctopusTheme.colorScheme.gray500,
    textAlign: TextAlign? = null,
    style: TextStyle = OctopusTheme.typography.body2,
) {
    val linkColor = OctopusTheme.colorScheme.link
    val currentOnLinkClick by rememberUpdatedState(onLinkClick)
    val annotatedText =
        remember(text, linkColor) {
            buildRoutedMarkdown(
                text = text,
                linkStyle = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                onLinkClick = { url -> currentOnLinkClick(url) },
            )
        }
    Text(
        modifier = modifier,
        text = annotatedText,
        color = color,
        textAlign = textAlign,
        style = style,
    )
}
