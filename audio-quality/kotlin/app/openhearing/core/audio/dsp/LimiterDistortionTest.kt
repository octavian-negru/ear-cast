package app.openhearing.core.audio.dsp

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class LimiterDistortionTest {
    @Test
    fun `limiting a sustained tone preserves its waveform instead of flattening peaks`() {
        for (rate in listOf(8_000, 16_000, 24_000, 32_000, 44_100, 48_000)) {
            for (frequency in listOf(100.0, 500.0, 1_000.0, 3_000.0)) {
                val output = FloatArray(rate) { (2.0 * sin(2 * PI * frequency * it / rate)).toFloat() }
                LookaheadLimiter(0.25f, rate).processInPlace(output)
                // Fit the fundamental over a settled, whole-period interval. All
                // residual energy is unwanted distortion/modulation, not loudness.
                val start = rate / 2
                val count = rate - start
                var sine = 0.0
                var cosine = 0.0
                for (i in start until rate) {
                    sine += output[i] * sin(2 * PI * frequency * i / rate)
                    cosine += output[i] * cos(2 * PI * frequency * i / rate)
                }
                sine *= 2.0 / count
                cosine *= 2.0 / count
                var residual = 0.0
                for (i in start until rate) {
                    val expected = sine * sin(2 * PI * frequency * i / rate) + cosine * cos(2 * PI * frequency * i / rate)
                    residual += (output[i] - expected) * (output[i] - expected)
                }
                val distortion = sqrt(residual / count) / sqrt((sine * sine + cosine * cosine) / 2)
                assertTrue(distortion < 0.002, "$rate Hz / $frequency Hz: distortion was ${100 * distortion}%")
                assertTrue(output.all { it.isFinite() && abs(it) <= 0.250001f })
            }
        }
    }
}
