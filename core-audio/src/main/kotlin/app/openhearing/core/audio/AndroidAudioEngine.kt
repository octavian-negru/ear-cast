package app.openhearing.core.audio

import android.content.Context
import android.media.AudioRecord
import android.media.AudioRouting
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
)

/** Cancellable capture -> float DSP -> playback, with explicit, verified device routing. */
class AndroidAudioEngine(private val context: Context, private val onStatus: (AudioSessionStatus) -> Unit = {}) :
    AudioEngine {
    @Volatile private var requested = false

    @Volatile private var running = false

    @Volatile private var record: AudioRecord? = null

    @Volatile private var track: AudioTrack? = null

    @Volatile private var routeLost = false

    @Volatile private var generation = 0L
    private var thread: Thread? = null

    override val isRunning: Boolean get() = running

    override fun start(format: AudioFormat, processor: AudioProcessor) = startSession(format) { actual ->
        require(actual == format) { "Use startSession to build a processor for the negotiated headset format." }
        processor
    }

    /** Construct DSP after selecting the transport rate, so filters and time constants agree with I/O. */
    @Synchronized
    fun startSession(format: AudioFormat, processorFactory: (AudioFormat) -> AudioProcessor) {
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
        try {
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
            finishSession(route, failure)
        }
    }

    private fun finishSession(route: AssistAudioRoute, failure: String?) {
        running = false
        silence()
        runCatching { record?.release() }
        runCatching { track?.release() }
        record = null
        track = null
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

    private fun runRoutedSession(
        requestedFormat: AudioFormat,
        route: AssistAudioRoute,
        processorFactory: (AudioFormat) -> AudioProcessor,
        onLost: () -> Unit,
    ) {
        val format = AudioRoutePolicy.processingFormat(requestedFormat, route.sco, route.legacy)
        val processor = processorFactory(format)
        try {
            runStreams(format, route, processor, onLost)
        } finally {
            // Runs on the worker even if stream creation, routing or capture fails.
            (processor as? AutoCloseable)?.close()
        }
    }

    private fun runStreams(
        format: AudioFormat,
        route: AssistAudioRoute,
        processor: AudioProcessor,
        onLost: () -> Unit,
    ) {
        val outputChannels = if (route.sco) 1 else format.channelCount
        val streams = AssistAudioStreams(format, outputChannels, route.attributes)
        record = streams.createRecord(context)
        track = streams.createTrack()
        val capture = checkNotNull(record)
        val playback = checkNotNull(track)
        check(capture.setPreferredDevice(route.input)) { "Android rejected the selected microphone." }
        route.output?.let { check(playback.setPreferredDevice(it)) { "Android rejected the selected output." } }
        val listener = AudioRouting.OnRoutingChangedListener {
            if (running && !route.matches(capture.routedDevice, playback.routedDevice)) onLost()
        }
        val handler = Handler(Looper.getMainLooper())
        capture.addOnRoutingChangedListener(listener, handler)
        playback.addOnRoutingChangedListener(listener, handler)
        try {
            checkActive()
            capture.startRecording()
            playback.play()
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
        while (requested && !routeLost) {
            val read = capture.read(input, frames, input.size - frames, AudioRecord.READ_BLOCKING)
            checkActive()
            check(read > 0) { "Microphone stopped delivering audio ($read)." }
            frames += read
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
                    running = true
                    onStatus(
                        AudioSessionStatus(
                            AudioSessionState.RUNNING,
                            route.input.productName.toString(),
                            format.sampleRateHz,
                            outputChannels == 1,
                        ),
                    )
                }
                buffers.process(processor)
            }
            writeAll(playback, buffers.output)
        }
    }

    private fun writeAll(playback: AudioTrack, output: ShortArray) {
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
