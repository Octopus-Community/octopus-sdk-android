package com.octopuscommunity.sdk.domain.model

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Size
import kotlin.time.Duration

/** Former image type, replaced by [Resource]. */
@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "Use Resource instead.",
    replaceWith =
        ReplaceWith(
            "Resource",
            "com.octopuscommunity.sdk.domain.model",
        ),
)
class Image

/** A media attached to an item: an [Image] or a [Video]. */
sealed interface Media {
    /** An image, local or remote. */
    data class Image(
        override val source: Resource,
        override val dimensions: Size? = null,
        override val fileSize: Long? = null,
        val name: String = "",
        val contentType: String = "",
    ) : Media {
        /**
         * Creates an image from the local [localUri], reading its name, dimensions, size and content type
         * through [context].
         */
        constructor(context: Context, localUri: Uri) : this(
            source = Resource.Local(uri = localUri),
            name =
                localUri.takeIf { it.scheme == "content" }?.let { uri ->
                    context.contentResolver
                        .query(
                            uri,
                            arrayOf(OpenableColumns.DISPLAY_NAME),
                            null,
                            null,
                            null,
                        )?.use { cursor ->
                            cursor.moveToFirst().takeIf { it }?.let {
                                cursor
                                    .getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                    .takeIf { it != -1 }
                                    ?.let {
                                        cursor.getString(it)
                                    }
                            }
                        }
                } ?: localUri.lastPathSegment ?: "image_${System.currentTimeMillis()}",
            dimensions = localUri.getDimensions(context),
            fileSize =
                localUri.takeIf { it.scheme == "content" }?.let { uri ->
                    context.contentResolver
                        .query(
                            uri,
                            arrayOf(OpenableColumns.SIZE),
                            null,
                            null,
                            null,
                        )?.use { cursor ->
                            cursor.moveToFirst().takeIf { it }?.let {
                                cursor.getColumnIndex(OpenableColumns.SIZE).takeIf { it != -1 }?.let {
                                    cursor.getLong(it)
                                }
                            }
                        }
                },
            contentType = context.contentResolver.getType(localUri) ?: "image/*",
        )

        companion object {
            /** Maximum width and height of an uploaded image, in pixels. */
            const val UPLOAD_MAX_DIMENSION = 1500

            /** Compression quality of an uploaded image, from 0 to 100. */
            const val UPLOAD_QUALITY = 80
        }
    }

    /** A video, with its [id], optional [thumbnailUrl] and [duration]. */
    data class Video(
        override val source: Resource,
        override val dimensions: Size? = null,
        override val fileSize: Long? = null,
        val id: String,
        val thumbnailUrl: String? = null,
        val duration: Duration? = null,
    ) : Media

    /** Where the media is: a local uri or a remote url. */
    val source: Resource

    /** Dimensions of the media in pixels, when known. */
    val dimensions: Size? get() = null

    /** Size of the media file in bytes, when known. */
    val fileSize: Long? get() = null
}

/** Decodes the dimensions of the image at this uri without loading it in memory. */
fun Uri.getDimensions(context: Context) =
    context.contentResolver.openInputStream(this).use { inputStream ->
        val options =
            BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
        BitmapFactory.decodeStream(inputStream, null, options)
        Size(options.outWidth, options.outHeight)
    }
