package com.octopuscommunity.sdk.ui.common.snackbar

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.octopuscommunity.sdk.domain.model.CommunityConfig
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.LocalOctopusCommunityConfig
import com.octopuscommunity.sdk.ui.common.UiText
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.common.viewmodel.OctopusScreenViewModel
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModel
import com.octopuscommunity.sdk.ui.events.OctopusSnackbarEvents
import com.octopuscommunity.sdk.ui.medium
import kotlinx.coroutines.launch

@Composable
internal fun OctopusSnackbarHost(
    screenViewModel: OctopusScreenViewModel,
    modifier: Modifier = Modifier,
) {
    val viewModel = octopusViewModel<OctopusSnackbarViewModel>()

    val context = LocalContext.current
    val communityConfig by rememberUpdatedState(LocalOctopusCommunityConfig.current)

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            // Check if current snackbar is showing the same event
            val currentEvent =
                (snackbarHostState.currentSnackbarData?.visuals as? OctopusSnackbarVisuals)?.event

            if (currentEvent?.message == event.message) {
                snackbarHostState.currentSnackbarData?.dismiss()
            }

            // Launch in separate coroutine so collection continues
            launch {
                val result = snackbarHostState.showSnackbar(OctopusSnackbarVisuals(context, event, communityConfig))
                if (result == SnackbarResult.ActionPerformed) {
                    (event as? OctopusSnackbarEvents.Event.Error)?.onRetry?.invoke()
                }
            }
        }
    }

    OctopusSnackbarHost(
        modifier = modifier,
        hostState = snackbarHostState,
        onClick = { event ->
            when (event) {
                is OctopusSnackbarEvents.Event.Gamification -> {
                    screenViewModel.showGamificationSheet()
                }

                else -> {}
            }
        },
    )
}

@Composable
private fun OctopusSnackbarHost(
    hostState: SnackbarHostState,
    onClick: (event: OctopusSnackbarEvents.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(
        modifier = modifier,
        hostState = hostState,
    ) { snackbarData ->
        OctopusSnackbar(
            message = snackbarData.visuals.message,
            event = (snackbarData.visuals as? OctopusSnackbarVisuals)?.event,
            actionLabel = snackbarData.visuals.actionLabel,
            withDismissAction = snackbarData.visuals.withDismissAction,
            onAction = snackbarData::performAction,
            onDismiss = snackbarData::dismiss,
            onClick = onClick,
        )
    }
}

/**
 * The snackbar body itself, taking plain values instead of the [SnackbarData] it is fed from in
 * production. That is what makes it previewable — and so screenshot-testable — at all: driving the
 * real host means calling `showSnackbar` from a `LaunchedEffect` and capturing a frame of its enter
 * animation, which is not a stable reference.
 */
@Composable
internal fun OctopusSnackbar(
    message: String,
    event: OctopusSnackbarEvents.Event?,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    withDismissAction: Boolean = true,
    onAction: () -> Unit = {},
    onDismiss: () -> Unit = {},
    onClick: (event: OctopusSnackbarEvents.Event) -> Unit = {},
) {
    val contentColor =
        when (event) {
            is OctopusSnackbarEvents.Event.Success -> OctopusTheme.colorScheme.success
            is OctopusSnackbarEvents.Event.Error -> OctopusTheme.colorScheme.gray100
            is OctopusSnackbarEvents.Event.Info,
            is OctopusSnackbarEvents.Event.Gamification,
            -> OctopusTheme.colorScheme.gray900

            else -> OctopusTheme.colorScheme.gray100
        }
    // The `snackbarData` overload renders the message on its own, with no room for the leading
    // glyph the design puts on the lost-connection one — so the action and the dismiss button are
    // built here from the same data instead. That overload is also what insets a Material 3
    // snackbar from the screen edges (`padding(12.dp)` around the container); the slot overload
    // used here does not, which is why the snackbar used to run edge to edge. The margin is restored
    // below, outside the click target.
    Snackbar(
        modifier =
            modifier.padding(SnackbarMargin) then
                if (event?.clickable == true) {
                    Modifier.clickable { onClick(event) }
                } else {
                    Modifier
                },
        action =
            actionLabel?.let { label ->
                {
                    TextButton(onClick = onAction) {
                        Text(
                            text = label,
                            style = OctopusTheme.typography.body2.medium,
                            color = contentColor,
                        )
                    }
                }
            },
        dismissAction =
            if (withDismissAction) {
                {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            tint = contentColor,
                            contentDescription = stringResource(R.string.common_close),
                        )
                    }
                }
            } else {
                null
            },
        shape = RoundedCornerShape(4.dp),
        containerColor =
            when (event) {
                is OctopusSnackbarEvents.Event.Info -> OctopusTheme.colorScheme.onHover
                is OctopusSnackbarEvents.Event.Success -> OctopusTheme.colorScheme.successLow
                is OctopusSnackbarEvents.Event.Error -> OctopusTheme.colorScheme.gray900
                is OctopusSnackbarEvents.Event.Gamification -> OctopusTheme.colorScheme.primaryLow
                else -> OctopusTheme.colorScheme.gray900
            }.opaqueOver(background = OctopusTheme.colorScheme.background, content = contentColor),
        contentColor = contentColor,
        dismissActionContentColor = contentColor,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if ((event as? OctopusSnackbarEvents.Event.Error)?.isNoNetwork == true) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    painter = painterResource(R.drawable.ic_octopus_snackbar_no_network),
                    tint = contentColor,
                    contentDescription = null,
                )
            }
            Text(
                text = message,
                style = OctopusTheme.typography.body2,
            )
        }
    }
}

