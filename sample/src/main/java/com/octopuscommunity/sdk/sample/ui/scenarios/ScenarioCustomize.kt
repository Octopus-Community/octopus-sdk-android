package com.octopuscommunity.sdk.sample.ui.scenarios

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.sample.ui.components.TypePill
import com.octopuscommunity.sdk.sample.ui.components.sampleAccentColor
import com.octopuscommunity.sdk.sample.ui.components.sampleBorderColor
import com.octopuscommunity.sdk.sample.ui.components.sampleTintedBackground

/**
 * One parameter a human is allowed to retype on the scenario screen.
 *
 * [presetValues] is index-aligned with [ScenarioCustomizeSpec.presetLabels]: entry `n` is what
 * preset `n` writes into this field. That alignment is the whole mechanism — "is this custom?" is
 * *derived* from it (no value equals any preset's) rather than tracked as a flag, so a value the
 * tester types back to a preset's value correctly stops being custom.
 *
 * [value] / [onValueChange] bind straight to the same `ScenariosViewModel` input the preset buttons
 * write, which is what makes a customized run travel the *same* code path as a preset run instead
 * of a parallel one that could drift.
 */
data class ScenarioEditableParam(
    val name: String,
    val type: String,
    /** What this field means, and what a blank means — shown as the placeholder. */
    val help: String,
    val presetValues: List<String>,
    val value: String,
    val onValueChange: (String) -> Unit,
)

/**
 * The Customize contract for one scenario: which fields open up, what each preset puts in them, and
 * how to run the call with whatever is in them now.
 *
 * Only scenarios with a genuinely **open value domain** declare one — see
 * [scenarioCustomizeSpec] for which, and for why the others deliberately have none.
 */
data class ScenarioCustomizeSpec(
    /** `Preset N · <description>`, in the order the preset buttons appear in the panel below. */
    val presetLabels: List<String>,
    val params: List<ScenarioEditableParam>,
    /**
     * Catalog [ScenarioParam] names this spec supersedes — those rows render as live editable
     * fields instead of the catalog's descriptive prose, and must not also be drawn read-only.
     */
    val replacesCatalogParams: Set<String>,
    /** Runs the scenario's call with the values currently in [params]. */
    val onRun: () -> Unit,
) {
    private val currentValues: List<String> get() = params.map { it.value }

    /**
     * The fields' current values as one comparable string — what [ScenarioCustomizeState] snapshots
     * at each call start to tell a live result from a stale one. NUL-joined: no field value can
     * contain one, so two different value sets never collapse into the same key.
     */
    internal val valuesKey: String get() = currentValues.joinToString("\u0000")

    /** [valuesKey] as it will read once preset [index] has been applied — see [applyPreset]. */
    internal fun presetValuesKey(index: Int): String = presetValues(index).joinToString("\u0000")

    private fun presetValues(index: Int): List<String> = params.map { it.presetValues[index] }

    /** The preset every field currently matches, or `null` — which is exactly "Custom". */
    val matchingPreset: Int?
        get() = presetLabels.indices.firstOrNull { presetValues(it) == currentValues }

    fun applyPreset(index: Int) {
        params.forEach { it.onValueChange(it.presetValues[index]) }
    }

    init {
        require(params.all { it.presetValues.size == presetLabels.size }) {
            "every editable param must declare one value per preset"
        }
    }
}

/**
 * Customize-mode state for one scenario screen, derived from [spec] plus three remembered scalars.
 *
 * Recreated on every composition on purpose — it owns no state itself, only references to the
 * `rememberSaveable` holders created in [rememberScenarioCustomizeState], so Customize survives a
 * rotation along with the values themselves.
 */
