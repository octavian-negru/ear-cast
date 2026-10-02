package app.earcast.core.audio.dsp

import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Independent band-envelope reference for the Android transfer law. Inputs are
 * per-band RMS levels, not PCM. This tests gain behavior, not FFT reconstruction,
 * phase, latency, harmonic distortion, sample peaks or an OEM's effect engine.
 */
internal class MediaDynamicsReference(
    private val plans: List<MediaCompressionPlan>,
) {
    private val envelopes = DoubleArray(plans.size)
    private var limiterEnvelope = 0.0

    fun process(
        levelsDb: DoubleArray,
        elapsedMs: Double = 5.0,
    ): DoubleArray {
        val outputs =
            DoubleArray(plans.size) { index ->
                val plan = plans[index]
                val input = amplitude(levelsDb[index])
                val time = if (input > envelopes[index]) plan.attackMs else plan.releaseMs
                val retention = exp(-elapsedMs / time)
                envelopes[index] = input + retention * (envelopes[index] - input)
                val detectedDb = db(envelopes[index].coerceAtLeast(1e-6))
                val gainDb = outputDb(detectedDb, plan) - detectedDb
                input * amplitude(gainDb)
            }
        val rms = sqrt(outputs.sumOf { it * it })
        val time =
            if (rms > limiterEnvelope) {
                MediaDynamicsPlanner.LIMITER_ATTACK_MS
            } else {
                MediaDynamicsPlanner.LIMITER_RELEASE_MS
            }
        limiterEnvelope = rms + exp(-elapsedMs / time) * (limiterEnvelope - rms)
        val over = (db(limiterEnvelope) - MediaDynamicsPlanner.LIMITER_THRESHOLD_DB_FS).coerceAtLeast(0.0)
        val reductionDb = over * (1 - 1.0 / MediaDynamicsPlanner.LIMITER_RATIO)
        return DoubleArray(outputs.size) { db(outputs[it]) - reductionDb }
    }

    companion object {
        fun outputDb(
            inputDb: Double,
            plan: MediaCompressionPlan,
        ): Double {
            val over = inputDb - plan.thresholdDbFs
            val halfKnee = plan.kneeWidthDb / 2.0
            val compressed =
                when {
                    over <= -halfKnee -> 0.0
                    over >= halfKnee -> over
                    else -> (over + halfKnee).pow(2) / (2 * plan.kneeWidthDb)
                }
            return inputDb + (1.0 / plan.ratio - 1) * compressed + plan.postGainDb
        }

        fun amplitude(db: Double): Double = 10.0.pow(db / 20)

        fun db(amplitude: Double): Double = 20 * log10(amplitude)
    }
}
