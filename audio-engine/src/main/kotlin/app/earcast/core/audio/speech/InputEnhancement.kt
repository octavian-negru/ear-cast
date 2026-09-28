package app.earcast.core.audio.speech

import app.earcast.core.audio.SampleTransform
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
) : SampleTransform,
    AutoCloseable {
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
        for (i in buffer.indices step 2) {
            val input = buffer[i].takeIf { it.isFinite() } ?: 0f
            val cleaned = denoise(input)
            val low = bass?.processSample(cleaned.toDouble()) ?: cleaned.toDouble()
            // Speech presence belongs after WDRC in each downstream ear chain.
            val output = low.toFloat()
            buffer[i] = output
            buffer[i + 1] = output
        }
        downstream.process(buffer)
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
            denoiser?.close()
        }
    }
}
