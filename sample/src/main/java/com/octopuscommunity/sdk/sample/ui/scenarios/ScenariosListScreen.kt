package com.octopuscommunity.sdk.sample.ui.scenarios

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.octopuscommunity.sdk.sample.data.SampleSdkStatus
import com.octopuscommunity.sdk.sample.data.model.FeatureToggle
import com.octopuscommunity.sdk.sample.data.model.FeatureToggles
import com.octopuscommunity.sdk.sample.ui.components.ApiPill
import com.octopuscommunity.sdk.sample.ui.components.PrimaryPillButton
import com.octopuscommunity.sdk.sample.ui.components.SampleBanner
import com.octopuscommunity.sdk.sample.ui.components.SampleBannerTone
import com.octopuscommunity.sdk.sample.ui.components.SampleScenarioBlock
import com.octopuscommunity.sdk.sample.ui.components.SecondaryPillButton
import com.octopuscommunity.sdk.sample.ui.components.sampleBorderColor
import com.octopuscommunity.sdk.sample.ui.components.sampleSwitchColors
import com.octopuscommunity.sdk.sample.ui.scenarios.sections.CommunityConfigPreview
import com.octopuscommunity.sdk.sample.ui.theme.SampleMotion
import com.octopuscommunity.sdk.sample.ui.theme.SampleSpacing

/**
 * The Scenarios tab: every shared catalog scenario, grouped into the six sections the shared
 * design contract fixes, one screen per scenario behind each card.
 *
 * Four behaviours are load-bearing and easy to lose in a refactor:
 *
 * - **Search matches title, description and API name**, and *opens* the sections that match. A
 *   search that filters inside collapsed sections looks like a search that found nothing.
 * - **Collapse state is remembered** across navigation (`rememberSaveable`), so walking into a
 *   scenario and back does not re-collapse the section the tester is working through.
 * - **A feature toggle sits in its section's header, and nowhere else** — the one rule the design
 *   contract states twice. The scenario screens and Debug info *read*
 *   these two switches; Config never carries them. See [FeatureToggle].
 * - **The toggle stays visible when its section is collapsed.** It is part of the header, not of
 *   the body: a QA run resolves `scenarios-toggle-*` from a cold start, where every section is
 *   closed, and a toggle that first needs a section expanded is a toggle that is not there.
 *
 * - **An anchored open lands on its section.** [anchorSection] (a scenario's "Change it in
 *   Scenarios" line) expands that section and scrolls its header — and so the switch under it — to
 *   the top, once per list entry; the entry is replaced, so the other sections are back to collapsed.
 *
 * No ViewModel: search and collapse are transient UI state. The SDK-method state lives in the
 * shared [com.octopuscommunity.sdk.sample.ui.scenarios.ScenariosViewModel] instance hoisted by
 * `MainScaffold`, and the toggles live in the persisted sample config.
 *
 * [ScenarioStatus.PENDING] scenarios are filtered out entirely — they have nothing to show, and a
 * card that opens an empty screen is worse than no card. Status carries no badge on the card
 * (removed in the internal change review: BUILT/PARTIAL was noise to a tester).
 *
 * Exactly six section headers, the six the design contract names — no more. The live read-out of
 * the backend config used to be a seventh accordion and "Unified Profile" an eighth nested inside
 * it, which made a tester count eight sections and look for two of them in the contract. The
 * read-out is not a scenario, so it is now a plain panel above the sections, outside the accordion
 * grammar entirely; Unified Profile *is* a sign-in behaviour, so it sits inside "Sign-in & user"
 * with the scenarios it belongs to.
 */
