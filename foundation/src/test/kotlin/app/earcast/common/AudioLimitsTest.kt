package app.earcast.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Phase 0 guard rails for the safety ceiling. These are intentionally simple —
 * the comprehensive limiter tests arrive with the real limiter in Phase 2 — but
 * they pin the invariants now so a later change can't silently weaken them.
 */
class AudioLimitsTest {
    @Test
    fun `tone ceiling never exceeds the absolute output ceiling`() {
        assertTrue(
            AudioLimits.MAX_TONE_SPL_DB <= AudioLimits.MAX_OUTPUT_SPL_DB,
            "Test tones must not be allowed louder than the absolute output ceiling",
        )
    }

    @Test
    fun `master gain cap default does not exceed its hard maximum`() {
        assertTrue(AudioLimits.DEFAULT_MASTER_GAIN_CAP_DB <= AudioLimits.MAX_MASTER_GAIN_CAP_DB)
    }

    @Test
    fun `output at the ceiling is allowed but anything above it is rejected`() {
        assertTrue(AudioLimits.isWithinOutputCeiling(AcousticDb(AudioLimits.MAX_OUTPUT_SPL_DB)))
        assertFalse(AudioLimits.isWithinOutputCeiling(AcousticDb(AudioLimits.MAX_OUTPUT_SPL_DB + 0.1)))
    }

    @Test
    fun `tones must ramp gently`() {
        assertTrue(AudioLimits.MIN_TONE_RAMP_MS >= 20L, "Tone onsets must not be abrupt")
    }

    @Test
    fun `audiometric frequencies cover the standard screening range`() {
        assertEquals(250.0, FrequencyHz.AUDIOMETRIC.first().value)
        assertEquals(8000.0, FrequencyHz.AUDIOMETRIC.last().value)
    }
}
