package app.earcast.core.audio.dsp

import app.earcast.common.AudioLimits
import kotlin.math.log10

data class MediaCompressionPlan(
    val thresholdDbFs: Float,
    val ratio: Float,
    val attackMs: Float,
    val releaseMs: Float,
    val kneeWidthDb: Float,
    val postGainDb: Float,
)

data class MediaDynamicsBand(
    val cutoffHz: Float,
    val compression: MediaCompressionPlan,
)

/**
 * Spectral headroom amplification for Android's frequency-domain DynamicsProcessing.
 *
 * Each band receives a share w of the available power, instead of every band aiming
 * for full scale. For requested gain G and compression range D, the threshold is
 * 10 log10(w) - G - D and the ratio is 1 + G/D. Above the knee, output power is
 * w * (inputPower/w)^(1/ratio). A common ratio and shares summing to one bound the
 * settled total power to unity for any input spectrum whose total power is <= 1
 * (Jensen's inequality). The soft knee only reduces that bound.
 *
 * Quiet signals retain G; louder signals gradually approach the spectral budget.
 * This avoids four independent compressors driving the final limiter together.
 * It is a settled RMS bound, NOT a sample/true-peak ceiling: Android owns the
 * detectors, FFT reconstruction and transients. A linked final limiter is required.
 * Apply the per-ear profile AFTER these dynamics so compression cannot undo it.
 */
object MediaDynamicsPlanner {
    const val TOP_CUTOFF_HZ = 20_000.0f

    // The AOSP limiter detects RMS, not peaks. Reserve 4 dB for crest factor below
    // the existing -2 dB target. This reduces overshoot; it cannot guarantee true peaks.
    const val LIMITER_THRESHOLD_DB_FS = -6.0f
    const val LIMITER_ATTACK_MS = 1.0f
    const val LIMITER_RELEASE_MS = 100.0f
    const val LIMITER_RATIO = 100.0f

    private const val COMPRESSION_RANGE_DB = 12.0f
    private const val KNEE_WIDTH_DB = 6.0f
    private const val ATTACK_MS = 1.5f

    private data class BandShape(
        val cutoffHz: Float,
        val balancedShare: Float,
        val speechShare: Float,
        val releaseMs: Float,
    )

    // Fast, frame-based RMS attacks keep a new bass burst from engaging the
    // broadband limiter and ducking voices. Slow bass release avoids modulation;
    // consonant bands recover sooner. Broad bands avoid narrow spectral holes.
    // Shares are design choices, not a measured intelligibility prescription.
    private val shapes =
        listOf(
            BandShape(250f, 0.25f, 0.125f, 240f),
            BandShape(1_000f, 0.25f, 0.25f, 180f),
            BandShape(4_000f, 0.30f, 0.425f, 120f),
            BandShape(TOP_CUTOFF_HZ, 0.20f, 0.20f, 100f),
        )

    fun planBands(
        boostDb: Float,
        mode: MediaProcessingMode,
    ): List<MediaDynamicsBand> {
        require(boostDb.isFinite() && boostDb in 0f..AudioLimits.MAX_MEDIA_BOOST_DB)
        val ratio = 1f + boostDb / COMPRESSION_RANGE_DB
        return shapes.map { shape ->
            val share = if (mode == MediaProcessingMode.SPEECH_CLARITY) shape.speechShare else shape.balancedShare
            MediaDynamicsBand(
                cutoffHz = shape.cutoffHz,
                compression =
                    MediaCompressionPlan(
                        thresholdDbFs = 10f * log10(share) - boostDb - COMPRESSION_RANGE_DB,
                        ratio = ratio,
                        attackMs = ATTACK_MS,
                        releaseMs = shape.releaseMs,
                        kneeWidthDb = KNEE_WIDTH_DB,
                        postGainDb = boostDb,
                    ),
            )
        }
    }
}
