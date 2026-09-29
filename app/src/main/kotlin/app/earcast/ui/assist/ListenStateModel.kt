package app.earcast.ui.assist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.earcast.assist.LiveAudioController
import app.earcast.assist.LiveSessionBuilder
import app.earcast.assist.toOptions
import app.earcast.assist.toSettings
import app.earcast.common.AudioLimits
import app.earcast.core.audio.InputSource
import app.earcast.core.audio.StreamPhase
import app.earcast.core.audio.StreamStatus
import app.earcast.core.audio.dsp.ListeningPreset
import app.earcast.core.audio.dsp.ListeningTracker
import app.earcast.core.audio.speech.EnhancementOptions
import app.earcast.data.PreferenceStorage
import app.earcast.data.ProfileStorage
import app.earcast.data.SoundProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Relative listening-meter values for the card; see ListeningTracker. */
data class ListeningStats(
    val sessionSeconds: Long = 0,
    val todayPercent: Int = 0,
    /** Last-second output level mapped 0..1 for the live bar. */
    val levelFraction: Float = 0f,
    val showHighNote: Boolean = false,
)

data class ListenState(
    val hasProfile: Boolean = false,
    val running: Boolean = false,
    val masterGainDb: Double = DEFAULT_MASTER_GAIN_DB,
    val profiles: List<SoundProfile> = emptyList(),
    val activeProfileId: String? = null,
    val preset: ListeningPreset = ListeningPreset.STANDARD,
    val microphoneSource: InputSource = InputSource.PHONE,
    val exposure: ListeningStats = ListeningStats(),
    val sessionStatus: StreamStatus = StreamStatus(),
    val listeningOptions: EnhancementOptions = EnhancementOptions(),
) {
    val active: Boolean get() = sessionStatus.state == StreamPhase.CONNECTING || running

    companion object {
        const val DEFAULT_MASTER_GAIN_DB = AudioLimits.DEFAULT_MASTER_GAIN_CAP_DB
    }
}

/**
 * Drives assist mode: prepares the session via [LiveSessionBuilder], reflects
 * run state, and manages the saved profiles. Master gain changes apply live to a
 * running session (the chain clamps them; the limiter stays downstream). Actually
 * starting/stopping the foreground service is done by the screen (it needs a
 * Context + permissions).
 */
@HiltViewModel
class ListenStateModel
    @Inject
    constructor(
        private val controller: LiveAudioController,
        private val profileRepository: ProfileStorage,
        private val settingsRepository: PreferenceStorage,
        private val sessionFactory: LiveSessionBuilder,
    ) : ViewModel() {
        private val masterGain = MutableStateFlow(ListenState.DEFAULT_MASTER_GAIN_DB)

        private val session =
            controller.running

        private val exposureUi =
            combine(controller.exposure, settingsRepository.observeExposureToday()) { snapshot, stored ->
                val carried =
                    ListeningTracker.carriedUnits(stored.epochDay, stored.units, LocalDate.now().toEpochDay())
                val todayPercent = ListeningTracker.percentOf(carried + snapshot.unflushedUnits)
                ListeningStats(
                    sessionSeconds = snapshot.sessionSeconds,
                    todayPercent = todayPercent.toInt().coerceAtLeast(0),
                    levelFraction = levelFraction(snapshot.lastRmsDbfs),
                    showHighNote = todayPercent >= ListeningTracker.HIGH_PERCENT,
                )
            }

        private val runtime = combine(session, exposureUi) { s, e -> s to e }

        private val baseUiState =
            combine(
                profileRepository.observeActiveProfile(),
                profileRepository.observeProfiles(),
                runtime,
                masterGain,
                settingsRepository.observeAssistPreset(),
            ) { active, profiles, exposureRuntime, gain, presetName ->
                val (running, exposure) = exposureRuntime
                ListenState(
                    hasProfile = active?.audiogram?.thresholds?.isNotEmpty() == true,
                    running = running,
                    masterGainDb = active?.masterGainCapDb ?: gain,
                    profiles = profiles,
                    activeProfileId = active?.id,
                    preset = ListeningPreset.fromName(presetName),
                    exposure = exposure,
                )
            }

        val uiState: StateFlow<ListenState> =
            baseUiState
                .combine(settingsRepository.observeMicrophoneSource()) { state, microphoneName ->
                    state.copy(microphoneSource = InputSource.fromName(microphoneName))
                }.combine(settingsRepository.observeListeningSettings()) { state, settings ->
                    state.copy(listeningOptions = settings.toOptions())
                }.combine(controller.sessionStatus) { state, status -> state.copy(sessionStatus = status) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ListenState())

        /** Applies immediately (also to a running session) and persists on the active profile. */
        fun setMasterGain(db: Double) {
            masterGain.value = db
            controller.setMasterGainDb(db)
            viewModelScope.launch {
                val profile = profileRepository.observeActiveProfile().first()
                if (profile != null) {
                    profileRepository.save(profile.copy(masterGainCapDb = db))
                }
            }
        }

        fun selectProfile(profileId: String) {
            viewModelScope.launch {
                profileRepository.setActive(profileId)
                // A running session keeps its old curve; restarting applies the new
                // profile. Stop here so the user never hears an unexpected switch.
                if (controller.running.value) return@launch
                sessionFactory.prepare()
            }
        }

        fun deleteProfile(profileId: String) {
            viewModelScope.launch { profileRepository.delete(profileId) }
        }

        /** Persisted; a running session keeps its preset until restarted (like profiles). */
        fun setPreset(preset: ListeningPreset) {
            viewModelScope.launch {
                settingsRepository.setAssistPreset(preset.name)
                if (!controller.running.value) sessionFactory.prepare()
            }
        }

        /** Persist the input choice; it applies the next time Hearing Assist starts. */
        fun setMicrophoneSource(source: InputSource) {
            viewModelScope.launch {
                settingsRepository.setMicrophoneSource(source.name)
                if (!controller.running.value) sessionFactory.prepare()
            }
        }

        fun setListeningOptions(options: EnhancementOptions) {
            viewModelScope.launch {
                settingsRepository.setListeningSettings(options.toSettings())
            }
        }

        /** Prepare the controller config from the active profile. Returns true if ready. */
        suspend fun prepare(): Boolean = sessionFactory.prepare()

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L

            // Map the last-second RMS (dBFS) onto a 0..1 bar; −60 dBFS ≈ silence.
            const val LEVEL_FLOOR_DBFS = -60.0

            fun levelFraction(rmsDbfs: Double): Float =
                ((rmsDbfs - LEVEL_FLOOR_DBFS) / -LEVEL_FLOOR_DBFS)
                    .coerceIn(0.0, 1.0)
                    .toFloat()
        }
    }
