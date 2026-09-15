package app.openhearing.core.audio.speech

import app.openhearing.core.audio.InputTuning

/** Broad presence lift within the captured speech band; cannot restore missing Bluetooth bandwidth. */
enum class SpeechClarity(
    val gainDb: Double,
) {
    OFF(0.0),
    GENTLE(3.0),
    STRONG(6.0),
    ;

    companion object {
        fun fromName(name: String): SpeechClarity = entries.firstOrNull { it.name == name } ?: GENTLE
    }
}

/** Alternate complete speech enhancers; never run both suppressors in series. */
enum class SpeechEngine {
    RNNOISE,
    DPDFNET,
    ;

    companion object {
        // Preserve the engine that existing users have already compared on their headset.
        fun fromName(name: String): SpeechEngine = entries.firstOrNull { it.name == name } ?: RNNOISE
    }
}

/**
 * Neural speech enhancement. suppressionDb sets the retained dry contribution
 * (6 dB = about 50%, 12 dB = about 25%), not a guaranteed total attenuation bound.
 * Off preserves environmental sound; headset firmware may already suppress it.
 */
enum class NoiseReduction(
    val suppressionDb: Int,
) {
    OFF(0),
    GENTLE(6),
    STRONG(12),
    ;

    companion object {
        fun fromName(name: String): NoiseReduction = entries.firstOrNull { it.name == name } ?: OFF
    }
}

/** Attenuates low frequencies in all captured sound; does not identify the wearer. */
enum class VoiceComfort(
    val reductionDb: Double,
) {
    OFF(0.0),
    GENTLE(6.0),
    STRONG(12.0),
    ;

    companion object {
        fun fromName(name: String): VoiceComfort = entries.firstOrNull { it.name == name } ?: GENTLE
    }
}

/** Natural avoids adding Android's call effects; compatible retains the previous source on restrictive phones. */
enum class CaptureMode {
    NATURAL,
    CALL_COMPATIBLE,
    ;

    fun inputTuning(supportsUnprocessed: Boolean): InputTuning =
        when {
            this == CALL_COMPATIBLE -> InputTuning.COMMUNICATION
            supportsUnprocessed -> InputTuning.RAW_UNPROCESSED
            else -> InputTuning.RAW_VOICE_RECOGNITION
        }

    companion object {
        fun fromName(name: String): CaptureMode = entries.firstOrNull { it.name == name } ?: NATURAL
    }
}

/** Opt-in upward leveling after neural enhancement; confidence never gates audio. */
enum class QuietSpeech(
    val maximumGainDb: Float,
) {
    OFF(0f),
    GENTLE(6f),
    STRONG(12f),
    ;

    companion object {
        fun fromName(name: String): QuietSpeech = entries.firstOrNull { it.name == name } ?: OFF
    }
}

data class ListeningOptions(
    val noiseReduction: NoiseReduction = NoiseReduction.OFF,
    val voiceComfort: VoiceComfort = VoiceComfort.GENTLE,
    val captureMode: CaptureMode = CaptureMode.NATURAL,
    val speechClarity: SpeechClarity = SpeechClarity.GENTLE,
    val quietSpeech: QuietSpeech = QuietSpeech.OFF,
    val speechEngine: SpeechEngine = SpeechEngine.RNNOISE,
)
