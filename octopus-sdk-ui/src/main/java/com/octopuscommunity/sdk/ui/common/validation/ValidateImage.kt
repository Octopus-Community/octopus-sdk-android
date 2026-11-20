package com.octopuscommunity.sdk.ui.common.validation

import android.util.Size
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.UiText
import com.octopuscommunity.sdk.ui.common.validation.PictureValidationError.RatioTooBig
import com.octopuscommunity.sdk.ui.common.validation.PictureValidationError.SideTooSmall

sealed class PictureValidationError(
    override val message: UiText?,
) : ValidationError(message) {
    data class SideTooSmall(
        val maxRatioString: String,
        val minSize: Int,
    ) : PictureValidationError(
            message =
                UiText.StringResource(
                    R.string.picture_selection_error_max_ratio_min_size_formal,
                    listOf(maxRatioString, minSize),
                ),
        )

    data class RatioTooBig(
        val maxRatioString: String,
        val minSize: Int,
    ) : PictureValidationError(
            message =
                UiText.StringResource(
                    R.string.picture_selection_error_max_ratio_min_size_formal,
                    listOf(maxRatioString, minSize),
                ),
        )
}

class ValidateImage(
    private val minSize: Int = 50,
    private val maxRatio: Float = 32f / 9f,
    private val maxRatioText: String = "32:9",
) {
    operator fun invoke(imageSize: Size): PictureValidationError? {
        val smallestSize = minOf(imageSize.width, imageSize.height)
        val biggestSize = maxOf(imageSize.width, imageSize.height)

        return when {
            smallestSize < this.minSize -> SideTooSmall(maxRatioText, minSize)
            biggestSize / smallestSize > this.maxRatio -> RatioTooBig(maxRatioText, minSize)
            else -> null
        }
    }
}
