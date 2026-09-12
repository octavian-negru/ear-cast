package app.openhearing.di;

import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import app.openhearing.data.SettingsRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class DataModule_SettingsRepositoryFactory implements Factory<SettingsRepository> {
  private final Provider<DataStore<Preferences>> dataStoreProvider;

  public DataModule_SettingsRepositoryFactory(Provider<DataStore<Preferences>> dataStoreProvider) {
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public SettingsRepository get() {
    return settingsRepository(dataStoreProvider.get());
  }

  public static DataModule_SettingsRepositoryFactory create(
      Provider<DataStore<Preferences>> dataStoreProvider) {
    return new DataModule_SettingsRepositoryFactory(dataStoreProvider);
  }

  public static SettingsRepository settingsRepository(DataStore<Preferences> dataStore) {
    return Preconditions.checkNotNullFromProvides(DataModule.INSTANCE.settingsRepository(dataStore));
  }
}
