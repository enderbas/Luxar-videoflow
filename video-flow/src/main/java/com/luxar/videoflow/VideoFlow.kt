package com.luxar.videoflow

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaCodec
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.MainThread
import androidx.annotation.OptIn
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
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
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import java.io.File
import java.util.Locale
import kotlin.math.min

object VideoFlow {
    @MainThread
    fun initialize(
        container: FrameLayout,
        lifecycleOwner: LifecycleOwner,
        config: VideoFlowConfig = VideoFlowConfig(),
    ): VideoFlowEngine {
        checkMainThread()
        return VideoFlowEngine(container, lifecycleOwner, config)
    }
}

class VideoPlayerHandle internal constructor(
    private val engine: VideoFlowEngine,
    val id: String,
) {
    val snapshot: VideoPlayerSnapshot
        @MainThread get() = engine.snapshot(id)

    @MainThread
    fun play() = engine.play(id)

    @MainThread
    fun pause() = engine.pause(id)

    @MainThread
    fun retry() = engine.retry(id)

    @MainThread
    fun updatePlacement(placement: VideoPlacement) = engine.updatePlacement(id, placement)

    @MainThread
    fun stop() = engine.stop(id)
}

@OptIn(UnstableApi::class)
class VideoFlowEngine internal constructor(
    private val container: FrameLayout,
    private val lifecycleOwner: LifecycleOwner,
    val config: VideoFlowConfig,
) : DefaultLifecycleObserver {
    private val context = container.context
    private val slots = linkedMapOf<String, Slot>()
    private val listeners = linkedSetOf<VideoFlowListener>()
    private var foreground = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    private var released = false
    private var preparingSlot: Slot? = null
    private var runtimeLimit: Int

    val decoderCapacity: DecoderCapacity = HardwareDecoderDiscovery.discover(config)

    private val layoutListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
        applyAllPlacements()
    }

    init {
        runtimeLimit = decoderCapacity.availablePlayers
        lifecycleOwner.lifecycle.addObserver(this)
        container.addOnLayoutChangeListener(layoutListener)
    }

    @MainThread
    fun run(request: VideoRequest): VideoPlayerHandle {
        checkUsable()
        require(request.id !in slots) {
            "A player with id '${request.id}' already exists; stop it before reusing the id"
        }

        val showDiagnostics = request.showDiagnostics ?: config.showDiagnostics
        val view = VideoSlotView(context, request, showDiagnostics, config.focusableSlots)
        val slot = Slot(request, request.placement, view)
        slots[request.id] = slot
        container.addView(view)
        applyPlacement(slot)
        reorderViews()
        updateCapacityStates()
        notifyChanged(slot)

        if (config.autoFocusFirstSlot && slots.size == 1 && config.focusableSlots) {
            view.post { view.requestFocus() }
        }

        startNextIfPossible()
        return VideoPlayerHandle(this, request.id)
    }

    @MainThread
    fun stop(id: String) {
        checkMainThread()
        val slot = slots.remove(id) ?: return
        if (preparingSlot === slot) preparingSlot = null
        releaseRuntime(slot)
        slot.state = VideoPlayerState.RELEASED
        notifyChanged(slot)
        container.removeView(slot.view)
        updateCapacityStates()
        startNextIfPossible()
    }

    @MainThread
    fun play(id: String) {
        checkUsable()
        val slot = requireSlot(id)
        val runtime = slot.runtime
        if (runtime == null) {
            if (slot.state == VideoPlayerState.ERROR) slot.errorCode = null
            slot.state = VideoPlayerState.QUEUED
            startNextIfPossible()
        } else {
            runtime.player.playWhenReady = true
        }
    }

    @MainThread
    fun pause(id: String) {
        checkUsable()
        requireSlot(id).runtime?.player?.playWhenReady = false
    }

    @MainThread
    fun retry(id: String) {
        checkUsable()
        val slot = requireSlot(id)
        if (preparingSlot === slot) preparingSlot = null
        releaseRuntime(slot)
        slot.errorCode = null
        slot.state = VideoPlayerState.QUEUED
        notifyChanged(slot)
        startNextIfPossible()
    }

    @MainThread
    fun updatePlacement(id: String, placement: VideoPlacement) {
        checkUsable()
        val slot = requireSlot(id)
        slot.placement = placement
        applyPlacement(slot)
        reorderViews()
    }

    @MainThread
    fun snapshot(id: String): VideoPlayerSnapshot {
        checkMainThread()
        val slot = requireSlot(id)
        return slot.snapshot()
    }

    @MainThread
    fun addListener(listener: VideoFlowListener) {
        checkUsable()
        listeners += listener
        slots.values.forEach { listener.onPlayerChanged(it.snapshot()) }
    }

    @MainThread
    fun removeListener(listener: VideoFlowListener) {
        checkMainThread()
        listeners -= listener
    }

    @MainThread
    fun release() {
        checkMainThread()
        if (released) return
        released = true
        foreground = false
        preparingSlot = null
        lifecycleOwner.lifecycle.removeObserver(this)
        container.removeOnLayoutChangeListener(layoutListener)
        slots.values.forEach { slot ->
            releaseRuntime(slot)
            slot.state = VideoPlayerState.RELEASED
            notifyChanged(slot)
            container.removeView(slot.view)
        }
        slots.clear()
        listeners.clear()
    }

    override fun onStart(owner: LifecycleOwner) {
        checkMainThread()
        if (released) return
        foreground = true
        slots.values.filter { it.state != VideoPlayerState.ERROR }.forEach {
            if (it.runtime == null) it.state = VideoPlayerState.QUEUED
        }
        updateCapacityStates()
        startNextIfPossible()
    }

    override fun onStop(owner: LifecycleOwner) {
        checkMainThread()
        if (released) return
        foreground = false
        preparingSlot = null
        slots.values.forEach { slot ->
            releaseRuntime(slot)
            if (slot.state != VideoPlayerState.ERROR) {
                slot.state = VideoPlayerState.QUEUED
                notifyChanged(slot)
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        release()
    }

    private fun startNextIfPossible() {
        if (!foreground || released || preparingSlot != null) return

        val activeCount = slots.values.count { it.runtime != null }
        if (activeCount >= runtimeLimit) {
            updateCapacityStates()
            return
        }

        val slot = slots.values.firstOrNull {
            it.runtime == null &&
                (it.state == VideoPlayerState.QUEUED || it.state == VideoPlayerState.CAPACITY_LIMITED)
        } ?: return

        slot.state = VideoPlayerState.PREPARING
        slot.errorCode = null
        notifyChanged(slot)
        preparingSlot = slot

        val runtime = SlotRuntime(
            context = context,
            request = slot.request,
            sampleIntervalMs = config.metricsSampleIntervalMs,
            onState = { state ->
                if (slots[slot.request.id] !== slot || released) return@SlotRuntime
                slot.state = state
                notifyChanged(slot)
            },
            onMetrics = { metrics ->
                if (slots[slot.request.id] !== slot || released) return@SlotRuntime
                slot.metrics = metrics
                notifyChanged(slot)
            },
            onPrepared = {
                if (preparingSlot === slot) preparingSlot = null
                startNextIfPossible()
            },
            onFailure = { error, insufficientResources ->
                if (slots[slot.request.id] !== slot || released) return@SlotRuntime
                if (preparingSlot === slot) preparingSlot = null
                releaseRuntime(slot)
                slot.errorCode = error.errorCodeName
                if (insufficientResources) {
                    runtimeLimit = min(runtimeLimit, slots.values.count { it.runtime != null })
                    slot.state = VideoPlayerState.CAPACITY_LIMITED
                } else {
                    slot.state = VideoPlayerState.ERROR
                }
                notifyChanged(slot)
                updateCapacityStates()
                startNextIfPossible()
            },
        )
        slot.runtime = runtime
        slot.view.bind(runtime.player)
        runtime.prepare()
    }

    private fun releaseRuntime(slot: Slot) {
        slot.view.bind(null)
        slot.runtime?.release()
        slot.runtime = null
    }

    private fun updateCapacityStates() {
        val activeCount = slots.values.count { it.runtime != null }
        var available = (runtimeLimit - activeCount).coerceAtLeast(0)
        slots.values.filter { it.runtime == null && it.state != VideoPlayerState.ERROR }.forEach { slot ->
            val newState = if (available > 0) {
                available -= 1
                VideoPlayerState.QUEUED
            } else {
                VideoPlayerState.CAPACITY_LIMITED
            }
            if (slot.state != newState) {
                slot.state = newState
                notifyChanged(slot)
            }
        }
    }

    private fun applyAllPlacements() {
        slots.values.forEach(::applyPlacement)
    }

    private fun applyPlacement(slot: Slot) {
        val rect = PlacementResolver.resolve(
            placement = slot.placement,
            coordinateSpace = config.coordinateSpace,
            containerWidth = container.width,
            containerHeight = container.height,
        )
        slot.view.layoutParams = FrameLayout.LayoutParams(rect.width, rect.height).apply {
            leftMargin = rect.left
            topMargin = rect.top
        }
        slot.view.translationZ = slot.placement.zIndex.toFloat()
    }

    private fun reorderViews() {
        slots.values.sortedBy { it.placement.zIndex }.forEach { it.view.bringToFront() }
    }

    private fun notifyChanged(slot: Slot) {
        val snapshot = slot.snapshot()
        slot.view.render(snapshot)
        listeners.forEach { it.onPlayerChanged(snapshot) }
    }

    private fun requireSlot(id: String): Slot =
        requireNotNull(slots[id]) { "Unknown player id '$id'" }

    private fun checkUsable() {
        checkMainThread()
        check(!released) { "VideoFlowEngine has been released" }
    }

    private data class Slot(
        val request: VideoRequest,
        var placement: VideoPlacement,
        val view: VideoSlotView,
        var runtime: SlotRuntime? = null,
        var state: VideoPlayerState = VideoPlayerState.QUEUED,
        var metrics: VideoPlayerMetrics = VideoPlayerMetrics(),
        var errorCode: String? = null,
    ) {
        fun snapshot() = VideoPlayerSnapshot(request.id, state, metrics, errorCode)
    }
}

@UnstableApi
private class SlotRuntime(
    context: Context,
    request: VideoRequest,
    private val sampleIntervalMs: Long,
    private val onState: (VideoPlayerState) -> Unit,
    private val onMetrics: (VideoPlayerMetrics) -> Unit,
    private val onPrepared: () -> Unit,
    private val onFailure: (PlaybackException, Boolean) -> Unit,
) : Player.Listener, AnalyticsListener {
    private val appContext = context.applicationContext
    private val sourceByteLength = sourceByteLength(appContext, request.source)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var released = false
    private var advancedQueue = false
    private var metrics = VideoPlayerMetrics()
    private var lastRenderedBufferCount = 0
    private var lastSampleRealtimeMs = 0L

    val player: ExoPlayer

    private val sampler = object : Runnable {
        override fun run() {
            if (released) return
            sampleMetrics()
            mainHandler.postDelayed(this, sampleIntervalMs)
        }
    }

    init {
        val trackSelector = DefaultTrackSelector(appContext).apply {
            parameters = buildUponParameters()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, request.muted)
                .setTunnelingEnabled(false)
                .build()
        }
        val renderersFactory = DefaultRenderersFactory(appContext)
            .setMediaCodecSelector(HardwareOnlyMediaCodecSelector)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(1_000, 3_000, 250, 500)
            .setTargetBufferBytes(2 * 1024 * 1024)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        player = ExoPlayer.Builder(appContext, renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build()
            .apply {
                repeatMode = if (request.loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                volume = if (request.muted) 0f else 1f
                setMediaItem(MediaItem.fromUri(sourceUri(request.source)))
                addListener(this@SlotRuntime)
                addAnalyticsListener(this@SlotRuntime)
            }
    }

    fun prepare() {
        player.prepare()
        player.playWhenReady = true
        lastRenderedBufferCount = player.videoDecoderCounters?.renderedOutputBufferCount ?: 0
        lastSampleRealtimeMs = SystemClock.elapsedRealtime()
        mainHandler.postDelayed(sampler, sampleIntervalMs)
    }

    fun release() {
        if (released) return
        released = true
        mainHandler.removeCallbacks(sampler)
        player.removeAnalyticsListener(this)
        player.removeListener(this)
        player.release()
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            Player.STATE_IDLE -> Unit
            Player.STATE_BUFFERING -> onState(VideoPlayerState.PREPARING)
            Player.STATE_READY -> {
                advanceQueueOnce()
                onState(if (player.playWhenReady) VideoPlayerState.PLAYING else VideoPlayerState.PAUSED)
            }
            Player.STATE_ENDED -> onState(VideoPlayerState.PAUSED)
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (player.playbackState == Player.STATE_READY) {
            onState(if (isPlaying) VideoPlayerState.PLAYING else VideoPlayerState.PAUSED)
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        advanceQueueOnce()
        onFailure(error, error.hasInsufficientResourceCause())
    }

    override fun onVideoDecoderInitialized(
        eventTime: AnalyticsListener.EventTime,
        decoderName: String,
        initializedTimestampMs: Long,
        initializationDurationMs: Long,
    ) {
        metrics = metrics.copy(decoderName = decoderName)
        onMetrics(metrics)
    }

    override fun onVideoInputFormatChanged(
        eventTime: AnalyticsListener.EventTime,
        format: Format,
        decoderReuseEvaluation: DecoderReuseEvaluation?,
    ) {
        metrics = metrics.withFormat(format, estimatedBitrate(format))
        onMetrics(metrics)
    }

    private fun sampleMetrics() {
        val nowMs = SystemClock.elapsedRealtime()
        val elapsedMs = nowMs - lastSampleRealtimeMs
        val counters = player.videoDecoderCounters
        counters?.ensureUpdated()
        val renderedCount = counters?.renderedOutputBufferCount ?: lastRenderedBufferCount
        val renderedDelta = (renderedCount - lastRenderedBufferCount).coerceAtLeast(0)
        val renderedFps = if (elapsedMs > 0) renderedDelta * 1_000f / elapsedMs else metrics.renderedFps
        val format = player.videoFormat

        metrics = metrics.copy(
            renderedFps = renderedFps,
            droppedFrames = counters?.droppedBufferCount ?: metrics.droppedFrames,
            bufferedDurationMs = player.totalBufferedDuration.coerceAtLeast(0L),
        ).let { sampled ->
            if (format == null) sampled else sampled.withFormat(format, estimatedBitrate(format))
        }
        onMetrics(metrics)
        lastRenderedBufferCount = renderedCount
        lastSampleRealtimeMs = nowMs
    }

    private fun estimatedBitrate(format: Format): Long? {
        if (format.bitrate != Format.NO_VALUE) return format.bitrate.toLong()
        val durationMs = player.duration
        return if (durationMs != C.TIME_UNSET && durationMs > 0 && sourceByteLength > 0) {
            sourceByteLength * 8_000L / durationMs
        } else {
            metrics.bitrateBitsPerSecond
        }
    }

    private fun advanceQueueOnce() {
        if (advancedQueue) return
        advancedQueue = true
        onPrepared()
    }
}

@UnstableApi
private object HardwareOnlyMediaCodecSelector : MediaCodecSelector {
    override fun getDecoderInfos(
        mimeType: String,
        requiresSecureDecoder: Boolean,
        requiresTunnelingDecoder: Boolean,
    ): List<MediaCodecInfo> = MediaCodecSelector.DEFAULT
        .getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder)
        .filter { it.hardwareAccelerated && !it.softwareOnly }
}

@UnstableApi
private object HardwareDecoderDiscovery {
    fun discover(config: VideoFlowConfig): DecoderCapacity {
        val decoders = HardwareOnlyMediaCodecSelector.getDecoderInfos(
            config.videoMimeType,
            requiresSecureDecoder = false,
            requiresTunnelingDecoder = false,
        )
        val advertised = decoders.firstOrNull()
            ?.getMaxSupportedInstances()
            ?.takeIf { it != MediaCodecInfo.MAX_SUPPORTED_INSTANCES_UNKNOWN && it > 0 }
            ?: if (decoders.isEmpty()) 0 else config.unknownDecoderMaximum
        return DecoderCapacity(
            decoderNames = decoders.map { it.name },
            advertisedMaximum = advertised,
            reservedInstances = config.reserveHardwareDecoders,
            availablePlayers = (advertised - config.reserveHardwareDecoders).coerceAtLeast(0),
        )
    }
}

@OptIn(UnstableApi::class)
@SuppressLint("ViewConstructor", "SetTextI18n")
private class VideoSlotView(
    context: Context,
    private val request: VideoRequest,
    private val showDiagnostics: Boolean,
    focusable: Boolean,
) : FrameLayout(context) {
    private val playerView = PlayerView(context).apply {
        useController = false
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }
    private val title = TextView(context).apply {
        setTextColor(Color.WHITE)
        textSize = 14f
        setPadding(dp(10), dp(5), dp(10), dp(5))
        setBackgroundColor(Color.argb(190, 0, 0, 0))
    }
    private val diagnostics = TextView(context).apply {
        setTextColor(Color.rgb(216, 223, 235))
        textSize = 10f
        typeface = Typeface.MONOSPACE
        setPadding(dp(10), dp(5), dp(10), dp(5))
        setBackgroundColor(Color.argb(190, 0, 0, 0))
    }

    init {
        setBackgroundColor(Color.BLACK)
        tag = "video_slot_${request.id}"
        contentDescription = request.label
        isFocusable = focusable
        descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        addView(playerView)
        if (showDiagnostics) {
            addView(title, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = android.view.Gravity.TOP or android.view.Gravity.START
            })
            addView(diagnostics, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = android.view.Gravity.BOTTOM
            })
        }
        setOnFocusChangeListener { _, hasFocus -> updateBorder(hasFocus) }
        updateBorder(false)
    }

    fun bind(player: ExoPlayer?) {
        playerView.player = player
    }

    fun render(snapshot: VideoPlayerSnapshot) {
        if (!showDiagnostics) return
        title.text = "${request.label}\n${snapshot.state.displayName()}"
        diagnostics.text = DiagnosticsFormatter.format(snapshot.metrics)
        if (snapshot.errorCode != null) {
            title.setTextColor(Color.rgb(255, 138, 128))
        } else {
            title.setTextColor(Color.WHITE)
        }
    }

    private fun updateBorder(focused: Boolean) {
        foreground = GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
            setStroke(dp(if (focused) 4 else 1), if (focused) Color.rgb(139, 174, 255) else Color.rgb(37, 42, 52))
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}

private object DiagnosticsFormatter {
    fun format(metrics: VideoPlayerMetrics): String = listOf(
        listOf(
            resolution(metrics),
            codec(metrics.sampleMimeType),
            metrics.sourceFrameRate?.let { "${decimal(it)} source fps" } ?: "-- source fps",
        ).joinToString(SEPARATOR),
        listOf(
            metrics.renderedFps?.let { "${decimal(it)} render fps" } ?: "-- render fps",
            bitrate(metrics.bitrateBitsPerSecond),
            "${decimal(metrics.bufferedDurationMs / 1_000f)}s buffer",
        ).joinToString(SEPARATOR),
        "HW ${metrics.decoderName ?: "decoder pending"}${SEPARATOR}dropped ${metrics.droppedFrames}",
    ).joinToString("\n")

    private fun resolution(metrics: VideoPlayerMetrics) =
        if (metrics.width != null && metrics.height != null) "${metrics.width}×${metrics.height}" else "--×--"

    private fun codec(mimeType: String?) = when (mimeType) {
        "video/avc" -> "H.264/AVC"
        "video/hevc" -> "H.265/HEVC"
        "video/av01" -> "AV1"
        "video/x-vnd.on2.vp9" -> "VP9"
        else -> mimeType ?: "codec pending"
    }

    private fun bitrate(bitsPerSecond: Long?) = when {
        bitsPerSecond == null -> "-- bitrate"
        bitsPerSecond >= 1_000_000 -> "${twoDecimals(bitsPerSecond / 1_000_000f)} Mbps"
        else -> "${decimal(bitsPerSecond / 1_000f)} kbps"
    }

    private fun decimal(value: Float) = String.format(Locale.US, "%.1f", value)
    private fun twoDecimals(value: Float) = String.format(Locale.US, "%.2f", value)
    private const val SEPARATOR = " · "
}

private fun VideoPlayerMetrics.withFormat(format: Format, bitrate: Long?) = copy(
    sourceFrameRate = format.frameRate.takeIf { it > 0f },
    width = format.width.takeIf { it > 0 },
    height = format.height.takeIf { it > 0 },
    bitrateBitsPerSecond = bitrate,
    sampleMimeType = format.sampleMimeType,
)

private fun VideoPlayerState.displayName(): String = name.lowercase()
    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
    .replace('_', ' ')

private fun sourceUri(source: VideoSource): Uri = when (source) {
    is VideoSource.Asset -> Uri.parse("asset:///${source.path}")
    is VideoSource.FilePath -> Uri.fromFile(File(source.path))
    is VideoSource.ContentUri -> source.uri
}

private fun sourceByteLength(context: Context, source: VideoSource): Long = runCatching {
    when (source) {
        is VideoSource.Asset -> context.assets.openFd(source.path).use { it.length }
        is VideoSource.FilePath -> File(source.path).length()
        is VideoSource.ContentUri -> context.contentResolver.openAssetFileDescriptor(source.uri, "r")
            ?.use { it.length } ?: 0L
    }
}.getOrDefault(0L)

private fun PlaybackException.hasInsufficientResourceCause(): Boolean =
    generateSequence<Throwable>(this) { it.cause }.any {
        it is MediaCodec.CodecException && it.errorCode == MediaCodec.CodecException.ERROR_INSUFFICIENT_RESOURCE
    }

private fun checkMainThread() {
    check(Looper.myLooper() == Looper.getMainLooper()) {
        "VideoFlow must be accessed from the Android main thread"
    }
}
