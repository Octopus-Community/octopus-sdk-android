package com.octopuscommunity.sdk.sample.ui.scenarios.sections

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException

internal enum class HostLifecyclePreset(
    val label: String,
) {
    Remount("Preset 1 · Remount embedded view"),
    TabBack("Preset 2 · Tab store cleared on Back"),
    Detach("Preset 3 · View detached and re-attached"),
    Switch("Preset 4 · Switch community while open"),
    Restart("Preset 5 · Restart SDK while open"),
}

/** Each suspend action returns only after its host change has reached the composition/view. */
internal interface HostLifecycleActions {
    fun setupIssue(): String? = null

    suspend fun mount()

    suspend fun unmount()

    suspend fun openPost()

    suspend fun clickPostInCommunity()

    suspend fun awaitPostContent()

    suspend fun clearTabOnBack()

    suspend fun detachAndReattach()

    suspend fun switchCommunity()

    suspend fun restartSdk()

    suspend fun recompose()
}

/** No global exception handler: asynchronous SDK/composition failures must still crash the bench. */
internal suspend fun runHostLifecyclePreset(
    preset: HostLifecyclePreset,
    actions: HostLifecycleActions,
): String {
    return try {
        actions.setupIssue()?.let { return "SKIPPED: $it" }
        actions.unmount()
        actions.mount()
        when (preset) {
            HostLifecyclePreset.Remount -> {
                repeat(5) {
                    actions.unmount()
                    actions.mount()
                }
                actions.openPost()
                actions.awaitPostContent()
            }
            HostLifecyclePreset.TabBack -> actions.clearTabOnBack()
            HostLifecyclePreset.Detach -> {
                actions.detachAndReattach()
                actions.openPost()
                actions.awaitPostContent()
            }
            HostLifecyclePreset.Switch -> {
                actions.switchCommunity()
                actions.recompose()
                actions.clickPostInCommunity()
                actions.awaitPostContent()
            }
            HostLifecyclePreset.Restart -> {
                actions.restartSdk()
                actions.recompose()
                actions.clickPostInCommunity()
                actions.awaitPostContent()
            }
        }
        "OK: ${preset.label} completed"
    } catch (timeout: TimeoutCancellationException) {
        "FAILED: ${timeout.javaClass.simpleName}"
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (exception: Exception) {
        // Never expose an exception message: SDK failures may include configuration values.
        "FAILED: ${exception.javaClass.simpleName}"
    }
}
