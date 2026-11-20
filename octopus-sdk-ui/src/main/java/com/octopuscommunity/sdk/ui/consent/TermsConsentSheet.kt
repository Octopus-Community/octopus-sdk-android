package com.octopuscommunity.sdk.ui.consent

import androidx.annotation.StringRes
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.domain.model.CommunityConfig.TermsAcceptanceMode
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.components.OctopusButton
import com.octopuscommunity.sdk.ui.components.OctopusModalBottomSheet
import com.octopuscommunity.sdk.ui.semiBold

/**
 * The contribution the user is trying to make: it labels the consent sheet's button (see
 * [buttonRes]), and its [privacyAckRes] is the SDK's own wording of the single-checkbox mode's text
 * under the checkbox, the fallback of the community's own text. The texts the sheet shows are the
 * resolved [ConsentWordings], which already carry that fallback.
 */
internal enum class ConsentContribution(
    @StringRes private val publishButtonRes: Int,
    @StringRes val privacyAckRes: Int,
) {
    POST(R.string.consent_button_post, R.string.consent_ack_privacy_post),
    COMMENT(R.string.consent_button_comment, R.string.consent_ack_privacy_comment),
    REPLY(R.string.consent_button_reply, R.string.consent_ack_privacy_reply),
    ;

    /**
     * The label of the consent sheet's button: a plain "Accept" when the nickname choice comes right
     * after the sheet ([nicknameChoiceFollows]), since nothing is published yet then; otherwise
     * "Accept and post / comment / reply", the contribution going out on the tap. It is always the
     * SDK's wording: the community's consent texts never change it.
     */
    @StringRes
    fun buttonRes(nicknameChoiceFollows: Boolean): Int = if (nicknameChoiceFollows) R.string.consent_button_accept else publishButtonRes
}

/**
 * Explicit terms-acceptance bottom sheet shown at the user's first contribution when the community
 * runs an explicit consent mode. Its content differs by [TermsAcceptanceMode] (one checkbox per
 * document vs a single combined checkbox + a text under it); the behaviour (trigger, dismissal,
 * memorization) is identical. The action button is disabled until the required box(es) are checked;
 * tapping it calls [onAccept] (the caller records acceptance, publishes and dismisses). The button
 * reads "Accept" instead of "Accept and post / comment / reply" when [nicknameChoiceFollows]: the
 * nickname choice comes next and the contribution only goes out once the nickname is confirmed.
 *
 * The texts come from [wordings]: the SDK's, or the community's own markdown, whose links hand their
 * URL to [onNavigateToUrl]. [wordings] must be resolved for the same [mode] and [contribution] as the
 * ones passed here, since the sheet displays what it is given and does not resolve it again. The
 * sheet is as tall as its content; when the content is taller than the window allows, the title and
 * checkboxes scroll and the button stays at the bottom.
 *
 * [TermsAcceptanceMode.IMPLICIT] renders nothing — the caller only presents this for explicit modes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TermsConsentSheet(
    visible: Boolean,
    mode: TermsAcceptanceMode,
    contribution: ConsentContribution,
    wordings: ConsentWordings,
    nicknameChoiceFollows: Boolean,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToRules: () -> Unit,
    onNavigateToUrl: (url: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(visible) }

    LaunchedEffect(visible) {
        if (visible) {
            showSheet = true
        } else {
            if (sheetState.isVisible) sheetState.hide()
            showSheet = false
        }
    }

    if (!showSheet || !mode.isExplicit) return

    // The checkbox and scroll states live here, outside the sheet's locale wrapper: a locale override
    // recomposes the wrapper's content in a new group, which would reset anything remembered inside.
    var checkboxes by remember { mutableStateOf(ConsentCheckboxes()) }
    val scrollState = rememberScrollState()

    OctopusModalBottomSheet(
        modifier = modifier,
        sheetState = sheetState,
        onDismissRequest = onDismiss,
    ) {
        TermsConsentSheetContent(
            mode = mode,
            contribution = contribution,
            wordings = wordings,
            nicknameChoiceFollows = nicknameChoiceFollows,
            checkboxes = checkboxes,
            onCheckboxesChange = { checkboxes = it },
            scrollState = scrollState,
            onAccept = onAccept,
            onNavigateToTerms = onNavigateToTerms,
            onNavigateToPrivacy = onNavigateToPrivacy,
            onNavigateToRules = onNavigateToRules,
            onNavigateToUrl = onNavigateToUrl,
        )
    }
}

/** Which checkboxes of the consent sheet are checked. Only the ones of the sheet's mode are read. */
@Immutable
internal data class ConsentCheckboxes(
    val terms: Boolean = false,
    val privacy: Boolean = false,
    val rules: Boolean = false,
    val combined: Boolean = false,
) {
    /** Whether the checked boxes are enough to enable the sheet's action button in [mode]. */
    fun isValid(mode: TermsAcceptanceMode): Boolean =
        when (mode) {
            TermsAcceptanceMode.EXPLICIT_MULTI_CHECKBOX -> terms && privacy && rules
            TermsAcceptanceMode.EXPLICIT_SINGLE_CHECKBOX -> combined
            TermsAcceptanceMode.IMPLICIT -> false
        }
}

/**
 * The sheet's content: one scrollable area (title, checkbox rows, text under the single checkbox)
 * above the action button, which stays outside the scroll. The scrollable area takes its natural
 * height and only shrinks — then scrolls — when the incoming max height cannot also fit the button.
 * It is stateless: [TermsConsentSheet] owns the checkbox and scroll states, so they survive the
 * sheet's locale wrapper recomposing its content.
 */
