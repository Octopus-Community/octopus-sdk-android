package com.octopuscommunity.sdk.sample.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.octopuscommunity.sdk.sample.data.SampleSdkStatus

/**
 * The dot colour of a [SampleSdkStatus]: success once a user is signed in, danger when the SDK
 * cannot work at all (not started, offline), amber for the degraded states in between.
 */
val SampleSdkStatus.dotColor: Color
    @Composable @ReadOnlyComposable
    get() =
        when (this) {
            SampleSdkStatus.SignedIn -> sampleSuccessColor
            SampleSdkStatus.Guest, SampleSdkStatus.NoSession -> sampleAmberColor
            SampleSdkStatus.NotStarted, SampleSdkStatus.Unreachable -> sampleDangerColor
        }
