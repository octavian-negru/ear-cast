package app.openhearing.core.audio.dsp

import app.openhearing.audiogram.GainCurve
import app.openhearing.common.Hertz
import app.openhearing.common.SafetyConstants
import kotlin.math.sqrt

/**
 * One media-EQ band: [cutoffHz] is the band's upper edge (as the platform
 * DynamicsProcessing effect expects). Gains are relative cuts, with no positive boost.
 */
data class MediaEqBand(
    val centerHz: Double,
    val cutoffHz: Double,
    val leftGainDb: Double,
    val rightGainDb: Double,
)

/**
 * Pure planning for the experimental media EQ: samples the fitted per-ear curves
 * at standard audiometric centers, then removes the largest gain across BOTH
 * ears before bounding the adjustment range. This preserves spectral contrast
 * and ear balance without adding gain to already-mastered media. Normalizing
 * before clamping also avoids flattening every high-loss band to the same boost.
 */
object MediaEqPlanner {
    val BAND_CENTERS_HZ = listOf(250.0, 500.0, 1_000.0, 2_000.0, 4_000.0, 8_000.0)

    /** Upper cutoff of the final band; content above it keeps the last gain. */
    const val TOP_CUTOFF_HZ = 16_000.0

    fun plan(
        leftCurve: GainCurve,
        rightCurve: GainCurve,
        maxBandGainDb: Double = SafetyConstants.MEDIA_EQ_MAX_BAND_GAIN_DB,
    ): List<MediaEqBand> {
        require(maxBandGainDb.isFinite() && maxBandGainDb >= 0.0)
        val left = BAND_CENTERS_HZ.map { leftCurve.gainAt(Hertz(it)) }
        val right = BAND_CENTERS_HZ.map { rightCurve.gainAt(Hertz(it)) }
        require((left + right).all { it.isFinite() })
        val reference = maxOf(left.max(), right.max())
        return BAND_CENTERS_HZ.mapIndexed { index, center ->
            val cutoff =
                if (index == BAND_CENTERS_HZ.lastIndex) {
                    TOP_CUTOFF_HZ
                } else {
                    sqrt(center * BAND_CENTERS_HZ[index + 1])
                }
            MediaEqBand(
                centerHz = center,
                cutoffHz = cutoff,
                leftGainDb = (left[index] - reference).coerceIn(-maxBandGainDb, 0.0),
                rightGainDb = (right[index] - reference).coerceIn(-maxBandGainDb, 0.0),
            )
        }
    }
}
