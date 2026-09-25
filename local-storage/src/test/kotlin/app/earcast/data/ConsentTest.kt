package app.earcast.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConsentTest {
    @Test
    fun `legacy ad choices neither accept new terms nor become privacy consent`() =
        runTest {
            val data = ConsentPreferences()
            for (oldChoice in listOf(false, true)) {
                data.updateData {
                    mutablePreferencesOf(
                        booleanPreferencesKey("test_ads_allowed_v1") to oldChoice,
                        booleanPreferencesKey("consent_accepted") to true,
                        intPreferencesKey("consent_terms_version") to 2,
                    )
                }
                val store = PreferencesStore(data)
                assertFalse(store.observeConsentAccepted().first())
                store.setConsentAccepted(true)
                assertTrue(store.observeConsentAccepted().first())
                assertEquals(oldChoice, data.data.first()[booleanPreferencesKey("test_ads_allowed_v1")])
            }
        }

    @Test
    fun `new installs and legacy acknowledgment require explicit terms acceptance`() =
        runTest {
            val data = ConsentPreferences()
            val store = PreferencesStore(data)
            assertFalse(store.observeConsentAccepted().first())
            data.updateData { mutablePreferencesOf(booleanPreferencesKey("consent_accepted") to true) }
            assertFalse(store.observeConsentAccepted().first())
        }

    @Test
    fun `acceptance stores a version and timestamp and survives recreation`() =
        runTest {
            val data = ConsentPreferences()
            val before = System.currentTimeMillis()
            PreferencesStore(data).setConsentAccepted(true)
            val saved = data.data.first()
            assertEquals(3, saved[intPreferencesKey("consent_terms_version")])
            val acceptedAt = saved[longPreferencesKey("consent_accepted_at")]
            assertTrue(acceptedAt != null && acceptedAt in before..System.currentTimeMillis())
            assertTrue(PreferencesStore(data).observeConsentAccepted().first())
        }

    @Test
    fun `a different terms version requires renewed acceptance`() =
        runTest {
            val data = ConsentPreferences()
            for (version in listOf(0, 1, 2, 4)) {
                data.updateData {
                    mutablePreferencesOf(
                        booleanPreferencesKey("consent_accepted") to true,
                        intPreferencesKey("consent_terms_version") to version,
                    )
                }
                assertFalse(PreferencesStore(data).observeConsentAccepted().first())
            }
        }

    @Test
    fun `clearing consent removes the acceptance record`() =
        runTest {
            val data = ConsentPreferences()
            val store = PreferencesStore(data)
            store.setConsentAccepted(true)
            store.setConsentAccepted(false)
            assertFalse(PreferencesStore(data).observeConsentAccepted().first())
            assertNull(data.data.first()[intPreferencesKey("consent_terms_version")])
            assertNull(data.data.first()[longPreferencesKey("consent_accepted_at")])
        }
}

private class ConsentPreferences : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())
    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        state.value = transform(state.value)
        return state.value
    }
}
