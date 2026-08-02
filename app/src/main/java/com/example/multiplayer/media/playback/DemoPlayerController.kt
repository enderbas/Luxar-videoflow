package com.example.multiplayer.media.playback

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

@UnstableApi
class DemoPlayerController(
    context: Context,
    private val videos: List<DemoVideo>,
) {
    private val appContext = context.applicationContext
    private val mutableSlots = mutableStateListOf<DemoPlayerSlot>()
    private var nextSlotToPrepare = 0

    val slots: List<DemoPlayerSlot> = mutableSlots

    fun start() {
        if (mutableSlots.isNotEmpty()) return

        nextSlotToPrepare = 0
        mutableSlots += videos.map { video ->
            createSlot(video)
        }
        prepareNextSlot()
    }

    fun stop() {
        mutableSlots.forEach(DemoPlayerSlot::release)
        mutableSlots.clear()
        nextSlotToPrepare = 0
    }

    private fun createSlot(video: DemoVideo): DemoPlayerSlot {
        val trackSelector = DefaultTrackSelector(appContext).apply {
            parameters = buildUponParameters()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                .setTunnelingEnabled(false)
                .build()
        }
        val renderersFactory = DefaultRenderersFactory(appContext)
            .setMediaCodecSelector(HardwareOnlyMediaCodecSelector)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                MIN_BUFFER_MS,
                MAX_BUFFER_MS,
                PLAYBACK_BUFFER_MS,
                REBUFFER_MS,
            )
            .setTargetBufferBytes(TARGET_BUFFER_BYTES)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
        val player = ExoPlayer.Builder(appContext, renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_ONE
                volume = 0f
                setMediaItem(MediaItem.fromUri(Uri.parse("asset:///${video.assetPath}")))
            }

        return DemoPlayerSlot(
            video = video,
            player = player,
            assetByteLength = appContext.assets.openFd(video.assetPath).length,
            onPreparedOrFailed = ::prepareNextSlot,
        )
    }

    private fun prepareNextSlot() {
        val slot = mutableSlots.getOrNull(nextSlotToPrepare) ?: return
        nextSlotToPrepare += 1
        slot.prepare()
    }

    private companion object {
        const val MIN_BUFFER_MS = 1_000
        const val MAX_BUFFER_MS = 3_000
        const val PLAYBACK_BUFFER_MS = 250
        const val REBUFFER_MS = 500
        const val TARGET_BUFFER_BYTES = 2 * 1024 * 1024
    }
}

