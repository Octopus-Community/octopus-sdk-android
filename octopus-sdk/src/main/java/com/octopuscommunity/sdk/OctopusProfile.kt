package com.octopuscommunity.sdk

import com.octopuscommunity.sdk.domain.model.ClientUser
import com.octopuscommunity.sdk.domain.model.CurrentUserProfile
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository
import com.octopuscommunity.sdk.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * The public-facing profile of the connected user.
 *
 * Exposed via [OctopusSDK.profile]. Future profile fields will be added to this data class —
 * additive only; no breaking changes.
 *
 * Mirrors iOS [`OctopusProfile`](https://github.com/Octopus-Community/octopus-sdk-swift/blob/main/Sources/Octopus/OctopusProfile.swift).
 */
data class OctopusProfile(
    /**
     * Held entitlement identifiers (opaque tokens defined by the host app).
     *
     * Display only — the SDK never intersects this set against per-group requirements. Group
     * access decisions are pre-resolved by the backend and surfaced via [OctopusGroup.canAccess].
     */
    val entitlements: Set<String>,
    /**
     * The connected user's id in your own app's system — the id you passed to
     * [OctopusSDK.connectUser] (Unified Profile).
     *
     * `null` for guests and restored sessions without a host identity.
     */
    val clientUserId: String? = null,
)

/** Maps the connected profile and host identity to the public profile; guests have no host id. */
internal fun octopusProfile(
    profile: CurrentUserProfile?,
    clientUser: ClientUser?,
): OctopusProfile? =
    profile?.let {
        OctopusProfile(
            entitlements = it.entitlements.toSet(),
            clientUserId = clientUser?.userId?.takeUnless { _ -> it.isGuest },
        )
    }

/** Combines the connected profile with its locally stored host identity. */
internal fun octopusProfileFlow(
    connectionRepository: ConnectionRepository,
    profileRepository: ProfileRepository,
): Flow<OctopusProfile?> =
    combine(
        profileRepository.getProfileFlow(),
        connectionRepository.getClientUserFlow(),
    ) { profile, clientUser ->
        octopusProfile(profile = profile, clientUser = clientUser)
    }.distinctUntilChanged()
