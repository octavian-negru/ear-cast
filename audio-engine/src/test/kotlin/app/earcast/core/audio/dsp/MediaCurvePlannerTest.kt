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
    fun `media compression gives quiet signals the requested boost at every setting`() {
        val quietInputDbFs = -40.0f

        listOf(0.0f to -40.0f, 10.0f to -30.0f, AudioLimits.MAX_MEDIA_BOOST_DB to -15.0f)
            .forEach { (boostDb, expectedOutputDbFs) ->
                val plan = MediaDynamicsPlanner.plan(boostDb)
                assertTrue(quietInputDbFs < plan.thresholdDbFs - plan.kneeWidthDb / 2f)
                assertEquals(expectedOutputDbFs, quietInputDbFs + plan.postGainDb)
                assertEquals(MediaDynamicsPlanner.QUIET_INPUT_THRESHOLD_DB_FS, plan.thresholdDbFs)
            }
    }

    @Test
    fun `media compression tapers every allowed boost to the output ceiling at full scale`() {
        listOf(0.0f, 10.0f, AudioLimits.MAX_MEDIA_BOOST_DB).forEach { boostDb ->
            val plan = MediaDynamicsPlanner.plan(boostDb)
            val fullScaleOutputDb =
                plan.thresholdDbFs +
                    (0.0f - plan.thresholdDbFs) / plan.ratio +
                    plan.postGainDb

            val expected = if (boostDb == 0.0f) 0.0f else MediaDynamicsPlanner.OUTPUT_CEILING_DB_FS
            assertEquals(expected, fullScaleOutputDb, 0.0001f)
            assertTrue(plan.ratio >= 1.0f)
        }
    }

    @Test
    fun `maximum media boost remains useful at speech-like levels`() {
        val boostDb = AudioLimits.MAX_MEDIA_BOOST_DB
        val plan = MediaDynamicsPlanner.plan(boostDb)
        val inputDbFs = -30.0f
        val outputDbFs =
            plan.thresholdDbFs +
                (inputDbFs - plan.thresholdDbFs) / plan.ratio +
                plan.postGainDb

        assertEquals(-5.0f, outputDbFs, 0.0001f)
        assertEquals(boostDb, outputDbFs - inputDbFs, 0.0001f)
    }

    @Test
    fun `speech clarity separates dynamics into ordered frequency bands`() {
        val balanced = MediaDynamicsPlanner.planBands(20.0f, MediaProcessingMode.BALANCED)
        val speech = MediaDynamicsPlanner.planBands(20.0f, MediaProcessingMode.SPEECH_CLARITY)

        assertEquals(listOf(MediaDynamicsPlanner.TOP_CUTOFF_HZ), balanced.map { it.cutoffHz })
        assertEquals(listOf(250.0f, 1_000.0f, 4_000.0f, 20_000.0f), speech.map { it.cutoffHz })
        assertTrue(speech.zipWithNext().all { (lower, upper) -> lower.cutoffHz < upper.cutoffHz })
        assertTrue(speech.first().compression.releaseMs > speech.last().compression.releaseMs)
        assertTrue(speech.all { it.compression.postGainDb == 20.0f })
    }
}
