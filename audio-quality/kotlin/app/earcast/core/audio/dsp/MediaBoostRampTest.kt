package app.earcast.core.audio.dsp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MediaBoostRampTest {
    @Test
    fun `startup reaches maximum boost gradually and without overshoot`() {
        val ramp = MediaBoostRamp()
        ramp.setTarget(25f)
        repeat(50) {
            val previous = ramp.currentDb
            val next = ramp.advance(10f)
            assertTrue(next > previous && next - previous <= 0.5f)
            assertTrue(next <= 25f)
        }
        assertTrue(ramp.isSettled)
        assertEquals(25f, ramp.currentDb)
    }

    @Test
    fun `new requests replace old targets and downward changes take priority`() {
        val ramp = MediaBoostRamp()
        ramp.setTarget(25f)
        repeat(10) { ramp.advance(10f) }
        ramp.setTarget(0f)
        repeat(5) { ramp.advance(10f) }
        assertTrue(ramp.isSettled)
        assertEquals(0f, ramp.currentDb)
        ramp.setTarget(0.1f)
        assertFalse(ramp.isSettled)
        assertEquals(0.1f, ramp.advance(10f))
        assertTrue(ramp.isSettled)
    }

    @Test
    fun `a delayed callback never produces a large gain jump`() {
        val ramp = MediaBoostRamp()
        ramp.setTarget(25f)
        assertEquals(0f, ramp.advance(0f))
        assertEquals(0.5f, ramp.advance(1000f))
    }

    @Test
    fun `invalid controls cannot enter the ramp or planner`() {
        for (value in listOf(Float.NaN, Float.POSITIVE_INFINITY, -1f, 26f)) {
            assertThrows<IllegalArgumentException> { MediaBoostRamp().setTarget(value) }
            for (mode in MediaProcessingMode.entries) {
                assertThrows<IllegalArgumentException> { MediaDynamicsPlanner.planBands(value, mode) }
            }
        }
        for (elapsed in listOf(Float.NaN, Float.POSITIVE_INFINITY, -1f)) {
            assertThrows<IllegalArgumentException> { MediaBoostRamp().advance(elapsed) }
        }
    }
}
