package com.octopuscommunity.sdk.sample.ui.scenarios.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.OctopusSDK
import com.octopuscommunity.sdk.domain.model.CommunityConfig
import com.octopuscommunity.sdk.domain.model.CommunityConfig.ConsentTexts
import com.octopuscommunity.sdk.domain.model.CommunityConfig.TermsAcceptanceMode
import com.octopuscommunity.sdk.sample.ui.community.HostOverrides
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * QA tooling for the community's own consent wordings.
 *
 * Each preset applies a DEBUG-only local override of the consent texts AND of the matching
 * terms-acceptance mode — texts only show when their mode is the active one — so every wording can
 * be checked without touching a community's dashboard. Then switch to the Community tab and start a
 * first contribution with a user who has not accepted the terms yet (the terms-acceptance scenario
 * explains how to get one). Every brand and URL here is fictional: this sample ships publicly.
 *
 * Test ids (`qa-preset-consentTexts-N` / `consentTexts-result`) are the shared scenario
 * catalog's, so the QA pipeline drives both platforms with the same ids.
 */
@Composable
fun ConsentTextsSection() {
    // Polled for the same switchCommunity-survival reason as TermsAcceptanceSection.
    val effectiveConfig by produceState<CommunityConfig?>(initialValue = null) {
        while (true) {
            value =
                try {
                    OctopusSDK.communityConfigRepository.getCommunityConfig()
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (e: Exception) {
                    null
                }
            delay(1_000)
        }
    }
    val hasOverride =
        HostOverrides.consentTextsOverride != null || HostOverrides.termsAcceptanceModeOverride != null

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionIntro(
            "Debug-only: show the community's own consent wordings (applied live — switch to the " +
                "Community tab to see them). Each preset also forces its terms-acceptance mode. An " +
                "empty wording keeps the SDK's, and texts written for another mode are ignored.",
        )

        if (hasOverride) {
            OverrideActiveBadge(testId = "consentTexts-override-badge")
        }

        CONSENT_TEXTS_PRESETS.forEach { preset ->
            ModeButton(
                label = preset.label,
                selected =
                    HostOverrides.consentTextsOverride == preset.texts &&
                        HostOverrides.termsAcceptanceModeOverride == preset.mode,
                testId = preset.testId,
            ) {
                HostOverrides.applyTermsAcceptanceModeOverride(preset.mode)
                HostOverrides.applyConsentTextsOverride(preset.texts)
            }
        }
        // Stays in the composition while disabled: the QA pipeline resolves its id from a clean
        // state too (see ModeButton).
        ModeButton(
            label = "Clear overrides (backend texts and mode)",
            selected = false,
            enabled = hasOverride,
            testId = "qa-preset-consentTexts-clear",
        ) {
            HostOverrides.applyConsentTextsOverride(null)
            HostOverrides.applyTermsAcceptanceModeOverride(null)
        }

        val effective =
            effectiveConfig?.let { "${modeLabel(it.termsAcceptanceMode)} · ${describe(it.consentTexts)}" } ?: "…"
        val source = if (HostOverrides.consentTextsOverride != null) "local override" else "backend value"
        Text(
            modifier = Modifier.testTag("consentTexts-result"),
            text = "Effective: $effective  ($source)",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private class ConsentTextsPreset(
    val testId: String,
    val label: String,
    val mode: TermsAcceptanceMode,
    val texts: ConsentTexts,
)

private const val SHORT_NOTICE =
    "By posting, you agree to the [Pinecone Run Club charter](https://example.com/charter)."

private const val LONG_NOTICE =
    "By posting, you agree to the [Pinecone Run Club charter](https://example.com/charter) and to our " +
        "[event rules](https://example.com/events).\n\n" +
        "**Be kind on the trail.** Members who post race results, photos or routes keep the rights to " +
        "them and let the club show them in the app.\n\n" +
        "*Your data:* we keep your posts for as long as your account exists. Read the " +
        "[privacy notice](https://example.com/privacy) to learn how to export or delete them.\n\n" +
        "Questions? Write to the volunteers through the [contact page](https://example.com/contact)."

private val CONSENT_TEXTS_PRESETS =
    listOf(
        ConsentTextsPreset(
            testId = "qa-preset-consentTexts-1",
            label = "Implicit — short notice with a link",
            mode = TermsAcceptanceMode.IMPLICIT,
            texts = ConsentTexts.Implicit(notice = SHORT_NOTICE),
        ),
        ConsentTextsPreset(
            testId = "qa-preset-consentTexts-2",
            label = "Multi checkbox — short labels with links",
            mode = TermsAcceptanceMode.EXPLICIT_MULTI_CHECKBOX,
            texts =
                ConsentTexts.MultiCheckbox(
                    termsLabel = "I accept the **club terms** ([read them](https://example.com/terms))",
                    privacyLabel = "I accept the [privacy notice](https://example.com/privacy)",
                    rulesLabel = "I will follow the [*trail etiquette*](https://example.com/etiquette)",
                ),
        ),
        ConsentTextsPreset(
            testId = "qa-preset-consentTexts-3",
            label = "Single checkbox — short label and text below",
            mode = TermsAcceptanceMode.EXPLICIT_SINGLE_CHECKBOX,
            texts =
                ConsentTexts.SingleCheckbox(
                    label =
                        "I accept the [club terms](https://example.com/terms) and the " +
                            "[trail etiquette](https://example.com/etiquette)",
                    belowText = "Your data is handled as described in our [privacy notice](https://example.com/privacy).",
                ),
        ),
        ConsentTextsPreset(
            testId = "qa-preset-consentTexts-4",
            label = "Implicit — long notice (scrolls past 5 lines)",
            mode = TermsAcceptanceMode.IMPLICIT,
            texts = ConsentTexts.Implicit(notice = LONG_NOTICE),
        ),
        ConsentTextsPreset(
            testId = "qa-preset-consentTexts-5",
            label = "Multi checkbox — long labels, several links",
            mode = TermsAcceptanceMode.EXPLICIT_MULTI_CHECKBOX,
            texts =
                ConsentTexts.MultiCheckbox(
                    termsLabel =
                        "I have read and accept the **Pinecone Run Club membership terms**, including the " +
                            "[event rules](https://example.com/events) and the [refund policy](https://example.com/refunds).\n" +
                            "Membership renews every season.",
                    privacyLabel =
                        "I accept the [privacy notice](https://example.com/privacy). *Race results are public* " +
                            "and stay on the club's leaderboard after I leave.",
                    rulesLabel =
                        "I will follow the [trail etiquette](https://example.com/etiquette): give way uphill, " +
                            "keep dogs on a leash, and take my litter home.\n\nRepeated breaches end a membership.",
                ),
        ),
        ConsentTextsPreset(
            testId = "qa-preset-consentTexts-6",
            label = "Single checkbox — long, multi-paragraph",
            mode = TermsAcceptanceMode.EXPLICIT_SINGLE_CHECKBOX,
            texts =
                ConsentTexts.SingleCheckbox(
                    label =
                        "I accept the **Pinecone Run Club membership terms** ([read them](https://example.com/terms)) " +
                            "and the [trail etiquette](https://example.com/etiquette).",
                    belowText =
                        "We use your posts, photos and race results to run the club.\n\n" +
                            "They stay visible to members until you delete them. Read the " +
                            "[privacy notice](https://example.com/privacy) to export or erase your data, or write " +
                            "to the volunteers through the [contact page](https://example.com/contact).\n\n" +
                            "# This heading stays plain text\n- and so does this list item",
                ),
        ),
        ConsentTextsPreset(
            testId = "qa-preset-consentTexts-7",
            label = "Single checkbox — empty fields (SDK wordings)",
            mode = TermsAcceptanceMode.EXPLICIT_SINGLE_CHECKBOX,
            texts = ConsentTexts.SingleCheckbox(label = "", belowText = "   "),
        ),
        ConsentTextsPreset(
            testId = "qa-preset-consentTexts-8",
            label = "Multi checkbox with implicit texts (SDK wordings)",
            mode = TermsAcceptanceMode.EXPLICIT_MULTI_CHECKBOX,
            texts = ConsentTexts.Implicit(notice = SHORT_NOTICE),
        ),
    )

private fun modeLabel(mode: TermsAcceptanceMode): String =
    when (mode) {
        TermsAcceptanceMode.IMPLICIT -> "implicit"
        TermsAcceptanceMode.EXPLICIT_MULTI_CHECKBOX -> "multi checkbox"
        TermsAcceptanceMode.EXPLICIT_SINGLE_CHECKBOX -> "single checkbox"
    }

private fun describe(texts: ConsentTexts?): String =
    when (texts) {
        null -> "no texts (SDK wordings)"
        is ConsentTexts.Implicit -> "implicit texts · notice ${filled(texts.notice)}"
        is ConsentTexts.MultiCheckbox -> {
            val setCount = listOf(texts.termsLabel, texts.privacyLabel, texts.rulesLabel).count { !it.isNullOrBlank() }
            "multi checkbox texts · $setCount/3 labels set"
        }
        is ConsentTexts.SingleCheckbox ->
            "single checkbox texts · label ${filled(texts.label)}, text below ${filled(texts.belowText)}"
    }

private fun filled(text: String?) = if (text.isNullOrBlank()) "empty" else "set"
