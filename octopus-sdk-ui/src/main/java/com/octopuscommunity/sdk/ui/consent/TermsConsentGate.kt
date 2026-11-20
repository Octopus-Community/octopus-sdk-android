package com.octopuscommunity.sdk.ui.consent

import com.octopuscommunity.sdk.domain.model.CommunityConfig.TermsAcceptanceMode

/**
 * Decides whether a first contribution (post / comment / reply) must present the explicit
 * terms-acceptance consent sheet before proceeding (internal ticket).
 *
 * When this returns `true` the send block short-circuits: it presents the consent sheet and stops,
 * which defers BOTH the nickname choice AND publishing to the post-consent re-entry. That is what
 * guarantees the order **consent → nickname choice → publish** (the consent sheet is shown before the
 * nickname choice, not after it).
 *
 * On the re-entry, [consentGivenThisSession] is `true`, so this returns `false` and the block goes
 * on: to the nickname choice when the member's nickname is not confirmed yet
 * (`shouldPresentNicknameChoice`), then to publishing.
 *
 * Implicit mode never shows the sheet; an already-accepted user is never re-prompted.
 */
internal fun shouldPresentConsentSheet(
    mode: TermsAcceptanceMode,
    hasAcceptedCgu: Boolean,
    consentGivenThisSession: Boolean,
): Boolean = mode.isExplicit && !hasAcceptedCgu && !consentGivenThisSession
