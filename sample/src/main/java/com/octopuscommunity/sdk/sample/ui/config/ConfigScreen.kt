package com.octopuscommunity.sdk.sample.ui.config

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.octopuscommunity.sdk.domain.model.ProfileField
import com.octopuscommunity.sdk.sample.BuildConfig
import com.octopuscommunity.sdk.sample.data.SDKConfigResolver
import com.octopuscommunity.sdk.sample.data.availableAuthPresets
import com.octopuscommunity.sdk.sample.data.model.FeatureToggle
import com.octopuscommunity.sdk.sample.data.model.SDKConfig
import com.octopuscommunity.sdk.sample.data.pickerUserIds
import com.octopuscommunity.sdk.sample.data.resolveAuthPreset
import com.octopuscommunity.sdk.sample.extensions.LocalConfigOptions
import com.octopuscommunity.sdk.sample.ui.components.ExpandableSection
import com.octopuscommunity.sdk.sample.ui.components.PrimaryPillButton
import com.octopuscommunity.sdk.sample.ui.components.SampleSegmentedChoice
import com.octopuscommunity.sdk.sample.ui.components.SampleSwitchRow
import com.octopuscommunity.sdk.sample.ui.components.SampleTopAppBar
import com.octopuscommunity.sdk.sample.ui.components.SampleTopAppBarSize
import com.octopuscommunity.sdk.sample.ui.components.SecondaryPillButton
import com.octopuscommunity.sdk.sample.ui.components.SummaryRow
import com.octopuscommunity.sdk.sample.ui.components.sampleFilterChipBorder
import com.octopuscommunity.sdk.sample.ui.components.sampleFilterChipColors
import com.octopuscommunity.sdk.sample.ui.theme.SampleSpacing
import com.octopuscommunity.sdk.sample.ui.theme.SampleTheme
import com.octopuscommunity.sdk.sample.ui.theme.samplePageColor
import com.octopuscommunity.sdk.sample.ui.theme.samplePageContentColor
import kotlinx.serialization.json.JsonObject
import org.koin.androidx.compose.koinViewModel

