package app.earcast.data

import kotlinx.coroutines.flow.Flow

/** App-level settings and the one-time disclaimer consent flag. */
interface PreferenceStorage {
    /** True once the user has acknowledged the safety/legal disclaimer. */
    fun observeConsentAccepted(): Flow<Boolean>

    suspend fun setConsentAccepted(accepted: Boolean)

    /** High-contrast theme preference (accessibility). */
    fun observeHighContrast(): Flow<Boolean>

    suspend fun setHighContrast(enabled: Boolean)

    /**
     * The "comfort" output ceiling as a linear amplitude in (0, 1]. This is the
     * maximum loudness the assist limiter will allow, set by the user during
     * comfort calibration. A conservative default applies until calibrated. See
     * docs/CALIBRATION.md.
     */
    fun observeComfortCeiling(): Flow<Float>

    suspend fun setComfortCeiling(value: Float)

    /**
     * Assist environment preset, stored by name (e.g. "STANDARD"). The app layer
     * maps it to the DSP preset enum; unknown names fall back to standard.
     */
    fun observeAssistPreset(): Flow<String>

    suspend fun setAssistPreset(name: String)

    /** Saved microphone choice; app maps unknown names to the phone microphone. */
    fun observeMicrophoneSource(): Flow<String>

    suspend fun setMicrophoneSource(name: String)

    fun observeListeningSettings(): Flow<SoundPreferences>

    suspend fun setListeningSettings(settings: SoundPreferences)

    /** Media EQ (profile applied to other apps' audio) on/off. */
    fun observeMediaEqEnabled(): Flow<Boolean>

    suspend fun setMediaEqEnabled(enabled: Boolean)

    /** Independent media boost in dB; does not change any assist profile. */
    fun observeMediaBoostDb(): Flow<Float>

    suspend fun setMediaBoostDb(db: Float)

    /**
     * Accumulated relative listening-exposure units for one epoch day (see the
     * ListeningTracker in :audio-engine for the unit definition — deliberately
     * relative, never dB SPL). Reset implicitly when the stored day changes.
     */
    fun observeExposureToday(): Flow<DailyListening>

    /** Add [units] to [epochDay]; a different stored day is replaced, not summed. */
    suspend fun addExposureUnits(
        units: Double,
        epochDay: Long,
    )
}

/** Exposure units accumulated on [epochDay] (may be a past day until next write). */
data class DailyListening(
    val epochDay: Long,
    val units: Double,
)

/** Names are mapped to DSP enums by the app, keeping persistence independent of audio. */
data class SoundPreferences(
    val noiseReduction: String = "OFF",
    val voiceComfort: String = "GENTLE",
    val captureMode: String = "NATURAL",
    val speechClarity: String = "GENTLE",
    val quietSpeech: String = "OFF",
    val speechEngine: String = "RNNOISE",
)
