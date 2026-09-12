package app.openhearing.core.audio.dsp

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

class MultibandWdrcTest {
    @Test
    fun `crossovers reconstruct flat magnitude when compression is inactive`() {
        for (rate in listOf(8_000, 16_000, 48_000)) {
            for (frequency in listOf(80.0, 350.0, 700.0, 1200.0, 1800.0, 2400.0, 3000.0, rate * 0.45)) {
                val samples = tone(rate, frequency, 0.001)
                MultibandWdrc(rate).process(samples)
                val gainDb = 20 * log10(amplitude(samples, rate, frequency) / 0.001)
                assertEquals(0.0, gainDb, 0.05, "rate=$rate frequency=$frequency")
            }
        }
    }

    @Test
    fun `loud bass leaves quiet speech presence audible instead of broadband ducking`() {
        for (rate in listOf(8_000, 16_000, 48_000)) {
            val speech = tone(rate, 3000.0, 0.01)
            val mixed = speech.copyOf()
            val bass = tone(rate, 120.0, 0.7)
            for (i in mixed.indices) mixed[i] += bass[i]
            val broadband = mixed.copyOf().also { Wdrc(rate).process(it) }
            MultibandWdrc(rate).process(mixed)
            MultibandWdrc(rate).process(speech)
            val speechLevel = amplitude(mixed, rate, 3000.0)
            assertTrue(speechLevel > amplitude(broadband, rate, 3000.0) * 4, "speech detail must survive loud bass")
            assertTrue(speechLevel > amplitude(speech, rate, 3000.0) * 0.9, "bass must not drive high-band envelope")
            assertTrue(amplitude(mixed, rate, 120.0) < 0.15, "bass itself must still be compressed")
        }
    }

    @Test
    fun `state is continuous across block boundaries and reset restores a fresh session`() {
        val input = FloatArray(16_000) { (0.3 * sin(it * 0.07) + 0.07 * cos(it * 0.73)).toFloat() }
        val whole = input.copyOf().also { MultibandWdrc(16_000).process(it) }
        val compressor = MultibandWdrc(16_000)
        val chunked = FloatArray(input.size)
        var offset = 0
        while (offset < input.size) {
            val end = minOf(offset + 37, input.size)
            val chunk = input.copyOfRange(offset, end)
            compressor.process(chunk)
            chunk.copyInto(chunked, offset)
            offset = end
        }
        assertArrayEquals(whole, chunked)
        compressor.reset()
        val restarted = input.copyOf().also { compressor.process(it) }
        assertArrayEquals(whole, restarted)
    }

    private fun tone(rate: Int, frequency: Double, level: Double): FloatArray =
        FloatArray(rate) { (level * sin(2 * PI * frequency * it / rate)).toFloat() }

    /** Synchronous tone measurement rejects the other band and ignores startup. */
    private fun amplitude(samples: FloatArray, rate: Int, frequency: Double): Double {
        var real = 0.0
        var imaginary = 0.0
        val start = samples.size / 2
        for (i in start until samples.size) {
            val phase = 2 * PI * frequency * i / rate
            real += samples[i] * cos(phase)
            imaginary += samples[i] * sin(phase)
        }
        return 2 * sqrt(real * real + imaginary * imaginary) / (samples.size - start)
    }
}
