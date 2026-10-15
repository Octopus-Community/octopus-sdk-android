package com.octopuscommunity.sdk.sample.ui.scenarios

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.sample.data.SampleFixtures
import com.octopuscommunity.sdk.sample.ui.components.SecondaryPillButton

/*
 * The three presentation shapes of the shared catalog — `fullscreen`, `modal`, `sheet` —
 * as one story told side by side, the way the Flutter and iOS samples already tell it.
 *
 * What these panels do NOT do is call an SDK method: `api_symbols` is an argued empty list on all
 * three (see the catalog's own block above the `fullscreen` entry). The host presentation shape is
 * a host-side route or container around the ONE embedded surface every other scenario already
 * claims, so there is no per-shape entry point to call. Each preset therefore opens a container the
 * host owns and lets the tester judge the SDK's behaviour inside it — the bottom inset it must add
 * when a host bar sits behind it, whether its sub-navigation stays in the container, and whether a
 * deep link lands inside the presented instance rather than opening a second one.
 *
 * None of the three opens with a `SectionIntro`. The detail screen already prints the scenario's
 * catalog subtitle immediately above (Zone 1), and a paraphrase of it two lines later reads as two
 * different instructions to a tester who is scanning, not as emphasis — the same reason the
 * `initialScreen` panel prints the blurb once and then only per-subsection headings that add
 * something. Where a check existed only in an intro (the sheet's reduced height), it moved INTO the
 * catalog subtitle rather than being deleted: each check is stated exactly once, in Zone 1.
 *
 * Consequently the `<id>-result` line here reports what the HOST just did (which container it
 * opened, with which argument), not a call result. A pass/fail still needs eyes on the container,
 * which is why all three carry the "Verify in Community" step's sibling: the container itself.
 */

/**
 * What the deep-link preset of every shape says when it is tapped with an empty Post ID field.
 *
 * Greying the button out instead would be worse where it matters most: the QA runner taps by
 * resource id, and a disabled node swallows the tap silently — the run then reports "no result"
 * with nothing to distinguish "the SDK never opened the post" from "nobody typed an id". Saying so
 * in the node the runner already reads (`<shape>-result`) turns that into a legible failure.
 */
private const val MISSING_POST_ID =
    "No post id — paste one in the field above, then tap again."

/** Shared result line — one `Text` node carrying the scenario's `result_test_id`. */
@Composable
private fun PresentationResult(
    text: String?,
    resultTestTag: String,
    isError: Boolean = false,
) {
    // Nothing to report until a preset has run: a lone "—" read as a glitch, not as "idle". The
    // node carrying the result id appears with the first result, which is when QA reads it.
    if (text == null) return
    Text(
        modifier = Modifier.fillMaxWidth().testTag(resultTestTag),
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color =
            if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
    )
}

/**
 * The Post ID field the deep-link preset of each shape reads.
 *
 * Deliberately shared rather than three near-identical fields: a tester who pastes an id on the
 * Sheet card and then opens the Modal card expects the same question to be asked the same way, and
 * the QA runner types into one id per shape (`<shape>-postid-input`), mirroring
 * `pushNotifications-postid-input`.
 */
@Composable
private fun PostIdField(
    value: String,
    onValueChange: (String) -> Unit,
    testTag: String,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Post ID (deep-link preset)") },
        modifier = Modifier.fillMaxWidth().testTag(testTag),
        singleLine = true,
    )
}

/**
 * `fullscreen` scenario: the community as a first-class pushed destination of the HOST's own
 * navigation graph — `CommunityFullScreenRoute`, reachable with the two variations the catalog's
 * presets name.
 *
 * Preset 3 is the one worth reading twice: it pushes the same route with a host bottom bar under
 * the embedded content, which is the only way the bottom safe-area inset question becomes
 * observable. An SDK that paints under the host's bar looks perfectly fine without one.
 */