/** The margin Material 3's own `Snackbar(snackbarData)` puts around the container. */
private val SnackbarMargin = 12.dp

internal class OctopusSnackbarVisuals(
    context: Context,
    val event: OctopusSnackbarEvents.Event,
    communityConfig: CommunityConfig? = null,
) : SnackbarVisuals {
    override val message = event.message.asString(context, communityConfig)

    /** "Retry" on a load failure the screen can re-run; nothing to act on otherwise. */
    override val actionLabel =
        (event as? OctopusSnackbarEvents.Event.Error)
            ?.onRetry
            ?.let { context.getString(R.string.common_retry) }
    override val withDismissAction = true

    /**
     * [SnackbarDuration.Long] rather than `Short`: these snackbars now carry a "Retry" action, and
     * the ~4 s of `Short` is not enough to read a failure message and reach for the button — Material
     * itself asks for the longer duration as soon as a snackbar is actionable. Manual dismissal is
     * still offered (see [withDismissAction]), and the design stays transient: no persistent offline
     * banner (iso-iOS).
     */
    override val duration = SnackbarDuration.Long
}

val SnackbarData.event get() = (visuals as? OctopusSnackbarVisuals)?.event

/**
 * This color as it looks laid over [background], made fully opaque.
 *
 * A snackbar floats over the screen content, and its colors come from the host's theme: a host can
 * hand over a translucent one, and the content behind then bleeds through the message. Compositing
 * onto [background] keeps the look of a color the host meant to be seen over its own surface; the
 * white or black floor underneath covers a translucent [background] too. That floor only shows when
 * [background] is translucent, which is exactly when its color says nothing about the palette in
 * use (`Color.Transparent` is black), so it is picked to contrast with the [content] drawn on the
 * snackbar instead: a black floor under light text, a white one under dark text.
 */
internal fun Color.opaqueOver(
    background: Color,
    content: Color,
): Color =
    compositeOver(background)
        .compositeOver(
            if (content.isSpecified && content.luminance() > 0.5f) Color.Black else Color.White,
        )

@Preview(showBackground = true)
@Composable
private fun OctopusSnackbarErrorPreview() {
    OctopusTheme {
        OctopusSnackbar(
            message = "Something went wrong. Please try again.",
            event =
                OctopusSnackbarEvents.Event.Error(
                    message = UiText.StringResource(R.string.error_unknown_formal),
                    onRetry = {},
                ),
            actionLabel = stringResource(R.string.common_retry),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OctopusSnackbarNoNetworkPreview() {
    OctopusTheme {
        OctopusSnackbar(
            message = "No internet connection.",
            event =
                OctopusSnackbarEvents.Event.Error(
                    message = UiText.StringResource(R.string.error_no_network_formal),
                    onRetry = {},
                    isNoNetwork = true,
                ),
            actionLabel = stringResource(R.string.common_retry),
        )
    }
}
