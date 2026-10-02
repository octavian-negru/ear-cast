package app.earcast.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.earcast.audiogram.HearingCurve
import app.earcast.audiogram.HearingCurveCodec
import app.earcast.common.AudioLimits
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * DataStore (Preferences) keys. Profiles are stored as an encoded list (see
 * [SavedProfilesCodec]); the legacy single-profile keys are migrated on read and
 * cleared on the next write.
 */
private object PreferenceKeys {
    val CONSENT = booleanPreferencesKey("consent_accepted")
    val CONSENT_VERSION = intPreferencesKey("consent_terms_version")
    val CONSENT_ACCEPTED_AT = longPreferencesKey("consent_accepted_at")
    val BACKGROUND_SETUP_REVIEWED = booleanPreferencesKey("background_setup_reviewed")
    val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
    val LISTENING_FEATURES = stringPreferencesKey("listening_features")
    val COMFORT_CEILING = floatPreferencesKey("comfort_ceiling")
    val ASSIST_PRESET = stringPreferencesKey("assist_preset")
    val MICROPHONE_SOURCE = stringPreferencesKey("microphone_source")
    val NOISE_REDUCTION = stringPreferencesKey("noise_reduction")
    val VOICE_COMFORT = stringPreferencesKey("voice_comfort")
    val CAPTURE_MODE = stringPreferencesKey("capture_mode")
    val SPEECH_CLARITY = stringPreferencesKey("speech_clarity")
    val QUIET_SPEECH = stringPreferencesKey("quiet_speech")
    val SPEECH_ENGINE = stringPreferencesKey("speech_engine")
    val MEDIA_EQ_ENABLED = booleanPreferencesKey("media_eq_enabled")
    val MEDIA_BOOST_DB = floatPreferencesKey("media_boost_db")
    val MEDIA_PROCESSING_MODE = stringPreferencesKey("media_processing_mode")
    val PROFILES = stringPreferencesKey("profiles")
    val ACTIVE_PROFILE_ID = stringPreferencesKey("active_profile_id")
    val EXPOSURE_EPOCH_DAY = longPreferencesKey("exposure_epoch_day")
    val EXPOSURE_UNITS = doublePreferencesKey("exposure_units")

    // Legacy single-profile keys (pre-multi-profile builds).
    val PROFILE_NAME = stringPreferencesKey("profile_name")
    val PROFILE_AUDIOGRAM = stringPreferencesKey("profile_audiogram")
    val PROFILE_MASTER_CAP = doublePreferencesKey("profile_master_cap_db")
}

private const val LEGACY_PROFILE_ID = "active"

/** Unrelated preference writes must not retrigger UI or audio configuration work. */
private fun <T> DataStore<Preferences>.observe(get: (Preferences) -> T): Flow<T> = data.map(get).distinctUntilChanged()

