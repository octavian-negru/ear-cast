package app.earcast.core.audio.dsp

import app.earcast.audiogram.FrequencyGainCurve
import app.earcast.audiogram.FrequencyGainPoint
import app.earcast.common.FrequencyHz
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin

/** Covers SignalMeter, MeteredTransform, and ListeningTracker. */
class ExposureMeteringRegressionTest {
    private val sampleRate = 48_000

    private fun sine(
        amp: Double,
        n: Int,
    ) = FloatArray(n) { (amp * sin(2.0 * PI * 1000.0 * it / sampleRate)).toFloat() }

    @Test
    fun `meter reports the RMS of a known sine`() {
        val meter = SignalMeter()
        meter.accumulate(sine(amp = 1.0, n = sampleRate))
        val window = meter.drain()
        // Full-scale sine: RMS = 1/sqrt(2) = -3.01 dBFS.
        assertEquals(-3.01, window.rmsDbfs, 0.05)
        assertEquals(sampleRate.toLong(), window.sampleCount)
    }

    @Test
    fun `drain resets the accumulators`() {
        val meter = SignalMeter()
        meter.accumulate(sine(amp = 0.5, n = 1024))
        meter.drain()
        val second = meter.drain()
        assertEquals(0L, second.sampleCount)
        assertEquals(0.0, second.sumSquares)
    }

    @Test
    fun `windows accumulate across multiple blocks`() {
        val meter = SignalMeter()
        repeat(4) { meter.accumulate(sine(amp = 0.5, n = 1000)) }
        assertEquals(4_000L, meter.drain().sampleCount)
    }

    @Test
    fun `metered processor delegates and observes post-limiter output`() {
        val curve =
            FrequencyGainCurve(
                listOf(
                    FrequencyGainPoint(FrequencyHz(250.0), 40.0),
                    FrequencyGainPoint(FrequencyHz(8000.0), 40.0),
                ),
            )
        val ceiling = 0.5f
        val chain =
            MonoListeningChain(curve, sampleRate, masterGainDb = 40.0, ceilingLinear = ceiling)
        val meter = SignalMeter()
        val metered = MeteredTransform(chain, meter)

        val buffer = sine(amp = 0.9, n = sampleRate)
        metered.process(buffer)

        // Delegate ran (limiter enforced)...
        assertTrue(buffer.all { abs(it) <= ceiling + 1e-4f }, "chain/limiter did not run")
        // ...and the meter saw the same post-limiter samples (safety-ordering check).
        val window = meter.drain()
        assertTrue(window.rmsLinear <= ceiling + 1e-4, "meter must observe post-limiter output")
        assertTrue(window.sampleCount == sampleRate.toLong())
    }

    @Test
    fun `three dB more level halves the time to the same units`() {
        val quiet = MeterWindow(sumSquares = 0.25 * 1000, sampleCount = 1000) // meanSquare 0.25
        val loud = MeterWindow(sumSquares = 0.5 * 1000, sampleCount = 1000) // +3 dB power
        val quietUnits = ListeningTracker.unitsFor(quiet, seconds = 2.0)
        val loudUnits = ListeningTracker.unitsFor(loud, seconds = 1.0)
        assertEquals(quietUnits, loudUnits, 1e-9)
    }

    @Test
    fun `reference hours at reference level is exactly one hundred percent`() {
        val refPower = 10.0.pow(ListeningTracker.REF_LEVEL_DBFS / 10.0)
        val window = MeterWindow(sumSquares = refPower * 1000, sampleCount = 1000)
        val units = ListeningTracker.unitsFor(window, seconds = ListeningTracker.REF_HOURS * 3600.0)
        assertEquals(100.0, ListeningTracker.percentOf(units), 1e-6)
    }

    @Test
    fun `silence accrues nothing`() {
        val silent = MeterWindow(sumSquares = 0.0, sampleCount = 48_000)
        assertEquals(0.0, ListeningTracker.unitsFor(silent, seconds = 3600.0))
    }

    @Test
    fun `day rollover clears carried units`() {
        assertEquals(42.0, ListeningTracker.carriedUnits(19_900, 42.0, 19_900))
        assertEquals(0.0, ListeningTracker.carriedUnits(19_900, 42.0, 19_901))
    }
}