/**
 * The SDK configuration screen, in its two lives.
 *
 * Before the shell exists it is onboarding and its button reads **Start SDK**; reached again from
 * Settings → Configuration with the SDK already up, the same button reads **Apply** —
 * nothing is being started, an already-running SDK is being reconfigured, and the label should not
 * claim otherwise. Either way [onStarted] receives the one-line outcome (restart, no restart,
 * nothing changed) for the caller to show, so the tap is never silent.
 *
 * [section] is the anchor of a link that is about one setting (see [ConfigSection]): that section
 * opens expanded and is scrolled into view once, when the screen is first shown. Every other
 * section keeps its default state, and a later recomposition or rotation does not scroll again.
 *
 * Host app integration (the host callbacks and the URL policy) is not set here any more: it is an
 * integration decision, not something a tester picks before opening the community, so it moved to
 * Settings › Developer tools › Host app integration. The last row of this screen points there, and
 * keeps the `config-section-integration` tag and the [ConfigSection.INTEGRATION] anchor working.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ConfigScreen(
    onStarted: (outcome: String) -> Unit = {},
    isRevisit: Boolean = false,
    section: ConfigSection? = null,
    onOpenHostIntegration: () -> Unit = {},
    onOpenUnifiedProfile: () -> Unit = {},
    viewModel: ConfigViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val unifiedProfileActive by viewModel.unifiedProfileActive.collectAsStateWithLifecycle()

    val anchor = section?.resolve(serverSectionShown = !BuildConfig.IS_PROD_FLAVOR)
    val anchorRequester = remember { BringIntoViewRequester() }
    // Keyed on the anchor: a deep link that lands on an already-shown Config (singleTop reuses the
    // entry and only swaps its arguments) must apply the new anchor, not skip it as already done.
    var anchorShown by rememberSaveable(anchor) { mutableStateOf(false) }
    LaunchedEffect(anchor) {
        if (anchor != null && !anchorShown) {
            // One frame so the anchored section, expanded from its first composition, has been
            // laid out and the scroll range covers it.
            withFrameNanos { }
            anchorRequester.bringIntoView()
            anchorShown = true
        }
    }

    // Every section carries a stable test tag; only the anchored one carries the requester.
    fun sectionModifier(of: ConfigSection): Modifier =
        Modifier
            .testTag("config-section-${of.tag}")
            .then(if (of == anchor) Modifier.bringIntoViewRequester(anchorRequester) else Modifier)

    Surface(
        modifier = Modifier.fillMaxSize().testTag("config-screen"),
        // Transparent over the dark halo the window frame paints (see `SampleWindowFrame`).
        color = samplePageColor,
        contentColor = samplePageContentColor,
    ) {
        // Outer, non-scrolling Column: app bar, then the sections scrolling in the remaining
        // height, then the docked Start SDK / Apply action, which never scrolls away.
        // `imePadding` sits on this outer Column so the keyboard lifts the docked action with the
        // scroll area instead of covering it while a field has focus.
        // DS screen: Config uses the medium bar, its expanded title collapsing into the row as the
        // sections scroll.
        val barScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .imePadding()
                    .nestedScroll(barScrollBehavior.nestedScrollConnection),
        ) {
            // The screen names itself in the app bar like every other sample screen, badge included:
            // Config is a top-level destination (reached from Settings "Configuration"), not a splash.
            SampleTopAppBar(
                title = "Configuration",
                size = SampleTopAppBarSize.Medium,
                scrollBehavior = barScrollBehavior,
            )

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Server environment (visible on demo flavor only, never on prod flavor). One label
                // across the three samples — the "(internal)" qualifier was the flavor gate saying
                // itself twice.
                if (!BuildConfig.IS_PROD_FLAVOR) {
                    ExpandableSection(
                        "Server environment",
                        modifier = sectionModifier(ConfigSection.SERVER),
                        initiallyExpanded = ConfigSection.SERVER.isInitiallyExpanded(anchor),
                    ) {
                        Column(modifier = Modifier.testTag("config-serverEnv-select")) {
                            ServerEnvironmentSection(
                                state = state,
                                onKindChange = viewModel::setServerEnvKind,
                                onHostChange = viewModel::setCustomServerHost,
                                onPortChange = viewModel::setCustomServerPort,
                                localOptions = viewModel.localOptions,
                                onSelectLocalConfiguration = viewModel::selectLocalConfiguration,
                            )
                        }
                    }
                }

                // Community: rarely changed (the preselected key fits most tests), so it sits under
                // the server it belongs to and opens collapsed, its choice summarised in the header.
                // A keyless build has nothing preselected and opens it, since a key must be pasted.
                val resolvedPreset = resolveAuthPreset(state.forceLogin, state.managedFields)
                val localKey = viewModel.localOptions.keySource(state)
                ExpandableSection(
                    "Community",
                    modifier = sectionModifier(ConfigSection.COMMUNITY),
                    initiallyExpanded =
                        ConfigSection.COMMUNITY.isInitiallyExpanded(anchor) || availableAuthPresets.isEmpty(),
                    summary =
                        when {
                            // A directory selection supplies its own key, whatever is picked below.
                            localKey != null -> localKey
                            state.useCustomCommunity ->
                                "Custom API key \u00b7 ${SDKConfigResolver.maskApiKey(state.customApiKey)}"
                            availableAuthPresets.isNotEmpty() -> resolvedPreset.label
                            else -> null
                        },
                ) {
                    Column(modifier = Modifier.testTag("config-apiKeySource-select")) {
                        if (availableAuthPresets.isNotEmpty()) {
                            // Named-key picker (sample-UX parity with the Flutter Config
                            // screen). Picking a preset applies its auth shape below —
                            // the key itself stays derived from that shape, so it can
                            // never disagree with the SSO connection mode.
                            Text(
                                text = "Pick the community to open. The preselected one fits most tests.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            availableAuthPresets.forEach { preset ->
                                RadioRow(
                                    label = preset.label,
                                    selected = !state.useCustomCommunity && preset.id == resolvedPreset.id,
                                    onClick = { viewModel.applyAuthPreset(preset) },
                                )
                            }
                            RadioRow(
                                label = "Custom… (paste any API key)",
                                selected = state.useCustomCommunity,
                                onClick = { viewModel.setUseCustomCommunity(true) },
                            )
                            if (!state.useCustomCommunity) {
                                Text(
                                    text = "Resolved key: ${SDKConfigResolver.maskApiKey(resolvedPreset.key)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            // Keyless build (cold clone / public CI): no named keys
                            // injected — legacy custom-key switch only.
                            SwitchRow(
                                label = "Use custom API key",
                                checked = state.useCustomCommunity,
                                onCheckedChange = viewModel::setUseCustomCommunity,
                            )
                        }

                        if (state.useCustomCommunity) {
                            var apiKeyVisible by rememberSaveable { mutableStateOf(false) }
                            OutlinedTextField(
                                value = state.customApiKey,
                                onValueChange = viewModel::setCustomApiKey,
                                label = { Text("API Key") },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        // Mask the raw key from accessibility services
                                        // (uiautomator, TalkBack) whenever it is visually
                                        // hidden — a plain PasswordVisualTransformation only
                                        // dots out the glyphs, not the a11y node value.
                                        // Keep a single stable semantics node and toggle the
                                        // property inside it (avoids structural churn of the
                                        // semantics tree on each visibility toggle).
                                        .semantics { if (!apiKeyVisible) password() },
                                singleLine = true,
                                visualTransformation =
                                    if (apiKeyVisible) {
                                        VisualTransformation.None
                                    } else {
                                        PasswordVisualTransformation()
                                    },
                                trailingIcon = {
                                    IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                        Icon(
                                            imageVector =
                                                if (apiKeyVisible) {
                                                    Icons.Default.VisibilityOff
                                                } else {
                                                    Icons.Default.Visibility
                                                },
                                            contentDescription =
                                                if (apiKeyVisible) {
                                                    "Hide API key"
                                                } else {
                                                    "Show API key"
                                                },
                                        )
                                    }
                                },
                            )
                        }
                    }
                }

                ExpandableSection(
                    "Connect your users (SSO)",
                    modifier = sectionModifier(ConfigSection.AUTH),
                    initiallyExpanded = ConfigSection.AUTH.isInitiallyExpanded(anchor),
                ) {
                    Column(modifier = Modifier.testTag("config-authMode-select")) {
                        // The SSO user, first under the header as the DS draws it: one chip per
                        // stable test identity, plus the stored id when it is a custom one typed on
                        // Settings → Account. Applied on Start SDK / Apply, not on tap.
                        val ssoUserIds = (pickerUserIds + listOfNotNull(state.ssoUserId)).distinct()
                        FlowRow(
                            modifier = Modifier.testTag("config-sso-user-select"),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ssoUserIds.forEach { id ->
                                val selected = id == state.ssoUserId
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.setSsoUserId(id) },
                                    label = { Text(id) },
                                    leadingIcon =
                                        if (selected) {
                                            { Icon(Icons.Default.Check, contentDescription = null) }
                                        } else {
                                            null
                                        },
                                    colors = sampleFilterChipColors(),
                                    border = sampleFilterChipBorder(selected = selected),
                                    modifier = Modifier.testTag("config-sso-user-$id"),
                                )
                            }
                        }
                        if (availableAuthPresets.isNotEmpty() && !state.useCustomCommunity) {
                            Text(
                                text = "These settings pick the matching demo community above.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        // Force login is a feature toggle, and a feature toggle lives in one
                        // place: the Scenarios section header. Config reads it here — the value
                        // still selects which named API key this configuration resolves to — but
                        // no longer offers a second switch that could disagree with the first.
                        Text(
                            text =
                                "${FeatureToggle.ForceLogin.label} \u2014 " +
                                    FeatureToggle.ForceLogin.readOnlyValueLine(state.forceLogin),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = FeatureToggle.ForceLogin.effectLine(state.forceLogin),
                            style = MaterialTheme.typography.bodySmall,
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        UnifiedProfile(
                            active = unifiedProfileActive,
                            managedFields = state.managedFields,
                            communityLine =
                                unifiedProfileCommunityLine(
                                    forceLogin = state.forceLogin,
                                    managedFields = state.managedFields,
                                    useCustomCommunity = state.useCustomCommunity,
                                    localKey = localKey,
                                ),
                            onOpenScenario = if (isRevisit) onOpenUnifiedProfile else null,
                            onToggleField = viewModel::toggleManagedField,
                        )
                    }
                }

                // Theme, in the vocabulary Settings → Appearance uses: the light/dark choice first,
                // then a preset, not a checkbox — the DS Config screen draws both. Appearance is not
                // part of the SDK re-init key, so changing it on Apply never restarts the SDK.
                ExpandableSection(
                    "Theme",
                    modifier = sectionModifier(ConfigSection.THEME),
                    initiallyExpanded = ConfigSection.THEME.isInitiallyExpanded(anchor),
                ) {
                    Column(
                        modifier = Modifier.testTag("config-theme-select"),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SampleSegmentedChoice(
                            options =
                                listOf(
                                    SDKConfig.Appearance.System to "System",
                                    SDKConfig.Appearance.Light to "Light",
                                    SDKConfig.Appearance.Dark to "Dark",
                                ),
                            selected = state.appearance,
                            onSelect = viewModel::setAppearance,
                            modifier = Modifier.testTag("config-appearance-select"),
                        )
                        SampleSegmentedChoice(
                            options =
                                listOf(
                                    true to "Octopus navy",
                                    false to "SDK default",
                                ),
                            selected = state.theme !is SDKConfig.Theme.SdkDefault,
                            onSelect = viewModel::setThemePreset,
                            modifier = Modifier.testTag("config-theme-preset-select"),
                        )

                        Text(
                            text =
                                "\"SDK default\" hands the SDK no theme — what a fresh " +
                                    "integration looks like.",
                            style = MaterialTheme.typography.bodySmall,
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Host app integration moved to Developer tools (see the KDoc above). This row keeps
                // the section's tag and anchor, so an old `?section=INTEGRATION` link still lands on
                // something that says where the setting went.
                SummaryRow(
                    icon = Icons.Default.Tune,
                    title = "Host app integration",
                    subtitle = "Profile, sign-in, links: now in Settings › Developer tools",
                    onClick = onOpenHostIntegration,
                    modifier = sectionModifier(ConfigSection.INTEGRATION).padding(top = SampleSpacing.space2),
                )

                Spacer(Modifier.height(16.dp))
            }

            // DS screen 01: the primary action is docked under the scroll, on `surface` with a
            // hairline above it, so "Start SDK" is reachable whatever section is open.
            val hairline = SampleTheme.colors.hairline
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(SampleTheme.colors.surface)
                        .drawBehind { drawRect(hairline, size = Size(size.width, 1.dp.toPx())) }
                        .padding(horizontal = SampleSpacing.space4, vertical = SampleSpacing.space3),
            ) {
                var showProdConfirm by rememberSaveable { mutableStateOf(false) }
                PrimaryPillButton(
                    text = if (isRevisit) "Apply" else "Start SDK",
                    onClick = {
                        if (state.serverEnvKind != ConfigViewModel.ServerEnvKind.DEFAULT) {
                            showProdConfirm = true
                        } else {
                            onStarted(viewModel.saveAndStart())
                        }
                    },
                    minHeight = 56,
                    // DS card 01 leads Start SDK with `play`; Apply reconfigures a running SDK and
                    // starts nothing, so it keeps the bare label.
                    icon = if (isRevisit) null else Icons.Filled.PlayArrow,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag("config-start-button"),
                )

                if (showProdConfirm) {
                    NonDefaultServerConfirmDialog(
                        serverEnvKind = state.serverEnvKind,
                        customHost = state.customServerHost,
                        onConfirm = {
                            showProdConfirm = false
                            onStarted(viewModel.saveAndStart())
                        },
                        onDismiss = { showProdConfirm = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun ServerEnvironmentSection(
    state: ConfigViewModel.State,
    onKindChange: (ConfigViewModel.ServerEnvKind) -> Unit,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    localOptions: LocalConfigOptions,
    onSelectLocalConfiguration: (JsonObject?) -> Unit,
) {
    Text(
        text =
            "Keep the demo server unless another environment is required. Prod and custom hosts are " +
                "for controlled testing — never publish test content on a live community.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    RadioRow(
        label = "Demo server (default)",
        selected = state.serverEnvKind == ConfigViewModel.ServerEnvKind.DEFAULT,
        onClick = { onKindChange(ConfigViewModel.ServerEnvKind.DEFAULT) },
    )
    RadioRow(
        label = "Prod (api.8pus.io)",
        selected = state.serverEnvKind == ConfigViewModel.ServerEnvKind.PROD,
        onClick = { onKindChange(ConfigViewModel.ServerEnvKind.PROD) },
    )
    RadioRow(
        label = "Custom host",
        selected = state.serverEnvKind == ConfigViewModel.ServerEnvKind.CUSTOM,
        onClick = { onKindChange(ConfigViewModel.ServerEnvKind.CUSTOM) },
    )

    if (state.serverEnvKind == ConfigViewModel.ServerEnvKind.CUSTOM) {
        OutlinedTextField(
            value = state.customServerHost,
            onValueChange = onHostChange,
            label = { Text("Host") },
            placeholder = { Text("api.example.com") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = state.customServerPort,
            onValueChange = onPortChange,
            label = { Text("Port") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
    }

    // A directory selection supplies both the endpoint and the matching community key.
    // It is a separate mode from entering a custom host.
    localOptions.ServerOptions(state, onKindChange, onSelectLocalConfiguration)

    if (state.serverEnvKind != ConfigViewModel.ServerEnvKind.DEFAULT) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
            Text(
                text =
                    "Non-default server selected — client communities may be reachable. " +
                        "DO NOT publish any test content.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun NonDefaultServerConfirmDialog(
    serverEnvKind: ConfigViewModel.ServerEnvKind,
    customHost: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = { Text("Non-default server") },
        text = {
            val target =
                when (serverEnvKind) {
                    ConfigViewModel.ServerEnvKind.PROD -> "api.8pus.io (PROD)"
                    ConfigViewModel.ServerEnvKind.CUSTOM -> customHost.ifBlank { "custom" }
                    // A directory selection can also reach live communities, so the warning applies here too.
                    ConfigViewModel.ServerEnvKind.LOCAL -> "a selected backend environment"
                    ConfigViewModel.ServerEnvKind.DEFAULT -> "default"
                }
            Text(
                text =
                    "You are about to connect the SDK to $target. " +
                        "Client communities may be reachable.\n\n" +
                        "DO NOT publish any test content. Continue only for controlled testing.",
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("I understand, start") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SampleSwitchRow(
        title = label,
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
    )
}

@Composable
internal fun RadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    // The whole row selects, label included, and reads as one radio button to TalkBack.
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Without its own click the button drops its 48dp box: restore it to keep the row's layout.
        RadioButton(selected = selected, onClick = null, modifier = Modifier.minimumInteractiveComponentSize())
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Read-only Config entry: the dedicated scenario owns the switch; field subsets stay editable here. */
@Composable
private fun UnifiedProfile(
    active: Boolean,
    managedFields: Set<ProfileField>,
    communityLine: String?,
    onOpenScenario: (() -> Unit)?,
    onToggleField: (ProfileField) -> Unit,
) {
    val on = managedFields.isNotEmpty()
    Text(
        text = "Unified profile: ${if (active) "On — host profile navigation" else "Off — native Octopus profiles"}",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.testTag("config-unified-profile-status"),
    )
    if (onOpenScenario != null) {
        SecondaryPillButton(text = "Open Unified Profile scenario", onClick = onOpenScenario)
    } else {
        Text("Start the SDK to try the Unified Profile scenario.", style = MaterialTheme.typography.bodySmall)
    }
    // Managed fields also select a demo key, so expose that choice alongside the field editor.
    if (communityLine != null) {
        Text(
            text = communityLine,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("config-unified-profile-community"),
        )
    }
    if (!on) return
    var detailOpen by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button) { detailOpen = !detailOpen }
                .semantics { stateDescription = if (detailOpen) "Expanded" else "Collapsed" }
                .testTag("config-unified-profile-detail"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Fields the app owns",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${managedFields.size} of ${ProfileField.ALL.size}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            imageVector = if (detailOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (detailOpen) {
        ProfileField.ALL.forEach { field ->
            CheckboxRow(
                label = field.displayName,
                apiName = field.name,
                checked = field in managedFields,
                onCheckedChange = { onToggleField(field) },
            )
        }
    }
}