/** [PreferenceStorage] backed by Preferences DataStore. */
class PreferencesStore(
    private val dataStore: DataStore<Preferences>,
) : PreferenceStorage {
    override fun observeConsentAccepted(): Flow<Boolean> =
        dataStore.observe {
            it[PreferenceKeys.CONSENT] == true && it[PreferenceKeys.CONSENT_VERSION] == CURRENT_TERMS_VERSION
        }

    override suspend fun setConsentAccepted(accepted: Boolean) {
        dataStore.edit {
            it[PreferenceKeys.CONSENT] = accepted
            if (accepted) {
                it[PreferenceKeys.CONSENT_VERSION] = CURRENT_TERMS_VERSION
                it[PreferenceKeys.CONSENT_ACCEPTED_AT] = System.currentTimeMillis()
            } else {
                it.remove(PreferenceKeys.CONSENT_VERSION)
                it.remove(PreferenceKeys.CONSENT_ACCEPTED_AT)
            }
        }
    }

    override fun observeBackgroundSetupReviewed(): Flow<Boolean> =
        dataStore.observe { it[PreferenceKeys.BACKGROUND_SETUP_REVIEWED] ?: false }

    override suspend fun markBackgroundSetupReviewed() {
        dataStore.edit { it[PreferenceKeys.BACKGROUND_SETUP_REVIEWED] = true }
    }

    override fun observeHighContrast(): Flow<Boolean> = dataStore.observe { it[PreferenceKeys.HIGH_CONTRAST] ?: false }

    override suspend fun setHighContrast(enabled: Boolean) {
        dataStore.edit { it[PreferenceKeys.HIGH_CONTRAST] = enabled }
    }

    override fun observeListeningFeatures(): Flow<ListeningFeatures> =
        dataStore.observe { ListeningFeatures.fromName(it[PreferenceKeys.LISTENING_FEATURES]) }

    override suspend fun setListeningFeatures(features: ListeningFeatures) {
        dataStore.edit {
            it[PreferenceKeys.LISTENING_FEATURES] = features.name
            if (!features.mediaSoundEnabled) it[PreferenceKeys.MEDIA_EQ_ENABLED] = false
        }
    }

    override fun observeComfortCeiling(): Flow<Float> =
        dataStore.observe {
            (it[PreferenceKeys.COMFORT_CEILING] ?: DEFAULT_COMFORT_CEILING).coerceIn(MIN_CEILING, MAX_CEILING)
        }

    override suspend fun setComfortCeiling(value: Float) {
        dataStore.edit {
            it[PreferenceKeys.COMFORT_CEILING] = value.coerceIn(MIN_CEILING, MAX_CEILING)
        }
    }

    override fun observeAssistPreset(): Flow<String> =
        dataStore.observe {
            it[PreferenceKeys.ASSIST_PRESET] ?: DEFAULT_PRESET
        }

    override suspend fun setAssistPreset(name: String) {
        dataStore.edit { it[PreferenceKeys.ASSIST_PRESET] = name }
    }

    override fun observeMicrophoneSource(): Flow<String> =
        dataStore.observe {
            it[PreferenceKeys.MICROPHONE_SOURCE] ?: "PHONE"
        }

    override suspend fun setMicrophoneSource(name: String) {
        dataStore.edit { it[PreferenceKeys.MICROPHONE_SOURCE] = name }
    }

    override fun observeListeningSettings(): Flow<SoundPreferences> =
        dataStore.observe {
            val defaults = SoundPreferences()
            SoundPreferences(
                noiseReduction = it[PreferenceKeys.NOISE_REDUCTION] ?: defaults.noiseReduction,
                voiceComfort = it[PreferenceKeys.VOICE_COMFORT] ?: defaults.voiceComfort,
                captureMode = it[PreferenceKeys.CAPTURE_MODE] ?: defaults.captureMode,
                speechClarity = it[PreferenceKeys.SPEECH_CLARITY] ?: defaults.speechClarity,
                quietSpeech = it[PreferenceKeys.QUIET_SPEECH] ?: defaults.quietSpeech,
                speechEngine = it[PreferenceKeys.SPEECH_ENGINE] ?: defaults.speechEngine,
            )
        }

    override suspend fun setListeningSettings(settings: SoundPreferences) {
        dataStore.edit {
            it[PreferenceKeys.NOISE_REDUCTION] = settings.noiseReduction
            it[PreferenceKeys.VOICE_COMFORT] = settings.voiceComfort
            it[PreferenceKeys.CAPTURE_MODE] = settings.captureMode
            it[PreferenceKeys.SPEECH_CLARITY] = settings.speechClarity
            it[PreferenceKeys.QUIET_SPEECH] = settings.quietSpeech
            it[PreferenceKeys.SPEECH_ENGINE] = settings.speechEngine
        }
    }

    override fun observeMediaEqEnabled(): Flow<Boolean> =
        dataStore.observe {
            it[PreferenceKeys.MEDIA_EQ_ENABLED] ?: false
        }

    override suspend fun setMediaEqEnabled(enabled: Boolean) {
        dataStore.edit {
            val features = ListeningFeatures.fromName(it[PreferenceKeys.LISTENING_FEATURES])
            it[PreferenceKeys.MEDIA_EQ_ENABLED] = enabled && features.mediaSoundEnabled
        }
    }

    override fun observeMediaBoostDb(): Flow<Float> =
        dataStore.observe { preferences ->
            val saved = preferences[PreferenceKeys.MEDIA_BOOST_DB]
            saved?.takeIf { it.isFinite() }?.coerceIn(0f, AudioLimits.MAX_MEDIA_BOOST_DB)
                ?: AudioLimits.DEFAULT_MEDIA_BOOST_DB
        }

    override suspend fun setMediaBoostDb(db: Float) {
        require(db.isFinite())
        dataStore.edit { it[PreferenceKeys.MEDIA_BOOST_DB] = db.coerceIn(0f, AudioLimits.MAX_MEDIA_BOOST_DB) }
    }

    override fun observeMediaProcessingMode(): Flow<String> =
        dataStore.observe { it[PreferenceKeys.MEDIA_PROCESSING_MODE] ?: DEFAULT_MEDIA_PROCESSING_MODE }

    override suspend fun setMediaProcessingMode(name: String) {
        dataStore.edit { it[PreferenceKeys.MEDIA_PROCESSING_MODE] = name }
    }

    override fun observeExposureToday(): Flow<DailyListening> =
        dataStore.observe {
            DailyListening(
                epochDay = it[PreferenceKeys.EXPOSURE_EPOCH_DAY] ?: 0L,
                units = it[PreferenceKeys.EXPOSURE_UNITS] ?: 0.0,
            )
        }

    override suspend fun addExposureUnits(
        units: Double,
        epochDay: Long,
    ) {
        dataStore.edit {
            val sameDay = it[PreferenceKeys.EXPOSURE_EPOCH_DAY] == epochDay
            val carried = if (sameDay) it[PreferenceKeys.EXPOSURE_UNITS] ?: 0.0 else 0.0
            it[PreferenceKeys.EXPOSURE_EPOCH_DAY] = epochDay
            it[PreferenceKeys.EXPOSURE_UNITS] = carried + units
        }
    }

    private companion object {
        // Bump when the in-app terms change so existing users review them again.
        const val CURRENT_TERMS_VERSION = 5

        // Conservative default until the user calibrates; bounded for safety.
        const val DEFAULT_COMFORT_CEILING = 0.5f
        const val MIN_CEILING = 0.1f
        const val MAX_CEILING = 0.9f
        const val DEFAULT_PRESET = "STANDARD"
        const val DEFAULT_MEDIA_PROCESSING_MODE = "BALANCED"
    }
}

