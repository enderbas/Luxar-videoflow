package com.example.multiplayer.ui.home

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import com.example.multiplayer.R
import com.example.multiplayer.media.playback.DemoPlayerController
import com.example.multiplayer.media.playback.DemoPlayerSlot
import com.example.multiplayer.media.playback.DemoPlayerStatus
import com.example.multiplayer.media.playback.DemoVideo
import com.example.multiplayer.media.playback.demoVideos

@OptIn(UnstableApi::class)
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember {
        DemoPlayerController(context, demoVideos)
    }

    DisposableEffect(lifecycleOwner, controller) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> controller.start()
                Lifecycle.Event.ON_STOP -> controller.stop()
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            controller.start()
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            controller.stop()
        }
    }

    VideoWall(
        videos = demoVideos,
        slots = controller.slots,
    )
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoWall(
    videos: List<DemoVideo>,
    slots: List<DemoPlayerSlot>,
) {
    val firstTileFocus = remember { FocusRequester() }
    val columns = WallLayoutSpec.columnsFor(videos.size)

    LaunchedEffect(Unit) {
        firstTileFocus.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp, vertical = 18.dp)
            .testTag("video_wall"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = stringResource(R.string.demo_wall_status),
                color = Color(0xFF9AA6BA),
                fontSize = 14.sp,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        videos.chunked(columns).forEachIndexed { rowIndex, rowVideos ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowVideos.forEachIndexed { columnIndex, video ->
                    val videoIndex = rowIndex * columns + columnIndex
                    VideoTile(
                        video = video,
                        slot = slots.getOrNull(videoIndex),
                        modifier = Modifier
                            .weight(1f)
                            .then(
                                if (videoIndex == 0) {
                                    Modifier.focusRequester(firstTileFocus)
                                } else {
                                    Modifier
                                },
                            ),
                    )
                }
            }
            if (rowIndex < videos.lastIndex / columns) {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoTile(
    video: DemoVideo,
    slot: DemoPlayerSlot?,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(4.dp)

    Box(
        modifier = modifier
            .fillMaxSize()
            .onFocusChanged { isFocused = it.isFocused }
            .border(
                width = if (isFocused) 4.dp else 1.dp,
                color = if (isFocused) Color(0xFF8BAEFF) else Color(0xFF252A34),
                shape = shape,
            )
            .background(Color.Black, shape)
            .padding(if (isFocused) 4.dp else 1.dp)
            .focusable()
            .testTag("video_tile_${video.id}"),
    ) {
        if (slot != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    PlayerView(viewContext).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        player = slot.player
                    }
                },
                update = { playerView ->
                    playerView.player = slot.player
                },
                onRelease = { playerView ->
                    playerView.player = null
                },
            )
        }

        TileOverlay(
            video = video,
            slot = slot,
            isFocused = isFocused,
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun BoxScope.TileOverlay(
    video: DemoVideo,
    slot: DemoPlayerSlot?,
    isFocused: Boolean,
) {
    Column(
        modifier = Modifier
            .align(Alignment.TopStart)
            .background(Color(0xB3000000))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = video.label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal,
        )
        val statusText = when (slot?.status) {
            DemoPlayerStatus.WAITING, null -> stringResource(R.string.player_waiting)
            DemoPlayerStatus.PREPARING -> stringResource(R.string.player_preparing)
            DemoPlayerStatus.PLAYING -> stringResource(R.string.player_playing)
            DemoPlayerStatus.PAUSED -> stringResource(R.string.player_paused)
            DemoPlayerStatus.ERROR -> slot.errorMessage ?: stringResource(R.string.player_error)
        }
        Text(
            text = statusText,
            color = if (slot?.status == DemoPlayerStatus.ERROR) {
                Color(0xFFFF8A80)
            } else {
                Color(0xFFC7D0E0)
            },
            fontSize = 11.sp,
        )
    }

    val metrics = slot?.metrics
    if (metrics != null) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color(0xB3000000))
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("video_diagnostics_${video.id}"),
        ) {
            DiagnosticText(PlaybackDiagnosticsFormatter.mediaLine(metrics))
            DiagnosticText(PlaybackDiagnosticsFormatter.performanceLine(metrics))
            DiagnosticText(PlaybackDiagnosticsFormatter.decoderLine(metrics))
        }
    }
}

@Composable
private fun DiagnosticText(text: String) {
    Text(
        text = text,
        color = Color(0xFFD4DBE8),
        fontSize = 10.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
