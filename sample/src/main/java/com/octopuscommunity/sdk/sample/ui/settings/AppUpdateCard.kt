package com.octopuscommunity.sdk.sample.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.sample.BuildConfig
import com.octopuscommunity.sdk.sample.data.AppUpdateChecker
import com.octopuscommunity.sdk.sample.ui.components.PrimaryPillButton
import com.octopuscommunity.sdk.sample.ui.components.SampleCard
import com.octopuscommunity.sdk.sample.ui.components.SecondaryPillButton
import com.octopuscommunity.sdk.sample.ui.components.sampleDangerColor

/**
 * The App block: which build this is, where it came from, and the Play in-app update check.
 *
 * Lives on Settings › About since the 3-tab shell dropped Home (DS v1). It stays discoverable from
 * the tab a tester lands on: an available update badges the Settings tab and the About row, and
 * `MainActivity` still announces it once per versionCode with a snackbar.
 *
 * The `sample-update-*` test ids are a contract with shared QA runner
 * (`sample update contract`) — renaming one silently breaks the update scenario
 * rather than failing a build, which is exactly how it was lost once already (internal change).
 */
@Composable
internal fun AppUpdateCard(
    appUpdateStatus: AppUpdateChecker.Status,
    onCheckForUpdate: () -> Unit,
    onStartUpdate: () -> Unit,
) {
    val isUpdateAvailable = appUpdateStatus is AppUpdateChecker.Status.UpdateAvailable
    val statusLabel =
        when (appUpdateStatus) {
            AppUpdateChecker.Status.Unknown -> "Tap to check for updates"
            AppUpdateChecker.Status.UpToDate -> "App is up to date"
            is AppUpdateChecker.Status.UpdateAvailable -> "A new version is available"
            AppUpdateChecker.Status.UpdateNotInstallable ->
                "A new version exists, but Play won't install it over this build"
            AppUpdateChecker.Status.NotFromPlayStore -> "Not installed from Play Store"
            is AppUpdateChecker.Status.Error ->
                appUpdateStatus.code?.let { "Update check failed (code $it) — tap to retry" }
                    ?: "Update check failed — tap to retry"
        }
    val statusColor =
        when (appUpdateStatus) {
            AppUpdateChecker.Status.UpdateNotInstallable,
            is AppUpdateChecker.Status.Error,
            -> sampleDangerColor
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

    SampleCard(
        modifier = Modifier.testTag("sample-update-card"),
        title = "App",
        subtitle = "Sample ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.bodySmall,
                color = statusColor,
                modifier = Modifier.testTag("sample-update-status"),
            )
            if (isUpdateAvailable) {
                PrimaryPillButton(
                    text = "Update now",
                    onClick = onStartUpdate,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag("sample-update-action"),
                    minHeight = 44,
                )
            } else {
                SecondaryPillButton(
                    text = "Check for update",
                    onClick = onCheckForUpdate,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .testTag("sample-update-action"),
                )
            }
        }
    }
}
