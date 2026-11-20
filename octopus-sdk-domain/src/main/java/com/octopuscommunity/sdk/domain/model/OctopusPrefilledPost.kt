package com.octopuscommunity.sdk.domain.model

import android.net.Uri

/**
 * A payload carrying content that prefills the Octopus post editor when
 * the host opens the SDK via `navigateToOctopusCreatePost(...)`.
 *
 * The host supplies any combination of [text], [images], [topicId] and
 * [cta]; the user can freely edit text, images and group in the editor
 * before publishing. The CTA travels invisibly through the editor and
 * is attached to the published post — the user cannot view, edit, or
 * remove it.
 *
 * **Validation.** [text] and [images] are both optional: a payload with
 * only a [topicId] and/or [cta] (and empty content) opens the editor on
 * the preselected group with empty, user-editable fields. The constructor
 * validates whatever is provided (text length, image count, CTA label /
 * URL) and throws an [OctopusPrefilledPost.ValidationError] — or, above
 * [Post.Draft.MAX_IMAGE_COUNT] images, an
 * [OctopusPrefilledPost.TooManyImagesError] — on failure. Empty strings for
 * [text], empty values in [images] and blank values for [topicId] are
 * normalised before validation. Wrap the call in `runCatching { … }` if you
 * want a `Result<OctopusPrefilledPost>`.
 *
 * **Image bytes are not decoded here** because doing so requires a
 * [android.content.Context] the public API does not need to expose;
 * image-dimension errors (too small, ratio too large, decode failure)
 * surface inside the editor via the existing media-validation pipeline
 * — same shape as a user-picked image. iOS hosts get those errors at
 * construction time; Android hosts get them when the editor opens.
 * Document this in your integration guide if you migrate a
 * cross-platform host that relies on early failure.
 *
 * The Bridge Share feature behaves the same on every Octopus SDK platform.
 *
 * [images] comes last and has no default value, so every call resolves to exactly one constructor:
 * one that names `images` takes this one, any other the constructor without it. Java keeps the
 * `(text, image, topicId, cta)` constructor it had, `null` image included.
 *
 * @param image Deprecated single image. Cannot be set together with [images].
 * @throws IllegalArgumentException when both [image] and [images] are set.
 */
