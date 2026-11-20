package com.octopuscommunity.sdk.ui.profile.components

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.components.MenuItem
import com.octopuscommunity.sdk.ui.components.OctopusMenuSheet
import com.octopuscommunity.sdk.ui.utils.ImagePicker
import com.octopuscommunity.sdk.ui.utils.rememberImagePicker

/**
 * Edits a profile picture: picks one directly when there is none, otherwise offers to replace or
 * delete it. Shared by the profile editor and the nickname choice screen.
 */
@Stable
internal class ProfilePictureEditor(
    private val picker: () -> ImagePicker,
    private val hasPicture: () -> Boolean,
) {
    var showsMenu by mutableStateOf(false)
        private set

    fun edit() {
        if (hasPicture()) showsMenu = true else picker().launch()
    }

    internal fun replace() {
        showsMenu = false
        picker().launch()
    }

    internal fun dismissMenu() {
        showsMenu = false
    }
}

/**
 * [rememberImagePicker] returns a new [ImagePicker] on every composition, so the editor is kept
 * stable and reads the latest picker and [hasPicture] through [rememberUpdatedState].
 */
@Composable
internal fun rememberProfilePictureEditor(
    hasPicture: Boolean,
    onPicked: (Uri) -> Unit,
): ProfilePictureEditor {
    val currentHasPicture by rememberUpdatedState(hasPicture)
    val currentPicker by rememberUpdatedState(rememberImagePicker(onImageSelected = onPicked))
    return remember { ProfilePictureEditor(picker = { currentPicker }, hasPicture = { currentHasPicture }) }
}

/** The replace / delete menu of [editor], shown while it is open. */
@Composable
internal fun ProfilePictureEditMenu(
    editor: ProfilePictureEditor,
    onDeleted: () -> Unit,
) {
    if (!editor.showsMenu) return
    OctopusMenuSheet(
        items =
            listOf(
                MenuItem(
                    icon = OctopusTheme.icons.profile.editPicture(),
                    title = stringResource(R.string.profile_edit_picture_change),
                    onClick = editor::replace,
                ),
                MenuItem(
                    icon = OctopusTheme.icons.content.delete(),
                    title = stringResource(R.string.profile_edit_picture_delete),
                    isDestructive = true,
                    onClick = {
                        editor.dismissMenu()
                        onDeleted()
                    },
                ),
            ),
        onDismissRequest = editor::dismissMenu,
    )
}
