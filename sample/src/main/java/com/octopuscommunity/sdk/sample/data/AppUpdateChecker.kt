package com.octopuscommunity.sdk.sample.data

import android.content.Context
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallException
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallErrorCode
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Asks Play whether a newer sample build is available on the track this build was
 * installed from.
 *
 * Most runs of this class find nothing, and a large share of runs cannot check at all —
 * a `debug` build, an APK pushed by Gradle, an emulator image with no Play Store. Those
 * are the normal outcomes, not errors, which is why they get their own status instead of
 * being folded into a failure the UI would report as "check failed — tap to retry".
 */
class AppUpdateChecker(
    context: Context,
) {
    val manager: AppUpdateManager = AppUpdateManagerFactory.create(context.applicationContext)

    sealed interface Status {
        data object Unknown : Status

        data object UpToDate : Status

        data class UpdateAvailable(
            val info: AppUpdateInfo,
        ) : Status

        /**
         * A newer build exists, but Play refuses the in-place update flow for it. Rare —
         * it takes a track configured to forbid it — and deliberately NOT reported as
         * [UpToDate]: telling a tester their stale build is current is the one answer
         * this class must never give.
         */
        data object UpdateNotInstallable : Status

        /** Not installed by Play (sideload, Gradle install, no Play Store on the image). */
        data object NotFromPlayStore : Status

        data class Error(
            val code: Int?,
        ) : Status
    }

    suspend fun check(): Status =
        suspendCancellableCoroutine { cont ->
            manager.appUpdateInfo
                .addOnSuccessListener { info ->
                    val status =
                        when (
                            availabilityOf(
                                availability = info.updateAvailability(),
                                immediateAllowed = info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE),
                            )
                        ) {
                            Availability.UP_TO_DATE -> Status.UpToDate
                            Availability.INSTALLABLE -> Status.UpdateAvailable(info)
                            Availability.NOT_INSTALLABLE -> Status.UpdateNotInstallable
                        }
                    if (cont.isActive) cont.resume(status)
                }.addOnFailureListener { error ->
                    if (cont.isActive) {
                        cont.resume(statusOf((error as? InstallException)?.errorCode))
                    }
                }
        }

    /**
     * What Play said, before it is paired with the [AppUpdateInfo] the flow needs.
     * Split out so the decision is testable on the JVM — an `AppUpdateInfo` cannot be
     * built outside Play services, and the part worth testing is which outcome each
     * combination reports, not the callback plumbing.
     */
    enum class Availability { UP_TO_DATE, INSTALLABLE, NOT_INSTALLABLE }

    companion object {
        fun availabilityOf(
            availability: Int,
            immediateAllowed: Boolean,
        ): Availability =
            when {
                availability != UpdateAvailability.UPDATE_AVAILABLE -> Availability.UP_TO_DATE
                immediateAllowed -> Availability.INSTALLABLE
                else -> Availability.NOT_INSTALLABLE
            }

        /**
         * Maps a failed `appUpdateInfo` task. The three codes folded into
         * [Status.NotFromPlayStore] all mean the same thing to a tester — "Play does not
         * own this install, so it has nothing to offer" — and only one of them
         * (`ERROR_INSTALL_NOT_ALLOWED`) was recognised before, which left the two
         * commonest cases, a sideloaded APK and an emulator image with no Play Store,
         * reporting a retryable failure that could never succeed.
         */
        fun statusOf(errorCode: Int?): Status =
            when (errorCode) {
                InstallErrorCode.ERROR_INSTALL_NOT_ALLOWED,
                InstallErrorCode.ERROR_APP_NOT_OWNED,
                InstallErrorCode.ERROR_PLAY_STORE_NOT_FOUND,
                -> Status.NotFromPlayStore

                else -> Status.Error(errorCode)
            }
    }
}
