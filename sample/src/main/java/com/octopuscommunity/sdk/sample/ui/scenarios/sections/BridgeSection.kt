package com.octopuscommunity.sdk.sample.ui.scenarios.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.octopuscommunity.sdk.InternalOctopusApi
import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.domain.model.ClientPost
import com.octopuscommunity.sdk.domain.model.OctopusGroup
import com.octopuscommunity.sdk.domain.model.OctopusReactionKind
import com.octopuscommunity.sdk.domain.model.Resource
import com.octopuscommunity.sdk.domain.network.onFailure
import com.octopuscommunity.sdk.domain.network.onSuccess
import com.octopuscommunity.sdk.sample.R
import com.octopuscommunity.sdk.sample.data.SampleFixtures
import com.octopuscommunity.sdk.sample.data.describe
import com.octopuscommunity.sdk.sample.data.utils.TokenProvider
import com.octopuscommunity.sdk.sample.ui.components.LiveFlowDisplay
import com.octopuscommunity.sdk.sample.ui.components.SecondaryPillButton
import com.octopuscommunity.sdk.sample.ui.components.sampleDefaultSwitchColors
import kotlinx.coroutines.launch

/**
 * `bridge` scenario: fetchOrCreateClientObjectRelatedPost (+ tokenProvider), then react/unreact on
 * the post it returned.
 *
 * Re-attached after being orphaned by the internal change Scenarios refactor (the legacy Explorer's Bridge
 * section had no qa-preset ids at all). Presets 1-2 both call [OctopusSDK.fetchOrCreateClientObjectRelatedPost]
 * — preset 1 with a fixed objectId ("recipe-1", so repeat taps fetch the SAME post) and preset 2
 * with a fresh objectId every tap (so it always creates a new one) — and open the result via
 * [onOpenPost]. Presets 3-4 call [OctopusSDK.setReaction] on the last post either preset returned;
 * `setReaction` itself is exercised (and its Gate A claim already booked) by the `reactions`
 * scenario, so it is not re-listed in this scenario's own `api_symbols`.
 *
 * [OctopusSDK.setBridgeShareTokenProvider] is registered once on entry, in addition to the inline
 * `tokenProvider` passed to each fetchOrCreate call below — a real host could rely on either
 * mechanism, so this scenario genuinely exercises both. That registration is process-wide and
 * never unregistered — visiting this scenario once changes global SDK state (which provider
 * answers a bridge share-token request) for the rest of the session, with no UI indicating it.
 *
 * Also restores what was a "Known gap" versus the pre-internal change legacy Explorer's Bridge section: the
 * "Bridge Group" block (open a group/topic in bridge mode via [onOpenBridgeTopic], with quick-access
 * buttons for [groups]), an "Error cases" block for both invalid-id paths, and a bundled-image
 * toggle so a `Resource.Local` attachment can be posted through the bridge (to confirm a signed
 * image still lands in a pictures-off community). None of these carry a catalog `presets[].test_id`
 * — they are not gated by `coverage / coverage` — so they use plain testTags, not `qa-preset-*` ids.
 */
