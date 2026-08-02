package com.luxar.videoflow

import android.net.Uri

sealed interface VideoSource {
    data class Asset(val path: String) : VideoSource {
        init {
            require(path.isNotBlank()) { "Asset path must not be blank" }
            require(!path.startsWith('/')) { "Asset path must be relative" }
        }
    }

    data class FilePath(val path: String) : VideoSource {
        init {
            require(path.isNotBlank()) { "File path must not be blank" }
        }
    }

    data class ContentUri(val uri: Uri) : VideoSource {
        init {
            require(uri.scheme == "content") { "ContentUri requires a content:// URI" }
        }
    }
}

sealed interface CoordinateSpace {
    data object Normalized : CoordinateSpace

    data class Reference(val width: Int, val height: Int) : CoordinateSpace {
        init {
            require(width > 0) { "Reference width must be positive" }
            require(height > 0) { "Reference height must be positive" }
        }
    }
}

data class VideoPlacement(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val zIndex: Int = 0,
) {
    init {
        require(x.isFinite() && y.isFinite()) { "Placement position must be finite" }
        require(width.isFinite() && width > 0f) { "Placement width must be positive and finite" }
        require(height.isFinite() && height > 0f) { "Placement height must be positive and finite" }
    }
}

data class VideoRequest(
    val id: String,
    val source: VideoSource,
    val placement: VideoPlacement,
    val label: String = id,
    val loop: Boolean = true,
    val muted: Boolean = true,
    val showDiagnostics: Boolean? = null,
) {
    init {
        require(id.isNotBlank()) { "Video id must not be blank" }
    }
}

enum class VideoPlayerState {
    QUEUED,
    CAPACITY_LIMITED,
    PREPARING,
    PLAYING,
    PAUSED,
    ERROR,
    RELEASED,
}

data class VideoPlayerMetrics(
    val renderedFps: Float? = null,
    val sourceFrameRate: Float? = null,
    val width: Int? = null,
    val height: Int? = null,
    val bitrateBitsPerSecond: Long? = null,
    val sampleMimeType: String? = null,
    val decoderName: String? = null,
    val droppedFrames: Int = 0,
    val bufferedDurationMs: Long = 0,
)

data class VideoPlayerSnapshot(
    val id: String,
    val source: VideoSource,
    val state: VideoPlayerState,
    val metrics: VideoPlayerMetrics = VideoPlayerMetrics(),
    val errorCode: String? = null,
)

fun interface VideoFlowListener {
    fun onPlayerChanged(snapshot: VideoPlayerSnapshot)
}

data class DecoderCapacity(
    val decoderNames: List<String>,
    val advertisedMaximum: Int,
    val reservedInstances: Int,
    val availablePlayers: Int,
)

data class VideoFlowConfig(
    val coordinateSpace: CoordinateSpace = CoordinateSpace.Reference(1920, 1080),
    val reserveHardwareDecoders: Int = 2,
    val unknownDecoderMaximum: Int = 4,
    val videoMimeType: String = "video/avc",
    val showDiagnostics: Boolean = false,
    val focusableSlots: Boolean = true,
    val autoFocusFirstSlot: Boolean = true,
    val metricsSampleIntervalMs: Long = 1_000L,
) {
    init {
        require(reserveHardwareDecoders >= 0) { "Decoder reserve must not be negative" }
        require(unknownDecoderMaximum > 0) { "Unknown decoder maximum must be positive" }
        require(metricsSampleIntervalMs >= 250L) { "Metrics interval must be at least 250 ms" }
        require(videoMimeType.startsWith("video/")) { "A video MIME type is required" }
    }
}