class OctopusPrefilledPost(
    text: String? = null,
    image: Uri? = null,
    topicId: String? = null,
    cta: OctopusPostCTA? = null,
    images: List<Uri>,
) {
    /** A prefilled post carrying at most the one [image]. */
    constructor(
        text: String? = null,
        image: Uri? = null,
        topicId: String? = null,
        cta: OctopusPostCTA? = null,
    ) : this(text = text, image = image, topicId = topicId, cta = cta, images = emptyList())

    /** An empty prefill, as the no-argument constructor it had when all its parameters had defaults. */
    constructor() : this(images = emptyList())

    /** Text the editor opens with. `null` when the host provided text-less prefill. */
    val text: String?

    /**
     * Local content URIs of the images the editor opens with, in display order, at most
     * [Post.Draft.MAX_IMAGE_COUNT]. Empty when the host provided image-less prefill. Empty URIs given
     * to the constructor are dropped. The host is responsible for image acquisition (download from
     * CDN, load from disk, etc.) — the SDK does not fetch remote URLs.
     *
     * **Permission persistence.** When a URI is a content URI granted
     * to the host via `ActivityResultContracts.PickMultipleVisualMedia` or a
     * similar contract, the read grant lives on the launching activity
     * by default and may be revoked after a configuration change or
     * process death. If the editor needs to decode the images after
     * recreation, the host should call
     * `contentResolver.takePersistableUriPermission(uri, FLAG_GRANT_READ_URI_PERMISSION)`
     * for each of them before navigating; otherwise the prefilled images may disappear
     * on rotation+recreate.
     */
    val images: List<Uri>

    /** The first of [images], or `null` when there is none. */
    @Deprecated("Use images.", ReplaceWith("images.firstOrNull()"), level = DeprecationLevel.WARNING)
    val image: Uri? get() = images.firstOrNull()

    /**
     * Identifier of the group the post should land in. If `null` or
     * inaccessible to the current user, the editor forces the user to
     * pick a group manually before publishing.
     */
    val topicId: String?

    /**
     * Optional call-to-action attached to the published post. Not
     * displayed in the editor (invisible passthrough).
     */
    val cta: OctopusPostCTA?

    init {
        val normalizedText = text?.takeIf { it.isNotEmpty() }
        require(image == null || images.isEmpty()) { "Set images only: image is deprecated." }
        val normalizedImages = (images.ifEmpty { listOfNotNull(image) }).filter { it.toString().isNotEmpty() }
        val normalizedTopicId = topicId?.takeIf { it.isNotBlank() }

        // Text and image are both optional: a Bridge Share can open the editor on a preselected
        // group (with an optional CTA) and empty, user-editable fields. Any provided text is still
        // validated below, and the editor re-validates the final content at publish time.
        if (normalizedText != null) {
            val range = Post.Draft.TEXT_LENGTH_RANGE
            if (normalizedText.length < range.first) {
                throw ValidationError.TextTooShort(range.first)
            }
            if (normalizedText.length > range.last) {
                throw ValidationError.TextTooLong(range.last)
            }
        }
        if (normalizedImages.size > Post.Draft.MAX_IMAGE_COUNT) {
            throw TooManyImagesError(max = Post.Draft.MAX_IMAGE_COUNT, count = normalizedImages.size)
        }
        if (cta != null) {
            if (cta.label.isBlank()) throw ValidationError.CtaLabelEmpty
            if (cta.url.toString().isBlank()) throw ValidationError.CtaUrlEmpty
        }

        this.text = normalizedText
        this.images = normalizedImages
        this.topicId = normalizedTopicId
        this.cta = cta
    }

    /**
     * Validation failures thrown by the [OctopusPrefilledPost]
     * constructor. Subclasses `IllegalArgumentException` so a caller
     * that doesn't need to discriminate by case can `catch
     * (e: IllegalArgumentException)` (or rely on standard
     * `require()`-style propagation); the sealed hierarchy lets a
     * caller `when` over the specific reason if it wants.
     *
     * Too many images is reported by [TooManyImagesError] instead, a separate type: a new
     * subclass here would break the exhaustive `when` callers wrote over these reasons.
     */
    sealed class ValidationError(
        message: String,
    ) : IllegalArgumentException(message) {
        /**
         * Retained for backward compatibility. `text` and `image` are both
         * optional now, so an empty payload no longer throws — this is kept
         * only so existing `when` branches stay exhaustive.
         */
        object ContentEmpty : ValidationError(
            "OctopusPrefilledPost.ValidationError.ContentEmpty: " +
                "at least one of `text` or `image` must be provided.",
        )

        /**
         * The text length is below the editor's minimum publish length. Since that minimum is one
         * character ([Post.Draft.TEXT_LENGTH_RANGE]) and empty text is normalised to `null`, this is
         * no longer thrown; it is kept so existing `when` branches stay exhaustive.
         */
        data class TextTooShort(
            val min: Int,
        ) : ValidationError(
                "OctopusPrefilledPost.ValidationError.TextTooShort: " +
                    "prefilled text is shorter than $min characters.",
            )

        /**
         * The text length exceeds the editor's maximum publish length.
         */
        data class TextTooLong(
            val max: Int,
        ) : ValidationError(
                "OctopusPrefilledPost.ValidationError.TextTooLong: " +
                    "prefilled text exceeds $max characters.",
            )

        /**
         * The CTA's `label` is empty or whitespace-only.
         */
        object CtaLabelEmpty : ValidationError(
            "OctopusPrefilledPost.ValidationError.CtaLabelEmpty: " +
                "CTA `label` must not be empty or blank.",
        )

        /**
         * The CTA's `url` has an empty / blank string representation.
         */
        object CtaUrlEmpty : ValidationError(
            "OctopusPrefilledPost.ValidationError.CtaUrlEmpty: " +
                "CTA `url` must have a non-empty string representation.",
        )
    }

    /**
     * Thrown by the [OctopusPrefilledPost] constructor when more than [max] images are provided
     * (empty URIs are not counted).
     *
     * It subclasses `IllegalArgumentException` like [ValidationError], so a generic `catch (e:
     * IllegalArgumentException)` still covers it, but it is not a [ValidationError]: adding a
     * subclass to that sealed hierarchy would break the exhaustive `when` callers wrote over it.
     *
     * @property max The maximum number of images a post carries.
     * @property count The number of (non-empty) images that were provided.
     */
    data class TooManyImagesError(
        val max: Int,
        val count: Int,
    ) : IllegalArgumentException(
            "OctopusPrefilledPost.TooManyImagesError: $count images provided, at most $max are accepted.",
        )
}
