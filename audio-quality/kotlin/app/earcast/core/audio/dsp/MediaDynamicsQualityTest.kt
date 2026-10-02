package app.earcast.core.audio.dsp

import app.earcast.common.AudioLimits
import app.earcast.core.audio.dsp.MediaDynamicsReference.Companion.amplitude
import app.earcast.core.audio.dsp.MediaDynamicsReference.Companion.db
import app.earcast.core.audio.dsp.MediaDynamicsReference.Companion.outputDb
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

class MediaDynamicsQualityTest {
    @Test
    fun `zero boost is exact unity in every band and approaches unity continuously`() {
        for (mode in MediaProcessingMode.entries) {
            val zero = MediaDynamicsPlanner.planBands(0f, mode)
            val tiny = MediaDynamicsPlanner.planBands(0.001f, mode)
            for (input in -100..0) {
                zero.zip(tiny).forEach { (bypass, active) ->
                    assertEquals(input.toDouble(), outputDb(input.toDouble(), bypass.compression), 1e-6)
                    assertTrue(abs(outputDb(input.toDouble(), active.compression) - input) < 0.002)
                }
            }
        }
    }

    @Test
    fun `quiet detail receives the requested gain without gating or extra makeup`() {
        for (mode in MediaProcessingMode.entries) {
            for (step in 0..250) {
                val gain = step / 10f
                MediaDynamicsPlanner.planBands(gain, mode).forEach { band ->
                    for (input in listOf(-100.0, -70.0, -50.0)) {
                        assertEquals(input + gain, outputDb(input, band.compression), 1e-5)
                    }
                }
            }
        }
    }

    @Test
    fun `settled summed power stays bounded for sparse and dense spectra at every boost`() {
        val random = Random(721)
        val spectra =
            List(400) { DoubleArray(4) { 10.0.pow(random.nextDouble(-8.0, 0.0)) } } +
                List(4) { selected -> DoubleArray(4) { if (it == selected) 1.0 else 0.0 } }
        for (mode in MediaProcessingMode.entries) {
            for (step in 0..250) {
                val plans = MediaDynamicsPlanner.planBands(step / 10f, mode)
                for (spectrum in spectra) {
                    val sum = spectrum.sum()
                    val outputPower =
                        spectrum.indices.sumOf { band ->
                            val inputPower = spectrum[band] / sum
                            if (inputPower == 0.0) {
                                0.0
                            } else {
                                amplitude(outputDb(db(sqrt(inputPower)), plans[band].compression)).pow(2)
                            }
                        }
                    assertTrue(outputPower <= 1.00001, "$mode / ${step / 10f} dB: power $outputPower")
                }
            }
        }
    }

    @Test
    fun `maximum boost preserves more level contrast than the previous ten to one curve`() {
        val bands = MediaDynamicsPlanner.planBands(AudioLimits.MAX_MEDIA_BOOST_DB, MediaProcessingMode.BALANCED)
        for (band in bands) {
            val contrast = outputDb(-12.0, band.compression) - outputDb(-24.0, band.compression)
            // The previous 10:1 curve reduced this 12 dB contrast to 1.2 dB.
            assertTrue(contrast > 3.8, "Contrast was $contrast dB")
            assertTrue(band.compression.ratio < 3.1f)
        }
    }

    @Test
    fun `shared headroom avoids the old multiband sum overloading the limiter`() {
        val inputDb = db(0.5) // Four bands at 0.25 power each: total input power = 1.
        val old = MediaCompressionPlan(-30f, 10f, 5f, 120f, 4f, 25f)
        val previousPower = 4 * amplitude(outputDb(inputDb, old)).pow(2)
        val newPower =
            MediaDynamicsPlanner.planBands(25f, MediaProcessingMode.BALANCED).sumOf {
                amplitude(outputDb(inputDb, it.compression)).pow(2)
            }
        assertTrue(db(sqrt(previousPower)) > 3.4)
        assertTrue(db(sqrt(newPower)) <= 0.0)
        println("Settled four-band sum: previous=${db(sqrt(previousPower))} dBFS, spectral=${db(sqrt(newPower))} dBFS RMS")
    }

    @Test
    fun `soft knee has continuous level and slope`() {
        for (mode in MediaProcessingMode.entries) {
            for (gain in listOf(0.1f, 6f, 15f, 25f)) {
                for ((_, plan) in MediaDynamicsPlanner.planBands(gain, mode)) {
                    for (boundary in listOf(-1, 1)) {
                        val level = plan.thresholdDbFs + boundary * plan.kneeWidthDb / 2.0
                        val epsilon = 0.001
                        val below = (outputDb(level, plan) - outputDb(level - epsilon, plan)) / epsilon
                        val above = (outputDb(level + epsilon, plan) - outputDb(level, plan)) / epsilon
                        assertEquals(below, above, 0.0002)
                    }
                }
            }
        }
    }

    @Test
    fun `bass burst barely ducks the vocal band compared with the old broadband compressor`() {
        val plans = MediaDynamicsPlanner.planBands(20f, MediaProcessingMode.BALANCED).map { it.compression }
        val reference = MediaDynamicsReference(plans)
        val quietBass = doubleArrayOf(-45.0, -50.0, -32.0, -50.0)
        val loudBass = quietBass.copyOf().also { it[0] = -6.0 }
        var before = doubleArrayOf()
        repeat(600) { before = reference.process(quietBass) }
        var lowestVocal = before[2]
        repeat(100) { lowestVocal = minOf(lowestVocal, reference.process(loudBass)[2]) }
        val newDuckDb = before[2] - lowestVocal

        // Commit 657f148 at +20 dB: threshold -30, ratio 3.75, broadband detector.
        val old = MediaCompressionPlan(-30f, 3.75f, 5f, 120f, 4f, 20f)
        fun oldVocal(levels: DoubleArray): Double {
            val level = db(sqrt(levels.sumOf { amplitude(it).pow(2) }))
            return levels[2] + outputDb(level, old) - level
        }
        val oldDuckDb = oldVocal(quietBass) - oldVocal(loudBass)
        assertTrue(newDuckDb < 1.0, "New vocal ducking: $newDuckDb dB")
        assertTrue(oldDuckDb > 15.0, "Old vocal ducking: $oldDuckDb dB")
        println("Band-envelope bass burst: previous ducking=$oldDuckDb dB, spectral ducking=$newDuckDb dB")
    }

    @Test
    fun `speech mode shifts loud mix headroom toward voices but keeps quiet timbre`() {
        val balanced = MediaDynamicsPlanner.planBands(25f, MediaProcessingMode.BALANCED)
        val speech = MediaDynamicsPlanner.planBands(25f, MediaProcessingMode.SPEECH_CLARITY)
        fun difference(index: Int): Double =
            outputDb(-18.0, speech[index].compression) - outputDb(-18.0, balanced[index].compression)
        assertTrue(difference(2) > 1.0)
        assertTrue(difference(0) < -2.0)
        balanced.zip(speech).forEach { (music, voice) ->
            assertEquals(outputDb(-50.0, music.compression), outputDb(-50.0, voice.compression), 1e-6)
        }
    }
}
