package com.octopuscommunity.sdk.sample.ui.settings

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import com.octopuscommunity.sdk.domain.model.CurrentUserProfile
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository.ConnectionState
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [23, 24, 34], application = Application::class)
class DebugInfoClipboardTest {
    @Test
    fun `guest and signed-in IDs copy raw text with sensitive previews and version-appropriate feedback`() {
        val context: Context = RuntimeEnvironment.getApplication()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        listOf(true, false).forEach { isGuest ->
            ShadowToast.reset()
            val profile = CurrentUserProfile(userId = "octopus-user", isGuest = isGuest, nickname = "Test user")
            val info = debugConnectionInfo(ConnectionState.Connected(profile))

            copyDebugUserId(context, info.userId!!)

            val clip = clipboard.primaryClip!!
            assertEquals("octopus-user", clip.getItemAt(0).text.toString())
            assertEquals("User ID", clip.description.label.toString())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                assertTrue(clip.description.extras!!.getBoolean("android.content.extra.IS_SENSITIVE"))
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                assertEquals("User ID copied", ShadowToast.getTextOfLatestToast())
                assertEquals(Toast.LENGTH_SHORT, ShadowToast.getLatestToast().duration)
            } else {
                assertEquals(0, ShadowToast.shownToastCount())
            }
        }
    }
}
