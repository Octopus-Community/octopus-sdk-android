package com.octopuscommunity.sdk.sample.ui.community

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.octopuscommunity.sdk.domain.model.OctopusCommunityData
import com.octopuscommunity.sdk.sample.ui.navigation.ClientProfileRoute
import org.koin.androidx.compose.koinViewModel

/**
 * Sample wiring for the SDK's `onNavigateToProfile` (Unified Profile). The host handles every
 * profile tap (self included) by pushing [ClientProfileRoute] onto its own stack, for the tapped
 * member's host-side [clientUserId]. Unified Profile needs both the backend `exposeClientUserId`
 * flag and this callback. The dedicated scenario switch sets both; when off, the sample clears
 * the flag override and omits the callback, so taps open native profiles.
 */
fun NavController.onClientProfileTap(clientUserId: String) {
    navigate(ClientProfileRoute(clientUserId = clientUserId))
}

/**
 * Stand-in for a host app's own public profile screen. It demonstrates the two things a real
 * integration would surface here, both keyed on the member's host-side [clientUserId] handed over by
 * the SDK's `onNavigateToProfile`:
 *  - that **clientUserId** (the host's own id for the member — never an Octopus id), and
 *  - the read-only **Octopus community stats** via `OctopusSDK.fetchCommunityDataByClientUserIdResult()`.
 *
 * It also offers a jump into the member's Octopus posts via [onSeePosts]
 * (`navigateToOctopusActivityByClientUserId`).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun ClientProfileScreen(
    clientUserId: String,
    onBack: () -> Unit,
    onSeePosts: () -> Unit,
    viewModel: ClientProfileViewModel = koinViewModel(),
) {
    LaunchedEffect(clientUserId) { viewModel.load(clientUserId) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        // testTagsAsResourceId is hoisted once, at the Activity's own composition root
        // (MainActivity.kt) — this screen is a main-NavHost destination and a descendant of it,
        // so no local declaration is needed here.
        topBar = {
            TopAppBar(
                title = { Text("Client profile (host app)") },
                navigationIcon = {
                    // Tagged so the QA pipeline has a deterministic in-app way back from here.
                    // Preset 6 of the `communityData` scenario leaves the scenario screen, and
                    // walking back with repeated system Back is a documented Tester trap (it exits
                    // the app once the in-app stack is empty).
                    IconButton(onClick = onBack, modifier = Modifier.testTag("clientProfile-back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Host-owned profile screen",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text =
                        "The Octopus SDK handed this profile tap to the app via its onNavigateToProfile " +
                            "callback (Unified Profile).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider()

            // clientUserId — the host's own id for this member, handed over by onNavigateToProfile.
            LabeledValue(label = "clientUserId (from the callback)", value = clientUserId)
            Text(
                text =
                    "The SDK handed the app this member's clientUserId — the host's own id for them. " +
                        "Members without one (guests, BO/admin-created) never reach here: they open the " +
                        "Octopus activity (posts) screen instead.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider()

            // Octopus community stats a host can surface in its own profile UI — resolved from the
            // host's own client id, so the host never handles an Octopus id.
            Text(
                text = "Octopus community data — fetchCommunityDataByClientUserIdResult()",
                style = MaterialTheme.typography.labelLarge,
            )
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }

                state.communityDataError != null -> {
                    Text(
                        modifier = Modifier.testTag("clientProfile-error"),
                        text = "Error: ${state.communityDataError}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                else -> CommunityDataFields(state.communityData)
            }

            HorizontalDivider()

            // Jump into the member's Octopus posts, still keyed on the host's own client id
            // (navigateToOctopusActivityByClientUserId).
            Button(onClick = onSeePosts, modifier = Modifier.fillMaxWidth()) {
                Text("See their Octopus posts")
            }
        }
    }
}

// The three outcomes carry the cross-platform test ids from the shared scenario catalog
// (`clientProfile-data` / `clientProfile-error` / `clientProfile-unknown`), so the QA pipeline can
// tell "rendered the stats" from "member unknown" from "fetch failed" without reading pixels.
@Composable
private fun CommunityDataFields(data: OctopusCommunityData?) {
    if (data == null) {
        Text(
            modifier = Modifier.testTag("clientProfile-unknown"),
            text =
                "null — no Octopus profile for this clientUserId. Either the member has never used " +
                    "the community, or the community is not configured to expose client user ids.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Column(
        modifier = Modifier.testTag("clientProfile-data"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LabeledValue(label = "userId", value = data.userId)
        LabeledValue(label = "messageCount", value = data.messageCount?.toString() ?: "—")
        LabeledValue(
            label = "gamification level",
            value = data.gamification?.level?.toString() ?: "— (gamification off)",
        )
        LabeledValue(
            label = "gamification score",
            value = data.gamification?.score?.toString() ?: "— (not exposed to consumers)",
        )
    }
}

@Composable
private fun LabeledValue(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp,
        )
    }
}
