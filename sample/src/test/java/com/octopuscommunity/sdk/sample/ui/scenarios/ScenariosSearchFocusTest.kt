package com.octopuscommunity.sdk.sample.ui.scenarios

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.octopuscommunity.sdk.sample.ui.components.LocalSampleTopBarInset
import com.octopuscommunity.sdk.sample.ui.theme.SampleAppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w480dp-h800dp")
class ScenariosSearchFocusTest {
    private val compose = createComposeRule()
    private val host =
        object : ExternalResource() {
            override fun before() {
                // Set this before the activity attaches, so clearFocus does not reselect the
                // first focusable field as it would in keyboard navigation mode.
                InstrumentationRegistry.getInstrumentation().setInTouchMode(true)
                val application = RuntimeEnvironment.getApplication()
                shadowOf(application.packageManager).addActivityIfNotPresent(ComponentName(application, ComponentActivity::class.java))
            }
        }

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(host).around(compose)

    private var keyboardController by mutableStateOf<SoftwareKeyboardController?>(null)
    private val search get() = compose.onNodeWithTag("scenarios-search-input")
    private val list get() = compose.onNode(hasScrollAction())

    private fun setContent() {
        compose.setContent {
            SampleAppTheme {
                // A nonzero parent offset and bar inset exercise root-space hit testing.
                Box(Modifier.padding(start = 24.dp, top = 32.dp)) {
                    Box(Modifier.size(width = 360.dp, height = 600.dp).testTag("scenarios-host")) {
                        CompositionLocalProvider(
                            LocalSampleTopBarInset provides 56.dp,
                            LocalSoftwareKeyboardController provides keyboardController,
                        ) {
                            ScenariosListScreen(apiKey = "", onScenarioClick = {})
                        }
                    }
                }
            }
        }
    }

    private fun focusSearch() {
        search.performClick().assertIsFocused()
    }

    @Test
    fun `swiping the list dismisses search focus and still scrolls`() {
        setContent()
        focusSearch()
        val scrollBefore = list.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()

        list.performTouchInput { swipeUp() }

        search.assertIsNotFocused()
        val scrollAfter = list.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
        assertTrue(scrollAfter > scrollBefore)
    }

    @Test
    fun `tapping a section header dismisses focus and still expands the section`() {
        setContent()
        list.performScrollToNode(hasTestTag("scenarios-section-presentation"))
        focusSearch()

        compose.onNodeWithTag("scenarios-section-presentation").performTouchInput { click() }

        search.assertIsNotFocused()
        compose.onNodeWithTag("scenarios-initialScreen-card").assertIsDisplayed()
    }

    @Test
    fun `IME search dismisses focus`() {
        val keyboard = RecordingKeyboardController()
        keyboardController = keyboard
        setContent()
        focusSearch()
        val hides = keyboard.hides

        search.performImeAction()

        assertTrue(keyboard.hides > hides, "IME dismissal must hide the keyboard")
        search.assertIsNotFocused()
    }

    @Test
    fun `tapping the left gutter dismisses focus`() {
        assertOutsideTapDismisses { Offset(1.dp.toPx(), searchCenterY()) }
    }

    @Test
    fun `tapping the right gutter dismisses focus`() {
        assertOutsideTapDismisses { Offset(width - 1.dp.toPx(), searchCenterY()) }
    }

    @Test
    fun `tapping the gap below the top bar dismisses focus`() {
        assertOutsideTapDismisses { Offset(centerX, 60.dp.toPx()) }
    }

    private fun assertOutsideTapDismisses(position: androidx.compose.ui.test.TouchInjectionScope.() -> Offset) {
        setContent()
        focusSearch()

        compose.onNodeWithTag("scenarios-host").performTouchInput { click(position()) }

        search.assertIsNotFocused()
    }

    private fun searchCenterY(): Float =
        search
            .fetchSemanticsNode()
            .boundsInRoot.center.y -
            compose
                .onNodeWithTag("scenarios-host")
                .fetchSemanticsNode()
                .boundsInRoot.top

    @Test
    fun `tapping inside the search field keeps focus with an offset parent`() {
        val keyboard = RecordingKeyboardController()
        keyboardController = keyboard
        setContent()
        focusSearch()
        val hides = keyboard.hides

        search.performTouchInput { click(Offset(1.dp.toPx(), centerY)) }
        search.assertIsFocused()
        search.performTouchInput { click(Offset(width - 1.dp.toPx(), centerY)) }
        search.assertIsFocused()
        assertEquals(hides, keyboard.hides)
    }

    @Test
    fun `scrolling without a pointer event dismisses focus`() {
        setContent()
        focusSearch()

        list.performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy -> assertTrue(scrollBy(0f, 150f)) }

        search.assertIsNotFocused()
    }

    @Test
    fun `outside taps use the current keyboard controller after recomposition`() {
        assertCurrentKeyboardControllerUsed {
            compose.onNodeWithTag("scenarios-host").performTouchInput { click(Offset(1f, 1f)) }
        }
    }

    @Test
    fun `scroll dismissal uses the current keyboard controller after recomposition`() {
        assertCurrentKeyboardControllerUsed {
            list.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 150f) }
        }
    }

    private fun assertCurrentKeyboardControllerUsed(dismiss: () -> Unit) {
        val original = RecordingKeyboardController()
        keyboardController = original
        setContent()
        focusSearch()
        val originalHides = original.hides
        val current = RecordingKeyboardController()
        compose.runOnIdle { keyboardController = current }
        search.assertIsFocused()

        dismiss()

        search.assertIsNotFocused()
        assertEquals(originalHides, original.hides)
        assertTrue(current.hides > 0)
    }

    private class RecordingKeyboardController : SoftwareKeyboardController {
        var hides = 0

        override fun show() = Unit

        override fun hide() {
            hides++
        }
    }
}
