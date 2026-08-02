package com.example.multiplayer.ui.home

import com.example.multiplayer.media.playback.DemoPlaybackMetrics
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackDiagnosticsFormatterTest {
    private val metrics = DemoPlaybackMetrics(
        renderedFps = 29.94f,
        sourceFrameRate = 30f,
        width = 1280,
        height = 720,
        bitrateBitsPerSecond = 1_842_000,
        sampleMimeType = "video/avc",
        decoderName = "c2.vendor.avc.decoder",
        droppedFrames = 2,
        bufferedDurationMs = 2_450,
    )

    @Test
    fun formatsMediaDetails() {
        assertEquals(
            "1280×720 · H.264/AVC · 30.0 source fps",
            PlaybackDiagnosticsFormatter.mediaLine(metrics),
        )
    }

    @Test
    fun formatsRuntimePerformance() {
        assertEquals(
            "29.9 render fps · 1.84 Mbps · 2.5s buffer",
            PlaybackDiagnosticsFormatter.performanceLine(metrics),
        )
    }

    @Test
    fun formatsHardwareDecoderAndDrops() {
        assertEquals(
            "HW c2.vendor.avc.decoder · dropped 2",
            PlaybackDiagnosticsFormatter.decoderLine(metrics),
        )
    }
}
