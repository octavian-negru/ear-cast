package app.openhearing.core.audio

/**
 * The boundary between the real-time audio device and
 * the pure-Kotlin DSP core. Keeping I/O behind this interface is what lets the
 * DSP — including the safety limiter — be unit-tested with no device or emulator.
 *
 * AndroidAudioEngine uses AudioRecord/AudioTrack; hardware latency needs measurement.
 */
interface AudioEngine {
    /** True while the capture -> process -> playback loop is running. */
    val isRunning: Boolean

    /** Start the low-latency loop, routing each captured block through [processor]. */
    fun start(format: AudioFormat, processor: AudioProcessor)

    /** Stop the loop and release the audio device. Safe to call when not running. */
    fun stop()
}

/**
 * How the platform should pre-process the microphone signal before it reaches
 * the chain. Natural capture avoids platform call processing where supported;
 * communication tuning is an explicit compatibility option.
 */
enum class InputTuning {
    /** VOICE_COMMUNICATION: platform AEC/NS enabled where available. */
    COMMUNICATION,

    /** UNPROCESSED: request raw capture where reported; headset firmware may still process audio. */
    RAW_UNPROCESSED,

    /** VOICE_RECOGNITION: Android's recommended fallback without AGC or noise suppression. */
    RAW_VOICE_RECOGNITION,
}

/** Physical microphone selection, independent of microphone processing. */
enum class MicrophoneSource {
    PHONE,
    HEADSET,
    ;

    companion object {
        fun fromName(name: String): MicrophoneSource = entries.firstOrNull { it.name == name } ?: PHONE
    }
}

/** Audio stream parameters negotiated with the device. */
data class AudioFormat(
    val sampleRateHz: Int,
    val channelCount: Int,
    val framesPerBlock: Int,
    val inputTuning: InputTuning = InputTuning.COMMUNICATION,
    val microphoneSource: MicrophoneSource = MicrophoneSource.PHONE,
    val requireHeadphones: Boolean = false,
) {
    init {
        require(sampleRateHz > 0) { "sampleRateHz must be positive" }
        require(channelCount in 1..2) { "channelCount must be 1 (mono) or 2 (stereo)" }
        require(framesPerBlock > 0) { "framesPerBlock must be positive" }
    }
}

/**
 * Processes one block of interleaved float samples in place. Implementations must
 * be real-time safe: no allocation, locking, or blocking on the audio thread.
 *
 * The final stage of any processing chain MUST be an [OutputLimiter] so nothing
 * can exceed the safety ceiling on its way to the device.
 */
fun interface AudioProcessor {
    fun process(buffer: FloatArray)
}
