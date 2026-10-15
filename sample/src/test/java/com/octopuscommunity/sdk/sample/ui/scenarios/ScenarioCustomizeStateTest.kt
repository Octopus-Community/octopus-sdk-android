package com.octopuscommunity.sdk.sample.ui.scenarios

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Drives [ScenarioCustomizeState] straight through its remembered holders — the same objects
 * `rememberScenarioCustomizeState` hands it — so the preset / Custom / stale state machine is
 * checked without a device. Re-creating the state object over the same holders is exactly what a
 * recomposition (or a rotation restoring saved state) does.
 */
class ScenarioCustomizeStateTest {
    /** A one-input `locale` spec: preset 1 = fr, preset 2 = en, preset 3 = blank. */
    private class Fixture {
        val input = mutableStateOf("fr")
        var runs = 0
        val customizing: MutableState<Boolean> = mutableStateOf(false)
        val lastPreset: MutableState<Int> = mutableStateOf(0)
        val lastRunValues: MutableState<String?> = mutableStateOf(null)

        val spec: ScenarioCustomizeSpec
            get() =
                ScenarioCustomizeSpec(
                    presetLabels = listOf("Preset 1 · fr", "Preset 2 · en", "Preset 3 · none"),
                    params =
                        listOf(
                            ScenarioEditableParam(
                                name = "locale",
                                type = "String?",
                                help = "",
                                presetValues = listOf("fr", "en", ""),
                                value = input.value,
                                onValueChange = { input.value = it },
                            ),
                        ),
                    replacesCatalogParams = setOf("locale"),
                    onRun = { runs++ },
                )

        /** A fresh state object over the same holders, as each composition builds one. */
        fun state(lastCallRank: Long): ScenarioCustomizeState =
            ScenarioCustomizeState(
                spec = spec,
                customizingState = customizing,
                lastPresetState = lastPreset,
                lastRunValuesState = lastRunValues,
                lastCallRank = lastCallRank,
            )

        fun edit(
            state: ScenarioCustomizeState,
            value: String,
        ) = state.onEdit(state.spec!!.params.single(), value)
    }

    @Test
    fun `reset restores the preset customize was entered with, not one matched while typing`() {
        val f = Fixture()
        f.state(0L).runPreset(0) // fr
        f.state(1L).enterCustomize()
        f.edit(f.state(1L), "") // transiently equals preset 3
        f.edit(f.state(1L), "de")
        assertTrue(f.state(1L).isCustom)

        f.state(1L).resetToPreset()

        assertEquals("fr", f.input.value)
        assertFalse(f.customizing.value)
    }

    @Test
    fun `a preset button while customizing re-locks the fields and runs`() {
        val f = Fixture()
        f.state(0L).enterCustomize()
        f.edit(f.state(0L), "de")

        f.state(0L).runPreset(1)

        assertEquals("en", f.input.value)
        assertFalse(f.customizing.value)
        assertEquals(1, f.runs)
        assertEquals(1, f.lastPreset.value)
        // The snapshot is what ran ("en"), not what the composition's spec held when the button
        // was tapped ("de") — otherwise the fresh result would hide behind "values changed".
        assertFalse(f.state(1L).resultStale)
    }

    @Test
    fun `typing a preset value by hand leaves customize open, and so does a recomposition`() {
        val f = Fixture()
        f.state(0L).runPreset(0)
        f.state(1L).enterCustomize()
        f.edit(f.state(1L), "en") // equals preset 2, but typed

        // Recomposition / restored saved state: a new state object over the same holders.
        val restored = f.state(1L)

        assertTrue(restored.customizing)
        assertEquals(1, restored.spec!!.matchingPreset)
    }

    @Test
    fun `stale only while the fields differ from what actually ran`() {
        val f = Fixture()
        assertFalse("never run", f.state(0L).resultStale)

        f.state(0L).runPreset(0) // runs with fr → rank 1 once the call starts
        assertFalse("just ran", f.state(1L).resultStale)

        f.state(1L).enterCustomize()
        f.edit(f.state(1L), "de")
        assertTrue("edited after the run", f.state(1L).resultStale)

        f.edit(f.state(1L), "fr")
        assertFalse("typed back to what ran", f.state(1L).resultStale)

        f.edit(f.state(1L), "de")
        f.state(1L).resetToPreset() // back to fr, which is what ran
        assertFalse("reset to the preset that ran", f.state(1L).resultStale)

        f.edit(f.state(1L), "de")
        f.state(1L).run() // rank 2 once the call starts
        assertFalse("run clears staleness at call start", f.state(2L).resultStale)

        f.state(2L).runPreset(1) // fields held "de", the call runs with "en"
        assertFalse("preset button snapshots the values that ran", f.state(3L).resultStale)
        assertEquals(3, f.runs)
    }

    @Test
    fun `a result that predates this screen's editor is never stale`() {
        val f = Fixture()
        // The ViewModel remembers a call (rank 5) but this visit's holders are fresh.
        f.input.value = "de"
        assertFalse(f.state(5L).resultStale)
        assertTrue(f.state(5L).isCustom)
    }
}
