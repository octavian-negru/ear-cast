package app.earcast.core.audio.diagnostics

import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.io.Writer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.LockSupport
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Bounded single-producer/single-consumer recorder. The audio worker copies into
 * preallocated slots; only the writer does file I/O. A full queue ends recording
 * at the last contiguous block rather than blocking playback or hiding a gap.
 * Four float WAV channels: captured mono, enhanced mono, limited left, limited right.
 */
class AudioSessionRecorder(
    val directory: File,
    private val sampleRate: Int,
    private val framesPerBlock: Int,
    metadata: Map<String, String>,
    private val onFinished: (File, String?) -> Unit,
) : AutoCloseable {
    private class Slot(
        size: Int,
    ) {
        val samples = FloatArray(size)
        var processingNanos = 0L
    }

    private val slots = Array(128) { Slot(framesPerBlock * CHANNELS) }
    private val published = AtomicLong()
    private val consumed = AtomicLong()
    private val working = FloatArray(framesPerBlock * CHANNELS)
    private val maximumBlocks = sampleRate.toLong() * MAX_SECONDS / framesPerBlock

    @Volatile private var finished = false

    @Volatile private var stopReason = "session_ended"

    @Volatile private var streamMetadata = metadata.toMap()
    private var producerBlocks = 0L

    @Volatile private var maxCaptureBacklogFrames = 0L

    @Volatile private var lastTimestampNanos = -1L

    @Volatile private var outputUnderruns = 0
    private val writer = Thread(::writeSession, "Ear Cast-Diagnostics").apply { isDaemon = true }

    init {
        require(sampleRate > 0 && framesPerBlock > 0 && maximumBlocks > 0)
        check(directory.mkdirs()) { "Could not create a diagnostic recording directory" }
        writer.start()
    }

    fun streamStarted(metadata: Map<String, String>) {
        streamMetadata = streamMetadata + metadata
    }

    fun captureTiming(
        readFrames: Long,
        hardwareFrames: Long,
        timestampNanos: Long,
        underruns: Int,
    ) {
        if (hardwareFrames >= readFrames) {
            maxCaptureBacklogFrames = maxOf(maxCaptureBacklogFrames, hardwareFrames - readFrames)
        }
        if (timestampNanos >= 0) lastTimestampNanos = timestampNanos
        outputUnderruns = maxOf(outputUnderruns, underruns)
    }

    fun input(stereo: FloatArray) {
        if (finished) return
        require(stereo.size == framesPerBlock * 2)
        for (i in 0 until framesPerBlock) working[i * CHANNELS] = stereo[i * 2]
    }

    fun enhanced(stereo: FloatArray) {
        if (finished) return
        for (i in 0 until framesPerBlock) working[i * CHANNELS + 1] = stereo[i * 2]
    }

    fun output(
        stereo: FloatArray,
        processingNanos: Long,
    ) {
        if (finished) return
        if (producerBlocks - consumed.get() == slots.size.toLong()) {
            stopReason = "writer_queue_full_recording_stopped"
            finished = true
            return
        }
        val slot = slots[(producerBlocks % slots.size).toInt()]
        for (i in 0 until framesPerBlock) {
            working[i * CHANNELS + 2] = stereo[i * 2]
            working[i * CHANNELS + 3] = stereo[i * 2 + 1]
        }
        working.copyInto(slot.samples)
        slot.processingNanos = processingNanos
        producerBlocks++
        published.set(producerBlocks) // release: all slot writes precede publication
        if (producerBlocks >= maximumBlocks) {
            stopReason = "duration_limit"
            finished = true
        }
    }

    /** Audio worker only. The writer drains asynchronously; stopping never waits on disk. */
    override fun close() {
        finished = true
    }

    private class Summary {
        var written = 0L
        var squareSum = 0.0
        var peak = 0f
        var clipped = 0L
        var maxProcessingNanos = 0L
    }

    private fun writeSession() {
        var failure: String? = null
        val summary = Summary()
        try {
            RandomAccessFile(File(directory, "stages.wav"), "rw").use { wav ->
                writeHeader(wav, 0)
                try {
                    drainSlots(wav, summary)
                } finally {
                    // Finalize partial files after an I/O failure; retain only complete WAV frames.
                    val completeBytes = maxOf(0L, wav.length() - HEADER_BYTES) / (CHANNELS * 4) * CHANNELS * 4
                    wav.setLength(HEADER_BYTES + completeBytes)
                    writeHeader(wav, completeBytes)
                }
            }
        } catch (e: IOException) {
            failure = e.message ?: "Diagnostic recording failed"
            stopReason = "write_failed"
        } catch (e: SecurityException) {
            failure = e.message ?: "Diagnostic recording failed"
            stopReason = "write_failed"
        } finally {
            finished = true
            try {
                File(directory, "metadata.json").writeText(toJson(summaryMetadata(summary, failure)))
            } catch (e: IOException) {
                failure = e.message ?: "Could not write diagnostic metadata"
            } catch (e: SecurityException) {
                failure = e.message ?: "Could not write diagnostic metadata"
            }
            onFinished(directory, failure)
        }
    }

    private fun drainSlots(
        wav: RandomAccessFile,
        summary: Summary,
    ) {
        val bytes =
            ByteBuffer
                .allocate(framesPerBlock * CHANNELS * Float.SIZE_BYTES)
                .order(ByteOrder.LITTLE_ENDIAN)
        File(directory, "blocks.csv").bufferedWriter().use { timings ->
            timings.write("start_frame,frame_count,processing_ns\n")
            while (true) {
                if (!drainNextSlot(wav, timings, bytes, summary)) return
            }
        }
    }

    private fun drainNextSlot(
        wav: RandomAccessFile,
        timings: Writer,
        bytes: ByteBuffer,
        summary: Summary,
    ): Boolean {
        if (summary.written == published.get()) {
            // Re-read publication AFTER observing completion so close cannot lose the tail.
            if (finished && summary.written == published.get()) return false
            LockSupport.parkNanos(2_000_000)
            return true
        }
        val slot = slots[(summary.written % slots.size).toInt()]
        bytes.clear()
        for (sample in slot.samples) bytes.putFloat(sample)
        wav.write(bytes.array())
        for (i in 0 until framesPerBlock) {
            val raw = slot.samples[i * CHANNELS]
            summary.squareSum += raw.toDouble() * raw
            summary.peak = maxOf(summary.peak, abs(raw))
            if (abs(raw) >= 32767f / 32768f) summary.clipped++
        }
        summary.maxProcessingNanos = maxOf(summary.maxProcessingNanos, slot.processingNanos)
        timings.write("${summary.written * framesPerBlock},$framesPerBlock,${slot.processingNanos}\n")
        summary.written++
        consumed.set(summary.written) // release: producer may now reuse this slot
        return true
    }

    private fun summaryMetadata(
        summary: Summary,
        failure: String?,
    ): Map<String, String> {
        val rms = if (summary.written > 0) sqrt(summary.squareSum / (summary.written * framesPerBlock)) else 0.0
        return streamMetadata +
            mapOf(
                "schema" to "1",
                "sample_rate" to sampleRate.toString(),
                "channels" to "raw,enhanced,limited_left,limited_right",
                "tap_alignment" to "same_processing_clock_not_delay_compensated",
                "frames_written" to (summary.written * framesPerBlock).toString(),
                "stop_reason" to stopReason,
                "error" to failure.orEmpty(),
                "input_rms_dbfs" to if (rms > 0) (20 * log10(rms)).toString() else "-Infinity",
                "input_peak" to summary.peak.toString(),
                "input_clipped_samples" to summary.clipped.toString(),
                "max_processing_ns" to summary.maxProcessingNanos.toString(),
                "max_capture_backlog_frames" to maxCaptureBacklogFrames.toString(),
                "last_capture_timestamp_ns" to lastTimestampNanos.toString(),
                "output_underruns" to outputUnderruns.toString(),
                "capture_overruns" to "unknown_android_does_not_expose_a_counter",
            )
    }

    private fun writeHeader(
        file: RandomAccessFile,
        dataBytes: Long,
    ) {
        val header = ByteBuffer.allocate(HEADER_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray(Charsets.US_ASCII)).putInt((dataBytes + HEADER_BYTES - 8).toInt())
        header.put("WAVEfmt ".toByteArray(Charsets.US_ASCII)).putInt(16)
        header.putShort(3.toShort()).putShort(CHANNELS.toShort()) // IEEE float
        header.putInt(sampleRate).putInt(sampleRate * CHANNELS * 4)
        header.putShort((CHANNELS * 4).toShort()).putShort(32.toShort())
        header.put("fact".toByteArray(Charsets.US_ASCII)).putInt(4).putInt((dataBytes / (CHANNELS * 4)).toInt())
        header.put("data".toByteArray(Charsets.US_ASCII)).putInt(dataBytes.toInt())
        file.seek(0)
        file.write(header.array())
    }

    private fun toJson(values: Map<String, String>): String {
        fun quote(value: String): String =
            buildString {
                append('"')
                for (c in value) {
                    when {
                        c == '"' || c == '\\' -> {
                            append('\\')
                            append(c)
                        }
                        c.code < 32 -> append("\\u%04x".format(c.code))
                        else -> append(c)
                    }
                }
                append('"')
            }
        return values.entries.joinToString(",\n", "{\n", "\n}\n") { "${quote(it.key)}: ${quote(it.value)}" }
    }

    companion object {
        const val MAX_SECONDS = 30
        private const val CHANNELS = 4
        private const val HEADER_BYTES = 56
    }
}