/**
 * Encodes a profile list into a single preference string. Uses ASCII unit/record
 * separators, which cannot appear in the audiogram codec output; profile names
 * are sanitized on encode. Internal for tests.
 */
internal object SavedProfilesCodec {
    private const val FIELD = '\u001F'
    private const val RECORD = '\u001E'

    fun encode(profiles: List<SoundProfile>): String =
        profiles.joinToString(RECORD.toString()) { p ->
            listOf(
                sanitize(p.id),
                sanitize(p.name),
                p.masterGainCapDb.toString(),
                HearingCurveCodec.encode(p.audiogram),
            ).joinToString(FIELD.toString())
        }

    fun decode(text: String): List<SoundProfile> =
        text.split(RECORD).mapNotNull { record ->
            val fields = record.split(FIELD)
            if (fields.size < FIELD_COUNT) return@mapNotNull null
            runCatching {
                SoundProfile(
                    id = fields[0],
                    name = fields[1],
                    audiogram = HearingCurveCodec.decode(fields[3]),
                    masterGainCapDb = fields[2].toDouble(),
                )
            }.getOrNull()
        }

    private fun sanitize(value: String): String = value.replace(FIELD, ' ').replace(RECORD, ' ')

    private const val FIELD_COUNT = 4
}

/**
 * [ProfileStorage] backed by Preferences DataStore. Stores a list of named
 * profiles plus the active profile id; each hearing check or manual entry saves
 * a new profile, so the list doubles as result history (newest first).
 */
