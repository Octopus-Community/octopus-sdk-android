package com.octopuscommunity.sdk.domain.usecase

import com.octopuscommunity.sdk.domain.model.Sessions.Session
import com.octopuscommunity.sdk.domain.model.UserConfig
import com.octopuscommunity.sdk.domain.network.getOrElse
import com.octopuscommunity.sdk.domain.network.onSuccess
import com.octopuscommunity.sdk.domain.repository.CommunityConfigRepository
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository
import com.octopuscommunity.sdk.domain.repository.SessionRepository
import com.octopuscommunity.sdk.domain.repository.UserRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Resets per-process state and resolves community access when the host app process is created.
 *
 * Clears translation toggles and played videos and restores muted playback on the stored session.
 * Fetches the community config, whatever the access: it is how the SDK learns that Octopus closed
 * the community. Alongside, so that neither waits for the other, fetches the user's community access;
 * when access is granted it also refreshes the profile. A closure stored by an earlier launch keeps
 * both from calling the server.
 * When the access fetch fails, it falls back to the cached [UserConfig].
 * The app session is created by [StartAppSessionUseCase] on `AppStarted`, so `AppCreated` cannot
 * leave a never-entered session that would be closed or consume the first-session flag.
 */
class CreateAppSessionUseCase(
    val sessionRepository: SessionRepository,
    val userRepository: UserRepository,
    val connectionRepository: ConnectionRepository,
    val communityConfigRepository: CommunityConfigRepository,
) {
    /** Outcome of [CreateAppSessionUseCase]. */
    data class Result(
        /** The stored app session after the per-process reset, or `null` before the first start. */
        val session: Session.App?,
        /** Whether the user can access the community. */
        val canAccessCommunity: Boolean,
        /** Message to show when [canAccessCommunity] is `false`, if the backend provided one. */
        val accessDeniedMessage: String?,
    ) {
        /** Builds a result from [session] and the access fields of [userConfig]. */
        constructor(session: Session.App?, userConfig: UserConfig) : this(
            session = session,
            canAccessCommunity = userConfig.canAccessCommunity,
            accessDeniedMessage = userConfig.accessDeniedMessage,
        )
    }

    suspend operator fun invoke(): Result {
        val session =
            sessionRepository.updateAppSession {
                it?.copy(toggledTranslations = emptyMap(), playedVideos = emptyMap(), videosMuted = true)
            }
        return coroutineScope {
            val config = async { communityConfigRepository.fetchCommunityConfig() }
            val userConfig =
                userRepository
                    .fetchCommunityAccess()
                    .onSuccess { result ->
                        if (result.canAccessCommunity) {
                            connectionRepository.fetchProfile()
                        }
                    }.getOrElse {
                        userRepository.getUserConfig() ?: UserConfig()
                    }
            config.await()
            Result(session = session, userConfig = userConfig)
        }
    }
}