@Composable
fun FullscreenPresets(
    onOpenCommunityFullScreen: (postId: String?, withBottomBar: Boolean) -> Unit,
    onOpenComment: (String) -> Unit,
) {
    var postIdInput by rememberSaveable { mutableStateOf(SampleFixtures.postText) }
    var result by rememberSaveable { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PostIdField(
            value = postIdInput,
            onValueChange = { postIdInput = it },
            testTag = "fullscreen-postid-input",
        )

        SecondaryPillButton(
            modifier = Modifier.fillMaxWidth().testTag("qa-preset-fullscreen-1"),
            onClick = {
                result = "Pushed CommunityFullScreenRoute()"
                onOpenCommunityFullScreen(null, false)
            },
        ) { Text("Preset 1 · Open SDK as full-screen route") }

        SecondaryPillButton(
            modifier = Modifier.fillMaxWidth().testTag("qa-preset-fullscreen-2"),
            onClick = {
                val postId = postIdInput.trim()
                if (postId.isEmpty()) {
                    result = MISSING_POST_ID
                } else {
                    result = "Pushed CommunityFullScreenRoute(postId=$postId)"
                    onOpenCommunityFullScreen(postId, false)
                }
            },
        ) { Text("Preset 2 · Open deep-linked to sample post") }

        SecondaryPillButton(
            modifier = Modifier.fillMaxWidth().testTag("qa-preset-fullscreen-3"),
            onClick = {
                result = "Pushed CommunityFullScreenRoute(showHostBottomBar=true)"
                onOpenCommunityFullScreen(null, true)
            },
        ) { Text("Preset 3 · Behind a host bottom bar (clears bar + inset)") }

        SecondaryPillButton(
            onClick = { onOpenComment(SampleFixtures.commentOnPost) },
            modifier = Modifier.fillMaxWidth().testTag("fullscreen-open-qa-comment"),
        ) { Text("Open QA comment on the sample post") }
        Text(
            text = "Image and CTA post fixtures: TBD in the shared QA catalog.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PresentationResult(
            text = result,
            resultTestTag = "fullscreen-result",
            isError = result == MISSING_POST_ID,
        )
    }
}

/**
 * `modal` scenario: the community in a full-screen modal the host owns — a Compose `Dialog` with
 * `usePlatformDefaultWidth = false`, this platform's equivalent of Flutter's `fullscreenDialog`
 * route and iOS's `.fullScreenCover`.
 *
 * The modal is opened by the Activity, not by this panel: a container created inside a tab
 * destination would be torn down with that destination the moment the SDK navigated anywhere.
 */
@Composable
fun ModalPresets(onOpenCommunityModal: (postId: String?) -> Unit) {
    var postIdInput by rememberSaveable { mutableStateOf(SampleFixtures.postText) }
    var result by rememberSaveable { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PostIdField(
            value = postIdInput,
            onValueChange = { postIdInput = it },
            testTag = "modal-postid-input",
        )

        SecondaryPillButton(
            modifier = Modifier.fillMaxWidth().testTag("qa-preset-modal-1"),
            onClick = {
                result = "Opened full-screen modal"
                onOpenCommunityModal(null)
            },
        ) { Text("Preset 1 · Open SDK as full-screen modal") }

        SecondaryPillButton(
            modifier = Modifier.fillMaxWidth().testTag("qa-preset-modal-2"),
            onClick = {
                val postId = postIdInput.trim()
                if (postId.isEmpty()) {
                    result = MISSING_POST_ID
                } else {
                    result = "Opened full-screen modal, deep-linked to postId=$postId"
                    onOpenCommunityModal(postId)
                }
            },
        ) { Text("Preset 2 · Open deep-linked to sample post") }

        PresentationResult(
            text = result,
            resultTestTag = "modal-result",
            isError = result == MISSING_POST_ID,
        )
    }
}

/**
 * `sheet` scenario: the community in a non-fullscreen bottom sheet — the host `ModalBottomSheet`
 * built in MainActivity (formerly opened from the Home tab), with the catalog's ids and a deep-link
 * variant. Dismissed by dragging it down or tapping the scrim.
 */
@Composable
fun SheetPresets(onOpenCommunitySheet: (postId: String?) -> Unit) {
    var postIdInput by rememberSaveable { mutableStateOf(SampleFixtures.postText) }
    var result by rememberSaveable { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PostIdField(
            value = postIdInput,
            onValueChange = { postIdInput = it },
            testTag = "sheet-postid-input",
        )

        SecondaryPillButton(
            modifier = Modifier.fillMaxWidth().testTag("qa-preset-sheet-1"),
            onClick = {
                result = "Opened bottom sheet"
                onOpenCommunitySheet(null)
            },
        ) { Text("Preset 1 · Open SDK as bottom sheet") }

        SecondaryPillButton(
            modifier = Modifier.fillMaxWidth().testTag("qa-preset-sheet-2"),
            onClick = {
                val postId = postIdInput.trim()
                if (postId.isEmpty()) {
                    result = MISSING_POST_ID
                } else {
                    result = "Opened bottom sheet, deep-linked to postId=$postId"
                    onOpenCommunitySheet(postId)
                }
            },
        ) { Text("Preset 2 · Open deep-linked to sample post") }

        PresentationResult(
            text = result,
            resultTestTag = "sheet-result",
            isError = result == MISSING_POST_ID,
        )
    }
}
