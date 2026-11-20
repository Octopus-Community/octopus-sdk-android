package com.octopuscommunity.sdk.ui.consent

import com.octopuscommunity.sdk.domain.model.CommunityConfig.ConsentTexts
import com.octopuscommunity.sdk.domain.model.CommunityConfig.TermsAcceptanceMode

/**
 * The wording of every consent spot for [contribution].
 *
 * [mode] — the effective terms-acceptance mode, debug override included — decides: the community's
 * [consentTexts] only apply when their case is the one of [mode]; otherwise every spot keeps the SDK
 * wording. Within the matching case, each empty field keeps the SDK wording of that spot alone. The
 * text under the single checkbox replaces the SDK's per-contribution acknowledgement.
 */
internal fun resolveConsentWordings(
    mode: TermsAcceptanceMode,
    consentTexts: ConsentTexts?,
    contribution: ConsentContribution,
): ConsentWordings {
    val sdk = ConsentWordings.sdk(contribution)
    val multi =
        (consentTexts as? ConsentTexts.MultiCheckbox)
            ?.takeIf { mode == TermsAcceptanceMode.EXPLICIT_MULTI_CHECKBOX }
    val single =
        (consentTexts as? ConsentTexts.SingleCheckbox)
            ?.takeIf { mode == TermsAcceptanceMode.EXPLICIT_SINGLE_CHECKBOX }
    return ConsentWordings(
        implicitNotice = resolveImplicitNotice(mode = mode, consentTexts = consentTexts),
        termsLabel = multi?.termsLabel.orSdk(sdk.termsLabel),
        privacyLabel = multi?.privacyLabel.orSdk(sdk.privacyLabel),
        rulesLabel = multi?.rulesLabel.orSdk(sdk.rulesLabel),
        singleLabel = single?.label.orSdk(sdk.singleLabel),
        singleBelowText = single?.belowText.orSdk(sdk.singleBelowText),
    )
}

/**
 * The notice under the post / comment / reply editor: the community's when [mode] is
 * [TermsAcceptanceMode.IMPLICIT] and it set a non-empty one, the SDK's otherwise. Same for every
 * contribution.
 */
internal fun resolveImplicitNotice(
    mode: TermsAcceptanceMode,
    consentTexts: ConsentTexts?,
): ConsentWording {
    val implicit = (consentTexts as? ConsentTexts.Implicit)?.takeIf { mode == TermsAcceptanceMode.IMPLICIT }
    return implicit?.notice.orSdk(ConsentWordings.sdkImplicitNotice)
}

private fun String?.orSdk(fallback: ConsentWording): ConsentWording = if (isNullOrBlank()) fallback else ConsentWording.Custom(this)
