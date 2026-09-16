package app.earcast.audiogram

import app.earcast.common.AudioEar
import app.earcast.common.FrequencyHz
import app.earcast.common.HearingDb
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HearingCurveCodecTest {
    @Test
    fun `round-trips an audiogram`() {
        val original =
            HearingCurve(
                listOf(
                    HearingPoint(AudioEar.RIGHT, FrequencyHz(1000.0), HearingDb(25.0)),
                    HearingPoint(AudioEar.LEFT, FrequencyHz(4000.0), HearingDb(50.0)),
                ),
            )
        val decoded = HearingCurveCodec.decode(HearingCurveCodec.encode(original))
        assertEquals(original.thresholds, decoded.thresholds)
    }

    @Test
    fun `empty text decodes to an empty audiogram`() {
        assertTrue(HearingCurveCodec.decode("").thresholds.isEmpty())
    }

    @Test
    fun `malformed lines are skipped`() {
        val decoded = HearingCurveCodec.decode("LEFT,1000.0,30.0\ngarbage\n,,\nRIGHT,2000.0,40.0")
        assertEquals(2, decoded.thresholds.size)
    }
}
