package app.earcast.ads

import app.earcast.core.audio.StreamPhase
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TestAdPolicyTest {
    private val idle = TestAdPolicy(enabled = true, safetyAccepted = true, session = StreamPhase.STOPPED)

    @Test
    fun `disabled builds cannot request even after safety acceptance`() {
        StreamPhase.entries.forEach { session ->
            assertFalse(idle.copy(enabled = false, session = session).mayRequestAds)
        }
    }

    @Test
    fun `missing safety acceptance suppresses requests in every audio state`() {
        StreamPhase.entries.forEach { session ->
            assertFalse(idle.copy(safetyAccepted = false, session = session).mayRequestAds)
        }
    }

    @Test
    fun `connecting and running assist suppress banners even when started outside Home`() {
        assertFalse(idle.copy(session = StreamPhase.CONNECTING).mayRequestAds)
        assertFalse(idle.copy(session = StreamPhase.RUNNING).mayRequestAds)
    }

    @Test
    fun `stopping or failing assist allows test requests again`() {
        assertTrue(idle.mayRequestAds)
        assertTrue(idle.copy(session = StreamPhase.FAILED).mayRequestAds)
    }
}