@UnstableApi
class DemoPlayerSlot(
    val video: DemoVideo,
    val player: ExoPlayer,
    private val assetByteLength: Long,
    private val onPreparedOrFailed: () -> Unit,
) : Player.Listener, AnalyticsListener {
    var status by mutableStateOf(DemoPlayerStatus.WAITING)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var hasPrepared by mutableStateOf(false)
        private set
    var metrics by mutableStateOf(DemoPlaybackMetrics())
        private set

    private var advancedQueue = false
    private var released = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastRenderedBufferCount = 0
    private var lastSampleRealtimeMs = 0L
    private val metricsSampler = object : Runnable {
        override fun run() {
            if (released) return
            sampleMetrics()
            mainHandler.postDelayed(this, METRICS_SAMPLE_INTERVAL_MS)
        }
    }

    init {
        player.addListener(this)
        player.addAnalyticsListener(this)
    }

    fun prepare() {
        if (hasPrepared) return
        hasPrepared = true
        status = DemoPlayerStatus.PREPARING
        player.prepare()
        player.playWhenReady = true
        startMetricsSampling()
    }

    fun release() {
        if (released) return
        released = true
        mainHandler.removeCallbacks(metricsSampler)
        player.removeAnalyticsListener(this)
        player.removeListener(this)
        player.release()
    }

    override fun onVideoDecoderInitialized(
        eventTime: AnalyticsListener.EventTime,
        decoderName: String,
        initializedTimestampMs: Long,
        initializationDurationMs: Long,
    ) {
        metrics = metrics.copy(decoderName = decoderName)
    }

    override fun onVideoInputFormatChanged(
        eventTime: AnalyticsListener.EventTime,
        format: Format,
        decoderReuseEvaluation: DecoderReuseEvaluation?,
    ) {
        metrics = metrics.withFormat(format, estimatedBitrate(format))
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        status = when (playbackState) {
            Player.STATE_BUFFERING -> DemoPlayerStatus.PREPARING
            Player.STATE_READY -> {
                advanceQueueOnce()
                if (player.playWhenReady) {
                    DemoPlayerStatus.PLAYING
                } else {
                    DemoPlayerStatus.PAUSED
                }
            }
            Player.STATE_ENDED -> DemoPlayerStatus.PAUSED
            else -> status
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (player.playbackState == Player.STATE_READY) {
            status = if (isPlaying) {
                DemoPlayerStatus.PLAYING
            } else {
                DemoPlayerStatus.PAUSED
            }
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        status = DemoPlayerStatus.ERROR
        errorMessage = error.errorCodeName
        advanceQueueOnce()
    }

    private fun advanceQueueOnce() {
        if (advancedQueue) return
        advancedQueue = true
        onPreparedOrFailed()
    }

    private fun startMetricsSampling() {
        val counters = player.videoDecoderCounters
        lastRenderedBufferCount = counters?.renderedOutputBufferCount ?: 0
        lastSampleRealtimeMs = SystemClock.elapsedRealtime()
        mainHandler.postDelayed(metricsSampler, METRICS_SAMPLE_INTERVAL_MS)
    }

    private fun sampleMetrics() {
        val nowMs = SystemClock.elapsedRealtime()
        val elapsedMs = nowMs - lastSampleRealtimeMs
        val counters = player.videoDecoderCounters
        counters?.ensureUpdated()
        val renderedBufferCount = counters?.renderedOutputBufferCount ?: lastRenderedBufferCount
        val renderedDelta = (renderedBufferCount - lastRenderedBufferCount).coerceAtLeast(0)
        val renderedFps = if (elapsedMs > 0) {
            renderedDelta * 1_000f / elapsedMs
        } else {
            metrics.renderedFps
        }
        val format = player.videoFormat

        metrics = metrics.copy(
            renderedFps = renderedFps,
            droppedFrames = counters?.droppedBufferCount ?: metrics.droppedFrames,
            bufferedDurationMs = player.totalBufferedDuration.coerceAtLeast(0L),
        ).let { sampled ->
            if (format == null) sampled else sampled.withFormat(format, estimatedBitrate(format))
        }

        lastRenderedBufferCount = renderedBufferCount
        lastSampleRealtimeMs = nowMs
    }

    private fun estimatedBitrate(format: Format): Long? {
        if (format.bitrate != Format.NO_VALUE) return format.bitrate.toLong()

        val durationMs = player.duration
        return if (durationMs != C.TIME_UNSET && durationMs > 0 && assetByteLength > 0) {
            assetByteLength * 8_000L / durationMs
        } else {
            metrics.bitrateBitsPerSecond
        }
    }

    private companion object {
        const val METRICS_SAMPLE_INTERVAL_MS = 1_000L
    }
}

data class DemoPlaybackMetrics(
    val renderedFps: Float? = null,
    val sourceFrameRate: Float? = null,
    val width: Int? = null,
    val height: Int? = null,
    val bitrateBitsPerSecond: Long? = null,
    val sampleMimeType: String? = null,
    val codecString: String? = null,
    val decoderName: String? = null,
    val droppedFrames: Int = 0,
    val bufferedDurationMs: Long = 0,
)

@UnstableApi
private fun DemoPlaybackMetrics.withFormat(format: Format, bitrate: Long?) = copy(
    sourceFrameRate = format.frameRate.takeIf { it > 0f },
    width = format.width.takeIf { it > 0 },
    height = format.height.takeIf { it > 0 },
    bitrateBitsPerSecond = bitrate,
    sampleMimeType = format.sampleMimeType,
    codecString = format.codecs,
)

enum class DemoPlayerStatus {
    WAITING,
    PREPARING,
    PLAYING,
    PAUSED,
    ERROR,
}
