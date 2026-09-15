package app.openhearing.core.audio.dsp

import app.openhearing.audiogram.GainCurve
import app.openhearing.audiogram.GainPoint
import app.openhearing.common.Hertz
import app.openhearing.common.SafetyConstants
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MediaEqPlannerTest {
    private fun curve(vararg points: Pair<Double, Double>) = GainCurve(points.map { (f, g) -> GainPoint(Hertz(f), g) })

    @Test
    fun `bands cover the standard centers with ascending cutoffs`() {
        val flat = curve(1000.0 to 6.0)
        val plan = MediaEqPlanner.plan(flat, flat)
        assertEquals(MediaEqPlanner.BAND_CENTERS_HZ, plan.map { it.centerHz })
        assertTrue(plan.zipWithNext().all { (a, b) -> a.cutoffHz < b.cutoffHz }, "cutoffs must ascend")
        assertEquals(MediaEqPlanner.TOP_CUTOFF_HZ, plan.last().cutoffHz)
        assertTrue(plan.zipWithNext().all { (a, b) -> a.cutoffHz > a.centerHz && a.cutoffHz < b.centerHz })
    }

    @Test
    fun `gains follow each ear's curve independently`() {
        val left = curve(250.0 to 2.0, 8000.0 to 10.0)
        val right = curve(250.0 to 8.0, 8000.0 to 4.0)
        val plan = MediaEqPlanner.plan(left, right)
        assertEquals(-8.0, plan.first().leftGainDb)
        assertEquals(-2.0, plan.first().rightGainDb)
        assertEquals(0.0, plan.last().leftGainDb)
        assertEquals(-6.0, plan.last().rightGainDb)
    }

    @Test
    fun `relative cuts are bounded and never boost mastered media`() {
        val extreme = curve(250.0 to 45.0, 8000.0 to -20.0)
        val plan = MediaEqPlanner.plan(extreme, extreme)
        assertEquals(0.0, plan.first().leftGainDb)
        assertEquals(-SafetyConstants.MEDIA_EQ_MAX_BAND_GAIN_DB, plan.last().leftGainDb)
        assertTrue(
            plan.all {
                it.leftGainDb in -SafetyConstants.MEDIA_EQ_MAX_BAND_GAIN_DB..0.0 &&
                    it.rightGainDb in -SafetyConstants.MEDIA_EQ_MAX_BAND_GAIN_DB..0.0
            },
        )
    }

    @Test
    fun `a flat high-gain prescription adds no media amplification`() {
        val flat = curve(250.0 to 30.0, 8000.0 to 30.0)
        assertTrue(MediaEqPlanner.plan(flat, flat).all { it.leftGainDb == 0.0 && it.rightGainDb == 0.0 })
    }

    @Test
    fun `high-loss speech contrast survives normalization before clamping`() {
        val sloping = curve(250.0 to 20.0, 8000.0 to 30.0)
        val plan = MediaEqPlanner.plan(sloping, sloping)
        assertEquals(-10.0, plan.first().leftGainDb)
        assertEquals(0.0, plan.last().leftGainDb)
        assertTrue(plan.zipWithNext().all { (a, b) -> a.leftGainDb < b.leftGainDb })
    }
}
