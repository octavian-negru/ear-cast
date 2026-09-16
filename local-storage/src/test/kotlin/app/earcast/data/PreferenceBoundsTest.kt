package app.earcast.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private class MemoryPreferences : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())
    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        state.value = transform(state.value)
        return state.value
    }
}

class PreferenceBoundsTest {
    @Test
    fun `media boost defaults to six decibels and persists independently of assist settings`() =
        runTest {
            val store = MemoryPreferences()
            val repo = PreferencesStore(store)
            val listening = SoundPreferences(noiseReduction = "STRONG")
            repo.setListeningSettings(listening)
            repo.setMediaEqEnabled(true)
            assertEquals(6f, repo.observeMediaBoostDb().first())
            repo.setMediaBoostDb(15f)
            val restored = PreferencesStore(store)
            assertEquals(15f, restored.observeMediaBoostDb().first())
            assertEquals(listening, restored.observeListeningSettings().first())
            assertEquals(true, restored.observeMediaEqEnabled().first())
            repo.setMediaEqEnabled(false)
            assertEquals(15f, repo.observeMediaBoostDb().first())
        }

    @Test
    fun `media boost bounds stored values and recovers from invalid saved gain`() =
        runTest {
            val store = MemoryPreferences()
            val repo = PreferencesStore(store)
            repo.setMediaBoostDb(100f)
            assertEquals(15f, repo.observeMediaBoostDb().first())
            repo.setMediaBoostDb(-10f)
            assertEquals(0f, repo.observeMediaBoostDb().first())
            store.updateData { mutablePreferencesOf(floatPreferencesKey("media_boost_db") to Float.NaN) }
            assertEquals(6f, repo.observeMediaBoostDb().first())
        }

    @Test
    fun `clarity settings default conservatively and survive repository recreation together`() =
        runTest {
            val store = MemoryPreferences()
            val repo = PreferencesStore(store)
            assertEquals(SoundPreferences(), repo.observeListeningSettings().first())
            val settings = SoundPreferences("GENTLE", "OFF", "CALL_COMPATIBLE", "STRONG")
            repo.setListeningSettings(settings)
            assertEquals(settings, PreferencesStore(store).observeListeningSettings().first())
        }

    @Test
    fun `additional speech engines survive repository recreation`() =
        runTest {
            val store = MemoryPreferences()
            val repo = PreferencesStore(store)
            for (engine in listOf("SPEEX", "WIENER")) {
                val settings = SoundPreferences(speechEngine = engine, noiseReduction = "GENTLE")
                repo.setListeningSettings(settings)
                assertEquals(settings, PreferencesStore(store).observeListeningSettings().first())
            }
        }

    @Test
    fun `microphone choice defaults to phone and survives repository recreation`() =
        runTest {
            val store = MemoryPreferences()
            val repo = PreferencesStore(store)
            assertEquals("PHONE", repo.observeMicrophoneSource().first())
            repo.setMicrophoneSource("HEADSET")
            assertEquals("HEADSET", PreferencesStore(store).observeMicrophoneSource().first())
            repo.setMicrophoneSource("PHONE")
            assertEquals("PHONE", repo.observeMicrophoneSource().first())
        }

    @Test
    fun `defaults to zero units on day zero`() =
        runTest {
            val repo = PreferencesStore(MemoryPreferences())
            val exposure = repo.observeExposureToday().first()
            assertEquals(0L, exposure.epochDay)
            assertEquals(0.0, exposure.units)
        }

    @Test
    fun `same-day writes accumulate`() =
        runTest {
            val repo = PreferencesStore(MemoryPreferences())
            repo.addExposureUnits(1.5, epochDay = 20_000)
            repo.addExposureUnits(2.5, epochDay = 20_000)
            val exposure = repo.observeExposureToday().first()
            assertEquals(20_000L, exposure.epochDay)
            assertEquals(4.0, exposure.units, 1e-9)
        }

    @Test
    fun `a new day replaces instead of summing`() =
        runTest {
            val repo = PreferencesStore(MemoryPreferences())
            repo.addExposureUnits(9.0, epochDay = 20_000)
            repo.addExposureUnits(1.0, epochDay = 20_001)
            val exposure = repo.observeExposureToday().first()
            assertEquals(20_001L, exposure.epochDay)
            assertEquals(1.0, exposure.units, 1e-9)
        }
}
