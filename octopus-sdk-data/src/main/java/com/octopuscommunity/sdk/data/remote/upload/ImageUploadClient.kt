package com.octopuscommunity.sdk.data.remote.upload

import com.google.protobuf.ByteString
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Sends the bytes of a photo to the signed URL the backend handed out for it. */
internal fun interface ImageUploadClient {
    /**
     * Puts [body] at [url], typed [contentType]. The bytes are streamed as they are, never copied:
     * up to three photos are in flight at once.
     *
     * @throws ImageUploadHttpException when the storage answers outside 2xx.
     * @throws IOException when the storage cannot be reached.
     */
    suspend fun put(
        url: String,
        body: ByteString,
        contentType: String,
    )
}

/** The storage refused a photo: it answered [statusCode], outside 2xx. */
internal class ImageUploadHttpException(
    val statusCode: Int,
) : Exception("Image upload refused with HTTP $statusCode")

/**
 * [ImageUploadClient] over a plain HTTPS `PUT`, with no token nor auth header: the signature is in
 * the URL. Any 2xx answer means the photo arrived.
 */
internal class HttpImageUploadClient(
    private val timeoutMillis: Int = TIMEOUT_MILLIS,
) : ImageUploadClient {
    override suspend fun put(
        url: String,
        body: ByteString,
        contentType: String,
    ) {
        val connection = URL(url).openConnection() as HttpURLConnection
        coroutineScope {
            // The blocking I/O below does not see a cancellation: this watcher closes the connection
            // as soon as the upload is cancelled, which aborts the write or the wait for an answer.
            val abortOnCancel =
                launch(start = CoroutineStart.UNDISPATCHED) {
                    try {
                        awaitCancellation()
                    } finally {
                        connection.disconnect()
                    }
                }
            try {
                withContext(Dispatchers.IO) {
                    connection.requestMethod = "PUT"
                    connection.doOutput = true
                    connection.connectTimeout = timeoutMillis
                    connection.readTimeout = timeoutMillis
                    connection.setRequestProperty("Content-Type", contentType)
                    connection.setFixedLengthStreamingMode(body.size())
                    connection.outputStream.use { body.writeTo(it) }
                    val statusCode = connection.responseCode
                    if (statusCode !in 200..299) throw ImageUploadHttpException(statusCode)
                }
            } catch (exception: IOException) {
                // A connection the watcher closed ends the upload as cancelled, not as a network error.
                ensureActive()
                throw exception
            } finally {
                abortOnCancel.cancel()
            }
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 60_000
    }
}
