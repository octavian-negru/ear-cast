package app.openhearing.assist

import android.content.Context
import android.media.AudioManager
import app.openhearing.audiogram.GainCurve
import app.openhearing.core.audio.AndroidAudioEngine
import app.openhearing.core.audio.AudioFormat
import app.openhearing.core.audio.AudioSessionState
import app.openhearing.core.audio.AudioSessionStatus
import app.openhearing.core.audio.MicrophoneSource
import app.openhearing.core.audio.dsp.HearingAssistChain
import app.openhearing.core.audio.dsp.LevelWindow
import app.openhearing.core.audio.dsp.MeteredAudioProcessor
import app.openhearing.core.audio.dsp.OutputLevelMeter
import app.openhearing.core.audio.dsp.StereoAssistChain
import app.openhearing.core.audio.speech.ListeningOptions
import app.openhearing.core.audio.speech.NativeSpeexDenoiser
import app.openhearing.core.audio.speech.NoiseReduction
import app.openhearing.core.audio.speech.SpeechFrontEnd
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Live relative-exposure numbers for the current session, published by
 * [AssistService]'s sampling loop. `unflushedUnits` is the portion not yet
 * persisted (the UI adds it to the stored day so the meter never jumps).
 */
data class ExposureSnapshot(
    val sessionSeconds: Long = 0,
    val sessionUnits: Double = 0.0,
    val unflushedUnits: Double = 0.0,
    val lastRmsDbfs: Double = Double.NEGATIVE_INFINITY,
)

/** Immutable configuration for an assist session, derived from the active profile. */
data class AssistConfig(
    /** Per-ear fitted curves; left ear drives the left channel, right the right. */
    val leftGainCurve: GainCurve,
    val rightGainCurve: GainCurve,
    val masterGainDb: Double,
    /** Output ceiling (linear) from comfort calibration; the limiter enforces it. */
    val ceilingLinear: Float = HearingAssistChain.DEFAULT_CEILING_LINEAR,
    /** Optional low-cut from the environment preset (e.g. outdoors wind cut). */
    val highPassHz: Double? = null,
    val sampleRateHz: Int = DEFAULT_SAMPLE_RATE_HZ,
    val framesPerBlock: Int = DEFAULT_FRAMES_PER_BLOCK,
    val listeningOptions: ListeningOptions = ListeningOptions(),
    val microphoneSource: MicrophoneSource = MicrophoneSource.PHONE,
    val requireHeadphones: Boolean = false,
) {
    companion object {
        const val DEFAULT_SAMPLE_RATE_HZ = 48_000

        // ~4 ms at 48 kHz — a low-latency block size; the engine adjusts device buffers.
        const val DEFAULT_FRAMES_PER_BLOCK = 192

    }
}

/**
 * Single source of truth for Hearing Assist state, shared between the UI and the
 * foreground [AssistService]. The UI sets [config] and starts/stops the service;
 * the service drives the engine through here. Mute is immediate.
 */
@Singleton
class AssistController
@Inject
constructor(
    @ApplicationContext context: Context,
) {
    private val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val _sessionStatus = MutableStateFlow(AudioSessionStatus())
    val sessionStatus: StateFlow<AudioSessionStatus> = _sessionStatus.asStateFlow()

    private val engine = AndroidAudioEngine(context) { status ->
        _sessionStatus.value = status
        when (status.state) {
            AudioSessionState.RUNNING -> _running.value = true
            AudioSessionState.CONNECTING -> Unit
            AudioSessionState.STOPPED,
            AudioSessionState.FAILED,
            -> {
                _running.value = false
            }
        }
    }

    @Volatile
    private var config: AssistConfig? = null

    @Volatile
    private var chain: StereoAssistChain? = null

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    // Post-limiter output tap; drained ~1/s by the service's sampling loop.
    private val outputMeter = OutputLevelMeter()

    private val _exposure = MutableStateFlow(ExposureSnapshot())
    val exposure: StateFlow<ExposureSnapshot> = _exposure.asStateFlow()

    /** Drain the level window accumulated since the last call (service loop). */
    fun drainOutputLevel(): LevelWindow = outputMeter.drain()

    fun publishExposure(snapshot: ExposureSnapshot) {
        _exposure.value = snapshot
    }

    fun configure(newConfig: AssistConfig) {
        config = newConfig
    }

    fun hasConfig(): Boolean = config != null

    /**
     * Adjusts master gain immediately, including while the engine is running.
     * The chain clamps to the safety cap and the limiter stays downstream.
     */
    fun setMasterGainDb(db: Double) {
        config = config?.copy(masterGainDb = db)
        chain?.setMasterGainDb(db)
    }

    /** Build the per-ear chains from the current config and start the real-time loop. */
    fun startEngine() {
        val c = config ?: return
        if (engine.isRunning) return
        outputMeter.drain() // discard anything left from a previous session
        engine.startSession(
            AudioFormat(
                c.sampleRateHz,
                channelCount = 2,
                framesPerBlock = c.framesPerBlock,
                inputTuning = c.listeningOptions.captureMode.inputTuning(
                    manager.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true",
                ),
                microphoneSource = c.microphoneSource,
                requireHeadphones = c.requireHeadphones,
            ),
        ) { actual ->
            val current = config ?: c
            val left = HearingAssistChain(
                gainCurve = current.leftGainCurve,
                sampleRateHz = actual.sampleRateHz,
                masterGainDb = current.masterGainDb,
                ceilingLinear = current.ceilingLinear,
                highPassHz = current.highPassHz,
                feedbackGuardEnabled = true,
            )
            val right = HearingAssistChain(
                gainCurve = current.rightGainCurve,
                sampleRateHz = actual.sampleRateHz,
                masterGainDb = current.masterGainDb,
                ceilingLinear = current.ceilingLinear,
                highPassHz = current.highPassHz,
                feedbackGuardEnabled = true,
            )
            val newChain = StereoAssistChain(left, right, actual.framesPerBlock)
            chain = newChain
            // The meter taps the buffer AFTER the chain (post-limiter), so it
            // sees exactly what reaches the device; the chain stays untouched.
            SpeechFrontEnd(
                actual.sampleRateHz,
                c.listeningOptions,
                MeteredAudioProcessor(newChain, outputMeter),
                if (c.listeningOptions.noiseReduction == NoiseReduction.OFF) {
                    null
                } else {
                    NativeSpeexDenoiser(actual.sampleRateHz, c.listeningOptions.noiseReduction.suppressionDb)
                },
            )
        }
    }

    fun stopEngine() {
        engine.stop()
        chain = null
        _running.value = false
        _exposure.value = ExposureSnapshot()
    }
}
