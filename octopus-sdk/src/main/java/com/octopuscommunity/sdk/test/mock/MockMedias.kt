package com.octopuscommunity.sdk.test.mock

import android.util.Size
import com.octopuscommunity.sdk.domain.model.Media
import com.octopuscommunity.sdk.domain.model.Resource
import java.util.UUID
import kotlin.time.Duration

/** Mock images and videos. */
object MockMedias {
    private const val defaultVideoUrl = "https://storage.googleapis.com/exoplayer-test-media-1/mp4/android-screens-10s.mp4"
    private val imageContentTypes = listOf("image/jpeg", "image/png")
    private val videoContentTypes = listOf("video/mp4", "video/quicktime")
    private val contentTypes = imageContentTypes + videoContentTypes

    val default =
        createImage(
            url = "https://play-lh.googleusercontent.com/H6umM_74J78u2KrdnnnDwwLctekjZg5kghkx6LSSeQvt5plFI3E98bjWaLZm8lds0ixg=w480-h960-rw",
        )

    val defaultImage =
        createImage(
            url = "https://play-lh.googleusercontent.com/H6umM_74J78u2KrdnnnDwwLctekjZg5kghkx6LSSeQvt5plFI3E98bjWaLZm8lds0ixg=w480-h960-rw",
        )

    /** A stable 1280×720 Media3 sample video with a fixed ID and unknown file size. */
    val defaultVideo =
        createVideo(
            id = "fake_video",
            url = defaultVideoUrl,
            dimensions = Size(1280, 720),
            size = null,
        )

    fun createImage(
        url: String = "https://example.com/media_${UUID.randomUUID()}.${
            listOf("jpg", "mp4").random()
        }",
        dimensions: Size? = Size(1920, 1024),
        size: Long? = 50 * 1024 * 1024, // 50 MB
        name: String = "Sample Media ${('A'..'Z').random()}${('A'..'Z').random()}",
        contentType: String = imageContentTypes.random(),
    ) = Media.Image(
        source = Resource.Remote(url = url),
        dimensions = dimensions,
        fileSize = size,
        name = name,
        contentType = contentType,
    )

    /**
     * Creates a mock video, defaulting to a fixed 1280×720 Media3 sample URL.
     *
     * @param id Video identifier, randomly generated when omitted.
     * @param url Remote video URL; the default is `android-screens-10s.mp4`.
     * @param dimensions Pixel dimensions, inferred for the default video and Big Buck Bunny sample;
     * unknown URLs default to `null`. Pass explicit dimensions for other known videos.
     * @param size File size in bytes, defaulting to a synthetic 50 MiB rather than the remote size.
     * @param thumbnailUrl Optional remote thumbnail URL.
     * @param duration Optional video duration.
     */
    fun createVideo(
        id: String = "video_${UUID.randomUUID()}",
        url: String = defaultVideoUrl,
        dimensions: Size? =
            when (url) {
                defaultVideoUrl -> Size(1280, 720)
                "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4" -> Size(320, 180)
                else -> null
            },
        size: Long? = 50 * 1024 * 1024, // 50 MB
        thumbnailUrl: String? = null,
        duration: Duration? = null,
    ) = Media.Video(
        id = id,
        source = Resource.Remote(url = url),
        dimensions = dimensions,
        fileSize = size,
        thumbnailUrl = thumbnailUrl,
        duration = duration,
    )
}
