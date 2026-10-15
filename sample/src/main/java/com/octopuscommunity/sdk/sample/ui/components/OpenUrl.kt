package com.octopuscommunity.sdk.sample.ui.components

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Opens [url] with whatever app claims it; when nothing on the device does — the bare AOSP emulator
 * images the QA pipeline runs on ship with neither a browser nor a mail client — copies it to the
 * clipboard and says so, instead of crashing the screen that was about to file a report.
 *
 * Not browser-only: `mailto:` links from the Community tab come through here too, which is why the
 * message below names no particular kind of app.
 */
fun openUrlOrCopy(
    context: Context,
    url: String,
    clipLabel: String,
) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: ActivityNotFoundException) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(clipLabel, url))
        Toast
            .makeText(
                context,
                "No app on this device can open that link — copied to the clipboard",
                Toast.LENGTH_LONG,
            ).show()
    }
}
