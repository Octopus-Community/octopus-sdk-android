package com.octopuscommunity.sdk.ui.components.video

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.listen
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.SURFACE_TYPE_TEXTURE_VIEW
import androidx.media3.ui.compose.state.PlayPauseButtonState
import androidx.media3.ui.compose.state.ProgressStateWithTickInterval
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import androidx.media3.ui.compose.state.rememberProgressStateWithTickInterval
import com.octopuscommunity.sdk.domain.model.Media
import com.octopuscommunity.sdk.domain.model.Resource
import com.octopuscommunity.sdk.test.mock.MockMedias
import com.octopuscommunity.sdk.ui.OctopusTheme
import com.octopuscommunity.sdk.ui.R
import com.octopuscommunity.sdk.ui.common.medias.MediaPinchState
import com.octopuscommunity.sdk.ui.common.medias.mediaPinch
import com.octopuscommunity.sdk.ui.common.medias.viewer.ImageViewerSession
import com.octopuscommunity.sdk.ui.common.medias.viewer.shouldResumeAfterViewer
import com.octopuscommunity.sdk.ui.common.stringResource
import com.octopuscommunity.sdk.ui.common.viewmodel.octopusViewModel
import com.octopuscommunity.sdk.ui.components.OctopusButtonDefaults
import com.octopuscommunity.sdk.ui.components.OctopusContent
import com.octopuscommunity.sdk.ui.components.OctopusFilledIconButton
import com.octopuscommunity.sdk.ui.components.OctopusImage
import com.octopuscommunity.sdk.ui.components.OctopusMediumButton
import com.octopuscommunity.sdk.ui.components.OctopusProgressIndicator
import com.octopuscommunity.sdk.ui.medium
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import java.util.Locale
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@OptIn(UnstableApi::class)
@Composable
internal fun OctopusVideo(
    contentId: String,
    media: Media.Video,
    autoPlay: Boolean,
    modifier: Modifier = Modifier,
) {
    val viewModel =
        octopusViewModel<OctopusVideoViewModel>(
            key = "video_${contentId}_${media.id}",
            parameters = { parametersOf(contentId, media.id) },
        )

    val state by viewModel.state.collectAsStateWithLifecycle()

    OctopusVideo(
        modifier = modifier,
        media = media,
        state = state,
        autoPlay = autoPlay,
        onMuteChanged = viewModel::onMutedChange,
        onPlayingChanged = viewModel::onPlayingChanged,
        onLoadingChanged = viewModel::onLoadingChanged,
        onShowControls = viewModel::showControls,
    )
}

