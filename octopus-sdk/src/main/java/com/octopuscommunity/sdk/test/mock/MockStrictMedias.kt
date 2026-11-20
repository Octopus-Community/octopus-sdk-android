package com.octopuscommunity.sdk.test.mock

import com.octopuscommunity.sdk.domain.model.Medias
import kotlin.random.Random

/** Random [Medias] holding either images or one video, never both. */
object MockStrictMedias {
    /** Medias holding [imageCount] images, random by default. */
    fun createImage(imageCount: Int = Random.nextInt(0, 5)) =
        Medias(
            images = List(imageCount) { MockMedias.createImage(contentType = "image/jpeg") },
        )

    /** Medias holding one video. */
    fun createVideo() =
        Medias(
            video = MockMedias.createVideo(),
        )

    /** Medias holding, at random, images or one video. */
    fun create() =
        if (Random.nextBoolean()) {
            createImage()
        } else {
            createVideo()
        }
}