@OptIn(InternalOctopusApi::class)
@Composable
fun BridgeSection(
    groups: List<OctopusGroup>,
    onOpenPost: (postId: String) -> Unit,
    onOpenBridgeTopic: (groupId: String) -> Unit,
) {
    var lastPostId by rememberSaveable { mutableStateOf<String?>(null) }
    var isBusy by rememberSaveable { mutableStateOf(false) }
    var result by rememberSaveable { mutableStateOf<String?>(null) }
    var isError by rememberSaveable { mutableStateOf(false) }
    var groupId by rememberSaveable { mutableStateOf(SampleFixtures.topicDefault) }
    // Bundled-image attachment is opt-in and defaults off, so existing automated QA taps on
    // presets 1-2 keep exercising the exact same no-attachment payload as before this restoration.
    var includeImage by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // Same bundled drawable as the Bridge Share presets — a content URI the SDK can read without
    // the system picker. Lets us check a (signed) bridge non-share image still posts when the
    // community has post pictures disabled.
    val bundledImageUri = "android.resource://${context.packageName}/${R.drawable.ic_avatar}"

    LaunchedEffect(Unit) {
        OctopusSDK.setBridgeShareTokenProvider { fingerprint -> TokenProvider.getBridgeSignature(fingerprint) }
    }

    // remember { } is required here, not cosmetic: PostRepositoryImpl builds a fresh cold Flow on
    // every call, and collectAsStateWithLifecycle keys its own `remember` on the Flow *instance* —
    // without wrapping the call itself, every recomposition (each tap flips isBusy / result) would
    // build a new Flow, restart collection, and reset this display back to null.
    val recipeOnePostFlow = remember { OctopusSDK.getClientObjectRelatedPostFlow("recipe-1") }
    val recipeOnePost by recipeOnePostFlow.collectAsStateWithLifecycle(initialValue = null)

    fun fetchOrCreate(objectId: String) {
        scope.launch {
            isBusy = true
            OctopusSDK
                .fetchOrCreateClientObjectRelatedPost(
                    clientPost =
                        ClientPost(
                            objectId = objectId,
                            text = "Sample bridge post for object: $objectId",
                            attachment = if (includeImage) Resource.Local(bundledImageUri) else null,
                            catchPhrase = "Check out this content!",
                            viewObjectButtonText = "View in app",
                        ),
                    tokenProvider = { fingerprint -> TokenProvider.getBridgeSignature(fingerprint) },
                ).onSuccess { post ->
                    lastPostId = post.id
                    result = "Post: ${post.id}"
                    isError = false
                    onOpenPost(post.id)
                }.onFailure { error ->
                    result = "Error: ${error.describe()}"
                    isError = true
                }
            isBusy = false
        }
    }

    fun react(reaction: OctopusReactionKind?) {
        val postId = lastPostId ?: return
        scope.launch {
            isBusy = true
            OctopusSDK
                .setReaction(reaction, postId = postId)
                .onSuccess {
                    result = if (reaction != null) "Reacted on $postId" else "Unreacted on $postId"
                    isError = false
                }.onFailure { error ->
                    result = "Error: ${error.describe()}"
                    isError = true
                }
            isBusy = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionIntro(
            "fetchOrCreateClientObjectRelatedPost() fetches (or creates, on a first call) the post " +
                "tied to a client-side object id, then opens it. Presets 3-4 react / unreact on " +
                "whichever post presets 1-2 last returned.",
        )

        SecondaryPillButton(
            modifier = Modifier.fillMaxWidth().testTag("qa-preset-bridge-1"),
            enabled = !isBusy,
            onClick = { fetchOrCreate("recipe-1") },
        ) { Text("Stable recipe (objectId recipe-1)") }

        // Shown next to preset 2 only, not preset 1 above: "recipe-1" is a shared, stable objectId
        // that only ever CREATES on the very first call in the community's history and FETCHes
        // (ignoring this toggle) every tap after that, so surfacing it next to preset 1 would imply
        // an effect it doesn't reliably have. Preset 2's fresh objectId always CREATEs, so the
        // toggle is live here. Off by default, so preset 2 keeps posting the same no-attachment
        // payload automated QA has always exercised.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Attach bundled image on create (test picture-off)",
                style = MaterialTheme.typography.bodyMedium,
            )
            Switch(
                colors = sampleDefaultSwitchColors(),
                modifier = Modifier.testTag("bridge-attach-image-toggle"),
                checked = includeImage,
                onCheckedChange = { includeImage = it },
            )
        }

        SecondaryPillButton(
            modifier = Modifier.fillMaxWidth().testTag("qa-preset-bridge-2"),
            enabled = !isBusy,
            onClick = { fetchOrCreate("recipe-${System.currentTimeMillis()}") },
        ) { Text("Random recipe (fresh objectId each tap)") }

        SecondaryPillButton(
            modifier = Modifier.fillMaxWidth().testTag("qa-preset-bridge-3"),
            enabled = !isBusy && lastPostId != null,
            onClick = { react(OctopusReactionKind.Heart) },
        ) { Text("Heart-react on latest post") }

        SecondaryPillButton(
            modifier = Modifier.fillMaxWidth().testTag("qa-preset-bridge-4"),
            enabled = !isBusy && lastPostId != null,
            onClick = { react(null) },
        ) { Text("Unreact on latest post") }

        // Absent until a preset has run, rather than a lone "—" that reads as a glitch.
        result?.let { text ->
            Text(
                modifier = Modifier.fillMaxWidth().testTag("bridge-result"),
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

        LiveFlowDisplay(
            label = "getClientObjectRelatedPostFlow(\"recipe-1\")",
            value = recipeOnePost?.id ?: "null",
        )

        HorizontalDivider()

        // --- Bridge Group ---
        Text(
            text = "Bridge Group",
            style = MaterialTheme.typography.titleSmall,
        )
        OutlinedTextField(
            value = groupId,
            onValueChange = { groupId = it },
            label = { Text("Group ID") },
            modifier = Modifier.fillMaxWidth().testTag("bridge-group-id-input"),
            singleLine = true,
        )
        SecondaryPillButton(
            modifier = Modifier.fillMaxWidth().testTag("bridge-group-open"),
            enabled = groupId.isNotBlank(),
            onClick = { if (groupId.isNotBlank()) onOpenBridgeTopic(groupId) },
        ) { Text("Open Bridge Group") }

        // Quick-access buttons for fetched groups.
        if (groups.isNotEmpty()) {
            Text(
                text = "Available groups:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            groups.take(5).forEach { group ->
                SecondaryPillButton(
                    modifier = Modifier.fillMaxWidth().testTag("bridge-group-quick-${group.id}"),
                    onClick = { onOpenBridgeTopic(group.id) },
                ) { Text("Bridge → ${group.name}") }
            }
        }

        HorizontalDivider()

        // --- Error cases ---
        Text(
            text = "Error cases",
            style = MaterialTheme.typography.titleSmall,
        )
        SecondaryPillButton(
            modifier = Modifier.fillMaxWidth().testTag("bridge-error-post"),
            onClick = { onOpenPost("non-existing-post-id") },
        ) { Text("Bridge Post → Invalid ID") }
        SecondaryPillButton(
            modifier = Modifier.fillMaxWidth().testTag("bridge-error-group"),
            onClick = { onOpenBridgeTopic("non-existing-group-id") },
        ) { Text("Bridge Group → Invalid ID") }
    }
}
