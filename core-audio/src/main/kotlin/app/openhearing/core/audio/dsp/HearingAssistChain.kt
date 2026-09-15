package app.openhearing.core.audio.dsp

import app.openhearing.audiogram.GainCurve
import app.openhearing.common.SafetyConstants
import app.openhearing.core.audio.AudioProcessor
import kotlin.math.exp
import kotlin.math.pow

/**
 * The full real-time hearing-assist signal chain (one ear):
 *
 * ```
 * input -> fitted EQ -> WDRC -> speech presence -> feedback guard -> master gain -> LIMITER -> output
 * ```
 *
 * The [LookaheadLimiter] is always the final stage, so nothing leaves above the
 * ceiling regardless of the audiogram, the master gain, or a feedback event. The
 * master gain is hard-capped by [SafetyConstants]. Pure (no Android, no
 * allocation in [process]) so the whole chain is unit-testable.
 */
class HearingAssistChain(
    gainCurve: GainCurve,
    sampleRateHz: Int,
    masterGainDb: Double,
    ceilingLinear: Float = DEFAULT_CEILING_LINEAR,
    /** Optional low-cut (e.g. wind/rumble for the outdoors preset); null = off. */
    highPassHz: Double? = null,
    /**
     * The feedback guard is enabled for every production Hearing Assist input
     * source. This switch remains available for focused DSP tests; the limiter
     * is unaffected either way.
     */
    feedbackGuardEnabled: Boolean = true,
    /** Relative consonant emphasis after WDRC, with the shelf's added gain removed. */
    speechPresenceDb: Double = 0.0,
) : AudioProcessor {
    init {
        require(speechPresenceDb.isFinite() && speechPresenceDb in 0.0..6.0)
    }

    private val highPass: Biquad? =
        highPassHz?.let { Biquad.highPass(it, HIGH_PASS_Q, sampleRateHz) }
    private val eq = GainEqualizer(gainCurve, sampleRateHz)
    private val wdrc = MultibandWdrc(sampleRateHz)
    private val presence =
        if (speechPresenceDb == 0.0) {
            null
        } else {
            Biquad.highShelf(minOf(1_800.0, sampleRateHz * 0.2), speechPresenceDb, sampleRateHz)
        }

    // Retain the upper/lower speech contrast without another absolute treble boost.
    private val presenceTrim = 10.0.pow(-speechPresenceDb / 20.0)
    private val guard: FeedbackGuard? =
        if (feedbackGuardEnabled) {
            FeedbackGuard(sampleRateHz, activationRms = CHAIN_GUARD_ACTIVATION_RMS)
        } else {
            null
        }
    private val limiter = LookaheadLimiter(ceilingLinear, sampleRateHz)

    // Permit attenuation of the fitted signal. One volatile target read per block;
    // the audio thread ramps from silence at start and smooths live changes.
    @Volatile
    private var masterGainLinear: Float = linearGain(masterGainDb)
    private var smoothedGain = 0.0
    private val gainRiseCoef = exp(-1.0 / (0.060 * sampleRateHz))
    private val gainFallCoef = exp(-1.0 / (0.015 * sampleRateHz))

    /** Live-adjustable master gain, bounded by the shared attenuation/boost limits. */
    fun setMasterGainDb(db: Double) {
        masterGainLinear = linearGain(db)
    }

    override fun process(buffer: FloatArray) {
        highPass?.let { hp ->
            for (i in buffer.indices) {
                buffer[i] = hp.processSample(buffer[i].toDouble()).toFloat()
            }
        }
        eq.process(buffer)
        wdrc.process(buffer)
        presence?.let { filter ->
            for (i in buffer.indices) {
                buffer[i] = filter.processSample(buffer[i].toDouble()).toFloat()
            }
        }
        guard?.process(buffer)
        val gain = masterGainLinear // one volatile read per block
        for (i in buffer.indices) {
            val coefficient = if (gain > smoothedGain) gainRiseCoef else gainFallCoef
            smoothedGain = coefficient * smoothedGain + (1.0 - coefficient) * gain
            buffer[i] = (buffer[i] * presenceTrim * smoothedGain).toFloat()
        }
        limiter.processInPlace(buffer)
    }

    fun reset() {
        highPass?.reset()
        eq.reset()
        wdrc.reset()
        presence?.reset()
        guard?.reset()
        limiter.reset()
        smoothedGain = 0.0
    }

    companion object {
        /**
         * Digital output ceiling (linear). Conservative headroom below full scale.
         * Until the path is calibrated this is a digital cap, not a true dB SPL
         * cap; calibration may lower it further. See docs/SAFETY.md / docs/CALIBRATION.md.
         */
        const val DEFAULT_CEILING_LINEAR = 0.9f

        /** Gentle slope; the low-cut is a comfort feature, not surgical filtering. */
        private const val HIGH_PASS_Q = 0.707

        /**
         * Ratio-3 compression above -35 dBFS leaves strong tones around 0.05 RMS
         * within a band (somewhat higher at crossovers). The standalone guard's
         * 0.1 threshold would miss them. Tonality remains the other activation gate.
         */
        private const val CHAIN_GUARD_ACTIVATION_RMS = 0.03

        private fun linearGain(db: Double): Float =
            10.0
                .pow(
                    (if (db.isFinite()) db else SafetyConstants.MIN_MASTER_GAIN_DB)
                        .coerceIn(SafetyConstants.MIN_MASTER_GAIN_DB, SafetyConstants.MAX_MASTER_GAIN_CAP_DB) / 20.0,
                ).toFloat()
    }
}