@Composable
internal fun TermsConsentSheetContent(
    mode: TermsAcceptanceMode,
    contribution: ConsentContribution,
    wordings: ConsentWordings,
    nicknameChoiceFollows: Boolean,
    checkboxes: ConsentCheckboxes,
    onCheckboxesChange: (ConsentCheckboxes) -> Unit,
    scrollState: ScrollState,
    onAccept: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToRules: () -> Unit,
    onNavigateToUrl: (url: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isValid = checkboxes.isValid(mode)

    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(weight = 1f, fill = false)
                    .verticalScroll(scrollState),
        ) {
            Text(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 20.dp, bottom = 12.dp),
                text = stringResource(R.string.consent_title_formal),
                color = OctopusTheme.colorScheme.gray900,
                style = OctopusTheme.typography.body1.semiBold,
            )

            when (mode) {
                TermsAcceptanceMode.EXPLICIT_MULTI_CHECKBOX -> {
                    ConsentCheckboxRow(
                        checked = checkboxes.terms,
                        wording = wordings.termsLabel,
                        sdkLinkActions = listOf(onNavigateToTerms),
                        onNavigateToUrl = onNavigateToUrl,
                        onToggle = { onCheckboxesChange(checkboxes.copy(terms = !checkboxes.terms)) },
                    )
                    ConsentCheckboxRow(
                        checked = checkboxes.privacy,
                        wording = wordings.privacyLabel,
                        sdkLinkActions = listOf(onNavigateToPrivacy),
                        onNavigateToUrl = onNavigateToUrl,
                        onToggle = { onCheckboxesChange(checkboxes.copy(privacy = !checkboxes.privacy)) },
                    )
                    ConsentCheckboxRow(
                        checked = checkboxes.rules,
                        wording = wordings.rulesLabel,
                        sdkLinkActions = listOf(onNavigateToRules),
                        onNavigateToUrl = onNavigateToUrl,
                        onToggle = { onCheckboxesChange(checkboxes.copy(rules = !checkboxes.rules)) },
                    )
                }

                TermsAcceptanceMode.EXPLICIT_SINGLE_CHECKBOX -> {
                    ConsentCheckboxRow(
                        checked = checkboxes.combined,
                        wording = wordings.singleLabel,
                        sdkLinkActions = listOf(onNavigateToTerms, onNavigateToRules),
                        onNavigateToUrl = onNavigateToUrl,
                        onToggle = { onCheckboxesChange(checkboxes.copy(combined = !checkboxes.combined)) },
                    )
                    ConsentWordingText(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        wording = wordings.singleBelowText,
                        sdkLinkActions = listOf(onNavigateToPrivacy),
                        onNavigateToUrl = onNavigateToUrl,
                        color = OctopusTheme.colorScheme.gray900,
                        style = OctopusTheme.typography.body2,
                    )
                }

                TermsAcceptanceMode.IMPLICIT -> Unit
            }
        }

        OctopusButton(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .padding(top = 24.dp, bottom = 16.dp),
            text = stringResource(contribution.buttonRes(nicknameChoiceFollows)),
            enabled = isValid,
            onClick = { if (isValid) onAccept() },
        )
    }
}

/**
 * A checkbox row: tapping a link opens it (SDK wording: its legal action; community wording: the URL
 * written in it); tapping anywhere else on the row toggles the checkbox.
 *
 * The row is one checkbox for accessibility services: the label and the checked state are announced
 * together and a double tap toggles it. The links of the label stay reachable as children of that
 * node. The drawn box carries no semantics of its own, so it is not announced a second time.
 */
@Composable
private fun ConsentCheckboxRow(
    checked: Boolean,
    wording: ConsentWording,
    sdkLinkActions: List<() -> Unit>,
    onNavigateToUrl: (url: String) -> Unit,
    onToggle: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .toggleable(
                    value = checked,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Checkbox,
                    onValueChange = { onToggle() },
                ).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        ConsentCheckbox(checked = checked)
        // `weight(1f)` (not `fillMaxWidth`) so the label takes the space left after the checkbox and
        // wraps to multiple lines instead of overflowing / clipping — the Compose equivalent of the
        // iOS `.frame(maxWidth: .infinity)` on the row label.
        ConsentWordingText(
            modifier = Modifier.weight(1f),
            wording = wording,
            sdkLinkActions = sdkLinkActions,
            onNavigateToUrl = onNavigateToUrl,
            color = OctopusTheme.colorScheme.gray900,
            style = OctopusTheme.typography.body2,
        )
    }
}

/**
 * Squared, rounded-corner checkbox (design system): filled with the primary color + a white
 * checkmark when checked, an outlined 24dp box with a 2dp gray300 border when not. Themeable via
 * `primary` / `onPrimary` / `gray300`.
 */
@Composable
private fun ConsentCheckbox(checked: Boolean) {
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier =
            Modifier
                .size(24.dp)
                .clip(shape)
                .background(if (checked) OctopusTheme.colorScheme.primary else Color.Transparent)
                .border(
                    width = 2.dp,
                    color = if (checked) OctopusTheme.colorScheme.primary else OctopusTheme.colorScheme.gray300,
                    shape = shape,
                ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (checked) {
            Icon(
                modifier = Modifier.size(16.dp),
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = OctopusTheme.colorScheme.onPrimary,
            )
        }
    }
}
