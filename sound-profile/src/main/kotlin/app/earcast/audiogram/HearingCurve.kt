package app.earcast.audiogram

import app.earcast.common.AudioEar
import app.earcast.common.FrequencyHz
import app.earcast.common.HearingDb

/**
 * A single measured (or estimated) hearing threshold: for a given [ear] at a
 * given [frequency], the quietest level the listener could reliably detect.
 */
data class HearingPoint(
    val ear: AudioEar,
    val frequency: FrequencyHz,
    val level: HearingDb,
)

/** Per-ear screening thresholds; uncalibrated estimates, not a diagnostic audiogram. */
data class HearingCurve(
    val thresholds: List<HearingPoint>,
) {
    /** The threshold for [ear] at [frequency], or null if it was not measured. */
    fun thresholdAt(
        ear: AudioEar,
        frequency: FrequencyHz,
    ): HearingDb? = thresholds.firstOrNull { it.ear == ear && it.frequency == frequency }?.level

    /** Frequencies measured for [ear], in ascending order. */
    fun frequenciesFor(ear: AudioEar): List<FrequencyHz> =
        thresholds
            .filter { it.ear == ear }
            .map { it.frequency }
            .distinct()
            .sortedBy { it.value }

    companion object {
        /** An empty audiogram, e.g. before any screening has been run. */
        val EMPTY: HearingCurve = HearingCurve(emptyList())
    }
}
