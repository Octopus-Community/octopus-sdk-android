package com.octopuscommunity.sdk.domain.repository

/**
 * Optional, side-effect-free SSO refusal state exposed by a [ConnectionRepository]. Repositories
 * without SSO ban tracking need not implement this capability.
 */
interface SsoBanState {
    /**
     * Reads the server's last SSO ban message without connecting, refreshing a profile or writing
     * storage. The message is already localized by the server and can be displayed unchanged.
     *
     * The SDK keeps this refusal in memory only. After a cold start it is reapplied when the host
     * calls [ConnectionRepository.connectUser] and the server refuses that user again. A successful
     * exchange, a different client user or [ConnectionRepository.logout] clears it. It does not
     * prevent browsing on an existing guest session.
     */
    fun getUserBannedMessage(): String?
}

/**
 * Reads the optional [SsoBanState] without refreshing the connection. Returns `null` for repository
 * implementations without this capability, including existing, precompiled implementations.
 */
fun ConnectionRepository.getUserBannedMessage(): String? = (this as? SsoBanState)?.getUserBannedMessage()
