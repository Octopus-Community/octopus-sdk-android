package com.octopuscommunity.sdk.ui.consent

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.components.OctopusLinkedText2
import com.octopuscommunity.sdk.ui.components.OctopusMarkdownText

/** What one consent spot displays. */
internal sealed interface ConsentWording {
    /**
     * The SDK's translated wording. Its markdown links are mapped, in order, to the fixed legal
     * actions the spot provides (terms, privacy policy, community guidelines).
     */
    data class Sdk(
        @StringRes val resId: Int,
    ) : ConsentWording

    /** A markdown wording written by the community. Its links open the URL written in it. */
    data class Custom(
        val markdown: String,
    ) : ConsentWording
}

/**
 * The wording of every consent spot for one contribution. Each spot is resolved on its own, so a
 * community can replace some wordings and keep the SDK's for the others.
 */
@Immutable
internal data class ConsentWordings(
    /** Notice under the post / comment / reply editor, implicit mode. */
    val implicitNotice: ConsentWording,
    /** Terms checkbox, one-checkbox-per-document mode. */
    val termsLabel: ConsentWording,
    /** Privacy policy checkbox, one-checkbox-per-document mode. */
    val privacyLabel: ConsentWording,
    /** Community guidelines checkbox, one-checkbox-per-document mode. */
    val rulesLabel: ConsentWording,
    /** The checkbox of the single-checkbox mode. */
    val singleLabel: ConsentWording,
    /** The text under the single checkbox. */
    val singleBelowText: ConsentWording,
) {
    companion object {
        /** The SDK's notice under the editors, the same for posts, comments and replies. */
        val sdkImplicitNotice =
            ConsentWording.Sdk(R.string.content_create_legal_term_of_use_privacy_policy_community_guidelines)

        /** The SDK wording of every spot; only the text under the single checkbox depends on [contribution]. */
        fun sdk(contribution: ConsentContribution) =
            ConsentWordings(
                implicitNotice = sdkImplicitNotice,
                termsLabel = ConsentWording.Sdk(R.string.consent_accept_terms),
                privacyLabel = ConsentWording.Sdk(R.string.consent_accept_privacy),
                rulesLabel = ConsentWording.Sdk(R.string.consent_accept_rules),
                singleLabel = ConsentWording.Sdk(R.string.consent_accept_terms_rules),
                singleBelowText = ConsentWording.Sdk(contribution.privacyAckRes),
            )
    }
}

/**
 * Displays a [ConsentWording]: the SDK wording through [OctopusLinkedText2], its links bound in
 * order to [sdkLinkActions]; a community wording through [OctopusMarkdownText], its links handing
 * their URL to [onNavigateToUrl].
 */
@Composable
internal fun ConsentWordingText(
    wording: ConsentWording,
    sdkLinkActions: List<() -> Unit>,
    onNavigateToUrl: (url: String) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = OctopusTheme.colorScheme.gray900,
    textAlign: TextAlign? = null,
    style: TextStyle = OctopusTheme.typography.body2,
) {
    when (wording) {
        is ConsentWording.Sdk ->
            OctopusLinkedText2(
                modifier = modifier,
                text = stringResource(wording.resId),
                actions = sdkLinkActions,
                color = color,
                textAlign = textAlign,
                style = style,
            )

        is ConsentWording.Custom ->
            OctopusMarkdownText(
                modifier = modifier,
                text = wording.markdown,
                onLinkClick = onNavigateToUrl,
                color = color,
                textAlign = textAlign,
                style = style,
            )
    }
}
