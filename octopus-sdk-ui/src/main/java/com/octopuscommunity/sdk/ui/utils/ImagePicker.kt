package com.octopuscommunity.sdk.ui.utils

import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

fun interface ImagePicker {
    fun launch()
}

@Composable
internal fun rememberImagePicker(onImageSelected: (imageUri: Uri) -> Unit): ImagePicker {
    if (!hasImagePickerOwner()) return ImagePicker {}
    val onImageResult = { uri: Uri? ->
        if (uri != null) {
            onImageSelected(uri)
        }
    }
    val pickImage: () -> Unit
    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
            onResult = onImageResult,
        )
    pickImage = {
        launcher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }
    return ImagePicker { pickImage() }
}

/** Which picker opens for the image slots a draft has left. */
internal sealed interface ImagePickerMode {
    /** No slot left: nothing opens. */
    data object None : ImagePickerMode

    /** One slot left: the single picker, since the multiple one refuses a limit of 1. */
    data object Single : ImagePickerMode

    /** At least two slots left: the multiple picker, limited to [maxItems]. */
    data class Multiple(
        val maxItems: Int,
    ) : ImagePickerMode
}

/** The picker for [remainingSlots] image slots. */
internal fun imagePickerMode(remainingSlots: Int): ImagePickerMode =
    when {
        remainingSlots <= 0 -> ImagePickerMode.None
        remainingSlots == 1 -> ImagePickerMode.Single
        else -> ImagePickerMode.Multiple(maxItems = remainingSlots)
    }

/**
 * A picker of up to [remainingSlots] images, handing the picked ones to [onImagesSelected] in the
 * order they were picked. Without the system photo picker the selection falls back to a document
 * picker that ignores the limit, so the caller still caps what it keeps.
 */
@Composable
internal fun rememberImagesPicker(
    remainingSlots: Int,
    onImagesSelected: (imageUris: List<Uri>) -> Unit,
): ImagePicker {
    if (!hasImagePickerOwner()) return ImagePicker {}
    val mode = imagePickerMode(remainingSlots)
    val single =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) onImagesSelected(listOf(uri))
        }
    // `PickMultipleVisualMedia` requires a limit above 1; keep a valid one while a single slot is left.
    val maxItems = (mode as? ImagePickerMode.Multiple)?.maxItems ?: 2
    val multipleContract = remember(maxItems) { ActivityResultContracts.PickMultipleVisualMedia(maxItems) }
    val multiple =
        rememberLauncherForActivityResult(contract = multipleContract) { uris ->
            if (uris.isNotEmpty()) onImagesSelected(uris)
        }
    return ImagePicker {
        val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        when (mode) {
            ImagePickerMode.None -> Unit
            ImagePickerMode.Single -> single.launch(request)
            is ImagePickerMode.Multiple -> multiple.launch(request)
        }
    }
}

@Composable
private fun hasImagePickerOwner(): Boolean {
    val present = LocalActivityResultRegistryOwner.current != null
    androidx.compose.runtime.LaunchedEffect(present) {
        if (!present) {
            IntegrationDiagnostics.reportOnce(
                "ActivityResultRegistryOwner",
                "Image picking is disabled: the host must provide an ActivityResultRegistryOwner (for example ComponentActivity).",
            )
        }
    }
    return present
}
