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
    fun `wired and LE routes retain stereo processing and requested rate`() {
        assertEquals(requested, AudioRoutePolicy.processingFormat(requested, sco = false, legacy = false))
    }

    @Test
    fun `SCO rates preserve four millisecond blocks and two ear processing`() {
        for ((legacy, rate) in listOf(false to 16_000, true to 8_000)) {
            val format = AudioRoutePolicy.processingFormat(requested, sco = true, legacy = legacy)
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
        for (legacy in listOf(false, true)) {
            val format = AudioRoutePolicy.processingFormat(requested, sco = true, legacy = legacy)
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
