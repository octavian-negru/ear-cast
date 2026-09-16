package app.earcast.core.audio.dsp

import app.earcast.audiogram.FrequencyGainCurve
import app.earcast.audiogram.FrequencyGainPoint
import app.earcast.common.FrequencyHz
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ListeningPresetTest {
    private val curve =
        FrequencyGainCurve(
            listOf(
                FrequencyGainPoint(FrequencyHz(250.0), 5.0),
                FrequencyGainPoint(FrequencyHz(1000.0), 10.0),
                FrequencyGainPoint(FrequencyHz(2000.0), 12.0),
                FrequencyGainPoint(FrequencyHz(8000.0), 20.0),
            ),
        )

    @Test
    fun `standard preset leaves the curve untouched`() {
        assertEquals(curve.points, curve.withPreset(ListeningPreset.STANDARD).points)
        assertNull(ListeningPreset.STANDARD.highPassHz)
    }

    @Test
    fun `conversation preset boosts only the speech band`() {
        val shaped = curve.withPreset(ListeningPreset.CONVERSATION)
        val byFreq = shaped.points.associateBy { it.frequency.value }
        assertEquals(5.0, byFreq.getValue(250.0).gainDb)
        assertEquals(10.0 + ListeningPreset.CONVERSATION_BOOST_DB, byFreq.getValue(1000.0).gainDb)
        assertEquals(12.0 + ListeningPreset.CONVERSATION_BOOST_DB, byFreq.getValue(2000.0).gainDb)
        assertEquals(20.0, byFreq.getValue(8000.0).gainDb)
    }

    @Test
    fun `unknown preset names fall back to standard`() {
        assertEquals(ListeningPreset.STANDARD, ListeningPreset.fromName("SOMETHING_ELSE"))
        assertEquals(ListeningPreset.STANDARD, ListeningPreset.fromName(null))
        assertEquals(ListeningPreset.STANDARD, ListeningPreset.fromName("OUTDOORS"))
    }
}
