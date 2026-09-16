package app.earcast.audiogram

import app.earcast.common.AudioEar
import app.earcast.common.FrequencyHz

/** A tone to present: which [ear], which [frequency], at what [level]. */
data class ToneStimulus(
    val ear: AudioEar,
    val frequency: FrequencyHz,
    val level: app.earcast.common.HearingDb,
)

/**
 * Drives a full pure-tone screening: for each (ear, frequency) point it runs an
 * independent [AdaptiveThresholdSearch], in a fixed, sensible order, and
 * assembles the per-ear/per-frequency [HearingCurve].
 *
 * Android-free state machine: the UI presents [currentStimulus], asks the user,
 * and calls [submitResponse]. Fully unit-testable with a simulated listener.
 */
class ToneCheckProtocol(
    frequencies: List<FrequencyHz> = DEFAULT_SCREENING_FREQUENCIES,
    ears: List<AudioEar> = listOf(AudioEar.RIGHT, AudioEar.LEFT),
    private val config: ThresholdSearchConfig = ThresholdSearchConfig(),
) {
    /** The (ear, frequency) points to measure, in presentation order. */
    private val points: List<Pair<AudioEar, FrequencyHz>> =
        ears.flatMap { ear -> frequencies.map { freq -> ear to freq } }

    private var index = 0
    private var staircase = AdaptiveThresholdSearch(config)
    private val thresholds = mutableListOf<HearingPoint>()

    val totalPoints: Int = points.size

    fun completedPoints(): Int = index

    fun isComplete(): Boolean = index >= points.size

    /** The tone to present now, or null when the screening is complete. */
    fun currentStimulus(): ToneStimulus? {
        if (isComplete()) return null
        val (ear, freq) = points[index]
        return ToneStimulus(ear, freq, staircase.currentLevel())
    }

    /**
     * Record the response to the current stimulus and advance. When a frequency's
     * search converges, its threshold is added and the next point begins.
     */
    fun submitResponse(heard: Boolean) {
        check(!isComplete()) { "Screening already complete" }
        when (val step = staircase.submit(heard)) {
            is ThresholdSearchStep.Present -> Unit // keep going at the new level
            is ThresholdSearchStep.Done -> {
                val (ear, freq) = points[index]
                val level =
                    when (val outcome = step.outcome) {
                        is ThresholdSearchResult.HearingPoint -> outcome.level
                        // No response in range: record at the test ceiling so the
                        // audiogram and fitting treat it as (at least) this much loss.
                        ThresholdSearchResult.NoResponse -> app.earcast.common.HearingDb(config.maxLevelDbHl)
                    }
                thresholds += HearingPoint(ear, freq, level)
                index++
                if (!isComplete()) staircase = AdaptiveThresholdSearch(config)
            }
        }
    }

    /** The audiogram assembled so far (complete once [isComplete] is true). */
    fun audiogram(): HearingCurve = HearingCurve(thresholds.toList())

    companion object {
        /**
         * A 6-frequency screening subset (octave frequencies). Ordered 1 kHz first
         * — the most reliable starting reference — then up, then down, matching
         * common clinical practice.
         */
        val DEFAULT_SCREENING_FREQUENCIES: List<FrequencyHz> =
            listOf(1000.0, 2000.0, 4000.0, 8000.0, 500.0, 250.0).map(::FrequencyHz)
    }
}
