package com.octopuscommunity.sdk.sample.ui.components

import androidx.compose.ui.graphics.Color
import com.octopuscommunity.sdk.sample.ui.theme.SampleColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SampleUiTokensTest {
    private val light = SampleColors.Light
    private val dark = SampleColors.Dark

    @Test
    fun `segmented stays a pill up to four short options`() {
        assertFalse(segmentedNeedsChipRow(listOf("Light", "Dark", "System")))
        assertFalse(segmentedNeedsChipRow(listOf("A", "B", "C", "D")))
        assertFalse(segmentedNeedsChipRow(listOf("Twelve chars")))
    }

    @Test
    fun `segmented turns into a chip row past four options or a twelve character label`() {
        assertTrue(segmentedNeedsChipRow(listOf("A", "B", "C", "D", "E")))
        assertTrue(segmentedNeedsChipRow(listOf("Short", "Thirteen char")))
    }

    @Test
    fun `four options turn into a chip row once a label passes eight characters`() {
        assertFalse(segmentedNeedsChipRow(listOf("Open", "Locked", "Hidden", "Eightchr")))
        assertTrue(segmentedNeedsChipRow(listOf("Open", "Locked", "Hidden", "Ninechars")))
        // Three options keep the pill up to the twelve-character limit.
        assertFalse(segmentedNeedsChipRow(listOf("Open", "Locked", "Twelve chars")))
        assertFalse(segmentedNeedsChipRow(emptyList()))
    }

    @Test
    fun `result badge label is sentence case with an optional duration`() {
        assertEquals("Success · 124 ms", resultBadgeLabel(isError = false, durationMs = 124))
        assertEquals("Success", resultBadgeLabel(isError = false, durationMs = null))
        assertEquals("Failure", resultBadgeLabel(isError = true, durationMs = null))
        assertEquals("Failure · 9 ms", resultBadgeLabel(isError = true, durationMs = 9))
    }

    @Test
    fun `status labels are never all caps`() {
        SampleStatus.entries.filter { it != SampleStatus.NotApplicable }.forEach {
            assertFalse(it.defaultLabel == it.defaultLabel.uppercase(), "${it.name}: ${it.defaultLabel}")
        }
    }

    @Test
    fun `status badges are tonal pairs taken from the tokens`() {
        for (c in listOf(light, dark)) {
            assertEquals(c.successTint to c.successText, statusBadgeColors(SampleStatus.Passed, c))
            assertEquals(c.dangerTint to c.danger, statusBadgeColors(SampleStatus.Failed, c))
            assertEquals(c.warningTint to c.warningText, statusBadgeColors(SampleStatus.Blocked, c))
            assertEquals(c.accentTint to c.accentText, statusBadgeColors(SampleStatus.Running, c))
            assertEquals(Color.Transparent to c.accentText, statusBadgeColors(SampleStatus.Ready, c))
            assertEquals(Color.Transparent to c.textSecondary, statusBadgeColors(SampleStatus.NotApplicable, c))
        }
    }

    @Test
    fun `banner tones map to their tint and ink`() {
        for (c in listOf(light, dark)) {
            assertEquals(c.accentTint to c.accentText, bannerColors(SampleBannerTone.Info, c))
            assertEquals(c.warningTint to c.warningText, bannerColors(SampleBannerTone.Warning, c))
            assertEquals(c.dangerTint to c.danger, bannerColors(SampleBannerTone.Error, c))
        }
    }
}
