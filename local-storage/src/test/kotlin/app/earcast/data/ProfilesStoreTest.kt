package app.earcast.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import app.earcast.audiogram.HearingCurve
import app.earcast.audiogram.HearingPoint
import app.earcast.common.AudioEar
import app.earcast.common.FrequencyHz
import app.earcast.common.HearingDb
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private class MemoryDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())
    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        state.value = transform(state.value)
        return state.value
    }
}

class ProfilesStoreTest {
    private val audiogram =
        HearingCurve(
            listOf(
                HearingPoint(AudioEar.RIGHT, FrequencyHz(1000.0), HearingDb(20.0)),
                HearingPoint(AudioEar.LEFT, FrequencyHz(2000.0), HearingDb(35.0)),
            ),
        )

    private fun profile(
        id: String,
        name: String,
    ) = SoundProfile(id = id, name = name, audiogram = audiogram, masterGainCapDb = 12.0)

    @Test
    fun `codec round-trips a profile list`() {
        val profiles = listOf(profile("a", "First"), profile("b", "Second"))
        val decoded = SavedProfilesCodec.decode(SavedProfilesCodec.encode(profiles))
        assertEquals(profiles, decoded)
    }

    @Test
    fun `codec sanitizes separator characters in names`() {
        val hostile = profile("a", "Name\u001Fwith\u001Eseparators")
        val decoded = SavedProfilesCodec.decode(SavedProfilesCodec.encode(listOf(hostile)))
        assertEquals(1, decoded.size)
        assertEquals("Name with separators", decoded.single().name)
    }

    @Test
    fun `save makes the profile active and newest first`() =
        runTest {
            val repo = ProfilesStore(MemoryDataStore())
            repo.save(profile("a", "First"))
            repo.save(profile("b", "Second"))
            assertEquals(listOf("b", "a"), repo.observeProfiles().first().map { it.id })
            assertEquals("b", repo.observeActiveProfile().first()?.id)
        }

    @Test
    fun `setActive switches and delete falls back to remaining profile`() =
        runTest {
            val repo = ProfilesStore(MemoryDataStore())
            repo.save(profile("a", "First"))
            repo.save(profile("b", "Second"))
            repo.setActive("a")
            assertEquals("a", repo.observeActiveProfile().first()?.id)
            repo.delete("a")
            assertEquals("b", repo.observeActiveProfile().first()?.id)
            repo.delete("b")
            assertNull(repo.observeActiveProfile().first())
            assertTrue(repo.observeProfiles().first().isEmpty())
        }

    @Test
    fun `legacy single-profile keys are readable as a one-item list`() =
        runTest {
            val store = MemoryDataStore()
            store.updateData {
                mutablePreferencesOf().apply {
                    this[stringPreferencesKey("profile_name")] = "Old profile"
                    this[stringPreferencesKey("profile_audiogram")] =
                        app.earcast.audiogram.HearingCurveCodec
                            .encode(audiogram)
                }
            }
            val repo = ProfilesStore(store)
            val active = repo.observeActiveProfile().first()
            assertEquals("Old profile", active?.name)
            assertEquals(audiogram, active?.audiogram)
        }
}
