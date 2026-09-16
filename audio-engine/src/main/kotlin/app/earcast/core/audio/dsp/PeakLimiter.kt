package app.earcast.core.audio.dsp

import app.earcast.core.audio.OutputCeiling
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max

/**
 * SAFETY-CRITICAL. The production output limiter and the mandatory final stage of
 * the hearing-assist chain.
 *
 * It combines two mechanisms:
 * 1. a **look-ahead peak hold** — gain covers every sample still waiting for
 *    output, with slow recovery. Holding peaks avoids modulating gain on every
 *    waveform cycle; and
 * 2. a **hard brick-wall clamp** applied last, which mathematically guarantees no
 *    output sample ever exceeds the ceiling — even if the smooth stage hasn't fully
 *    caught up. The guarantee does not depend on the smoothing being perfect.
 *
 * Pure and sample-accurate, so the guarantee is exhaustively unit-tested (see the
 * limiter safety suite, the Phase 2 release gate).
 */
class PeakLimiter(
    override val ceilingLinear: Float,
    sampleRateHz: Int,
    lookaheadMs: Double = 2.0,
    releaseMs: Double = 60.0,
) : OutputCeiling {
    init {
        require(ceilingLinear in 0f..1f && ceilingLinear > 0f) { "ceiling must be in (0, 1]" }
    }

    private val ceiling = ceilingLinear.toDouble()
    private val delay = FloatArray(max(1, (lookaheadMs * 0.001 * sampleRateHz).toInt()))
    private var writeIndex = 0
    private var gain = 1.0

    // A monotonic peak queue covers the delayed sample plus future samples.
    // The minimum hold spans a half-cycle at 50 Hz without adding output delay.
    private val peakWindow = max(delay.size + 1, sampleRateHz / 100)
    private val peaks = DoubleArray(peakWindow)
    private val peakPositions = LongArray(peakWindow)
    private var peakHead = 0
    private var peakCount = 0
    private var position = 0L
    private val releaseCoef = exp(-1.0 / max(1.0, releaseMs * 0.001 * sampleRateHz))

    override fun processInPlace(buffer: FloatArray) {
        for (i in buffer.indices) {
            buffer[i] = processSample(buffer[i])
        }
    }

    private fun processSample(x: Float): Float {
        val input = if (x.isFinite()) x else 0f
        val peak = heldPeak(abs(input.toDouble()))
        val desired = if (peak > ceiling) ceiling / peak else 1.0
        // Never release above the gain required by a sample still in the delay.
        gain = minOf(desired, releaseCoef * gain + (1 - releaseCoef) * desired)

        val delayed = delay[writeIndex]
        delay[writeIndex] = input
        writeIndex = (writeIndex + 1) % delay.size

        var y = delayed * gain
        // Brick-wall backstop — the hard guarantee.
        if (y > ceiling) y = ceiling
        if (y < -ceiling) y = -ceiling
        return y.toFloat()
    }

    /** Amortized O(1), with fixed storage and no allocations on the audio thread. */
    private fun heldPeak(magnitude: Double): Double {
        while (peakCount > 0 && peakPositions[peakHead] <= position - peakWindow) {
            peakHead = (peakHead + 1) % peakWindow
            peakCount--
        }
        while (peakCount > 0 && peaks[(peakHead + peakCount - 1) % peakWindow] <= magnitude) {
            peakCount--
        }
        val tail = (peakHead + peakCount) % peakWindow
        peaks[tail] = magnitude
        peakPositions[tail] = position++
        peakCount++
        return peaks[peakHead]
    }

    fun reset() {
        delay.fill(0f)
        writeIndex = 0
        gain = 1.0
        peakHead = 0
        peakCount = 0
        position = 0L
    }
}
