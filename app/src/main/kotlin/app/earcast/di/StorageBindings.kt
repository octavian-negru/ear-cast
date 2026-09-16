package app.earcast.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import app.earcast.data.PreferenceStorage
import app.earcast.data.PreferencesStore
import app.earcast.data.ProfileStorage
import app.earcast.data.ProfilesStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides the persistence layer (DataStore-backed settings + profiles). */
@Module
@InstallIn(SingletonComponent::class)
object StorageBindings {
    @Provides
    @Singleton
    fun dataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> =
        PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("earcast")
        }

    @Provides
    @Singleton
    fun settingsRepository(store: DataStore<Preferences>): PreferenceStorage = PreferencesStore(store)

    @Provides
    @Singleton
    fun profileRepository(dataStore: DataStore<Preferences>): ProfileStorage = ProfilesStore(dataStore)
}