class ProfilesStore(
    private val dataStore: DataStore<Preferences>,
) : ProfileStorage {
    private fun Preferences.profileList(): List<SoundProfile> {
        val encoded = this[PreferenceKeys.PROFILES]
        if (encoded != null) return SavedProfilesCodec.decode(encoded)
        // Legacy single-profile storage from pre-multi-profile builds.
        val audiogramText = this[PreferenceKeys.PROFILE_AUDIOGRAM] ?: return emptyList()
        return listOf(
            SoundProfile(
                id = LEGACY_PROFILE_ID,
                name = this[PreferenceKeys.PROFILE_NAME] ?: "My profile",
                audiogram = HearingCurveCodec.decode(audiogramText),
                masterGainCapDb = this[PreferenceKeys.PROFILE_MASTER_CAP] ?: DEFAULT_MASTER_CAP_DB,
            ),
        )
    }

    private fun Preferences.activeProfile(): SoundProfile? {
        val profiles = profileList()
        val activeId = this[PreferenceKeys.ACTIVE_PROFILE_ID]
        return profiles.firstOrNull { it.id == activeId } ?: profiles.firstOrNull()
    }

    // Compare encoded values before decoding: exposure and settings writes share
    // this DataStore but do not change the audiograms.
    private fun Preferences.hasSameProfilesAs(other: Preferences): Boolean =
        this[PreferenceKeys.PROFILES] == other[PreferenceKeys.PROFILES] &&
            this[PreferenceKeys.PROFILE_AUDIOGRAM] == other[PreferenceKeys.PROFILE_AUDIOGRAM] &&
            this[PreferenceKeys.PROFILE_NAME] == other[PreferenceKeys.PROFILE_NAME] &&
            this[PreferenceKeys.PROFILE_MASTER_CAP] == other[PreferenceKeys.PROFILE_MASTER_CAP]

    override fun observeProfiles(): Flow<List<SoundProfile>> =
        dataStore.data
            .distinctUntilChanged { previous, current -> previous.hasSameProfilesAs(current) }
            .map { it.profileList() }
            .distinctUntilChanged()

    override fun observeActiveProfile(): Flow<SoundProfile?> =
        dataStore.data
            .distinctUntilChanged { previous, current ->
                previous.hasSameProfilesAs(current) &&
                    previous[PreferenceKeys.ACTIVE_PROFILE_ID] == current[PreferenceKeys.ACTIVE_PROFILE_ID]
            }.map { it.activeProfile() }
            .distinctUntilChanged()

    /** Upserts [profile] at the front (newest first) and makes it active. */
    override suspend fun save(profile: SoundProfile) {
        dataStore.edit { prefs ->
            val others = prefs.profileList().filterNot { it.id == profile.id }
            prefs.writeProfiles(listOf(profile) + others)
            prefs[PreferenceKeys.ACTIVE_PROFILE_ID] = profile.id
        }
    }

    override suspend fun setActive(profileId: String) {
        dataStore.edit { prefs ->
            if (prefs.profileList().any { it.id == profileId }) {
                prefs[PreferenceKeys.ACTIVE_PROFILE_ID] = profileId
            }
        }
    }

    override suspend fun delete(profileId: String) {
        dataStore.edit { prefs ->
            val remaining = prefs.profileList().filterNot { it.id == profileId }
            prefs.writeProfiles(remaining)
            if (prefs[PreferenceKeys.ACTIVE_PROFILE_ID] == profileId) {
                val next = remaining.firstOrNull()?.id
                if (next != null) {
                    prefs[PreferenceKeys.ACTIVE_PROFILE_ID] = next
                } else {
                    prefs.remove(PreferenceKeys.ACTIVE_PROFILE_ID)
                }
            }
        }
    }

    private fun MutablePreferences.writeProfiles(profiles: List<SoundProfile>) {
        this[PreferenceKeys.PROFILES] = SavedProfilesCodec.encode(profiles)
        // Legacy keys are superseded once the list exists.
        remove(PreferenceKeys.PROFILE_NAME)
        remove(PreferenceKeys.PROFILE_AUDIOGRAM)
        remove(PreferenceKeys.PROFILE_MASTER_CAP)
    }

    private companion object {
        const val DEFAULT_MASTER_CAP_DB = AudioLimits.DEFAULT_MASTER_GAIN_CAP_DB
    }
}

/** Builds a new uniquely-identified profile from a hearing-check or manual-entry result. */
fun newProfileFrom(
    audiogram: HearingCurve,
    name: String,
    masterGainCapDb: Double = AudioLimits.DEFAULT_MASTER_GAIN_CAP_DB,
) = SoundProfile(
    id = UUID.randomUUID().toString(),
    name = name,
    audiogram = audiogram,
    masterGainCapDb = masterGainCapDb,
)