@Composable
fun ScenariosListScreen(
    apiKey: String,
    onScenarioClick: (scenarioId: String) -> Unit,
    toggles: FeatureToggles = FeatureToggles.Default,
    unifiedProfileActive: Boolean = false,
    onSetFeatureToggle: ((FeatureToggle, Boolean) -> Unit)? = null,
    anchorSection: ScenarioSection? = null,
    sdkStatus: SampleSdkStatus = SampleSdkStatus.SignedIn,
    onConnectUser: () -> Unit = {},
    onOpenCommunityTab: () -> Unit = {},
    onOpenCommunityModal: () -> Unit = {},
    onOpenCommunitySheet: () -> Unit = {},
    onOpenCommunityFullScreen: () -> Unit = {},
) {
    var query by rememberSaveable { mutableStateOf("") }
    val expanded = rememberSaveable(saver = expandedSectionsSaver) { mutableStateOf(setOf<String>()) }
    val listState = rememberLazyListState()

    val sections =
        remember(query, unifiedProfileActive) {
            ScenarioCatalog
                .liveSections(unifiedProfileActive)
                .mapValues { (_, specs) -> specs.filter { it.matches(query) } }
                .filterValues { it.isNotEmpty() }
        }

    // Applied once per list entry: coming Back to this list from a scenario must not re-open a
    // section the tester has since collapsed. A new anchored open arrives as a new entry
    // (MainScaffold replaces it), with this flag back to false.
    var anchorApplied by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(anchorSection) {
        if (anchorSection == null || anchorApplied) return@LaunchedEffect
        anchorApplied = true
        expanded.value = expanded.value + anchorSection.name
        val index =
            scenarioSectionItemIndex(
                sections = sections.keys.toList(),
                target = anchorSection,
                hasLivePanel = query.isBlank(),
            ) ?: return@LaunchedEffect
        withFrameNanos { }
        listState.animateScrollToItem(index)
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = SampleSpacing.gutter)
                .padding(top = SampleSpacing.space4)
                .testTag("scenarios-screen"),
        verticalArrangement = Arrangement.spacedBy(SampleSpacing.space4),
    ) {
        // Above the search, outside the list: the state every scenario below runs in, still on
        // screen while a search narrows the list.
        ScenariosStatusBanner(status = sdkStatus, onConnectUser = onConnectUser)

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search scenarios") },
            leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().testTag("scenarios-search-input"),
            singleLine = true,
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = SampleSpacing.space6),
            verticalArrangement = Arrangement.spacedBy(SampleSpacing.space3),
        ) {
            // Live state, not a scenario: a tinted panel like every other sample-layer block, with
            // no section header of its own. Hidden while searching — a search is a hunt for a
            // scenario, and this panel is never the answer.
            //
            // "Open the community" shares that item: it is the first thing a tester does, it is
            // not one of the catalog's scenarios (each container keeps its own scenario card in
            // "Presentation"), and one item 0 keeps scenarioSectionItemIndex's arithmetic intact.
            if (query.isBlank()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(SampleSpacing.space3)) {
                        OpenCommunityCard(
                            onOpenTab = onOpenCommunityTab,
                            onOpenModal = onOpenCommunityModal,
                            onOpenSheet = onOpenCommunitySheet,
                            onOpenFullScreen = onOpenCommunityFullScreen,
                        )
                        Column(modifier = Modifier.testTag("scenarios-live-community-config")) {
                            CommunityConfigPreview(apiKey = apiKey)
                        }
                    }
                }
            }

            sections.forEach { (section, specs) ->
                item(key = section.name) {
                    val isOpen = query.isNotBlank() || section.name in expanded.value
                    ScenarioSectionBlock(
                        // A search that filters a section out (or back in) slides the others
                        // into place instead of jumping.
                        modifier = Modifier.animateItem(),
                        section = section,
                        specs = specs,
                        isExpanded = isOpen,
                        // A search result is opened *for* the tester; toggling then applies to the
                        // section's own remembered state, not to the search.
                        onToggle = {
                            expanded.value =
                                if (section.name in expanded.value) {
                                    expanded.value - section.name
                                } else {
                                    expanded.value + section.name
                                }
                        },
                        featureToggle = section.featureToggle,
                        toggles = toggles,
                        onSetFeatureToggle = onSetFeatureToggle,
                        onScenarioClick = onScenarioClick,
                    )
                }
            }

            if (sections.isEmpty()) {
                item {
                    Text(
                        text = "No scenario matches \"$query\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * The guest and unreachable bands: the two states in which most scenarios below fail or read-only,
 * said once at the top instead of discovered one failed call at a time. Driven by the same
 * [SampleSdkStatus] as Settings and Debug info.
 */
@Composable
private fun ScenariosStatusBanner(
    status: SampleSdkStatus,
    onConnectUser: () -> Unit,
) {
    AnimatedVisibility(
        visible = status == SampleSdkStatus.Unreachable,
        enter = SampleMotion.reveal,
        exit = SampleMotion.conceal,
    ) {
        SampleBanner(
            tone = SampleBannerTone.Error,
            title = "Community unreachable",
            message = SampleSdkStatus.Unreachable.detail,
            inline = true,
            modifier = Modifier.testTag("scenarios-band-unreachable"),
        )
    }
    AnimatedVisibility(
        visible = status == SampleSdkStatus.Guest,
        enter = SampleMotion.reveal,
        exit = SampleMotion.conceal,
    ) {
        SampleBanner(
            tone = SampleBannerTone.Warning,
            title = "Guest session",
            message = SampleSdkStatus.Guest.detail,
            actionLabel = "Connect",
            onAction = onConnectUser,
            inline = true,
            modifier = Modifier.testTag("scenarios-band-guest"),
            actionModifier = Modifier.testTag("scenarios-band-guest-connect"),
        )
    }
}

/**
 * The first scenario of the DS: open the community in each of the four containers a host offers
 * — its own tab, a full-screen modal with a close button, a bottom sheet, a full-screen route
 * pushed over the tab bar — so the difference between them is a tap apart. Two rows of two, so
 * "Full screen" never has to squeeze into a quarter of the card.
 */
@Composable
private fun OpenCommunityCard(
    onOpenTab: () -> Unit,
    onOpenModal: () -> Unit,
    onOpenSheet: () -> Unit,
    onOpenFullScreen: () -> Unit,
) {
    SampleScenarioBlock(
        title = "Open the community",
        icon = Icons.Default.Forum,
        tryTone = true,
        modifier = Modifier.testTag("scenarios-open-community-card"),
    ) {
        Text(
            text = "The same community, in the container your app gives it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryPillButton(
                    text = "Tab",
                    onClick = onOpenTab,
                    modifier = Modifier.weight(1f).testTag("open-community-tab-button"),
                )
                SecondaryPillButton(
                    text = "Modal",
                    onClick = onOpenModal,
                    modifier = Modifier.weight(1f).testTag("open-community-modal-button"),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryPillButton(
                    text = "Sheet",
                    onClick = onOpenSheet,
                    modifier = Modifier.weight(1f).testTag("open-community-sheet-button"),
                )
                SecondaryPillButton(
                    text = "Full screen",
                    onClick = onOpenFullScreen,
                    modifier = Modifier.weight(1f).testTag("open-community-fullscreen-button"),
                )
            }
        }
    }
}

/**
 * The [LazyColumn] item index of [target]'s section header, or `null` when the section is not
 * listed (a search filtered it out). [sections] is the displayed order; [hasLivePanel] is whether
 * the live community-config panel occupies item 0 (it is hidden while searching).
 */
internal fun scenarioSectionItemIndex(
    sections: List<ScenarioSection>,
    target: ScenarioSection,
    hasLivePanel: Boolean,
): Int? =
    sections
        .indexOf(target)
        .takeIf { it >= 0 }
        ?.plus(if (hasLivePanel) 1 else 0)

@Composable
private fun ScenarioSectionBlock(
    modifier: Modifier = Modifier,
    section: ScenarioSection,
    specs: List<ScenarioSpec>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    featureToggle: FeatureToggle?,
    toggles: FeatureToggles,
    onSetFeatureToggle: ((FeatureToggle, Boolean) -> Unit)?,
    onScenarioClick: (String) -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            shape = RoundedCornerShape(12.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable(onClick = onToggle)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("scenarios-section-${section.name.lowercase()}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = section.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (featureToggle != null && onSetFeatureToggle != null) {
                        Text(
                            text = "${featureToggle.label}: ${featureToggle.effectLine(toggles[featureToggle])}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                CountPill(count = specs.size)
                if (featureToggle != null && onSetFeatureToggle != null) {
                    Switch(
                        checked = toggles[featureToggle],
                        onCheckedChange = { onSetFeatureToggle(featureToggle, it) },
                        colors = sampleSwitchColors(),
                        modifier = Modifier.testTag(featureToggle.testTag),
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                )
            }
        }

        AnimatedVisibility(visible = isExpanded, enter = SampleMotion.reveal, exit = SampleMotion.conceal) {
            val bodyTopGap = SampleSpacing.space2
            Column(
                modifier = Modifier.padding(top = bodyTopGap),
                verticalArrangement = Arrangement.spacedBy(SampleSpacing.space2),
            ) {
                specs.forEach { spec ->
                    ScenarioCard(spec = spec, onClick = { onScenarioClick(spec.id) })
                }
            }
        }
    }
}

/** How many scenarios the section holds — the filtered count while a search is running. */
@Composable
private fun CountPill(count: Int) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun ScenarioCard(
    spec: ScenarioSpec,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(1.dp, sampleBorderColor, RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = SampleSpacing.space4, vertical = SampleSpacing.space3)
                .testTag(spec.cardTestId),
        verticalArrangement = Arrangement.spacedBy(SampleSpacing.space1),
    ) {
        Text(
            text = spec.title,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.5.sp),
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = spec.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        ApiPill(spec.api)
    }
}

/** Search hits the API name too — that is often the only word an integrator knows to type. */
private fun ScenarioSpec.matches(query: String): Boolean =
    query.isBlank() ||
        title.contains(query, ignoreCase = true) ||
        subtitle.contains(query, ignoreCase = true) ||
        api.contains(query, ignoreCase = true) ||
        id.contains(query, ignoreCase = true)

private val expandedSectionsSaver =
    androidx.compose.runtime.saveable.Saver<
        androidx.compose.runtime.MutableState<Set<String>>,
        List<String>,
    >(
        save = { it.value.toList() },
        restore = { androidx.compose.runtime.mutableStateOf(it.toSet()) },
    )
