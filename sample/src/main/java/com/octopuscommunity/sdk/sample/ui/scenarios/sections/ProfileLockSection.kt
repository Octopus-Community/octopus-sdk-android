package com.octopuscommunity.sdk.sample.ui.scenarios.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.InternalOctopusApi
import com.octopuscommunity.sdk.domain.model.ProfileFieldLockState
import com.octopuscommunity.sdk.domain.model.ProfileFieldsLock
import com.octopuscommunity.sdk.sample.ui.community.HostOverrides

/**
 * QA tooling for the per-field profile lock (internal ticket).
 *
 * Applies a DEBUG-only local override of the community per-field lock — without a backend-driven
 * config — so the lock can be exercised in the sample before the BE serves it. Each preset maps to
 * one of the combinations described in the PRD; open your profile after applying one to observe the
 * effect on the profile + edit screens.
 *
 * State is routed through [HostOverrides.profileFieldsLockOverride] rather than a local
 * `rememberSaveable` label (as before the override-UX pass): the currently-applied preset renders
 * selected ([ModeButton]), an [OverrideActiveBadge] shows only while an override is active, and the
 * "Clear override" button — `qa-preset-profileFieldsLock-clear`, a first-class catalog preset, not
 * sample-added sugar — stays present but disabled ([ModeButton]'s `enabled` param) while there is
 * nothing to clear, so its id always resolves at runtime. A tester can no longer lose track of
 * whether a debug override is still in effect after navigating away and back.
 *
 * Test ids (`qa-preset-profileFieldsLock-N` / `profileFieldsLock-result`) mirror the iOS sample and
 * the shared scenario catalog, so the QA pipeline drives both platforms with the same ids.
 * `testTagsAsResourceId` surfaces them as resource-ids in the uiautomator / `android layout` tree.
 */
@OptIn(InternalOctopusApi::class)
@Composable
fun ProfileLockSection() {
    val applied = HostOverrides.profileFieldsLockOverride
    val hasOverride = applied != null

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionIntro(
            "Debug-only: override the community per-field profile lock locally, then open " +
                "your profile to see the effect on the profile and edit screens.",
        )

        if (hasOverride) {
            OverrideActiveBadge(testId = "profileFieldsLock-override-badge")
        }

        Preset(
            label = "All editable (default / no-op)",
            testId = "qa-preset-profileFieldsLock-1",
            lock = ProfileFieldsLock.AllEditable,
            applied = applied,
        )
        Preset(
            label = "Pseudo + avatar read-only, bio hidden",
            testId = "qa-preset-profileFieldsLock-2",
            lock =
                ProfileFieldsLock(
                    nickname = ProfileFieldLockState.READ_ONLY,
                    avatar = ProfileFieldLockState.READ_ONLY,
                    bio = ProfileFieldLockState.DISABLED,
                ),
            applied = applied,
        )
        Preset(
            label = "Bio-only editable",
            testId = "qa-preset-profileFieldsLock-3",
            lock =
                ProfileFieldsLock(
                    nickname = ProfileFieldLockState.READ_ONLY,
                    avatar = ProfileFieldLockState.READ_ONLY,
                    bio = ProfileFieldLockState.EDITABLE,
                ),
            applied = applied,
        )

        // qa-preset-profileFieldsLock-clear is a first-class catalog preset, not sample-added UX
        // sugar — it must stay in the composition (enabled = hasOverride) rather than being removed
        // when there is nothing to clear, or a QA teardown / up-front resolve of this id from a
        // clean state finds no node.
        ModeButton(
            label = "Clear override (backend default)",
            selected = false,
            enabled = hasOverride,
            testId = "qa-preset-profileFieldsLock-clear",
            onClick = { HostOverrides.applyProfileFieldsLockOverride(null) },
        )

        Text(
            modifier = Modifier.testTag("profileFieldsLock-result"),
            text = "Applied: ${applied?.let(::lockLabel) ?: "none (backend default)"}",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@OptIn(InternalOctopusApi::class)
@Composable
private fun Preset(
    label: String,
    testId: String,
    lock: ProfileFieldsLock,
    applied: ProfileFieldsLock?,
) {
    ModeButton(
        label = label,
        selected = applied == lock,
        testId = testId,
        onClick = { HostOverrides.applyProfileFieldsLockOverride(lock) },
    )
}

private fun lockLabel(lock: ProfileFieldsLock): String =
    when (lock) {
        ProfileFieldsLock.AllEditable -> "All editable (default / no-op)"
        else ->
            "nickname=${lock.nickname}, avatar=${lock.avatar}, bio=${lock.bio}"
    }
