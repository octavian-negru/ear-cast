package app.openhearing.core.audio.speech

import app.openhearing.core.audio.AudioProcessor
import app.openhearing.core.audio.dsp.Biquad

/**
 * Conditions the mono microphone BEFORE per-ear fitting, compression and limiting.
 * The input is mono duplicated into interleaved stereo by the capture engine.
 * Denoising uses fixed frames across arbitrary I/O block boundaries, with one frame
 * of adapter buffering plus the denoiser's own algorithm/resampler delay, with no
 * allocations in process. Off adds no buffering or noise gate.
 * The downstream processor must end in an output limiter.
 */
class SpeechFrontEnd(
    sampleRateHz: Int,
    options: ListeningOptions,
    private val downstream: AudioProcessor,
    private val denoiser: FrameDenoiser? = null,
) : AudioProcessor, AutoCloseable {
    private val bass = if (options.voiceComfort == VoiceComfort.OFF) {
        null
    } else {
        Biquad.lowShelf(450.0, -options.voiceComfort.reductionDb, sampleRateHz)
    }
    private val presence = if (options.speechClarity == SpeechClarity.OFF) {
        null
    } else {
        // Stay well inside Nyquist, including the 8 kHz narrowband fallback.
        Biquad.peaking(minOf(2_500.0, sampleRateHz * 0.3), options.speechClarity.gainDb, 0.8, sampleRateHz)
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
            val output = (presence?.processSample(low) ?: low).toFloat()
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
