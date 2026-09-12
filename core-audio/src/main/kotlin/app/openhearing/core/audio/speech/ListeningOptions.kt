package app.openhearing.core.audio.speech

/** Noise suppression is optional: headset firmware may already suppress background sound. */
enum class NoiseReduction(val suppressionDb: Int) {
    OFF(0), GENTLE(6), STRONG(12);

    companion object {
        fun fromName(name: String): NoiseReduction = entries.firstOrNull { it.name == name } ?: OFF
    }
}

/** Attenuates low frequencies in all captured sound; does not identify the wearer. */
enum class VoiceComfort(val reductionDb: Double) {
    OFF(0.0), GENTLE(6.0), STRONG(12.0);

    companion object {
        fun fromName(name: String): VoiceComfort = entries.firstOrNull { it.name == name } ?: GENTLE
    }
}

/** Natural avoids adding Android's call effects; compatible retains the previous source on restrictive phones. */
enum class CaptureMode {
    NATURAL, CALL_COMPATIBLE;

    companion object {
        fun fromName(name: String): CaptureMode = entries.firstOrNull { it.name == name } ?: NATURAL
    }
}

data class ListeningOptions(
    val noiseReduction: NoiseReduction = NoiseReduction.OFF,
    val voiceComfort: VoiceComfort = VoiceComfort.GENTLE,
    val captureMode: CaptureMode = CaptureMode.NATURAL,
)