@Stable
class ScenarioCustomizeState internal constructor(
    val spec: ScenarioCustomizeSpec?,
    private val customizingState: MutableState<Boolean>,
    private val lastPresetState: MutableState<Int>,
    private val lastRunValuesState: MutableState<String?>,
    private val lastCallRank: Long,
) {
    /** True once the human asked for the fields to open. */
    val customizing: Boolean get() = customizingState.value

    /** True when no preset matches what is in the fields — the dashed `Custom` chip's condition. */
    val isCustom: Boolean get() = spec != null && spec.matchingPreset == null

    /**
     * True when the fields no longer hold the values the last call was made with, so the result on
     * screen answers a question nobody is asking any more.
     *
     * Compares *values*, not edit counters: a snapshot of the fields is taken at every call start
     * ([run] / [runPreset]), and staleness is simply "the fields differ from that snapshot". That
     * makes it both clear the instant Run is pressed and go away again when the tester types — or
     * resets — the fields back to what actually ran, which an "edited since" flag cannot do.
     *
     * `lastCallRank > 0` (from `ScenariosViewModel.MethodState.rank`) keeps "never run at all" out of
     * this: that state already has its own line, and "values changed" about a result that never
     * existed is noise. A snapshot of `null` means no call went through this screen yet — e.g. the
     * result predates this visit — and is never stale either.
     *
     * Known limit: the snapshot is this screen's, not the ViewModel's. A run of the same method
     * fired from another scenario screen with other values, while this screen's saved state still holds an
     * older snapshot, reads as stale here even though the result shown is the freshest one. Sample
     * nuisance only.
     */
    val resultStale: Boolean
        get() {
            val ran = lastRunValuesState.value ?: return false
            return lastCallRank > 0L && spec != null && spec.valuesKey != ran
        }

    fun enterCustomize() {
        // Anchor "Reset to preset" to the preset the fields hold right now. Nothing moves it while
        // the person types: a field cleared on its way to a new value transiently equals another
        // preset, and Reset restoring *that* one is what the target forbids ("restores the preset
        // exactly").
        //
        // Fields already custom on entry (values left over from an earlier visit — the ViewModel
        // outlives this screen's saved state) match no preset, so the anchor deliberately stays
        // where it was: the last preset run from this screen, or Preset 1 on a fresh visit.
        spec?.matchingPreset?.let { lastPresetState.value = it }
        customizingState.value = true
    }

    /** Fires the scenario's call with whatever is in the fields now — the Customize Run button. */
    fun run() {
        val spec = spec ?: return
        run(spec, spec.valuesKey)
    }

    private fun run(
        spec: ScenarioCustomizeSpec,
        valuesKey: String,
    ) {
        lastRunValuesState.value = valuesKey
        spec.onRun()
    }

    /**
     * What a preset button on the panel below does: write that preset's values, close Customize,
     * and fire the same call the Customize Run button fires.
     *
     * Going through here rather than through the ViewModel setters directly is what makes "which
     * preset am I on" answerable at all — preset values exist once, in [ScenarioCustomizeSpec], so a
     * button and the `Custom` chip cannot come to disagree about what preset 2 is.
     */
    fun runPreset(index: Int) {
        val spec = spec ?: return
        spec.applyPreset(index)
        lastPresetState.value = index
        customizingState.value = false
        // Not `run()`: this `spec` is the composition's snapshot and still carries the values from
        // *before* `applyPreset` wrote to the ViewModel. The call below runs with the preset's
        // values, so that is what the staleness snapshot has to record.
        run(spec, spec.presetValuesKey(index))
    }

    /**
     * Puts the last selected preset back and re-locks the fields.
     *
     * Re-locking is a local decision, not in the mock: the mock leaves Customize by tapping a preset
     * chip, and Android's preset chips are the panel's preset *buttons* below (they keep the
     * `qa-preset-*` ids). Without this, a tester who opened Customize by accident would have to fire
     * an SDK call to close it again.
     */
    fun resetToPreset() {
        spec?.applyPreset(lastPresetState.value)
        customizingState.value = false
    }

    internal fun onEdit(
        param: ScenarioEditableParam,
        value: String,
    ) {
        param.onValueChange(value)
    }
}

/**
 * Wires [spec] to remembered state.
 *
 * Every way of leaving Customize is an explicit call on the returned state — a preset button
 * ([ScenarioCustomizeState.runPreset]) or Reset ([ScenarioCustomizeState.resetToPreset]) — so
 * nothing here watches the values: typing a preset's value by hand leaves the fields open, and a
 * rotation restores exactly the open/closed state the tester left.
 */
