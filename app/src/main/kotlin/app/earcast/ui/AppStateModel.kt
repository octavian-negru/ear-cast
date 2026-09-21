package app.earcast.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.earcast.assist.LiveSessionBuilder
import app.earcast.audiogram.HearingCurve
import app.earcast.common.AudioLimits
import app.earcast.common.FrequencyHz
import app.earcast.core.audio.TestSignalGenerator
import app.earcast.core.audio.TestSignalPlayer
import app.earcast.core.audio.dsp.MediaCurvePlanner
import app.earcast.core.audio.dsp.MediaProcessingMode
import app.earcast.core.audio.dsp.MediaSoundConfig
import app.earcast.data.PreferenceStorage
import app.earcast.data.ProfileStorage
import app.earcast.mediaeq.MediaSoundController
import app.earcast.ui.dintest.SpokenDigitLibrary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** [consentAccepted] is null while loading, then true/false. */
data class AppState(
    val consentAccepted: Boolean? = null,
    val highContrast: Boolean = false,
    val comfortCeiling: Float = 0.5f,
    val hasProfile: Boolean = false,
    val audiogram: HearingCurve? = null,
    val mediaEqEnabled: Boolean = false,
    val mediaEqSupported: Boolean = false,
    val mediaEqFailed: Boolean = false,
    val mediaBoostDb: Float = AudioLimits.DEFAULT_MEDIA_BOOST_DB,
    val mediaProcessingMode: MediaProcessingMode = MediaProcessingMode.BALANCED,
    /** True when the digits-in-noise corpus ships in this build (docs/DIN.md). */
    val dinAvailable: Boolean = false,
)

@HiltViewModel
class AppStateModel
    @Inject
    constructor(
        private val settings: PreferenceStorage,
        private val profileRepository: ProfileStorage,
        private val sessionFactory: LiveSessionBuilder,
        private val mediaEq: MediaSoundController,
        private val toneGenerator: TestSignalGenerator,
        private val tonePlayer: TestSignalPlayer,
        digitCorpus: SpokenDigitLibrary,
    ) : ViewModel() {
        private val dinAvailable = digitCorpus.isAvailable()
        private var previewJob: Job? = null
        private val mediaEqFailed = MutableStateFlow(false)

        private val baseUiState =
            combine(
                combine(
                    settings.observeConsentAccepted(),
                    settings.observeHighContrast(),
                    settings.observeComfortCeiling(),
                ) { consent, highContrast, ceiling -> Triple(consent, highContrast, ceiling) },
                combine(
                    settings.observeMediaEqEnabled(),
                    profileRepository.observeActiveProfile(),
                    mediaEqFailed,
                ) { eqEnabled, profile, failed -> Triple(eqEnabled, profile, failed) },
            ) { (consent, highContrast, ceiling), (eqEnabled, profile, failed) ->
                AppState(
                    consentAccepted = consent,
                    highContrast = highContrast,
                    comfortCeiling = ceiling,
                    hasProfile = profile?.audiogram?.thresholds?.isNotEmpty() == true,
                    audiogram = profile?.audiogram,
                    mediaEqEnabled = eqEnabled,
                    mediaEqSupported = mediaEq.isSupported,
                    mediaEqFailed = failed,
                    dinAvailable = dinAvailable,
                )
            }

        private val uiStateWithBoost =
            baseUiState.combine(settings.observeMediaBoostDb()) { state, boost -> state.copy(mediaBoostDb = boost) }

        val uiState: StateFlow<AppState> =
            uiStateWithBoost
                .combine(
                    settings.observeMediaProcessingMode(),
                ) { state, mode ->
                    state.copy(mediaProcessingMode = MediaProcessingMode.fromName(mode))
                }.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                    AppState(),
                )

        init {
            // Media EQ follows the persisted toggle and the active profile: applied
            // (or re-applied with fresh curves) when on, released when off. On a
            // device that rejects the global effect, the toggle snaps back off and
            // the failure is surfaced.
            viewModelScope.launch {
                combine(
                    settings.observeMediaEqEnabled(),
                    profileRepository.observeActiveProfile(),
                    settings.observeMediaBoostDb(),
                    settings.observeMediaProcessingMode(),
                ) { enabled, _, boost, mode -> Triple(enabled, boost, MediaProcessingMode.fromName(mode)) }
                    .collect { (enabled, boost, mode) ->
                        if (enabled) {
                            val ok = applyMediaEq(boost, mode)
                            mediaEqFailed.value = !ok
                            if (!ok) settings.setMediaEqEnabled(false)
                        } else {
                            mediaEq.release()
                        }
                    }
            }
        }

        fun acceptDisclaimer() {
            viewModelScope.launch { settings.setConsentAccepted(true) }
        }

        fun setHighContrast(enabled: Boolean) {
            viewModelScope.launch { settings.setHighContrast(enabled) }
        }

        fun setComfortCeiling(value: Float) {
            viewModelScope.launch { settings.setComfortCeiling(value) }
        }

        fun setMediaEq(enabled: Boolean) {
            viewModelScope.launch {
                mediaEqFailed.value = false
                settings.setMediaEqEnabled(enabled)
            }
        }

        fun setMediaBoost(db: Float) {
            viewModelScope.launch { settings.setMediaBoostDb(db) }
        }

        fun setMediaProcessingMode(mode: MediaProcessingMode) {
            viewModelScope.launch { settings.setMediaProcessingMode(mode.name) }
        }

        private suspend fun applyMediaEq(
            boostDb: Float,
            mode: MediaProcessingMode,
        ): Boolean {
            val curves = sessionFactory.activeEarCurves() ?: return false
            return mediaEq.apply(MediaSoundConfig(MediaCurvePlanner.plan(curves.first, curves.second), boostDb, mode))
        }

        /**
         * Play a short 1 kHz tone at the chosen ceiling so the user can hear how
         * loud the maximum will be and pick a comfortable level. The TestSignalPlayer's
         * own limiter still bounds the output.
         */
        fun previewComfort(ceiling: Float) {
            previewJob?.cancel()
            previewJob =
                viewModelScope.launch {
                    val tone = toneGenerator.generate(FrequencyHz(1000.0), durationMs = 800, amplitude = ceiling)
                    runCatching { tonePlayer.play(tone) }
                }
        }

        override fun onCleared() {
            previewJob?.cancel()
            tonePlayer.release()
            // Deliberately NOT releasing the media EQ here: the whole point is that
            // it keeps shaping other apps' audio after the user leaves this app. It
            // dies with the process; the toggle re-applies it on next launch.
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
