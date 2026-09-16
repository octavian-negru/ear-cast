package app.earcast.data

import app.earcast.audiogram.HearingCurve
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SoundProfileTest {
    @Test
    fun `a fresh profile starts from an empty audiogram`() {
        val profile =
            SoundProfile(
                id = "default",
                name = "My profile",
                audiogram = HearingCurve.EMPTY,
                masterGainCapDb = 20.0,
            )
        assertEquals("My profile", profile.name)
        assertTrue(profile.audiogram.thresholds.isEmpty())
    }
}
