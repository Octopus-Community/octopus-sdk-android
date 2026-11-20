package com.octopuscommunity.sdk.ui.posts

import androidx.annotation.StringRes
import com.octopuscommunity.sdk.ui.R

/**
 * Template of a published post's TalkBack summary. The media picks it: an image reads "… and
 * attached an image", two images or more "… and attached N images", a video "… and attached a
 * video", no media the text-only template. The one exception is images that come with no text and
 * no poll: they read "… shared an image" (or "… shared N images"), a template that takes no quoted
 * text. A poll never picks the template, because its options are
 * appended to the text by [accessibilitySummaryText]. So a post carrying an image and a poll uses
 * the text-and-image template, with or without text, and the poll options are read before the image
 * is mentioned.
 *
 * @param hasText whether the displayed text is not blank.
 */
@Suppress("DEPRECATION")
internal fun accessibilitySummaryTemplate(
    media: PostMedia?,
    hasText: Boolean,
    hasPoll: Boolean,
): AccessibilitySummaryTemplate =
    when (media) {
        is PostMedia.Image -> accessibilitySummaryTemplate(PostMedia.Images(listOf(media.media)), hasText, hasPoll)
        is PostMedia.Images -> {
            val count = media.medias.size
            when {
                count >= 2 && (hasText || hasPoll) ->
                    AccessibilitySummaryTemplate(
                        R.string.accessibility_post_summary_text_and_images_author,
                        imageCount = count,
                    )
                count >= 2 ->
                    AccessibilitySummaryTemplate(
                        R.string.accessibility_post_summary_images_only_author,
                        quotesText = false,
                        imageCount = count,
                    )
                hasText || hasPoll ->
                    AccessibilitySummaryTemplate(R.string.accessibility_post_summary_text_and_image_author)
                else ->
                    AccessibilitySummaryTemplate(R.string.accessibility_post_summary_image_only_author, quotesText = false)
            }
        }
        is PostMedia.Video -> AccessibilitySummaryTemplate(R.string.accessibility_post_summary_text_and_video_author)
        null -> AccessibilitySummaryTemplate(R.string.accessibility_post_summary_text_only_author)
    }

/**
 * A TalkBack summary template, [resId], and whether it quotes the post's text: when [quotesText] it
 * takes the text from [accessibilitySummaryText] after the author, date and group. When
 * [imageCount] is set, the number of images comes last.
 */
internal data class AccessibilitySummaryTemplate(
    @StringRes val resId: Int,
    val quotesText: Boolean = true,
    val imageCount: Int? = null,
)

/**
 * Text read in a published post's TalkBack summary: the post [text], followed by the options of
 * [poll] when there is one, in the displayed language, the option the current user voted for
 * carrying [selectedSuffix]. Without text, the options are read alone.
 */
internal fun accessibilitySummaryText(
    text: String,
    poll: PostPoll?,
    translated: Boolean,
    selectedSuffix: String,
): String {
    val textContent = text.trim()
    if (poll == null) return textContent
    val options =
        poll.poll.answers.joinToString(", ") { answer ->
            val suffix = if (answer == poll.currentUserVote) selectedSuffix else ""
            "${answer.text.getText(translated = translated)}$suffix"
        }
    if (textContent.isEmpty()) return options
    return "$textContent, $options"
}
