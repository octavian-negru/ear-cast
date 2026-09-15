package app.openhearing.core.audio.dsp

import app.openhearing.audiogram.GainCurve
import app.openhearing.audiogram.GainPoint
import app.openhearing.common.Hertz
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

class SpeechPresenceTest {
    private val rates = listOf(8_000, 16_000, 24_000, 32_000, 44_100, 48_000)
    private val flat = GainCurve(listOf(GainPoint(Hertz(1_000.0), 0.0)))

    @Test
    fun `consonant lift survives active compression without lifting bass`() {
        for (rate in rates) {
            val consonant = minOf(5_000.0, rate * 0.4)
            // Loud enough to engage WDRC; a boost before ratio-3 compression
            // would retain only about one third of the requested gain.
            val before = render(rate, consonant, 0.0)
            val after = render(rate, consonant, 6.0)
            val lift = 20 * log10(rms(after, rate / 2) / rms(before, rate / 2))
            assertTrue(lift in 5.0..6.1, "$rate Hz: upper speech band lift was $lift dB")
            val bassBefore = render(rate, 200.0, 0.0)
            val bassAfter = render(rate, 200.0, 6.0)
            val bassLift = 20 * log10(rms(bassAfter, rate / 2) / rms(bassBefore, rate / 2))
            assertTrue(abs(bassLift) < 0.1, "$rate Hz: speech shaping also amplified bass")
        }
    }

    @Test
    fun `shelf is independent of block boundaries and resets completely`() {
        for (rate in rates) {
            val input = FloatArray(rate) { (0.05 * sin(2 * PI * 2700 * it / rate)).toFloat() }
            val chain = HearingAssistChain(flat, rate, 0.0, feedbackGuardEnabled = false, speechPresenceDb = 6.0)
            val whole = input.copyOf().also(chain::process)
            chain.reset()
            val split = FloatArray(input.size)
            var offset = 0
            while (offset < input.size) {
                val end = minOf(offset + 137, input.size)
                val block = input.copyOfRange(offset, end)
                chain.process(block)
                block.copyInto(split, offset)
                offset = end
            }
            assertArrayEquals(whole, split)
            assertTrue(split.all { it.isFinite() && abs(it) <= 0.900001f })
        }
    }

    private fun render(
        rate: Int,
        frequency: Double,
        presenceDb: Double,
    ): FloatArray {
        val input = FloatArray(rate) { (0.2 * sin(2 * PI * frequency * it / rate)).toFloat() }
        HearingAssistChain(flat, rate, 0.0, feedbackGuardEnabled = false, speechPresenceDb = presenceDb).process(input)
        return input
    }

    private fun rms(
        samples: FloatArray,
        start: Int,
    ): Double {
        var energy = 0.0
        for (i in start until samples.size) energy += samples[i].toDouble() * samples[i]
        return sqrt(energy / (samples.size - start))
    }
}
