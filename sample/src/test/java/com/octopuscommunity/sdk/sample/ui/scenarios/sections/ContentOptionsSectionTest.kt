package com.octopuscommunity.sdk.sample.ui.scenarios.sections

import android.app.Application
import android.content.ComponentName
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.octopuscommunity.sdk.InternalOctopusApi
import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.domain.model.CommunityConfig.ContentOptions
import com.octopuscommunity.sdk.sample.ui.community.HostOverrides
import com.octopuscommunity.sdk.sample.ui.theme.SampleAppTheme
import kotlinx.coroutines.runBlocking
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(InternalOctopusApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ContentOptionsSectionTest {
    private val compose = createComposeRule()
    private val host =
        object : ExternalResource() {
            override fun before() {
                val app = RuntimeEnvironment.getApplication()
                shadowOf(app.packageManager).addActivityIfNotPresent(ComponentName(app, ComponentActivity::class.java))
                // The SDK's uninitialised preview/test graph uses in-memory fakes only.
                HostOverrides.applyContentOptionsOverride(null)
            }

            override fun after() {
                HostOverrides.applyContentOptionsOverride(null)
            }
        }

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(host).around(compose)

    @Test
    fun `tap presets keep catalog order and apply independent options through the SDK`() {
        val backendOptions = runBlocking { OctopusSDK.communityConfigRepository.getCommunityConfig()!!.contentOptions }
        compose.setContent {
            SampleAppTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    ContentOptionsSection()
                }
            }
        }

        val presetIds =
            compose
                .onAllNodes(hasClickAction())
                .fetchSemanticsNodes()
                .mapNotNull { it.config.getOrNull(SemanticsProperties.TestTag) }
                .filter { it.startsWith("qa-preset-contentOptions-") }
        assertEquals(
            (1..10).map { "qa-preset-contentOptions-$it" } + "qa-preset-contentOptions-clear",
            presetIds,
        )
        compose.onNodeWithTag("qa-preset-contentOptions-clear").assertIsNotEnabled()

        val cases =
            listOf(
                Triple(8, "Post: image tap opens the post", true to false),
                Triple(9, "Post: group name opens the group", false to true),
                Triple(10, "Post: image and group name taps", true to true),
            )
        for ((id, label, taps) in cases) {
            // A tap preset must reset creation flags changed by the previous preset.
            compose.onNodeWithTag("qa-preset-contentOptions-7").performScrollTo().performClick()
            compose.runOnIdle { assertFalse(HostOverrides.contentOptionsOverride!!.post.textRequired) }
            compose.onNodeWithTag("qa-preset-contentOptions-2").performScrollTo().performClick()
            compose.runOnIdle { assertFalse(HostOverrides.contentOptionsOverride!!.post.enablePictures) }

            compose
                .onNodeWithTag("qa-preset-contentOptions-$id")
                .performScrollTo()
                .assertIsDisplayed()
                .assertTextEquals("Preset $id · $label")
                .performClick()

            compose.runOnIdle {
                val applied = HostOverrides.contentOptionsOverride!!
                assertTrue(applied.post.enablePictures)
                assertTrue(applied.post.enablePolls)
                assertTrue(applied.post.textRequired)
                assertTrue(applied.comment.enablePictures)
                assertTrue(applied.reply.enablePictures)
                assertEquals(taps.first, applied.post.imageTapOpensPost)
                assertEquals(taps.second, applied.post.groupNameTapOpensGroup)
                assertEquals(applied, runBlocking { OctopusSDK.communityConfigRepository.getCommunityConfig()!!.contentOptions })
            }
            compose.onNodeWithTag("contentOptions-result").performScrollTo().assertTextEquals("Applied: $label")
            compose.onNodeWithTag("contentOptions-override-badge").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("qa-preset-contentOptions-clear").assertIsEnabled()
        }

        compose.onNodeWithTag("qa-preset-contentOptions-1").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(ContentOptions(), HostOverrides.contentOptionsOverride) }
        compose
            .onNodeWithTag("qa-preset-contentOptions-clear")
            .performScrollTo()
            .performClick()
            .assertIsNotEnabled()
        compose.onNodeWithTag("contentOptions-result").assertTextEquals("Applied: none (backend default)")
        compose.onNodeWithTag("contentOptions-override-badge").assertDoesNotExist()
        compose.runOnIdle {
            assertNull(HostOverrides.contentOptionsOverride)
            assertEquals(backendOptions, runBlocking { OctopusSDK.communityConfigRepository.getCommunityConfig()!!.contentOptions })
        }
    }
}