@OptIn(UnstableApi::class)
@Composable
internal fun OctopusVideo(
    media: Media.Video,
    autoPlay: Boolean,
    state: OctopusVideoViewModel.UiState,
    modifier: Modifier = Modifier,
    maxAspectRatio: Float? = null,
    onMuteChanged: (isMuted: Boolean) -> Unit = {},
    onPlayingChanged: (playing: Boolean, position: Duration, duration: Duration) -> Unit,
    onLoadingChanged: (loading: Boolean) -> Unit,
    onShowControls: (show: Boolean) -> Unit,
) = OctopusContent {
    val context = LocalContext.current
    val pinchState = remember(media.id) { MediaPinchState() }

    val player = koinInject<Player>()
    val exoPlayer = player as? ExoPlayer
    val progressState =
        rememberProgressStateWithTickInterval(
            player = player,
            tickIntervalMs = 1000,
        )

    var playerPosition by remember { mutableStateOf(state.position) }
    val playPauseButtonState = rememberPlayPauseButtonState(player)

    // Create MediaSource on IO thread, then set it on the player
    LaunchedEffect(media) {
        val mediaSource =
            withContext(Dispatchers.IO) {
                ProgressiveMediaSource
                    .Factory(
                        DefaultDataSource.Factory(context),
                    ).createMediaSource(
                        when (val source = media.source) {
                            is Resource.Local -> MediaItem.fromUri(source.uri)
                            is Resource.Remote -> MediaItem.fromUri(source.url)
                        },
                    )
            }
        // setMediaSource and prepare must be called on main thread
        exoPlayer?.setMediaSource(mediaSource, state.position.inWholeMilliseconds)
        exoPlayer?.prepare()
    }

    LaunchedEffect(player) {
        player.listen { events ->
            if (events.contains(Player.EVENT_IS_PLAYING_CHANGED)) {
                playerPosition =
                    when (player.playbackState) {
                        Player.STATE_ENDED -> player.duration.milliseconds
                        else -> player.currentPosition.milliseconds
                    }
                onPlayingChanged(player.isPlaying, playerPosition, player.duration.milliseconds)
            }
            if (events.contains(Player.EVENT_IS_LOADING_CHANGED)) {
                onLoadingChanged(player.isLoading)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            player.stop()
            player.release()
        }
    }

    val isAtStart by
        remember(player.currentPosition) { derivedStateOf { player.currentPosition == 0L } }
    val isAtEnded = state.position == player.duration.milliseconds

    LaunchedEffect(state.position) {
        if (playerPosition != state.position) {
            playerPosition = state.position
            player.seekTo(state.position.inWholeMilliseconds)
        }
    }

    val play =
        remember(autoPlay, isAtEnded) {
            autoPlay && !isAtEnded
        }

    // The full-screen image viewer is a dialog window: it does not stop the host's lifecycle, so
    // pause explicitly while one is open and resume afterwards only if this video was playing.
    val imageViewerOpen = ImageViewerSession.count.collectAsStateWithLifecycle().value > 0
    var resumeAfterViewer by remember { mutableStateOf(false) }
    LaunchedEffect(imageViewerOpen) {
        if (imageViewerOpen) {
            resumeAfterViewer =
                shouldResumeAfterViewer(
                    playWhenReady = player.playWhenReady,
                    isEnded = player.playbackState == Player.STATE_ENDED,
                )
            player.playWhenReady = false
        } else if (resumeAfterViewer) {
            resumeAfterViewer = false
            player.playWhenReady = true
        }
    }

    LaunchedEffect(play) {
        if (!imageViewerOpen) player.playWhenReady = play
    }

    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        if (play && !imageViewerOpen) player.playWhenReady = true
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        player.playWhenReady = false
    }

    LaunchedEffect(state.isMuted) {
        if (state.isMuted) player.mute() else player.unmute()
    }

    Box(
        modifier =
            modifier
                .then(
                    media.dimensions?.let { dimensions ->
                        val aspectRatio = dimensions.width.toFloat() / dimensions.height.toFloat()
                        Modifier.aspectRatio(max(aspectRatio, maxAspectRatio ?: 0f))
                    } ?: Modifier,
                ).mediaPinch(pinchState)
                .clickable {
                    if (!state.showControls) {
                        onShowControls(true)
                    } else {
                        playPauseButtonState.onClick()
                    }
                },
    ) {
        // TextureView participates in the recorded graphics layer, keeping the same player
        // and live surface while the window overlay draws above the card and app bars.
        ContentFrame(
            modifier = Modifier.fillMaxSize(),
            surfaceType = SURFACE_TYPE_TEXTURE_VIEW,
            keepContentOnReset = true,
            player = player,
            shutter = {
                OctopusVideoThumbnail(
                    modifier = Modifier.fillMaxSize(),
                    thumbnailUrl = media.thumbnailUrl,
                )
            },
        )

        AnimatedVisibility(
            modifier = Modifier.fillMaxSize(),
            visible = isAtEnded || isAtStart,
        ) {
            OctopusVideoThumbnail(
                modifier = Modifier.fillMaxSize(),
                thumbnailUrl = media.thumbnailUrl,
            )
        }

        AnimatedVisibility(
            modifier = Modifier.fillMaxSize(),
            visible = (state.showControls || isAtEnded) && !state.showLoading,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                OctopusVideoControls(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .align(Alignment.BottomEnd),
                    isEnded = isAtEnded,
                    playPauseButtonState = playPauseButtonState,
                    progressState = progressState,
                    muted = state.isMuted,
                    onTogglePlayPause = {
                        if (isAtEnded) {
                            player.seekToDefaultPosition()
                        }
                        player.playWhenReady = !player.isPlaying
                    },
                    onMuteChanged = onMuteChanged,
                )
            }
        }

        AnimatedVisibility(
            modifier = Modifier.fillMaxSize(),
            visible = isAtEnded && !state.showLoading,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                OctopusMediumButton(
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    text = stringResource(R.string.content_video_replay),
                    colors =
                        OctopusButtonDefaults.buttonColors(
                            containerColor =
                                OctopusTheme.colorScheme.gray800
                                    .copy(alpha = 0.6f),
                            contentColor = OctopusTheme.colorScheme.onHover,
                        ),
                    onClick = {
                        player.seekToDefaultPosition()
                        player.playWhenReady = true
                    },
                )
            }
        }

        AnimatedVisibility(
            modifier = Modifier.fillMaxSize(),
            visible = state.showLoading,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                OctopusProgressIndicator(
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .size(36.dp)
                            .padding(8.dp),
                    strokeWidth = 2.dp,
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun OctopusVideoControls(
    isEnded: Boolean,
    playPauseButtonState: PlayPauseButtonState,
    progressState: ProgressStateWithTickInterval,
    muted: Boolean = true,
    onTogglePlayPause: () -> Unit,
    onMuteChanged: (isMuted: Boolean) -> Unit,
    modifier: Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OctopusFilledIconButton(
            modifier =
                Modifier
                    .size(40.dp),
            icon =
                when {
                    isEnded ->
                        OctopusTheme.icons.content.video
                            .replay()
                    playPauseButtonState.showPlay ->
                        OctopusTheme.icons.content.video
                            .play()
                    else ->
                        OctopusTheme.icons.content.video
                            .pause()
                },
            contentDescription =
                stringResource(
                    when {
                        isEnded -> R.string.content_video_replay
                        playPauseButtonState.showPlay -> R.string.accessibility_video_play
                        else -> R.string.accessibility_video_pause
                    },
                ),
            containerColor =
                OctopusTheme.colorScheme.gray800
                    .copy(alpha = 0.6f),
            contentColor = OctopusTheme.colorScheme.onHover,
            onClick = {
                onTogglePlayPause()
            },
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            modifier =
                Modifier
                    .background(
                        color =
                            OctopusTheme.colorScheme.gray800
                                .copy(alpha = 0.6f),
                        shape = RoundedCornerShape(size = 90.dp),
                    ).padding(horizontal = 12.dp, vertical = 7.dp),
            text = "${
                progressState.currentPositionMs.milliseconds.toDurationString()
            } / ${
                progressState.durationMs.milliseconds.toDurationString()
            }",
            color = OctopusTheme.colorScheme.onHover,
            style = OctopusTheme.typography.body2.medium,
        )
        Spacer(modifier = Modifier.width(8.dp))

        OctopusFilledIconButton(
            modifier =
                Modifier
                    .size(40.dp),
            icon =
                if (muted) {
                    OctopusTheme.icons.content.video
                        .muted()
                } else {
                    OctopusTheme.icons.content.video
                        .notMuted()
                },
            contentDescription =
                stringResource(
                    if (muted) {
                        R.string.accessibility_video_unmute
                    } else {
                        R.string.accessibility_video_mute
                    },
                ),
            containerColor =
                OctopusTheme.colorScheme.gray800
                    .copy(alpha = 0.6f),
            contentColor = OctopusTheme.colorScheme.onHover,
            onClick = { onMuteChanged(!muted) },
        )
    }
}

@Composable
fun OctopusVideoThumbnail(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .background(OctopusTheme.colorScheme.gray300),
        contentAlignment = Alignment.Center,
    ) {
        if (thumbnailUrl != null) {
            OctopusImage(
                modifier = Modifier.fillMaxSize(),
                data = thumbnailUrl,
            )
        }
    }
}

private fun Duration.toDurationString() =
    toComponents { hours, minutes, seconds, _ ->
        if (hours > 0) {
            String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
        }
    }

@Preview(showBackground = true)
@Composable
private fun OctopusVideoPreview() {
    OctopusTheme {
        OctopusVideo(
            media = MockMedias.defaultVideo,
            state = OctopusVideoViewModel.UiState(),
            autoPlay = false,
            onMuteChanged = {},
            onPlayingChanged = { _, _, _ -> },
            onLoadingChanged = {},
            onShowControls = {},
        )
    }
}
