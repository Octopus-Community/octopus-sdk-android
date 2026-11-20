package com.octopuscommunity.sdk.data.mapper

import com.octopuscommunity.ApiKeyServiceProto
import com.octopuscommunity.sdk.domain.model.CommunityClosure
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/**
 * The closure this config describes, `null` when the community is open.
 *
 * The date the server gives is kept when it is still to come, however close. A date already past (or none) would make
 * the SDK check again at every launch, so it waits a floor instead: a day for a closure, an hour for a temporary
 * disabling, which is expected to end sooner.
 */
internal fun ApiKeyServiceProto.ApiKeyConfig.toCommunityClosure(nowMillis: Long): CommunityClosure? {
    val (kind, message, nextCheckAt) =
        when (disabledCase) {
            ApiKeyServiceProto.ApiKeyConfig.DisabledCase.CLOSED ->
                Triple(CommunityClosure.Kind.Closed, closed.message, closed.nextCheckAt)
            ApiKeyServiceProto.ApiKeyConfig.DisabledCase.TEMPORARILYDISABLED ->
                Triple(CommunityClosure.Kind.TemporarilyDisabled, temporarilyDisabled.message, temporarilyDisabled.nextCheckAt)
            ApiKeyServiceProto.ApiKeyConfig.DisabledCase.DISABLED_NOT_SET, null -> return null
        }
    val floor =
        when (kind) {
            CommunityClosure.Kind.Closed -> 1.days
            CommunityClosure.Kind.TemporarilyDisabled -> 1.hours
        }
    return CommunityClosure(
        kind = kind,
        message = message,
        nextCheckAtMillis = if (nextCheckAt > nowMillis) nextCheckAt else nowMillis + floor.inWholeMilliseconds,
    )
}
