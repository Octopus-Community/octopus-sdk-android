package com.octopuscommunity.sdk.sample.data

import com.google.android.play.core.install.model.InstallErrorCode
import com.google.android.play.core.install.model.UpdateAvailability
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers the two decisions [AppUpdateChecker] makes on Play's answer. The rest of the
 * class is callback plumbing around an `AppUpdateInfo` that cannot be built off-device.
 *
 * The stake is one-sided: reporting "up to date" to a tester holding a stale build is
 * the single answer that makes the whole card worse than not having it, so every case
 * that is *not* a plain "no update" is asserted to land somewhere else.
 */
class AppUpdateCheckerTest {
    @Test
    fun `no update available is up to date`() {
        assertEquals(
            AppUpdateChecker.Availability.UP_TO_DATE,
            AppUpdateChecker.availabilityOf(
                availability = UpdateAvailability.UPDATE_NOT_AVAILABLE,
                immediateAllowed = true,
            ),
        )
    }

    @Test
    fun `unknown availability is reported as up to date, not as a failure`() {
        assertEquals(
            AppUpdateChecker.Availability.UP_TO_DATE,
            AppUpdateChecker.availabilityOf(
                availability = UpdateAvailability.UNKNOWN,
                immediateAllowed = true,
            ),
        )
    }

    @Test
    fun `an update Play will install is installable`() {
        assertEquals(
            AppUpdateChecker.Availability.INSTALLABLE,
            AppUpdateChecker.availabilityOf(
                availability = UpdateAvailability.UPDATE_AVAILABLE,
                immediateAllowed = true,
            ),
        )
    }

    @Test
    fun `an update Play refuses to install is not silently reported as up to date`() {
        assertEquals(
            AppUpdateChecker.Availability.NOT_INSTALLABLE,
            AppUpdateChecker.availabilityOf(
                availability = UpdateAvailability.UPDATE_AVAILABLE,
                immediateAllowed = false,
            ),
        )
    }

    @Test
    fun `an update already downloaded and waiting is not offered as a fresh one`() {
        assertEquals(
            AppUpdateChecker.Availability.UP_TO_DATE,
            AppUpdateChecker.availabilityOf(
                availability = UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS,
                immediateAllowed = true,
            ),
        )
    }

    @Test
    fun `the three no-ownership codes all read as not from Play Store`() {
        listOf(
            InstallErrorCode.ERROR_INSTALL_NOT_ALLOWED,
            InstallErrorCode.ERROR_APP_NOT_OWNED,
            InstallErrorCode.ERROR_PLAY_STORE_NOT_FOUND,
        ).forEach { code ->
            assertEquals(
                AppUpdateChecker.Status.NotFromPlayStore,
                AppUpdateChecker.statusOf(code),
                "error code $code",
            )
        }
    }

    @Test
    fun `an internal Play error stays a retryable error and keeps its code`() {
        assertEquals(
            AppUpdateChecker.Status.Error(InstallErrorCode.ERROR_INTERNAL_ERROR),
            AppUpdateChecker.statusOf(InstallErrorCode.ERROR_INTERNAL_ERROR),
        )
    }

    @Test
    fun `a failure that is not an InstallException has no code`() {
        assertEquals(AppUpdateChecker.Status.Error(null), AppUpdateChecker.statusOf(null))
    }
}