@Composable
fun rememberScenarioCustomizeState(
    scenarioId: String,
    spec: ScenarioCustomizeSpec?,
    lastCallRank: Long,
): ScenarioCustomizeState {
    val customizing = rememberSaveable(scenarioId) { mutableStateOf(false) }
    val lastPreset = rememberSaveable(scenarioId) { mutableStateOf(0) }
    val lastRunValues = rememberSaveable(scenarioId) { mutableStateOf<String?>(null) }

    return ScenarioCustomizeState(
        spec = spec,
        customizingState = customizing,
        lastPresetState = lastPreset,
        lastRunValuesState = lastRunValues,
        lastCallRank = lastCallRank,
    )
}

/**
 * Zone 3 of the scenario skeleton: what the call takes, and — where it means anything — the way to
 * change it.
 *
 * Locked by default, and that is a contract rather than a preference: `sample architecture contract` §5.2 requires
 * a preset tap to be *genuinely the only interaction*, "no field left for the Tester to type into".
 * Customize is therefore opt-in per visit, it carries **no test id**, and the QA pipeline never sees
 * an open field. Presets stay single-tap and keep their `qa-preset-<scenario>-<n>` ids on the panel
 * buttons below.
 */
@Composable
fun ScenarioParametersBlock(
    params: List<ScenarioParam>,
    customize: ScenarioCustomizeState,
) {
    val spec = customize.spec
    val readOnlyParams = params.filterNot { spec != null && it.name in spec.replacesCatalogParams }
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(sampleTintedBackground)
                .border(1.dp, sampleBorderColor, RoundedCornerShape(12.dp))
                .padding(12.dp)
                .testTag("scenario-parameters"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ParametersHeader(customize)

        if (spec != null) {
            PresetStateRow(
                label =
                    spec.matchingPreset
                        ?.let { spec.presetLabels[it] }
                        ?: "Values you typed",
                isCustom = customize.isCustom,
            )
        }

        spec?.params?.forEach { param ->
            EditableParamRow(
                param = param,
                editing = customize.customizing,
                onValueChange = { customize.onEdit(param, it) },
            )
        }

        readOnlyParams.forEach { param ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                ParamLabel(param.name, param.type)
                Text(
                    text = param.value,
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * "Parameters", plus the one action that changes what the block is for.
 *
 * The title takes `weight(1f, fill = false)` so it wraps *before* the action does: at 320 dp with a
 * ×1.3 font scale the affordance is the half that must survive, and a clipped "Customi…" is worse
 * than a title on two lines. The action is a 48 dp row, not an inline link.
 */
@Composable
private fun ParametersHeader(customize: ScenarioCustomizeState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Parameters",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (customize.spec != null) {
            val editing = customize.customizing
            Text(
                text = if (editing) "Reset to preset" else "Customize ›",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = sampleAccentColor,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (editing) customize.resetToPreset() else customize.enterCustomize()
                        }.heightIn(min = 48.dp)
                        .padding(horizontal = 8.dp, vertical = 14.dp),
            )
        }
    }
}

/**
 * Which preset the fields currently hold — or the dashed `Custom` chip once they hold something no
 * preset does.
 *
 * A state display, deliberately not a control: the presets themselves are the panel's buttons below,
 * and putting a second tappable copy of them here would be two controls for one action on one
 * screen — the thing the design contract forbids for feature toggles, for the same reason.
 */
@Composable
private fun PresetStateRow(
    label: String,
    isCustom: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (isCustom) {
            val accent = sampleAccentColor
            Text(
                text = "Custom",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = accent,
                modifier =
                    Modifier
                        .drawBehind {
                            drawRoundRect(
                                color = accent,
                                cornerRadius = CornerRadius(8.dp.toPx()),
                                style =
                                    Stroke(
                                        width = 1.dp.toPx(),
                                        pathEffect =
                                            PathEffect.dashPathEffect(
                                                floatArrayOf(6.dp.toPx(), 4.dp.toPx()),
                                            ),
                                    ),
                            )
                        }.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

/** One editable parameter: the same name + type pill in both modes, a text field only when open. */
@Composable
private fun EditableParamRow(
    param: ScenarioEditableParam,
    editing: Boolean,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        ParamLabel(param.name, param.type)
        if (editing) {
            OutlinedTextField(
                value = param.value,
                onValueChange = onValueChange,
                singleLine = true,
                placeholder = {
                    Text(text = param.help, style = MaterialTheme.typography.bodySmall)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Text(
                // A blank value is a real, meaningful answer for these fields (no locale override,
                // no properties) — printing nothing would read as "unset by mistake".
                text = param.value.ifBlank { param.help },
                style = MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ParamLabel(
    name: String,
    type: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            fontWeight = FontWeight.SemiBold,
            // Measured after the type pill: `String` and `String?` are different answers, so the
            // pill must survive a 31-char name at 320dp × 1.3 — the name is what wraps.
            modifier = Modifier.weight(1f, fill = false),
        )
        TypePill(type)
    }
}

/**
 * Which scenarios open up under Customize, and — just as deliberately — which do not.
 *
 * A scenario earns a spec when its parameter has an **open domain**: a value a person could
 * plausibly want to send that no preset offers. That is a much shorter list than "scenarios with
 * parameters", and the exclusions are the interesting half:
 * - **Closed domains.** `communityAccess`, `forceOctopusABTests`, `trackABTests` take one `Boolean`
 *   and ship a preset for each of its two values. There is nothing left to customize — the `Custom`
 *   chip could never light.
 * - **Discrete modes.** `theme`, `reactions`, `contentOptions`, `profileFieldsLock`,
 *   `termsAcceptance`, `forceNicknameChoice`, the three presentation modes: a preset picks a mode,
 *   not a value. The design is explicit that "Customize edits values, never the function or the
 *   field set".
 * - **Owned by another screen.** `connection`'s user is edited on the Account screen, which is the
 *   one place it can be changed. Re-opening it here would be the same boolean-in-two-places defect
 *   the feature-state line exists to avoid.
 * - **Never customizable.** `lifecycle`'s `apiKey` is a credential. It keeps its own paste field and
 *   stays out of any preset/reset machinery.
 * - **Already free-form inline.** `groups`, `communityData` and `initialScreen` expose their id
 *   field inside their own panel. Folding those into Customize is worth doing (it would put them
 *   back behind the §5.2 lock) but it moves QA-driven panels, so it is left to a follow-up.
 */
@Composable
fun scenarioCustomizeSpec(
    scenarioId: String,
    state: ScenariosViewModel.State,
    viewModel: ScenariosViewModel,
): ScenarioCustomizeSpec? =
    when (scenarioId) {
        "locale" ->
            ScenarioCustomizeSpec(
                presetLabels =
                    listOf(
                        "Preset 1 · Force fr",
                        "Preset 2 · Force en",
                        "Preset 3 · Reset to system",
                    ),
                params =
                    listOf(
                        ScenarioEditableParam(
                            name = "locale",
                            type = "Locale?",
                            help = "blank — the system default",
                            presetValues = listOf("fr", "en", ""),
                            value = state.localeInput,
                            onValueChange = viewModel::setLocaleInput,
                        ),
                    ),
                replacesCatalogParams = setOf("locale"),
                onRun = viewModel::executeOverrideLocale,
            )

        "customEvents" ->
            ScenarioCustomizeSpec(
                presetLabels =
                    listOf(
                        "Preset 1 · Sample event, no properties",
                        "Preset 2 · Sample event, with properties",
                    ),
                params =
                    listOf(
                        ScenarioEditableParam(
                            name = "event.name",
                            type = "String",
                            help = DEFAULT_CUSTOM_EVENT_NAME,
                            presetValues = listOf(DEFAULT_CUSTOM_EVENT_NAME, DEFAULT_CUSTOM_EVENT_NAME),
                            value = state.customEventNameInput,
                            onValueChange = viewModel::setCustomEventNameInput,
                        ),
                        ScenarioEditableParam(
                            name = "event.properties[\"sample_prop\"]",
                            type = "String?",
                            help = "blank — send no properties at all",
                            presetValues = listOf("", DEFAULT_CUSTOM_EVENT_PROPERTY),
                            value = state.customEventPropertyInput,
                            onValueChange = viewModel::setCustomEventPropertyInput,
                        ),
                    ),
                replacesCatalogParams = setOf("event", "event.properties"),
                onRun = viewModel::executeTrackCustomEventFromInputs,
            )

        else -> null
    }
