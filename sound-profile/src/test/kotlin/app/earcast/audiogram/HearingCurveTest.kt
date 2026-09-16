package app.earcast.audiogram

import app.earcast.common.AudioEar
import app.earcast.common.FrequencyHz
import app.earcast.common.HearingDb
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HearingCurveTest {
    private val sample =
        HearingCurve(
            listOf(
                HearingPoint(AudioEar.LEFT, FrequencyHz(1000.0), HearingDb(20.0)),
                HearingPoint(AudioEar.LEFT, FrequencyHz(2000.0), HearingDb(35.0)),
                HearingPoint(AudioEar.RIGHT, FrequencyHz(1000.0), HearingDb(15.0)),
            ),
        )

    @Test
    fun `looks up a measured threshold`() {
        assertEquals(35.0, sample.thresholdAt(AudioEar.LEFT, FrequencyHz(2000.0))?.value)
    }

    @Test
    fun `returns null for an unmeasured point`() {
        assertNull(sample.thresholdAt(AudioEar.RIGHT, FrequencyHz(8000.0)))
    }

    @Test
    fun `lists measured frequencies per ear in ascending order`() {
        assertEquals(listOf(1000.0, 2000.0), sample.frequenciesFor(AudioEar.LEFT).map { it.value })
        assertEquals(listOf(1000.0), sample.frequenciesFor(AudioEar.RIGHT).map { it.value })
    }

    @Test
    fun `empty audiogram has no thresholds`() {
        assertTrue(HearingCurve.EMPTY.thresholds.isEmpty())
    }
}
