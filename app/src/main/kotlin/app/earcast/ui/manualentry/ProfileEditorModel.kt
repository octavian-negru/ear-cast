package app.earcast.ui.manualentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.earcast.audiogram.HearingCurve
import app.earcast.audiogram.HearingPoint
import app.earcast.audiogram.ToneCheckProtocol
import app.earcast.common.AudioEar
import app.earcast.common.FrequencyHz
import app.earcast.common.HearingDb
import app.earcast.core.audio.TestSignalGenerator
import app.earcast.core.audio.TestSignalLevel
import app.earcast.core.audio.TestSignalPlayer
import app.earcast.data.ProfileStorage
import app.earcast.data.newProfileFrom
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class TonePreviewState(
    val isPlaying: Boolean = false,
)

/**
 * Manual audiogram entry: lets the user type in thresholds from a professional
 * hearing test instead of running the on-device check. Saves the result as a new
 * named profile, exactly like a completed hearing check.
 */
@HiltViewModel
class ProfileEditorModel
    @Inject
    constructor(
        private val profileRepository: ProfileStorage,
        private val toneGenerator: TestSignalGenerator,
        private val tonePlayer: TestSignalPlayer,
    ) : ViewModel() {
        private var previewJob: Job? = null

        /** Pitches shown for entry, ascending — same set the on-device check measures. */
        val frequencies: List<Double> =
            ToneCheckProtocol.DEFAULT_SCREENING_FREQUENCIES.map { it.value }.sorted()

        private val _levels =
            MutableStateFlow(
                mapOf(
                    AudioEar.RIGHT to frequencies.associateWith { DEFAULT_LEVEL_DB_HL },
                    AudioEar.LEFT to frequencies.associateWith { DEFAULT_LEVEL_DB_HL },
                ),
            )

        /** Current entry per ear and frequency, in dB HL. */
        val levels: StateFlow<Map<AudioEar, Map<Double, Int>>> = _levels.asStateFlow()

        private val _previewState = MutableStateFlow(TonePreviewState())
        val previewState: StateFlow<TonePreviewState> = _previewState.asStateFlow()

        fun setLevel(
            ear: AudioEar,
            frequencyHz: Double,
            dbHl: Int,
        ) {
            val clamped = dbHl.coerceIn(MIN_LEVEL_DB_HL, MAX_LEVEL_DB_HL)
            _levels.update { current ->
                current + (ear to current.getValue(ear) + (frequencyHz to clamped))
            }
        }

        fun save(onSaved: () -> Unit) {
            stopPreview()
            val thresholds =
                _levels.value.flatMap { (ear, byFreq) ->
                    byFreq.map { (freq, db) -> HearingPoint(ear, FrequencyHz(freq), HearingDb(db.toDouble())) }
                }
            viewModelScope.launch {
                profileRepository.save(
                    newProfileFrom(HearingCurve(thresholds), name = "Manual ${LocalDate.now()}"),
                )
                onSaved()
            }
        }

        fun previewTone(
            ear: AudioEar,
            frequencyHz: Double,
        ) {
            val level = _levels.value[ear]?.get(frequencyHz) ?: return
            previewJob?.cancel()
            tonePlayer.stop()
            _previewState.value = TonePreviewState(isPlaying = true)
            previewJob =
                viewModelScope.launch {
                    try {
                        val amplitude =
                            TestSignalLevel.amplitudeFor(
                                levelDbHl = level.toDouble(),
                                maxLevelDbHl = MAX_LEVEL_DB_HL.toDouble(),
                                ceiling = TestSignalGenerator.DEFAULT_MAX_AMPLITUDE * PREVIEW_CEILING,
                            )
                        val tone =
                            toneGenerator.generate(
                                frequency = FrequencyHz(frequencyHz),
                                durationMs = PREVIEW_DURATION_MS,
                                amplitude = amplitude,
                            )
                        runCatching { tonePlayer.play(tone) }
                    } finally {
                        _previewState.value = TonePreviewState()
                    }
                }
        }

        fun stopPreview() {
            previewJob?.cancel()
            tonePlayer.stop()
            _previewState.value = TonePreviewState()
        }

        override fun onCleared() {
            stopPreview()
            tonePlayer.release()
        }

        companion object {
            const val MIN_LEVEL_DB_HL = -10
            const val MAX_LEVEL_DB_HL = 90
            const val LEVEL_STEP_DB = 5
            private const val DEFAULT_LEVEL_DB_HL = 0
            private const val PREVIEW_CEILING = 0.5f
            private const val PREVIEW_DURATION_MS = 1_000L
        }
    }
