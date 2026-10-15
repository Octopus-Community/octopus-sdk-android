package com.octopuscommunity.sdk.sample.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast

/** Copies the raw ID without exposing it in clipboard previews, feedback or logs. */
internal fun copyDebugUserId(
    context: Context,
    userId: String,
) {
    val clip = ClipData.newPlainText("User ID", userId)
    // ClipDescription extras exist from API 24. The sensitive flag uses its wire name so it
    // also works below API 33, where ClipDescription.EXTRA_IS_SENSITIVE was introduced.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        clip.description.extras = PersistableBundle().apply { putBoolean("android.content.extra.IS_SENSITIVE", true) }
    }
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(clip)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, "User ID copied", Toast.LENGTH_SHORT).show()
    }
}
