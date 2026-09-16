package app.earcast.core.audio.dsp

import kotlin.math.sqrt

/**
 * Three independent soft-knee WDRC bands: bass, speech body, and speech presence.
 * Loud bass therefore does not turn down the entire speech spectrum.
 *
 * Fourth-order Linkwitz-Riley splits at 700 and 2400 Hz are phase aligned:
 * the low branch includes the upper split's all-pass response. With unity gain
 * the sum has flat magnitude, including at the crossover frequencies. Each ear
 * owns its envelopes and filters; no allocation or block-dependent timing.
 *
 * This is digital level control, not a calibrated hearing-aid fitting formula.
 * The recombined signal must still pass through the final output limiter.
 */
class BandCompressor(
    sampleRateHz: Int,
    ratio: Double = 3.0,
) {
    private val lower = Split(700.0, sampleRateHz)
    private val upper = Split(2400.0, sampleRateHz)
    private val lowPhase = BiquadFilter.allPass(2400.0, BUTTERWORTH_Q, sampleRateHz)
    private val lowCompressor = DynamicCompressor(sampleRateHz, ratio = ratio, releaseMs = 120.0)
    private val midCompressor = DynamicCompressor(sampleRateHz, ratio = ratio)
    private val highCompressor = DynamicCompressor(sampleRateHz, ratio = ratio)

    fun process(buffer: FloatArray) {
        for (i in buffer.indices) {
            val input = buffer[i].toDouble()
            val low = lowPhase.processSample(lower.low(input))
            val remaining = lower.high(input)
            val mid = upper.low(remaining)
            val high = upper.high(remaining)
            buffer[i] =
                (
                    lowCompressor.processSample(low) + midCompressor.processSample(mid) +
                        highCompressor.processSample(high)
                ).toFloat()
        }
    }

    fun reset() {
        lower.reset()
        upper.reset()
        lowPhase.reset()
        lowCompressor.reset()
        midCompressor.reset()
        highCompressor.reset()
    }

    private class Split(
        frequency: Double,
        rate: Int,
    ) {
        private val low1 = BiquadFilter.lowPass(frequency, BUTTERWORTH_Q, rate)
        private val low2 = BiquadFilter.lowPass(frequency, BUTTERWORTH_Q, rate)
        private val high1 = BiquadFilter.highPass(frequency, BUTTERWORTH_Q, rate)
        private val high2 = BiquadFilter.highPass(frequency, BUTTERWORTH_Q, rate)

        fun low(input: Double): Double = low2.processSample(low1.processSample(input))

        fun high(input: Double): Double = high2.processSample(high1.processSample(input))

        fun reset() {
            low1.reset()
            low2.reset()
            high1.reset()
            high2.reset()
        }
    }

    private companion object {
        val BUTTERWORTH_Q = 1.0 / sqrt(2.0)
    }
}
