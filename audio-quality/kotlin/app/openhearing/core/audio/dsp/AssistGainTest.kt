package app.openhearing.core.audio.dsp

import app.openhearing.audiogram.GainCurve
import app.openhearing.audiogram.GainPoint
import app.openhearing.common.Hertz
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

class AssistGainTest {
    private val rate = 16_000
    private val flat = GainCurve(listOf(GainPoint(Hertz(1_000.0), 0.0)))

    @Test
    fun `negative volume attenuates the fitted output and clamps at the minimum`() {
        val reference = tone(rate).also(chain(0.0)::process)
        for (gain in listOf(-12.0, -100.0, Double.NaN)) {
            val output = tone(rate).also(chain(gain)::process)
            assertTrue(output.all { it.isFinite() })
            val change = 20 * log10(rms(output, rate / 2, rate) / rms(reference, rate / 2, rate))
            assertEquals(-12.0, change, 0.01)
        }
    }

    @Test
    fun `startup fades in and live gain rises gradually to the requested level`() {
        val referenceChain = chain(0.0)
        val changedChain = chain(0.0)
        val startup = tone(rate).also(changedChain::process)
        assertTrue(rms(startup, 0, rate / 100) < rms(startup, rate / 2, rate) * 0.2)
        referenceChain.process(tone(rate))
        changedChain.setMasterGainDb(12.0)
        val reference = tone(rate / 2).also(referenceChain::process)
        val changed = tone(rate / 2).also(changedChain::process)
        val earlyRatio = rms(changed, 0, rate / 200) / rms(reference, 0, rate / 200)
        val lateDb = 20 * log10(rms(changed, rate / 4, rate / 2) / rms(reference, rate / 4, rate / 2))
        assertTrue(earlyRatio in 1.0..1.3, "gain jumped abruptly to $earlyRatio")
        assertEquals(12.0, lateDb, 0.1)

        changedChain.setMasterGainDb(-12.0)
        val lowered = tone(rate / 2).also(changedChain::process)
        val settledDb = 20 * log10(rms(lowered, rate / 4, rate / 2) / rms(reference, rate / 4, rate / 2))
        assertEquals(-12.0, settledDb, 0.01)
    }

    private fun chain(gain: Double) = HearingAssistChain(flat, rate, gain, feedbackGuardEnabled = false)

    private fun tone(size: Int) = FloatArray(size) { (0.0001 * sin(2 * PI * 1_000 * it / rate)).toFloat() }

    private fun rms(
        samples: FloatArray,
        start: Int,
        end: Int,
    ): Double = sqrt((start until end).sumOf { samples[it].toDouble() * samples[it] } / (end - start))
}
