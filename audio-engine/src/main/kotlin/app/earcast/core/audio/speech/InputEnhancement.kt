package app.earcast.core.audio.speech

import app.earcast.core.audio.SampleTransform
import app.earcast.core.audio.StreamObserver
import app.earcast.core.audio.diagnostics.AudioSessionRecorder
import app.earcast.core.audio.dsp.BiquadFilter

/**
 * Conditions the mono microphone BEFORE per-ear fitting, compression and limiting.
 * The input is mono duplicated into interleaved stereo by the capture engine.
 * Denoising uses fixed frames across arbitrary I/O block boundaries, with one frame
 * of adapter buffering plus the denoiser's own algorithm/resampler delay, with no
 * Kotlin frame allocations in process. A native backend may allocate internally.
 * Off adds no buffering or noise gate. Consonant shaping runs after downstream WDRC.
 * The downstream processor must end in an output limiter.
 */
class InputEnhancement(
    sampleRateHz: Int,
    options: EnhancementOptions,
    private val downstream: SampleTransform,
    private val denoiser: FrameFilter? = null,
    private val diagnostics: AudioSessionRecorder? = null,
) : SampleTransform,
    AutoCloseable,
    StreamObserver {
    override val wantsStreamDiagnostics: Boolean get() = diagnostics != null
    private val bass =
        if (options.voiceComfort == BassReduction.OFF) {
            null
        } else {
            BiquadFilter.lowShelf(450.0, -options.voiceComfort.reductionDb, sampleRateHz)
        }
    private val inputFrame = FloatArray(denoiser?.frameSize ?: 0)
    private val outputFrame = FloatArray(inputFrame.size)
    private var position = 0
    private var closed = false

    init {
        require(denoiser == null || denoiser.frameSize > 0)
    }

    override fun process(buffer: FloatArray) {
        check(!closed)
        require(buffer.size % 2 == 0)
        val started = if (diagnostics != null) System.nanoTime() else 0L
        diagnostics?.input(buffer)
        for (i in buffer.indices step 2) {
            val input = buffer[i].takeIf { it.isFinite() } ?: 0f
            val cleaned = denoise(input)
            val low = bass?.processSample(cleaned.toDouble()) ?: cleaned.toDouble()
            // Speech presence belongs after WDRC in each downstream ear chain.
            val output = low.toFloat()
            buffer[i] = output
            buffer[i + 1] = output
        }
        diagnostics?.enhanced(buffer)
        downstream.process(buffer)
        diagnostics?.output(buffer, System.nanoTime() - started)
    }

    override fun onStreamStarted(metadata: Map<String, String>) {
        val delay = denoiser?.algorithmDelaySamples
        diagnostics?.streamStarted(
            metadata + denoiser?.diagnosticMetadata.orEmpty() +
                mapOf(
                    "enhancement_delay_samples" to
                        if (denoiser == null) {
                            "0"
                        } else {
                            delay?.let { (it + denoiser.frameSize).toString() }.orEmpty()
                        },
                    "enhanced_tap" to "mono_enhancement_and_bass_before_fitting",
                    "speech_presence_position" to "after_per_ear_wdrc_before_feedback_guard_and_limiter",
                ),
        )
    }

    override fun onCaptureTiming(
        readFrames: Long,
        hardwareFrames: Long,
        timestampNanos: Long,
        outputUnderruns: Int,
    ) {
        diagnostics?.captureTiming(readFrames, hardwareFrames, timestampNanos, outputUnderruns)
    }

    private fun denoise(input: Float): Float {
        val processor = denoiser ?: return input
        val output = outputFrame[position]
        inputFrame[position] = input
        position++
        if (position == inputFrame.size) {
            processor.process(inputFrame)
            inputFrame.copyInto(outputFrame)
            position = 0
        }
        return output
    }

    override fun close() {
        if (!closed) {
            closed = true
            try {
                denoiser?.close()
            } finally {
                diagnostics?.close()
            }
        }
    }
}
