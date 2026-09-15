package app.openhearing.core.audio

import app.openhearing.audiogram.GainCurve
import app.openhearing.audiogram.GainPoint
import app.openhearing.common.Hertz
import app.openhearing.core.audio.dsp.HearingAssistChain
import app.openhearing.core.audio.dsp.StereoAssistChain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

class AudioRoutePolicyTest {
    private val requested = AudioFormat(48_000, 2, 192, microphoneSource = MicrophoneSource.HEADSET)

    @Test
    fun `mono devices and SCO use one output channel while unspecified devices retain stereo`() {
        assertEquals(1, AudioRoutePolicy.outputChannels(2, false, intArrayOf(1)))
        assertEquals(1, AudioRoutePolicy.outputChannels(2, true, intArrayOf(1, 2)))
        assertEquals(2, AudioRoutePolicy.outputChannels(2, false, intArrayOf()))
        assertEquals(2, AudioRoutePolicy.outputChannels(2, false, intArrayOf(1, 2)))
    }

    @Test
    fun `SCO always offers wideband then narrowband without silently enabling call effects`() {
        val natural = requested.copy(inputTuning = InputTuning.RAW_UNPROCESSED)
        val formats = AudioRoutePolicy.candidateFormats(natural, true, intArrayOf(), intArrayOf())
        assertEquals(listOf(16_000, 16_000, 8_000, 8_000), formats.map { it.sampleRateHz })
        assertEquals(InputTuning.RAW_VOICE_RECOGNITION, formats[1].inputTuning)
        assertTrue(formats.none { it.inputTuning == InputTuning.COMMUNICATION })
        assertTrue(formats.all { it.microphoneSource == MicrophoneSource.HEADSET && it.channelCount == 2 })
    }

    @Test
    fun `mutually advertised rate is tried first but other valid PCM rates remain fallbacks`() {
        val formats = AudioRoutePolicy.candidateFormats(requested, false, intArrayOf(44_100), intArrayOf(44_100))
        assertEquals(44_100, formats.first().sampleRateHz)
        assertTrue(formats.any { it.sampleRateHz == 48_000 })
        assertTrue(formats.all { it.framesPerBlock > 0 })
    }

    @Test
    fun `wired and LE routes retain stereo processing and requested rate`() {
        assertEquals(requested, AudioRoutePolicy.candidateFormats(requested, false, intArrayOf(), intArrayOf()).first())
    }

    @Test
    fun `SCO rates preserve four millisecond blocks and two ear processing`() {
        for (rate in listOf(16_000, 8_000)) {
            val format =
                AudioRoutePolicy
                    .candidateFormats(requested, true, intArrayOf(), intArrayOf())
                    .first { it.sampleRateHz == rate }
            assertEquals(rate, format.sampleRateHz)
            assertEquals(4.0, format.framesPerBlock * 1000.0 / rate)
            assertEquals(2, format.channelCount)
            assertEquals(MicrophoneSource.HEADSET, format.microphoneSource)
        }
    }

    @Test
    fun `mono fold averages both ears without doubling the limited peak`() {
        val output = FloatArray(4)
        AudioRoutePolicy.foldStereoToMono(floatArrayOf(0.5f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.4f, 0.2f), output, 4)
        assertEquals(0.5f, output[0])
        assertEquals(-0.5f, output[1])
        assertEquals(0f, output[2])
        assertEquals(0.3f, output[3], 0.00001f)
    }

    @Test
    fun `full assist chain remains finite and limited at both SCO rates`() {
        val curve = GainCurve(listOf(250.0, 1000.0, 4000.0, 8000.0).map { GainPoint(Hertz(it), 30.0) })
        for (format in AudioRoutePolicy.candidateFormats(requested, true, intArrayOf(), intArrayOf())) {
            fun ear() = HearingAssistChain(curve, format.sampleRateHz, masterGainDb = 30.0, ceilingLinear = 0.4f)
            val chain = StereoAssistChain(ear(), ear(), format.framesPerBlock)
            val mono = FloatArray(format.framesPerBlock)
            repeat(100) {
                val stereo = FloatArray(format.framesPerBlock * 2) { i -> if (i % 7 < 3) 1f else -1f }
                chain.process(stereo)
                AudioRoutePolicy.foldStereoToMono(stereo, mono, mono.size)
                assertTrue(mono.all { it.isFinite() && abs(it) <= 0.40001f })
            }
        }
    }

    @Test
    fun `unknown saved microphone values retain the phone default`() {
        assertEquals(MicrophoneSource.PHONE, MicrophoneSource.fromName("future-source"))
        assertEquals(MicrophoneSource.HEADSET, MicrophoneSource.fromName("HEADSET"))
    }
}
