package app.earcast.core.audio.speech

import app.earcast.core.audio.CaptureTuning

/** Relative presence emphasis without added treble gain; cannot restore missing Bluetooth bandwidth. */
enum class SpeechPresence(
    val gainDb: Double,
) {
    OFF(0.0),
    GENTLE(3.0),
    STRONG(6.0),
    ;

    companion object {
        fun fromName(name: String): SpeechPresence = entries.firstOrNull { it.name == name } ?: GENTLE
    }
}

/** Alternate complete speech enhancers; never run suppressors in series. */
enum class EnhancementEngine {
    RNNOISE,
    DPDFNET,
    SPEEX,
    WIENER,
    ;

    companion object {
        // Preserve the engine that existing users have already compared on their headset.
        fun fromName(name: String): EnhancementEngine = entries.firstOrNull { it.name == name } ?: RNNOISE
    }
}

/**
 * Noise reduction strength. For neural engines, suppressionDb sets the retained dry contribution
 * (6 dB = about 50%, 12 dB = about 25%), not a guaranteed total attenuation bound.
 * Classical engines use it as their spectral attenuation floor.
 * Off preserves environmental sound; headset firmware may already suppress it.
 */
enum class NoiseStrength(
    val suppressionDb: Int,
) {
    OFF(0),
    GENTLE(6),
    STRONG(12),
    ;

    companion object {
        fun fromName(name: String): NoiseStrength = entries.firstOrNull { it.name == name } ?: OFF
    }
}

/** Attenuates low frequencies in all captured sound; does not identify the wearer. */
enum class BassReduction(
    val reductionDb: Double,
) {
    OFF(0.0),
    GENTLE(6.0),
    STRONG(12.0),
    ;

    companion object {
        fun fromName(name: String): BassReduction = entries.firstOrNull { it.name == name } ?: GENTLE
    }
}

/** Natural avoids adding Android's call effects; compatible retains the previous source on restrictive phones. */
enum class InputMode {
    NATURAL,
    CALL_COMPATIBLE,
    ;

    fun inputTuning(supportsUnprocessed: Boolean): CaptureTuning =
        when {
            this == CALL_COMPATIBLE -> CaptureTuning.COMMUNICATION
            supportsUnprocessed -> CaptureTuning.RAW_UNPROCESSED
            else -> CaptureTuning.RAW_VOICE_RECOGNITION
        }

    companion object {
        fun fromName(name: String): InputMode = entries.firstOrNull { it.name == name } ?: NATURAL
    }
}

/** Opt-in upward leveling after neural enhancement; confidence never gates audio. */
enum class VoiceBoost(
    val maximumGainDb: Float,
) {
    OFF(0f),
    GENTLE(3f),
    STRONG(6f),
    ;

    companion object {
        fun fromName(name: String): VoiceBoost = entries.firstOrNull { it.name == name } ?: OFF
    }
}

data class EnhancementOptions(
    val noiseReduction: NoiseStrength = NoiseStrength.OFF,
    val voiceComfort: BassReduction = BassReduction.GENTLE,
    val captureMode: InputMode = InputMode.NATURAL,
    val speechClarity: SpeechPresence = SpeechPresence.GENTLE,
    val quietSpeech: VoiceBoost = VoiceBoost.OFF,
    val speechEngine: EnhancementEngine = EnhancementEngine.RNNOISE,
)
