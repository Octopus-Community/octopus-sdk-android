package com.octopuscommunity.sdk.sample.ui.scenarios.sections

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.MutableState
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.octopuscommunity.sdk.sample.data.usecase.DISCONNECT_FAILURE_MESSAGE
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Rule
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class CommunityDisconnectErrorTest {
    private val compose = createAndroidComposeRule<ComponentActivity>()
    private val testHost =
        object : ExternalResource() {
            override fun before() {
                val application = RuntimeEnvironment.getApplication()
                shadowOf(application.packageManager).addActivityIfNotPresent(ComponentName(application, ComponentActivity::class.java))
            }
        }

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(testHost).around(compose)

    @Test
    fun `section error survives recomposition until a successful sign-in`() {
        val signIns = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        lateinit var error: MutableState<String?>
        compose.setContent {
            error = rememberCommunityDisconnectError(signIns)
            Text(error.value.orEmpty())
        }
        compose.runOnIdle {
            error.value = DISCONNECT_FAILURE_MESSAGE
        }
        compose.runOnIdle { assertEquals(DISCONNECT_FAILURE_MESSAGE, error.value) }
        compose.runOnIdle { assertTrue(signIns.tryEmit(Unit)) }
        compose.runOnIdle { assertNull(error.value) }
    }

    @Test
    fun `section error clears again after the next successful sign-in`() {
        val signIns = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        lateinit var error: MutableState<String?>
        compose.setContent { error = rememberCommunityDisconnectError(signIns) }
        repeat(2) {
            compose.runOnIdle { error.value = DISCONNECT_FAILURE_MESSAGE }
            compose.runOnIdle { assertEquals(DISCONNECT_FAILURE_MESSAGE, error.value) }
            compose.runOnIdle { assertTrue(signIns.tryEmit(Unit)) }
            compose.runOnIdle { assertNull(error.value) }
        }
    }
}
