package app.openhearing.core.audio.dsp

import app.openhearing.audiogram.GainCurve
import app.openhearing.audiogram.GainPoint
import app.openhearing.common.Hertz
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

class GainEqualizerTest {
    private val rates = listOf(8_000, 16_000, 24_000, 32_000, 44_100, 48_000)
    private val frequencies = listOf(250.0, 500.0, 1_000.0, 2_000.0, 4_000.0, 8_000.0)

    @Test
    fun `flat prescriptions do not accumulate gain from neighbouring bands`() {
        for (rate in rates) {
            for (gain in listOf(0.0, 12.0, 40.0)) {
                val curve = curve(List(frequencies.size) { gain })
                for (frequency in frequencies.filter { it < rate / 2.0 }) {
                    assertEquals(gain, response(curve, rate, frequency), 0.1, "$rate Hz at $frequency Hz")
                }
            }
        }
    }

    @Test
    fun `sloping fits retain spectral shape without exceeding their maximum`() {
        val curve = curve(listOf(0.0, 3.0, 6.0, 9.0, 12.0, 15.0))
        for (rate in rates) {
            val usable = curve.points.filter { it.frequency.value < rate / 2.0 }
            for (point in usable) {
                val gain = response(curve, rate, point.frequency.value)
                assertEquals(point.gainDb, gain, 2.0, "$rate Hz: fitted response at ${point.frequency}")
            }
            for (index in 0..40) {
                val frequency = 100.0 * Math.pow(rate * 0.45 / 100.0, index / 40.0)
                val gain = response(curve, rate, frequency)
                assertTrue(gain in -0.01..(usable.last().gainDb + 1.0), "$rate Hz: excessive $gain dB")
            }
        }
    }

    @Test
    fun `equalizer reset and arbitrary block boundaries preserve the signal`() {
        val rate = 16_000
        val eq = GainEqualizer(curve(listOf(4.0, 8.0, 12.0, 6.0, 3.0, 0.0)), rate)
        val input = FloatArray(rate) { (0.001 * sin(2 * PI * 1_300 * it / rate)).toFloat() }
        val whole = input.copyOf().also(eq::process)
        eq.reset()
        val split = FloatArray(rate)
        var offset = 0
        while (offset < rate) {
            val end = minOf(offset + 137, rate)
            val block = input.copyOfRange(offset, end).also(eq::process)
            block.copyInto(split, offset)
            offset = end
        }
        assertArrayEquals(whole, split)
    }

    private fun curve(gains: List<Double>) = GainCurve(frequencies.zip(gains) { frequency, gain -> GainPoint(Hertz(frequency), gain) })

    private fun response(
        curve: GainCurve,
        rate: Int,
        frequency: Double,
    ): Double {
        val input = FloatArray(rate / 4) { (0.0001 * sin(2 * PI * frequency * it / rate)).toFloat() }
        val output = input.copyOf().also(GainEqualizer(curve, rate)::process)
        return 20 * log10(rms(output, rate / 8) / rms(input, rate / 8))
    }

    private fun rms(
        samples: FloatArray,
        start: Int,
    ): Double = sqrt(samples.drop(start).sumOf { it.toDouble() * it } / (samples.size - start))
}
