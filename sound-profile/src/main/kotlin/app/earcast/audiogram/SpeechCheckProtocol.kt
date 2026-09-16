package app.earcast.audiogram

import kotlin.random.Random

/**
 * Configuration for the digits-in-noise (DIN) speech screening. Defaults follow
 * the widely used Smits-style procedure: fixed 2 dB steps, one-down (whole
 * triplet correct) / one-up (any digit wrong), with the speech reception
 * threshold (SRT) taken as the mean SNR of the presentations after a short
 * approach phase.
 *
 * The key property: the result is an SNR (speech relative to noise), so it is
 * meaningful on **uncalibrated** consumer hardware — absolute playback level
 * cancels out of the ratio. Still a screening estimate, never a diagnosis.
 */
data class SpeechProtocolConfig(
    val startSnrDb: Double = 4.0,
    val stepDb: Double = 2.0,
    val totalTriplets: Int = 24,
    val minSnrDb: Double = -20.0,
    val maxSnrDb: Double = 16.0,
    /** 1-based index of the first *scored* triplet (earlier ones are approach). */
    val scoredFromTriplet: Int = 5,
) {
    init {
        require(stepDb > 0) { "step must be positive" }
        require(maxSnrDb > minSnrDb) { "max SNR must exceed min SNR" }
        require(totalTriplets >= scoredFromTriplet) { "must score at least one triplet" }
        require(scoredFromTriplet >= 1) { "scoredFromTriplet is 1-based" }
    }
}

/** What the caller should do next, returned after each triplet is submitted. */
sealed interface SpeechProtocolStep {
    /** Present another triplet at [snrDb], then submit the answer again. */
    data class Present(
        val snrDb: Double,
    ) : SpeechProtocolStep

    /** The screening is complete. */
    data class Done(
        val result: SpeechProtocolResult,
    ) : SpeechProtocolStep
}

/**
 * Result of a DIN screening: [srtSnrDb] is the mean presented SNR over the
 * scored triplets. [pinnedAtEdge] flags sessions that sat on the SNR range
 * limit — report those as "beyond the range" rather than as a clean number.
 */
data class SpeechProtocolResult(
    val srtSnrDb: Double,
    val tripletsPresented: Int,
    val pinnedAtEdge: Boolean,
)

/**
 * Adaptive SNR staircase for the DIN screening — the speech-in-noise analogue
 * of [AdaptiveThresholdSearch]. Deterministic and Android-free.
 */
class SnrSearch(
    private val config: SpeechProtocolConfig = SpeechProtocolConfig(),
) {
    private var snr = config.startSnrDb.coerceIn(config.minSnrDb, config.maxSnrDb)
    private val scoredSnrs = mutableListOf<Double>()
    private var presented = 0
    private var pinned = false

    /** The SNR the caller should present the current triplet at. */
    fun currentSnrDb(): Double = snr

    /** Submit whether the whole triplet was repeated correctly. */
    fun submit(tripletCorrect: Boolean): SpeechProtocolStep {
        presented++
        if (presented >= config.scoredFromTriplet) {
            scoredSnrs += snr
            if (snr <= config.minSnrDb || snr >= config.maxSnrDb) pinned = true
        }
        val delta = if (tripletCorrect) -config.stepDb else config.stepDb
        snr = (snr + delta).coerceIn(config.minSnrDb, config.maxSnrDb)
        return if (presented >= config.totalTriplets) {
            SpeechProtocolStep.Done(
                SpeechProtocolResult(
                    srtSnrDb = scoredSnrs.average(),
                    tripletsPresented = presented,
                    pinnedAtEdge = pinned,
                ),
            )
        } else {
            SpeechProtocolStep.Present(snr)
        }
    }
}

/**
 * Session driver for the DIN screening: owns the staircase and generates the
 * digit triplets (no repeated digit within a triplet). The digit alphabet
 * excludes 7 by default — the only two-syllable English digit — to keep token
 * difficulty roughly homogeneous. Randomness is injected for deterministic tests.
 */
class SpeechProtocol(
    config: SpeechProtocolConfig = SpeechProtocolConfig(),
    private val random: Random = Random.Default,
    private val alphabet: List<Int> = DEFAULT_ALPHABET,
) {
    init {
        require(alphabet.size >= TRIPLET_SIZE) { "alphabet must allow a distinct triplet" }
    }

    private val staircase = SnrSearch(config)
    private var triplet: List<Int> = newTriplet()
    private var finished: SpeechProtocolResult? = null

    fun currentTriplet(): List<Int> = triplet

    fun currentSnrDb(): Double = staircase.currentSnrDb()

    fun isComplete(): Boolean = finished != null

    fun result(): SpeechProtocolResult? = finished

    /** Score [answered] against the current triplet and advance the staircase. */
    fun submit(answered: List<Int>): SpeechProtocolStep {
        check(finished == null) { "screening already complete" }
        val step = staircase.submit(answered == triplet)
        when (step) {
            is SpeechProtocolStep.Present -> triplet = newTriplet()
            is SpeechProtocolStep.Done -> finished = step.result
        }
        return step
    }

    private fun newTriplet(): List<Int> = alphabet.shuffled(random).take(TRIPLET_SIZE)

    companion object {
        const val TRIPLET_SIZE = 3

        /** 0–9 minus the two-syllable "7". */
        val DEFAULT_ALPHABET = listOf(0, 1, 2, 3, 4, 5, 6, 8, 9)
    }
}
