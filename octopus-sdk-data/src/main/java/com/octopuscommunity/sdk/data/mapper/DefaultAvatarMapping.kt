package com.octopuscommunity.sdk.data.mapper

import com.octopuscommunity.GrpcModelProto
import com.octopuscommunity.sdk.domain.model.DefaultAvatar
import com.octopuscommunity.sdk.domain.model.Resource
import java.net.URI
import java.util.Locale

/**
 * A community default avatar as stored: the server's raw strings, read back by [defaultAvatarOf]. Either the
 * two colors or the image URL is set, never both, and only values that passed validation are written.
 */
internal data class DefaultAvatarColumns(
    val backgroundHex: String? = null,
    val initialsHex: String? = null,
    val imageUrl: String? = null,
)

/**
 * The columns to store for a proto `DefaultAvatar`, or `null` to keep today's rendering: field absent,
 * `oneof` empty or of a kind this SDK does not know (protobuf reports a newer kind as not set), a color that
 * is not `#RRGGBB`, or an image URL that is not an absolute http(s) URL.
 */
internal fun protoDefaultAvatarColumns(
    hasDefaultAvatar: Boolean,
    defaultAvatar: GrpcModelProto.DefaultAvatar,
): DefaultAvatarColumns? =
    if (!hasDefaultAvatar) {
        null
    } else {
        when (defaultAvatar.kindCase) {
            GrpcModelProto.DefaultAvatar.KindCase.COLOR -> defaultAvatar.color.toColumnsOrNull()

            GrpcModelProto.DefaultAvatar.KindCase.IMAGEURL ->
                defaultAvatar.imageUrl
                    .takeIf(::isValidDefaultAvatarImageUrl)
                    ?.let { DefaultAvatarColumns(imageUrl = it) }

            GrpcModelProto.DefaultAvatar.KindCase.KIND_NOT_SET, null -> null
        }
    }

/**
 * The picture URL to store next to [defaultAvatar]. The server sends a default avatar only to members without
 * a picture; their picture URL then holds at most a copy of the default image, made for SDKs that cannot read
 * the default avatar, which would otherwise pass for a real picture (removal offered, edit icon instead of
 * add). So it is dropped whenever a readable default avatar is stored, and kept otherwise: an unreadable
 * default avatar falls back to what those SDKs show.
 */
internal fun normalizedPictureUrl(
    pictureUrl: String?,
    defaultAvatar: DefaultAvatarColumns?,
): String? = pictureUrl?.ifEmpty { null }?.takeIf { defaultAvatar == null }

/** Reads stored columns back; `null` when they hold no valid default avatar. */
internal fun defaultAvatarOf(
    backgroundHex: String?,
    initialsHex: String?,
    imageUrl: String?,
): DefaultAvatar? {
    val background = parseOpaqueHexColor(backgroundHex)
    val initials = parseOpaqueHexColor(initialsHex)
    return when {
        background != null && initials != null -> DefaultAvatar.Color(background = background, initials = initials)
        imageUrl != null && isValidDefaultAvatarImageUrl(imageUrl) -> DefaultAvatar.Image(image = Resource.Remote(url = imageUrl))
        else -> null
    }
}

/** The columns that store this default avatar; all `null` when there is none. */
internal fun DefaultAvatar?.toColumns(): DefaultAvatarColumns =
    when (this) {
        is DefaultAvatar.Color -> DefaultAvatarColumns(backgroundHex = background.toOpaqueHex(), initialsHex = initials.toOpaqueHex())
        is DefaultAvatar.Image -> DefaultAvatarColumns(imageUrl = image.url)
        null -> DefaultAvatarColumns()
    }

/** `#RRGGBB`, in either case, as an opaque ARGB color int; anything else is `null`. */
internal fun parseOpaqueHexColor(hex: String?): Int? =
    hex
        ?.takeIf { OPAQUE_HEX_COLOR.matches(it) }
        ?.let { (0xFF000000L or it.substring(1).toLong(radix = 16)).toInt() }

/** Whether [url] is an absolute http(s) URL with a host. */
internal fun isValidDefaultAvatarImageUrl(url: String): Boolean {
    val uri = runCatching { URI(url) }.getOrNull() ?: return false
    return when (uri.scheme?.lowercase(Locale.ROOT)) {
        "http", "https" -> !uri.host.isNullOrEmpty()
        else -> false
    }
}

private fun GrpcModelProto.DefaultAvatarColor.toColumnsOrNull(): DefaultAvatarColumns? =
    if (parseOpaqueHexColor(backgroundHex) != null && parseOpaqueHexColor(initialsHex) != null) {
        DefaultAvatarColumns(backgroundHex = backgroundHex, initialsHex = initialsHex)
    } else {
        null
    }

private fun Int.toOpaqueHex(): String = String.format(Locale.ROOT, "#%06X", this and 0xFFFFFF)

private val OPAQUE_HEX_COLOR = Regex("^#[0-9A-Fa-f]{6}$")
