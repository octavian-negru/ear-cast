package app.openhearing.assist

import android.content.Context
import android.media.AudioManager
import android.os.Build
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
import app.openhearing.core.audio.diagnostics.SessionDiagnostics
import app.openhearing.core.audio.speech.ListeningOptions
import app.openhearing.core.audio.speech.NativeDpdfnetDenoiser
import app.openhearing.core.audio.speech.SpeechEngine
import app.openhearing.core.audio.speech.FrameDenoiser
import app.openhearing.core.audio.speech.NativeRnnoiseDenoiser
import app.openhearing.core.audio.speech.NoiseReduction
import app.openhearing.core.audio.speech.SpeechFrontEnd
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.UUID
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

/** Recording is opt-in for one start only, never restored by a tile or process restart. */
data class DiagnosticUiState(
    val armed: Boolean = false,
    val saving: Boolean = false,
    val notes: String = "",
    val message: String = "",
    val lastDirectory: File? = null,
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
    private val applicationContext = context.applicationContext
    private val diagnosticRoot = File(context.noBackupFilesDir, "audio-diagnostics")
    private val sharedRoot = File(context.cacheDir, "shared")
    private val _diagnostics = MutableStateFlow(DiagnosticUiState())
    val diagnostics: StateFlow<DiagnosticUiState> = _diagnostics.asStateFlow()
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

    fun armDiagnostics(armed: Boolean) {
        if (!_running.value && _sessionStatus.value.state != AudioSessionState.CONNECTING &&
            !_diagnostics.value.saving) {
            _diagnostics.value = _diagnostics.value.copy(armed = armed)
        }
    }

    fun setDiagnosticNotes(notes: String) {
        if (!_running.value && !_diagnostics.value.saving) {
            _diagnostics.value = _diagnostics.value.copy(notes = notes.take(512))
        }
    }

    /** Restore access to completed local files without re-arming recording. Call on an I/O dispatcher. */
    @Synchronized
    fun refreshDiagnostics() {
        if (_diagnostics.value.saving || _diagnostics.value.lastDirectory != null) return
        val latest = diagnosticRoot.listFiles()?.filter {
            File(it, "metadata.json").isFile && File(it, "stages.wav").isFile
        }?.maxByOrNull { File(it, "metadata.json").lastModified() }
        if (latest != null && !_diagnostics.value.saving && _diagnostics.value.lastDirectory == null) {
            _diagnostics.value = _diagnostics.value.copy(lastDirectory = latest, message = "Local recording available")
        }
    }

    /** Call on an I/O dispatcher. Never deletes a recording still being written. */
    @Synchronized
    fun deleteDiagnostics() {
        if (_diagnostics.value.saving || _running.value ||
            _sessionStatus.value.state == AudioSessionState.CONNECTING) return
        val folders = diagnosticRoot.listFiles()?.filter { it.isDirectory }.orEmpty()
        val exports = sharedRoot.listFiles()?.filter {
            it.name.startsWith("sound-") && (it.extension == "zip" || it.extension == "tmp")
        }.orEmpty()
        val deletedFolders = folders.map { it.deleteRecursively() }.all { it }
        val deletedExports = exports.map { it.delete() }.all { it }
        _diagnostics.value = _diagnostics.value.copy(
            lastDirectory = null,
            message = if (deletedFolders && deletedExports) {
                "Local recordings deleted"
            } else {
                "Some files could not be deleted"
            },
        )
    }

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
        if (engine.isRunning || _sessionStatus.value.state == AudioSessionState.CONNECTING) return
        val recordRequested = _diagnostics.value.armed
        _diagnostics.value = _diagnostics.value.copy(armed = false)
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
                speechPresenceDb = c.listeningOptions.speechClarity.gainDb,
            )
            val right = HearingAssistChain(
                gainCurve = current.rightGainCurve,
                sampleRateHz = actual.sampleRateHz,
                masterGainDb = current.masterGainDb,
                ceilingLinear = current.ceilingLinear,
                highPassHz = current.highPassHz,
                feedbackGuardEnabled = true,
                speechPresenceDb = c.listeningOptions.speechClarity.gainDb,
            )
            val newChain = StereoAssistChain(left, right, actual.framesPerBlock)
            chain = newChain
            // The meter taps the buffer AFTER the chain (post-limiter), so it
            // sees exactly what reaches the device; the chain stays untouched.
            val denoiser = createDenoiser(actual.sampleRateHz, c.listeningOptions)
            val recording = if (recordRequested) createDiagnostics(actual, current) else null
            SpeechFrontEnd(
                actual.sampleRateHz,
                c.listeningOptions,
                MeteredAudioProcessor(newChain, outputMeter),
                denoiser,
                recording,
            )
        }
    }

    private fun createDenoiser(rate: Int, options: ListeningOptions): FrameDenoiser? = when {
        options.noiseReduction == NoiseReduction.OFF -> null
        options.speechEngine == SpeechEngine.DPDFNET ->
            NativeDpdfnetDenoiser(applicationContext, rate, options.noiseReduction.suppressionDb)
        else -> NativeRnnoiseDenoiser(rate, options.noiseReduction.suppressionDb, options.quietSpeech.maximumGainDb)
    }

    @Synchronized
    private fun createDiagnostics(actual: AudioFormat, c: AssistConfig): SessionDiagnostics? {
        return try {
            check(!_diagnostics.value.saving) { "Previous recording is still saving" }
            val bytes = diagnosticRoot.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            check(bytes < 96L * 1024 * 1024) { "Recording storage is full. Export and delete local recordings first." }
            _diagnostics.value = _diagnostics.value.copy(saving = true, message = "Recording up to 30 seconds locally")
            SessionDiagnostics(
                File(diagnosticRoot, UUID.randomUUID().toString()), actual.sampleRateHz, actual.framesPerBlock,
                mapOf(
                    "created_utc" to java.time.Instant.now().toString(),
                    "phone" to "${Build.MANUFACTURER} ${Build.MODEL}",
                    "android_sdk" to Build.VERSION.SDK_INT.toString(),
                    "headset_firmware" to "unavailable_via_android_audio_api_see_notes",
                    "notes" to _diagnostics.value.notes,
                    "requested_sample_rate" to c.sampleRateHz.toString(),
                    "settings" to c.listeningOptions.toString(),
                    "initial_master_gain_db" to c.masterGainDb.toString(),
                    "ceiling_linear" to c.ceilingLinear.toString(),
                    "left_fit" to c.leftGainCurve.points.toString(), "right_fit" to c.rightGainCurve.points.toString(),
                    "bundled_rnnoise_revision" to "70f1d256acd4b34a572f999a05c87bf00b67730d",
                ),
            ) { directory, error ->
                _diagnostics.value = _diagnostics.value.copy(
                    saving = false, lastDirectory = directory,
                    message = error ?: "Recording saved on this device",
                )
            }
        } catch (e: Exception) {
            _diagnostics.value = _diagnostics.value.copy(saving = false, message = e.message ?: "Recording unavailable")
            null // Diagnostic storage failure must not end hearing assistance.
        }
    }

    fun stopEngine() {
        engine.stop()
        chain = null
        _running.value = false
        _exposure.value = ExposureSnapshot()
    }
}
