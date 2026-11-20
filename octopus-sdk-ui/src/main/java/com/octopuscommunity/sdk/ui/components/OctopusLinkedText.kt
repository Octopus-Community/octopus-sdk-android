package com.octopuscommunity.sdk.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import com.octopuscommunity.sdk.ui.OctopusTheme

internal data class TextLink(val tag: String, val text: String, val onClick: () -> Unit)

@Composable
internal fun OctopusLinkedText(
    text: String,
    links: List<TextLink>,
    modifier: Modifier = Modifier,
    color: Color = OctopusTheme.colorScheme.gray500,
    textAlign: TextAlign? = null,
    style: TextStyle = OctopusTheme.typography.body2
) {
    Text(
        modifier = modifier,
        text = buildAnnotatedString {
            var currentIndex = 0

            links.forEach { textLink ->
                val startIndex = text.indexOf("[${textLink.tag}]", currentIndex)
                if (startIndex != -1) {
                    append(text.substring(currentIndex, startIndex))

                    withLink(
                        link = LinkAnnotation.Clickable(
                            tag = textLink.tag,
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = OctopusTheme.colorScheme.link,
                                    textDecoration = TextDecoration.Underline
                                )
                            ),
                            linkInteractionListener = {
                                textLink.onClick()
                            }
                        )
                    ) {
                        append(textLink.text)
                    }

                    currentIndex = startIndex + textLink.tag.length + 2
                }
            }

            append(text.substring(currentIndex))
        },
        color = color,
        textAlign = textAlign,
        style = style
    )
}

@Composable
internal fun OctopusLinkedText2(
    text: String,
    actions: List<() -> Unit>,
    modifier: Modifier = Modifier,
    color: Color = OctopusTheme.colorScheme.gray500,
    textAlign: TextAlign? = null,
    style: TextStyle = OctopusTheme.typography.body2
) {
    Text(
        modifier = modifier,
        text = buildAnnotatedString {
            // Check if text contains markdown - style links [text](url)
            val markdownLinkRegex = Regex("\\[([^]]+)]\\(([^)]+)\\)")

            if (text.contains(markdownLinkRegex)) {
                // Handle markdown-style links
                val matches = markdownLinkRegex.findAll(text).toList()
                var offset = 0

                matches.forEachIndexed { index, match ->
                    val linkText = match.groupValues[1]
                    val linkTag = match.groupValues[2] // Use as unique tag for the link

                    // Add text before the link
                    append(text.substring(offset, match.range.first))

                    // Add the clickable link
                    withLink(
                        link = LinkAnnotation.Clickable(
                            tag = linkTag,
                            styles = TextLinkStyles(
                                style = SpanStyle(
                                    color = OctopusTheme.colorScheme.link,
                                    textDecoration = TextDecoration.Underline
                                )
                            ),
                            linkInteractionListener = {
                                // Execute the corresponding action from the list
                                if (index < actions.size) {
                                    actions[index]()
                                }
                            }
                        )
                    ) {
                        append(linkText)
                    }

                    offset = match.range.last + 1
                }

                // Add remaining text
                append(text.substring(offset))
            } else {
                append(text)
            }
        },
        color = color,
        textAlign = textAlign,
        style = style
    )
}

@Preview(showBackground = true)
@Composable
private fun OctopusLinkedTextPreview() {
    OctopusTheme {
        OctopusLinkedText(
            text = "If you have any doubts or questions, please refer to the [content_policy].",
            links = listOf(
                TextLink(
                    tag = "content_policy",
                    text = "Community Content Policy",
                    onClick = {}
                )
            )
        )
    }
}