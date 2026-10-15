package com.octopuscommunity.sdk.sample.ui.scenarios.sections

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.octopuscommunity.sdk.sample.data.usecase.CommunityConnectUseCase
import kotlinx.coroutines.flow.Flow
import org.koin.compose.koinInject

/** Keeps a section's disconnect error until a new signed-in session or the next attempt. */
@Composable
internal fun rememberCommunityDisconnectError(
    successfulSignIns: Flow<Unit> = koinInject<CommunityConnectUseCase>().successfulSignIns,
): MutableState<String?> {
    val error = remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        successfulSignIns.collect { error.value = null }
    }
    return error
}
