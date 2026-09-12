package app.openhearing.di;

import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import app.openhearing.data.ProfileRepository;
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
public final class DataModule_ProfileRepositoryFactory implements Factory<ProfileRepository> {
  private final Provider<DataStore<Preferences>> dataStoreProvider;

  public DataModule_ProfileRepositoryFactory(Provider<DataStore<Preferences>> dataStoreProvider) {
    this.dataStoreProvider = dataStoreProvider;
  }

  @Override
  public ProfileRepository get() {
    return profileRepository(dataStoreProvider.get());
  }

  public static DataModule_ProfileRepositoryFactory create(
      Provider<DataStore<Preferences>> dataStoreProvider) {
    return new DataModule_ProfileRepositoryFactory(dataStoreProvider);
  }

  public static ProfileRepository profileRepository(DataStore<Preferences> dataStore) {
    return Preconditions.checkNotNullFromProvides(DataModule.INSTANCE.profileRepository(dataStore));
  }
}
