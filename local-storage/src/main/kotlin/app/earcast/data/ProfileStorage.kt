package app.earcast.data

import app.earcast.audiogram.HearingCurve
import kotlinx.coroutines.flow.Flow

/**
 * A saved hearing-assist profile: a named audiogram plus the user's safety/volume
 * preferences. Phase 0 ships the model + repository interface; Phase 4 backs it
 * with DataStore/Room persistence.
 */
data class SoundProfile(
    val id: String,
    val name: String,
    val audiogram: HearingCurve,
    /** User master gain cap in dB; always bounded by AudioLimits in :foundation. */
    val masterGainCapDb: Double,
)

/** Persistence boundary for hearing profiles. Implementation lands in Phase 4. */
interface ProfileStorage {
    /** All saved profiles, newest first. */
    fun observeProfiles(): Flow<List<SoundProfile>>

    /** The currently active profile, or null if none is selected. */
    fun observeActiveProfile(): Flow<SoundProfile?>

    suspend fun save(profile: SoundProfile)

    suspend fun setActive(profileId: String)

    suspend fun delete(profileId: String)
}
