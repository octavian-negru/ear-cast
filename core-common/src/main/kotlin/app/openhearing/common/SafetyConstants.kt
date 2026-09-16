package app.openhearing.common

/**
 * SAFETY-CRITICAL constants. This object is the single source of truth for every
 * output-loudness limit in the app. The hearing test plays calibrated tones and
 * the assist mode amplifies live sound directly into someone's ears — any code
 * path that can exceed these limits is a critical bug.
 *
 * These are conservative defaults. The real, enforced limiter (and its explicit
 * tests) lands in Phase 2 in :core-audio and MUST reference these values rather
 * than redefining its own. The mapping from digital level to true dB SPL depends
 * on per-device/per-earbud calibration — see docs/SAFETY.md. Until calibrated,
 * the app must err on the quiet side.
 */
object SafetyConstants {
    /**
     * Absolute ceiling for any audio the app produces, in dB SPL. Chosen well
     * below levels associated with rapid noise-induced hearing damage and below
     * a typical hearing-aid maximum output (OSPL90). Conservative on purpose.
     */
    const val MAX_OUTPUT_SPL_DB: Double = 100.0

    /**
     * Ceiling for pure tones played during the hearing screening, in dB SPL.
     * Test tones never need to be as loud as the assist-mode ceiling; capping
     * them lower protects users who already have reduced loudness tolerance.
     */
    const val MAX_TONE_SPL_DB: Double = 90.0

    /**
     * Minimum rise/fall time for any tone, in milliseconds. Tones must ramp on
     * and off gently — never an instantaneous (clicky, startling) onset.
     */
    const val MIN_TONE_RAMP_MS: Long = 20L

    /**
     * Default master output cap applied on top of the audiogram-derived gain, in
     * decibels. The user can lower this but a hard maximum still applies. The UI
     * must always expose this cap plus an instant mute.
     */
    const val DEFAULT_MASTER_GAIN_CAP_DB: Double = 6.0

    /** Allow the volume control to attenuate even when fitted gain is too strong. */
    const val MIN_MASTER_GAIN_DB: Double = -12.0

    /** Largest master gain cap the UI may ever offer, in decibels. */
    const val MAX_MASTER_GAIN_CAP_DB: Double = 40.0

    /**
     * Largest relative band adjustment for media EQ, in decibels. Applied as
     * cuts below a common reference; overall amplification has its own media control.
     */
    const val MEDIA_EQ_MAX_BAND_GAIN_DB: Double = 12.0

    /** Independent media gain, applied before the platform limiter. */
    const val DEFAULT_MEDIA_BOOST_DB: Float = 6.0f
    const val MAX_MEDIA_BOOST_DB: Float = 15.0f

    /**
     * True when [outputSpl] is within the absolute safety ceiling. The Phase 2
     * limiter uses this as its invariant: output that fails this check must be
     * attenuated before it ever reaches the audio device.
     */
    fun isWithinOutputCeiling(outputSpl: DecibelsSpl): Boolean = outputSpl.value <= MAX_OUTPUT_SPL_DB
}
