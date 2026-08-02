package com.example.multiplayer.ui.home

import com.example.multiplayer.media.playback.DemoPlaybackMetrics
import java.util.Locale

object PlaybackDiagnosticsFormatter {
    fun mediaLine(metrics: DemoPlaybackMetrics): String = listOf(
        resolution(metrics),
        codec(metrics),
        metrics.sourceFrameRate?.let { "${decimal(it)} source fps" } ?: "-- source fps",
    ).joinToString(SEPARATOR)

    fun performanceLine(metrics: DemoPlaybackMetrics): String = listOf(
        metrics.renderedFps?.let { "${decimal(it)} render fps" } ?: "-- render fps",
        bitrate(metrics.bitrateBitsPerSecond),
        "${decimal(metrics.bufferedDurationMs / 1_000f)}s buffer",
    ).joinToString(SEPARATOR)

    fun decoderLine(metrics: DemoPlaybackMetrics): String {
        val decoder = metrics.decoderName ?: "decoder pending"
        return "HW $decoder${SEPARATOR}dropped ${metrics.droppedFrames}"
    }

    private fun resolution(metrics: DemoPlaybackMetrics): String =
        if (metrics.width != null && metrics.height != null) {
            "${metrics.width}×${metrics.height}"
        } else {
            "--×--"
        }

    private fun codec(metrics: DemoPlaybackMetrics): String = when (metrics.sampleMimeType) {
        "video/avc" -> "H.264/AVC"
        "video/hevc" -> "H.265/HEVC"
        "video/av01" -> "AV1"
        "video/x-vnd.on2.vp9" -> "VP9"
        else -> metrics.codecString ?: metrics.sampleMimeType ?: "codec pending"
    }

    private fun bitrate(bitsPerSecond: Long?): String = when {
        bitsPerSecond == null -> "-- bitrate"
        bitsPerSecond >= 1_000_000 -> "${twoDecimals(bitsPerSecond / 1_000_000f)} Mbps"
        else -> "${decimal(bitsPerSecond / 1_000f)} kbps"
    }

    private fun decimal(value: Float): String = String.format(Locale.US, "%.1f", value)

    private fun twoDecimals(value: Float): String = String.format(Locale.US, "%.2f", value)

    private const val SEPARATOR = " · "
}
