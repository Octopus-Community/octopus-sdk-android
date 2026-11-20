package com.octopuscommunity.sdk

import com.octopuscommunity.sdk.domain.model.CommunityClosure
import com.octopuscommunity.sdk.domain.model.SDKEvent
import com.octopuscommunity.sdk.domain.model.SessionEvent
import com.octopuscommunity.sdk.domain.network.onSuccess
import com.octopuscommunity.sdk.domain.repository.CommunityClosureRepository
import com.octopuscommunity.sdk.domain.repository.CommunityConfigRepository
import com.octopuscommunity.sdk.domain.repository.ConnectionRepository
import com.octopuscommunity.sdk.domain.repository.GroupRepository
import com.octopuscommunity.sdk.domain.repository.RootFeedRepository
import com.octopuscommunity.sdk.domain.repository.UserRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

/**
 * Checks a closed community again each time the app comes back to the foreground once the date to check it has come,
 * so that a reopening is seen without waiting for the next launch. An open community is not asked anything more: its
 * config is fetched once per launch.
 *
 * Suspends for as long as [events] is collected.
 */
internal suspend fun checkClosedCommunityWhenTheAppComesBack(
    events: Flow<SDKEvent>,
    closureRepository: CommunityClosureRepository,
    communityConfigRepository: CommunityConfigRepository,
    nowMillis: () -> Long = System::currentTimeMillis,
) {
    coroutineScope {
        // Launched rather than awaited: the events are not delivered past this collector until it returns, and the
        // check can wait long for the server.
        events.filterIsInstance<SessionEvent.AppStarted>().collect {
            if (closureRepository.isCheckDue(nowMillis())) {
                launch { communityConfigRepository.fetchCommunityConfig() }
            }
        }
    }
}

/**
 * Checks the config when the server refuses a call because the community is closed, while it is open: a community
 * closed while the app runs first shows that way, and the config tells until when. The calls running when it closes are
 * all refused: one check at a time answers them all.
 *
 * Suspends for as long as [communityClosedRefusals] is collected.
 */
internal suspend fun checkTheConfigWhenTheServerNamesAClosure(
    communityClosedRefusals: Flow<Unit>,
    closureRepository: CommunityClosureRepository,
    communityConfigRepository: CommunityConfigRepository,
) {
    coroutineScope {
        var check: Job? = null
        communityClosedRefusals.collect {
            // Calls are blocked while closed: only the check due at its date reaches the server.
            if (closureRepository.isClosed || check?.isActive == true) return@collect
            // Launched, as the check of a closed community: the server can take long to answer.
            check = launch { communityConfigRepository.fetchCommunityConfig() }
        }
    }
}

/** The least time between two reads of the access asked by calls the server refused to the user. */
internal const val MIN_INTERVAL_BETWEEN_REFUSAL_READS_MILLIS = 30_000L

/**
 * Reads the access again when the server refuses a call because the user themself has no access to the community
 * (banned, deleted, excluded, out of the audience): an access taken away while the app runs first shows that way, and
 * the screens follow the access without waiting for the next launch.
 *
 * The same when the server refuses a call because it ended the session it was sent with ([sessionEndedRefusals]):
 * the read is what replaces that session.
 *
 * - The calls of a screen are refused together: at most one read every [minIntervalMillis].
 * - Not while the stored access is already denied: the calls of such a user keep being refused (tracking,
 *   notifications), and the access denied screen reads the access by itself. Except for a session the server ended:
 *   that is the session of a banned installation once the ban is lifted, whose stored access is the ban, and the
 *   read is what lets it leave the ban screen without waiting for that screen to show again.
 * - Not while the community is closed, where the closure is what the screens show, nor without a session.
 *
 * Suspends for as long as the refusals are collected.
 */
internal suspend fun readTheAccessWhenTheServerRefusesTheUser(
    accessRefusals: Flow<Unit>,
    closureRepository: CommunityClosureRepository,
    userRepository: UserRepository,
    sessionEndedRefusals: Flow<Unit> = emptyFlow(),
    nowMillis: () -> Long = System::currentTimeMillis,
    minIntervalMillis: Long = MIN_INTERVAL_BETWEEN_REFUSAL_READS_MILLIS,
) {
    coroutineScope {
        var lastReadAtMillis: Long? = null
        merge(accessRefusals.map { false }, sessionEndedRefusals.map { true }).collect { endsSession ->
            if (closureRepository.isClosed || userRepository.getUser() == null) return@collect
            if (!endsSession && userRepository.getUserConfig()?.canAccessCommunity == false) return@collect
            val now = nowMillis()
            // A clock set back must not hold the reads for longer than the interval.
            lastReadAtMillis?.let { if (now - it in 0 until minIntervalMillis) return@collect }
            lastReadAtMillis = now
            // Launched, as the check of a closed community: the server can take long to answer.
            launch { userRepository.fetchCommunityAccess() }
        }
    }
}

/** Runs [resume] each time the community reopens after a closure. Suspends for as long as [closureFlow] is collected. */
internal suspend fun resumeWhenTheCommunityReopens(
    closureFlow: StateFlow<CommunityClosure?>,
    resume: suspend () -> Unit,
) {
    var wasClosed = closureFlow.value != null
    closureFlow.collect { closure ->
        val closed = closure != null
        if (wasClosed && !closed) resume()
        wasClosed = closed
    }
}

/**
 * Loads, once the community reopened, what a launch loads and the closure skipped, without waiting for the next launch:
 * the connection, the push token, the access and the profile, the groups and the root feeds.
 *
 * The connection goes through the session check, which decides from the stored state: the token of a user connected
 * during the closure is exchanged, a guest is connected only when there is neither a session nor such a user.
 */
internal suspend fun resumeAfterCommunityReopening(
    connectionRepository: ConnectionRepository,
    userRepository: UserRepository,
    groupRepository: GroupRepository,
    rootFeedRepository: RootFeedRepository,
    flushPendingNotificationsToken: suspend () -> Unit,
) {
    connectionRepository.checkConnection()
    flushPendingNotificationsToken()
    userRepository.checkPushNotificationsToken()
    userRepository.fetchCommunityAccess().onSuccess { access ->
        if (access.canAccessCommunity) connectionRepository.fetchProfile()
    }
    groupRepository.fetchGroups()
    // The purge emptied them, and a home already on screen only loads them again for another user.
    rootFeedRepository.fetchRootFeeds()
}
