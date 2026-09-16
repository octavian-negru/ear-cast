package app.earcast.audiogram

import app.earcast.common.AudioEar
import app.earcast.common.FrequencyHz
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

class ToneCheckProtocolTest {
    /** Run a full screening against per-(ear,frequency) true thresholds. */
    private fun runScreening(
        screening: ToneCheckProtocol,
        trueThreshold: (AudioEar, FrequencyHz) -> Double,
    ) {
        var guard = 0
        while (!screening.isComplete()) {
            val stimulus = screening.currentStimulus()
            assertNotNull(stimulus)
            val s = stimulus!!
            screening.submitResponse(s.level.value >= trueThreshold(s.ear, s.frequency))
            check(guard++ < 10_000) { "screening did not terminate" }
        }
    }

    @Test
    fun `measures both ears across all frequencies`() {
        val screening = ToneCheckProtocol()
        assertEquals(ToneCheckProtocol.DEFAULT_SCREENING_FREQUENCIES.size * 2, screening.totalPoints)

        // Flat 30 dB HL loss in both ears.
        runScreening(screening) { _, _ -> 30.0 }

        val audiogram = screening.audiogram()
        assertEquals(screening.totalPoints, audiogram.thresholds.size)
        for (ear in listOf(AudioEar.LEFT, AudioEar.RIGHT)) {
            for (freq in ToneCheckProtocol.DEFAULT_SCREENING_FREQUENCIES) {
                val measured = audiogram.thresholdAt(ear, freq)
                assertNotNull(measured, "missing $ear @ ${freq.value} Hz")
                assertTrue(abs(measured!!.value - 30.0) <= 5.0)
            }
        }
    }

    @Test
    fun `recovers a sloping high-frequency loss`() {
        val screening = ToneCheckProtocol(ears = listOf(AudioEar.RIGHT))
        // Worse hearing as frequency rises — a classic noise/age pattern.
        val truth: (AudioEar, FrequencyHz) -> Double = { _, f -> if (f.value >= 4000.0) 55.0 else 15.0 }

        runScreening(screening, truth)
        val a = screening.audiogram()

        assertTrue(a.thresholdAt(AudioEar.RIGHT, FrequencyHz(500.0))!!.value <= 20.0)
        assertTrue(a.thresholdAt(AudioEar.RIGHT, FrequencyHz(8000.0))!!.value >= 50.0)
    }

    @Test
    fun `progress advances and completes`() {
        val screening = ToneCheckProtocol(frequencies = listOf(FrequencyHz(1000.0)), ears = listOf(AudioEar.RIGHT))
        assertEquals(0, screening.completedPoints())
        assertFalse(screening.isComplete())
        runScreening(screening) { _, _ -> 20.0 }
        assertTrue(screening.isComplete())
        assertEquals(1, screening.completedPoints())
        assertEquals(null, screening.currentStimulus())
    }
}
