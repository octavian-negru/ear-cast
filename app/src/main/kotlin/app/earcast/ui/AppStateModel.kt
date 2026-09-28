package app.earcast.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.earcast.assist.LiveSessionBuilder
import app.earcast.audiogram.HearingCurve
import app.earcast.billing.ProBilling
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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

// Billing controls the optional ad-free upgrade; audio access requires safety consent.
@Suppress("LongParameterList")
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
        private val proBilling: ProBilling,
    ) : ViewModel() {
        val proState = proBilling.state

        fun buyPro(activity: Activity) = proBilling.buy(activity)

        fun restorePro() = proBilling.refresh()

        private val dinAvailable = digitCorpus.isAvailable()
        private var previewJob: Job? = null
        private val mediaEqFailed = MutableStateFlow(false)

        private data class MediaSettings(
            val enabled: Boolean,
            val boostDb: Float,
            val mode: MediaProcessingMode,
        )

        private val mediaSettings =
            combine(
                settings.observeMediaEqEnabled(),
                settings.observeConsentAccepted(),
                settings.observeMediaBoostDb(),
                settings.observeMediaProcessingMode(),
            ) { enabled, consent, boost, mode ->
                MediaSettings(enabled && consent, boost, MediaProcessingMode.fromName(mode))
            }.distinctUntilChanged()

        private val baseUiState =
            combine(
                settings.observeConsentAccepted(),
                settings.observeHighContrast(),
                settings.observeComfortCeiling(),
                profileRepository.observeActiveProfile(),
                mediaEqFailed,
            ) { consent, highContrast, ceiling, profile, failed ->
                AppState(
                    consentAccepted = consent,
                    highContrast = highContrast,
                    comfortCeiling = ceiling,
                    hasProfile = profile?.audiogram?.thresholds?.isNotEmpty() == true,
                    audiogram = profile?.audiogram,
                    mediaEqSupported = mediaEq.isSupported,
                    mediaEqFailed = failed,
                    dinAvailable = dinAvailable,
                )
            }

        val uiState: StateFlow<AppState> =
            baseUiState
                .combine(mediaSettings) { state, media ->
                    state.copy(
                        mediaEqEnabled = media.enabled,
                        mediaBoostDb = media.boostDb,
                        mediaProcessingMode = media.mode,
                    )
                }.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                    AppState(),
                )

        // Fit only when the profile changes, not on every boost-slider update.
        // Use the emitted profile so configuration comes from the same snapshot.
        private val mediaBands =
            profileRepository.observeActiveProfile().map { profile ->
                profile?.let(sessionFactory::earCurves)?.let { (left, right) -> MediaCurvePlanner.plan(left, right) }
            }

        init {
            // Reuse the native effect for boost-only changes. A rejected effect
            // switches the persisted toggle off and reports failure to the UI.
            viewModelScope.launch {
                combine(mediaSettings, mediaBands) { media, bands -> media to bands }
                    .distinctUntilChanged()
                    .collect { (media, bands) ->
                        if (media.enabled) {
                            val applied =
                                bands != null && mediaEq.apply(MediaSoundConfig(bands, media.boostDb, media.mode))
                            mediaEqFailed.value = !applied
                            if (!applied) settings.setMediaEqEnabled(false)
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
