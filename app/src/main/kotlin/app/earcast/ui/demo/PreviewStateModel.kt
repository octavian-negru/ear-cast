package app.earcast.ui.demo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.earcast.assist.LiveAudioController
import app.earcast.assist.LiveSessionBuilder
import app.earcast.core.audio.PreviewBufferSource
import app.earcast.core.audio.PreviewPlayer
import app.earcast.core.audio.PreviewSignalGenerator
import app.earcast.core.audio.TestSignalGenerator
import app.earcast.core.audio.dsp.PreviewRenderer
import app.earcast.data.PreferenceStorage
import app.earcast.data.ProfileStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class PreviewState(
    /** An active profile exists, so the demo can be rendered. */
    val available: Boolean = false,
    /** Assist is live-processing the mic; the demo stays out of its way. */
    val assistRunning: Boolean = false,
    val playing: Boolean = false,
    val processedActive: Boolean = false,
)

/**
 * Drives the "hear the difference" A/B demo: renders the synthesized clip raw
 * and through the user's real per-ear chains (limiter last, as in a live
 * session), then streams it with a live raw/processed toggle. The buffers are
 * rebuilt on every play so profile, gain, and comfort-ceiling changes are
 * always reflected.
 */
@HiltViewModel
class PreviewStateModel
    @Inject
    constructor(
        private val sessionFactory: LiveSessionBuilder,
        private val profileRepository: ProfileStorage,
        private val settingsRepository: PreferenceStorage,
        controller: LiveAudioController,
        private val player: PreviewPlayer,
    ) : ViewModel() {
        private val playing = MutableStateFlow(false)
        private val processed = MutableStateFlow(false)

        @Volatile
        private var activeSource: PreviewBufferSource? = null

        val uiState: StateFlow<PreviewState> =
            combine(
                profileRepository.observeActiveProfile(),
                controller.running,
                playing,
                processed,
            ) { profile, assistRunning, isPlaying, processedOn ->
                PreviewState(
                    available = profile?.audiogram?.thresholds?.isNotEmpty() == true,
                    assistRunning = assistRunning,
                    playing = isPlaying,
                    processedActive = processedOn,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PreviewState())

        fun togglePlayback() {
            if (playing.value) stop() else play()
        }

        /** Applies live: the source crossfades to the other version mid-playback. */
        fun setProcessed(on: Boolean) {
            processed.value = on
            activeSource?.processedActive = on
        }

        fun stop() {
            player.stop()
        }

        private fun play() {
            viewModelScope.launch {
                if (uiState.value.assistRunning || playing.value) return@launch
                val source = withContext(Dispatchers.Default) { buildSource() } ?: return@launch
                source.processedActive = processed.value
                activeSource = source
                playing.value = true
                try {
                    player.play(source)
                } finally {
                    playing.value = false
                    activeSource = null
                }
            }
        }

        private suspend fun buildSource(): PreviewBufferSource? {
            val curves = sessionFactory.activeEarCurves() ?: return null
            val profile = profileRepository.observeActiveProfile().first() ?: return null
            val ceiling = settingsRepository.observeComfortCeiling().first()
            val mono = PreviewSignalGenerator(sampleRateHz = SAMPLE_RATE_HZ).generate()
            val processedBuffer =
                PreviewRenderer.renderProcessed(
                    mono = mono,
                    leftCurve = curves.first,
                    rightCurve = curves.second,
                    masterGainDb = profile.masterGainCapDb,
                    ceilingLinear = ceiling,
                    sampleRateHz = SAMPLE_RATE_HZ,
                )
            return PreviewBufferSource(PreviewRenderer.stereoRaw(mono), processedBuffer)
        }

        override fun onCleared() {
            player.release()
        }

        private companion object {
            const val SAMPLE_RATE_HZ = TestSignalGenerator.DEFAULT_SAMPLE_RATE_HZ
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
