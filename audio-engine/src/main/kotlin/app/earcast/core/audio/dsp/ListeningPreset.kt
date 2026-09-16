package app.earcast.core.audio.dsp

import app.earcast.audiogram.FrequencyGainCurve

/**
 * Speech profiles layered on top of the user's fitted gain curve. Deliberately
 * gentle: the profile does the personalization; a preset only nudges it for a
 * situation. The limiter downstream bounds everything regardless.
 */
enum class ListeningPreset(
    /** Optional low-cut frequency (wind/rumble suppression); null = no low cut. */
    val highPassHz: Double?,
) {
    /** The fitted profile as-is. */
    STANDARD(null),

    /** Extra clarity in the speech band (1–4 kHz). */
    CONVERSATION(null),

    ;

    companion object {
        const val CONVERSATION_BOOST_DB = 2.0
        const val SPEECH_BAND_LOW_HZ = 1_000.0
        const val SPEECH_BAND_HIGH_HZ = 4_000.0

        fun fromName(name: String?): ListeningPreset = entries.firstOrNull { it.name == name } ?: STANDARD
    }
}

/** Applies [preset] to this fitted curve (pure; the original curve is untouched). */
fun FrequencyGainCurve.withPreset(preset: ListeningPreset): FrequencyGainCurve =
    when (preset) {
        ListeningPreset.CONVERSATION ->
            FrequencyGainCurve(
                points.map { p ->
                    val inSpeechBand =
                        p.frequency.value in
                            ListeningPreset.SPEECH_BAND_LOW_HZ..ListeningPreset.SPEECH_BAND_HIGH_HZ
                    if (inSpeechBand) {
                        p.copy(gainDb = p.gainDb + ListeningPreset.CONVERSATION_BOOST_DB)
                    } else {
                        p
                    }
                },
            )
        ListeningPreset.STANDARD -> this
    }
