package app.earcast.core.audio.speech

import app.earcast.audiogram.FrequencyGainCurve
import app.earcast.audiogram.FrequencyGainPoint
import app.earcast.common.FrequencyHz
import app.earcast.core.audio.CaptureTuning
import app.earcast.core.audio.SampleTransform
import app.earcast.core.audio.dsp.MonoListeningChain
import app.earcast.core.audio.dsp.StereoListeningChain
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

class InputEnhancementTest {
    private val bypass = EnhancementOptions(voiceComfort = BassReduction.OFF, speechClarity = SpeechPresence.OFF)

    @Test
    fun `engine names preserve existing selections and support classical alternatives`() {
        for (engine in EnhancementEngine.entries) assertEquals(engine, EnhancementEngine.fromName(engine.name))
        assertEquals(EnhancementEngine.RNNOISE, EnhancementEngine.fromName("unknown"))
        assertEquals(EnhancementEngine.RNNOISE, EnhancementOptions().speechEngine)
    }

    @Test
    fun `natural capture uses advertised raw support with recognition fallback`() {
        assertEquals(CaptureTuning.RAW_UNPROCESSED, InputMode.NATURAL.inputTuning(true))
        assertEquals(CaptureTuning.RAW_VOICE_RECOGNITION, InputMode.NATURAL.inputTuning(false))
        for (supported in listOf(false, true)) {
            assertEquals(CaptureTuning.COMMUNICATION, InputMode.CALL_COMPATIBLE.inputTuning(supported))
        }
    }

    @Test
    fun `disabled conditioning preserves quiet speech without a gate or delay`() {
        val input = FloatArray(202) { i -> (sin(i / 2 * 0.2) * 1e-5).toFloat() }
        val expected = input.copyOf()
        InputEnhancement(16_000, bypass, SampleTransform {}).use { it.process(input) }
        assertArrayEquals(expected, input)
    }

    @Test
    fun `denoising preserves sample order and constant delay across uneven blocks`() {
        for (rate in listOf(8_000, 16_000, 24_000, 32_000, 44_100, 48_000)) {
            val frameSize = rate / 100
            val denoiser = FakeDenoiser(frameSize)
            val processor = InputEnhancement(rate, bypass, SampleTransform {}, denoiser)
            val input = FloatArray(frameSize * 10) { (it + 1) / (frameSize * 10f) }
            val output = render(processor, input, intArrayOf(64, 17, 1, 199, 1024))
            for (i in output.indices) {
                val expected = if (i < frameSize) 0f else input[i - frameSize] * 0.5f
                assertEquals(expected, output[i], 1e-7f)
            }
            assertEquals(10, denoiser.calls)
            processor.close()
            processor.close()
            assertEquals(1, denoiser.closes)
        }
    }

    @Test
    fun `bass reduction preserves upper speech detail at all rates`() {
        for (rate in listOf(8_000, 16_000, 24_000, 32_000, 44_100, 48_000)) {
            val bass = bypass.copy(voiceComfort = BassReduction.GENTLE)
            assertTrue(responseDb(rate, 100.0, bass) in -6.1..-5.5)
            assertTrue(abs(responseDb(rate, 2400.0, bass)) < 0.1)
        }
    }

    @Test
    fun `conditioning stays finite and final stereo output stays limited during loud transients`() {
        val curve = FrequencyGainCurve(listOf(FrequencyGainPoint(FrequencyHz(500.0), 30.0), FrequencyGainPoint(FrequencyHz(2500.0), 30.0)))
        for (rate in listOf(8_000, 16_000, 24_000, 32_000, 44_100, 48_000)) {
            val frames = rate / 250

            fun ear() = MonoListeningChain(curve, rate, 30.0, ceilingLinear = 0.3f, speechPresenceDb = 6.0)
            val options = EnhancementOptions(voiceComfort = BassReduction.STRONG, speechClarity = SpeechPresence.STRONG)
            val downstream = StereoListeningChain(ear(), ear(), frames)
            InputEnhancement(rate, options, downstream, FakeDenoiser(rate / 100)).use { chain ->
                repeat(300) { block ->
                    val audio =
                        FloatArray(frames * 2) { i ->
                            when {
                                block % 13 == 0 -> 1f
                                block % 17 == 0 -> Float.NaN
                                else -> sin(2 * PI * 2400 * (block * frames + i / 2) / rate).toFloat()
                            }
                        }
                    chain.process(audio)
                    assertTrue(audio.all { it.isFinite() && abs(it) <= 0.300001f })
                }
            }
        }
    }

    private fun responseDb(
        rate: Int,
        frequency: Double,
        options: EnhancementOptions,
    ): Double {
        val input = FloatArray(rate) { (0.01 * sin(2 * PI * frequency * it / rate)).toFloat() }
        val output =
            InputEnhancement(rate, options, SampleTransform {}).use {
                render(it, input, intArrayOf(rate / 250))
            }

        fun rms(samples: FloatArray): Double = sqrt(samples.drop(rate / 2).sumOf { it.toDouble() * it } / (rate / 2))
        return 20 * log10(rms(output) / rms(input))
    }

    private fun render(
        processor: SampleTransform,
        input: FloatArray,
        sizes: IntArray,
    ): FloatArray {
        val output = FloatArray(input.size)
        var offset = 0
        var block = 0
        while (offset < input.size) {
            val frames = minOf(sizes[block++ % sizes.size], input.size - offset)
            val stereo = FloatArray(frames * 2) { input[offset + it / 2] }
            processor.process(stereo)
            repeat(frames) {
                assertEquals(stereo[2 * it], stereo[2 * it + 1])
                output[offset + it] = stereo[2 * it]
            }
            offset += frames
        }
        return output
    }

    private class FakeDenoiser(
        override val frameSize: Int,
    ) : FrameFilter {
        var calls = 0
        var closes = 0

        override fun process(frame: FloatArray) {
            assertEquals(frameSize, frame.size)
            calls++
            for (i in frame.indices) frame[i] *= 0.5f
        }

        override fun close() {
            closes++
        }
    }
}
