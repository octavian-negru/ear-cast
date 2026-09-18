package app.earcast.core.audio.dsp

import app.earcast.audiogram.FrequencyGainCurve
import app.earcast.audiogram.FrequencyGainPoint
import app.earcast.common.AudioLimits
import app.earcast.common.FrequencyHz
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MediaCurvePlannerTest {
    private fun curve(vararg points: Pair<Double, Double>) =
        FrequencyGainCurve(
            points.map { (f, g) -> FrequencyGainPoint(FrequencyHz(f), g) },
        )

    @Test
    fun `bands cover the standard centers with ascending cutoffs`() {
        val flat = curve(1000.0 to 6.0)
        val plan = MediaCurvePlanner.plan(flat, flat)
        assertEquals(MediaCurvePlanner.BAND_CENTERS_HZ, plan.map { it.centerHz })
        assertTrue(plan.zipWithNext().all { (a, b) -> a.cutoffHz < b.cutoffHz }, "cutoffs must ascend")
        assertEquals(MediaCurvePlanner.TOP_CUTOFF_HZ, plan.last().cutoffHz)
        assertTrue(plan.zipWithNext().all { (a, b) -> a.cutoffHz > a.centerHz && a.cutoffHz < b.centerHz })
    }

    @Test
    fun `gains follow each ear's curve independently`() {
        val left = curve(250.0 to 2.0, 8000.0 to 10.0)
        val right = curve(250.0 to 8.0, 8000.0 to 4.0)
        val plan = MediaCurvePlanner.plan(left, right)
        assertEquals(-8.0, plan.first().leftGainDb)
        assertEquals(-2.0, plan.first().rightGainDb)
        assertEquals(0.0, plan.last().leftGainDb)
        assertEquals(-6.0, plan.last().rightGainDb)
    }

    @Test
    fun `relative cuts are bounded and never boost mastered media`() {
        val extreme = curve(250.0 to 45.0, 8000.0 to -20.0)
        val plan = MediaCurvePlanner.plan(extreme, extreme)
        assertEquals(0.0, plan.first().leftGainDb)
        assertEquals(-AudioLimits.MEDIA_EQ_MAX_BAND_GAIN_DB, plan.last().leftGainDb)
        assertTrue(
            plan.all {
                it.leftGainDb in -AudioLimits.MEDIA_EQ_MAX_BAND_GAIN_DB..0.0 &&
                    it.rightGainDb in -AudioLimits.MEDIA_EQ_MAX_BAND_GAIN_DB..0.0
            },
        )
    }

    @Test
    fun `a flat high-gain prescription adds no media amplification`() {
        val flat = curve(250.0 to 30.0, 8000.0 to 30.0)
        assertTrue(MediaCurvePlanner.plan(flat, flat).all { it.leftGainDb == 0.0 && it.rightGainDb == 0.0 })
    }

    @Test
    fun `high-loss speech contrast survives normalization before clamping`() {
        val sloping = curve(250.0 to 20.0, 8000.0 to 30.0)
        val plan = MediaCurvePlanner.plan(sloping, sloping)
        assertEquals(-10.0, plan.first().leftGainDb)
        assertEquals(0.0, plan.last().leftGainDb)
        assertTrue(plan.zipWithNext().all { (a, b) -> a.leftGainDb < b.leftGainDb })
    }

    @Test
    fun `media compression gives quiet signals the requested boost`() {
        val boostDb = 10.0f
        val plan = MediaDynamicsPlanner.plan(boostDb)
        val quietInputDbFs = -40.0f

        assertTrue(quietInputDbFs + boostDb < plan.thresholdDbFs - plan.kneeWidthDb / 2f)
        assertEquals(-30.0f, quietInputDbFs + boostDb)
    }

    @Test
    fun `media compression tapers every allowed boost to unity at full scale`() {
        listOf(0.0f, 10.0f, AudioLimits.MAX_MEDIA_BOOST_DB).forEach { boostDb ->
            val plan = MediaDynamicsPlanner.plan(boostDb)
            val fullScaleOutputDb =
                plan.thresholdDbFs +
                    (boostDb - plan.thresholdDbFs) / plan.ratio

            assertEquals(0.0f, fullScaleOutputDb, 0.0001f)
            assertTrue(plan.ratio >= 1.0f)
        }
    }
}
