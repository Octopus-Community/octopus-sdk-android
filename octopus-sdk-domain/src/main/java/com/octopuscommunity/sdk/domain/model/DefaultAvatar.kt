package com.octopuscommunity.sdk.domain.model

/**
 * What to draw for a member who has no picture of their own, as chosen by their community.
 *
 * Read from [MinimalProfile.defaultAvatar]. `null` there means the community leaves it to the SDK: the
 * member's initials on a color computed from their nickname. A member with a [MinimalProfile.picture] never
 * has one. The same value applies to light and dark mode.
 *
 * New kinds may be added as communities get new default avatar styles: keep an `else` branch when you `when`
 * over it.
 */
sealed interface DefaultAvatar {
    /**
     * The member's initials, drawn in [initials] on [background].
     *
     * @property background Background color, as an opaque ARGB color int (`0xFFRRGGBB`), usable as is with
     *   `androidx.compose.ui.graphics.Color(Int)` or `android.graphics.Color`.
     * @property initials Color of the initials, in the same format: black or white, picked for contrast with
     *   [background].
     */
    data class Color(
        val background: Int,
        val initials: Int,
    ) : DefaultAvatar

    /**
     * An image, drawn like a profile picture (square, cropped to the avatar's shape).
     *
     * @property image The image. A different image always comes with a different URL, so it can be cached by URL.
     */
    data class Image(
        val image: Resource.Remote,
    ) : DefaultAvatar
}
