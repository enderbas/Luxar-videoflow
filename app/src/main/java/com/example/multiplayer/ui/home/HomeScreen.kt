package com.example.multiplayer.ui.home

import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.tv.material3.Text
import com.example.multiplayer.R
import com.example.multiplayer.media.playback.demoVideos
import com.luxar.videoflow.CoordinateSpace
import com.luxar.videoflow.VideoFlow
import com.luxar.videoflow.VideoFlowConfig
import com.luxar.videoflow.VideoPlayerHandle
import com.luxar.videoflow.VideoRequest
import com.luxar.videoflow.VideoSource
import kotlinx.coroutines.delay

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val videoContainer = remember(context) { FrameLayout(context) }
    var playerHandles by remember { mutableStateOf<List<VideoPlayerHandle>>(emptyList()) }

    DisposableEffect(videoContainer, lifecycleOwner) {
        val flow = VideoFlow.initialize(
            container = videoContainer,
            lifecycleOwner = lifecycleOwner,
            config = VideoFlowConfig(
                coordinateSpace = CoordinateSpace.Normalized,
                reserveHardwareDecoders = 2,
                showDiagnostics = true,
                focusableSlots = true,
                autoFocusFirstSlot = true,
            ),
        )
        playerHandles = demoVideos.map { video ->
            flow.run(
                VideoRequest(
                    id = video.id,
                    label = video.label,
                    source = VideoSource.Asset(video.assetPath),
                    placement = video.placement,
                    loop = true,
                    muted = true,
                ),
            )
        }

        onDispose {
            playerHandles = emptyList()
            flow.release()
        }
    }

    LaunchedEffect(playerHandles) {
        if (playerHandles.size != demoVideos.size) return@LaunchedEffect

        var sourceRotation = 0
        while (true) {
            delay(SOURCE_ROTATION_INTERVAL_MS)
            sourceRotation = (sourceRotation + 1) % demoVideos.size
            playerHandles.forEachIndexed { playerIndex, handle ->
                val nextVideo = demoVideos[(playerIndex + sourceRotation) % demoVideos.size]
                handle.replaceSource(VideoSource.Asset(nextVideo.assetPath))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp, vertical = 18.dp),
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

        AndroidView(
            factory = { videoContainer },
            modifier = Modifier
                .fillMaxSize()
                .testTag("video_wall"),
        )
    }
}

private const val SOURCE_ROTATION_INTERVAL_MS = 6_000L