/** The key selected by the managed-field configuration, beside its read-only Unified Profile entry. */
private fun unifiedProfileCommunityLine(
    forceLogin: Boolean,
    managedFields: Set<ProfileField>,
    useCustomCommunity: Boolean,
    localKey: String?,
): String? {
    if (localKey != null) {
        return "Selected environment: its key is used ($localKey). Managed fields change who owns the " +
            "profile. Unified Profile is tried from its dedicated scenario."
    }
    if (useCustomCommunity) {
        return "Custom API key: the key is kept. Managed fields change who owns the profile. " +
            "Try Unified Profile from its dedicated scenario."
    }
    if (availableAuthPresets.isEmpty()) return null
    val current = resolveAuthPreset(forceLogin, managedFields)
    return "Key: \u201c${current.label}\u201d. The presets are keys on the same demo community; " +
        "changing managed fields may select another key when applied. " +
        "The Unified Profile scenario wires profile navigation and forces the flag while on."
}

/** A checkbox with its human label first and the SDK symbol under it, as the doc names both. */
@Composable
private fun CheckboxRow(
    label: String,
    apiName: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Column {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = apiName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The profile field as a tester reads it; the enum name stays visible as the API symbol. */
private val ProfileField.displayName: String
    get() =
        when (this) {
            ProfileField.NICKNAME -> "Nickname"
            ProfileField.BIO -> "Bio"
            ProfileField.PICTURE -> "Profile picture"
        }
