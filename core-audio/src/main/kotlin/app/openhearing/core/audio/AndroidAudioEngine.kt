package app.openhearing.core.audio

import android.content.Context
import android.media.AudioRecord
import android.media.AudioRouting
import android.media.AudioTimestamp
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock

enum class AudioSessionState { STOPPED, CONNECTING, RUNNING, FAILED }

data class AudioSessionStatus(
    val state: AudioSessionState = AudioSessionState.STOPPED,
    val message: String? = null,
    val sampleRateHz: Int = 0,
    val monoOutput: Boolean = false,
    val bluetoothCallAudio: Boolean = false,
)

/** Cancellable capture -> float DSP -> playback, with explicit, verified device routing. */
class AndroidAudioEngine(
    private val context: Context,
    private val bypassEffects: () -> AutoCloseable = { AutoCloseable {} },
    private val onStatus: (AudioSessionStatus) -> Unit = {},
) : AudioEngine {
    @Volatile private var requested = false

    @Volatile private var running = false

    @Volatile private var record: AudioRecord? = null

    @Volatile private var track: AudioTrack? = null

    @Volatile private var routeLost = false

    @Volatile private var generation = 0L
    private var thread: Thread? = null

    override val isRunning: Boolean get() = running

    override fun start(
        format: AudioFormat,
        processor: AudioProcessor,
    ) = startSession(format) { actual ->
        require(actual == format) { "Use startSession to build a processor for the negotiated headset format." }
        processor
    }

    /** Construct DSP after selecting the transport rate, so filters and time constants agree with I/O. */
    @Synchronized
    fun startSession(
        format: AudioFormat,
        processorFactory: (AudioFormat) -> AudioProcessor,
    ) {
        if (thread?.isAlive == true) return
        requested = true
        routeLost = false
        val sessionId = ++generation
        onStatus(AudioSessionStatus(AudioSessionState.CONNECTING))
        thread = Thread({ runSession(format, processorFactory, sessionId) }, "OpenHearing-Assist").apply { start() }
    }

    override fun stop() {
        requested = false
        silence()
        thread?.interrupt()
        thread?.join(STOP_JOIN_TIMEOUT_MS)
    }

    private fun silence() {
        runCatching { track?.pause() }
        runCatching { track?.flush() }
        runCatching { record?.stop() }
    }

    private fun loseRoute() {
        routeLost = true
        silence()
    }

    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    private fun runSession(
        requestedFormat: AudioFormat,
        processorFactory: (AudioFormat) -> AudioProcessor,
        sessionId: Long,
    ) {
        // Android can deliver a queued callback after its listener was removed.
        val onLost = { if (generation == sessionId && requested) loseRoute() }
        val route = AssistAudioRoute(context, onLost)
        var failure: String? = null
        var bypass: AutoCloseable? = null
        try {
            bypass = bypassEffects()
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            route.open(requestedFormat.microphoneSource, requestedFormat.requireHeadphones) { requested && !routeLost }
            checkActive()
            runRoutedSession(requestedFormat, route, processorFactory, onLost)
        } catch (e: InterruptedException) {
            if (requested) failure = "The audio connection was interrupted. Try starting again."
            Thread.currentThread().interrupt()
        } catch (e: SecurityException) {
            failure = "Microphone access is unavailable. Allow microphone access and start again."
        } catch (e: RuntimeException) {
            failure = e.message ?: "Audio could not start on this device. Try reconnecting your headset."
        } finally {
            try {
                finishSession(route, failure)
            } finally {
                bypass?.close()
            }
        }
    }

    private fun finishSession(
        route: AssistAudioRoute,
        failure: String?,
    ) {
        running = false
        releaseStreams()
        route.close()
        val failed = requested && (failure != null || routeLost)
        synchronized(this) {
            requested = false
            thread = null
            onStatus(
                AudioSessionStatus(
                    if (failed) AudioSessionState.FAILED else AudioSessionState.STOPPED,
                    if (routeLost) {
                        "Audio route lost or interrupted. Reconnect your headset and start again."
                    } else {
                        failure
                    },
                ),
            )
        }
    }

    private fun releaseStreams() {
        silence()
        runCatching { record?.release() }
        runCatching { track?.release() }
        record = null
        track = null
    }

    private fun runRoutedSession(
        requestedFormat: AudioFormat,
        route: AssistAudioRoute,
        processorFactory: (AudioFormat) -> AudioProcessor,
        onLost: () -> Unit,
    ) {
        val candidates =
            AudioRoutePolicy.candidateFormats(
                requestedFormat,
                route.sco,
                route.input.sampleRates,
                route.output?.sampleRates ?: intArrayOf(),
            )
        var lastFailure: RuntimeException? = null
        for (format in candidates) {
            checkActive()
            val preparationFailure: RuntimeException? =
                try {
                    prepareStreams(format, route)
                    null
                } catch (e: SecurityException) {
                    throw e
                } catch (e: IllegalArgumentException) {
                    e
                } catch (e: IllegalStateException) {
                    e
                }
            if (preparationFailure != null) {
                lastFailure = preparationFailure
                releaseStreams()
                continue
            }
            // Model initialization failures and failures after playback begins are NOT format retries.
            val processor = processorFactory(format)
            try {
                runStreams(format, route, processor, onLost)
            } finally {
                (processor as? AutoCloseable)?.close()
            }
            return
        }
        error("No compatible microphone/playback format could start. ${lastFailure?.message.orEmpty()}")
    }

    private fun prepareStreams(
        format: AudioFormat,
        route: AssistAudioRoute,
    ) {
        val channels =
            AudioRoutePolicy.outputChannels(
                format.channelCount,
                route.sco,
                route.output?.channelCounts ?: intArrayOf(),
            )
        val streams = AssistAudioStreams(format, channels, route.attributes)
        record = streams.createRecord(context)
        track = streams.createTrack()
        val capture = checkNotNull(record)
        val playback = checkNotNull(track)
        check(capture.state == AudioRecord.STATE_INITIALIZED && playback.state == AudioTrack.STATE_INITIALIZED) {
            "Audio stream initialization was rejected"
        }
        check(capture.sampleRate == format.sampleRateHz && playback.sampleRate == format.sampleRateHz) {
            "Android selected an unexpected PCM rate"
        }
        check(capture.setPreferredDevice(route.input)) { "Android rejected the selected microphone." }
        route.output?.let { check(playback.setPreferredDevice(it)) { "Android rejected the selected output." } }
        capture.startRecording()
        check(capture.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "Microphone could not start" }
        playback.play()
        check(playback.playState == AudioTrack.PLAYSTATE_PLAYING) { "Headset playback could not start" }
    }

    private fun runStreams(
        format: AudioFormat,
        route: AssistAudioRoute,
        processor: AudioProcessor,
        onLost: () -> Unit,
    ) {
        val outputChannels =
            AudioRoutePolicy.outputChannels(
                format.channelCount,
                route.sco,
                route.output?.channelCounts ?: intArrayOf(),
            )
        val capture = checkNotNull(record)
        val playback = checkNotNull(track)
        val listener =
            AudioRouting.OnRoutingChangedListener {
                if (running && !route.matches(capture.routedDevice, playback.routedDevice)) onLost()
            }
        val handler = Handler(Looper.getMainLooper())
        capture.addOnRoutingChangedListener(listener, handler)
        playback.addOnRoutingChangedListener(listener, handler)
        try {
            checkActive()
            pump(format, outputChannels, route, processor, capture, playback)
        } finally {
            capture.removeOnRoutingChangedListener(listener)
            playback.removeOnRoutingChangedListener(listener)
        }
    }

    private fun pump(
        format: AudioFormat,
        outputChannels: Int,
        route: AssistAudioRoute,
        processor: AudioProcessor,
        capture: AudioRecord,
        playback: AudioTrack,
    ) {
        val buffers = AssistPcmBuffer(format, outputChannels)
        val input = buffers.input
        val deadline = SystemClock.elapsedRealtime() + ROUTE_TIMEOUT_MS
        var frames = 0
        var readFrames = 0L
        var nextTelemetryFrame = 0L
        val timestamp = AudioTimestamp()
        val observer = (processor as? AudioStreamObserver)?.takeIf { it.wantsStreamDiagnostics }
        while (requested && !routeLost) {
            val read = capture.read(input, frames, input.size - frames, AudioRecord.READ_BLOCKING)
            checkActive()
            check(read > 0) { "Microphone stopped delivering audio ($read)." }
            frames += read
            readFrames += read
            if (frames < input.size) continue
            frames = 0
            val matched = route.matches(capture.routedDevice, playback.routedDevice)
            if (running) check(matched) { "The selected audio route disconnected." }
            if (!matched) {
                check(SystemClock.elapsedRealtime() < deadline) {
                    "Android could not activate the selected audio route."
                }
                // Only silence is submitted until BOTH actual routes are confirmed.
                buffers.output.fill(0)
            } else {
                if (!running) {
                    observer?.onStreamStarted(streamMetadata(format, outputChannels, capture, playback))
                    running = true
                    onStatus(
                        AudioSessionStatus(
                            AudioSessionState.RUNNING,
                            route.input.productName.toString(),
                            format.sampleRateHz,
                            outputChannels == 1,
                            route.sco,
                        ),
                    )
                }
                buffers.process(processor)
                if (observer != null && readFrames >= nextTelemetryFrame) {
                    val valid =
                        capture.getTimestamp(timestamp, AudioTimestamp.TIMEBASE_MONOTONIC) ==
                            AudioRecord.SUCCESS
                    observer.onCaptureTiming(
                        readFrames,
                        if (valid) timestamp.framePosition else -1L,
                        if (valid) timestamp.nanoTime else -1L,
                        playback.underrunCount,
                    )
                    nextTelemetryFrame = readFrames + format.sampleRateHz / 5
                }
            }
            writeAll(playback, buffers.output)
        }
    }

    private fun streamMetadata(
        format: AudioFormat,
        outputChannels: Int,
        capture: AudioRecord,
        playback: AudioTrack,
    ): Map<String, String> =
        mapOf(
            "routed_input" to capture.routedDevice?.productName.toString(),
            "routed_output" to playback.routedDevice?.productName.toString(),
            "input_type" to capture.routedDevice?.type.toString(),
            "output_type" to playback.routedDevice?.type.toString(),
            "android_capture_rate" to capture.sampleRate.toString(),
            "android_playback_rate" to playback.sampleRate.toString(),
            "capture_source" to capture.audioSource.toString(),
            "input_tuning" to format.inputTuning.name,
            "device_output_channels" to outputChannels.toString(),
            "capture_buffer_frames" to capture.bufferSizeInFrames.toString(),
        )

    private fun writeAll(
        playback: AudioTrack,
        output: ShortArray,
    ) {
        var offset = 0
        while (offset < output.size && requested && !routeLost) {
            val written = playback.write(output, offset, output.size - offset, AudioTrack.WRITE_BLOCKING)
            checkActive()
            check(written > 0) { "Headset stopped accepting audio ($written)." }
            offset += written
        }
    }

    private fun checkActive() {
        if (!requested) throw InterruptedException("Stopped")
        check(!routeLost) { "Audio route lost or interrupted." }
    }

    private companion object {
        const val STOP_JOIN_TIMEOUT_MS = 500L
        const val ROUTE_TIMEOUT_MS = 5_000L
    }
}
