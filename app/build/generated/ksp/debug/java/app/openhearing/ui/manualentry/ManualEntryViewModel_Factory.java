package app.openhearing.ui.manualentry;

import app.openhearing.data.ProfileRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
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
public final class ManualEntryViewModel_Factory implements Factory<ManualEntryViewModel> {
  private final Provider<ProfileRepository> profileRepositoryProvider;

  public ManualEntryViewModel_Factory(Provider<ProfileRepository> profileRepositoryProvider) {
    this.profileRepositoryProvider = profileRepositoryProvider;
  }

  @Override
  public ManualEntryViewModel get() {
    return newInstance(profileRepositoryProvider.get());
  }

  public static ManualEntryViewModel_Factory create(
      Provider<ProfileRepository> profileRepositoryProvider) {
    return new ManualEntryViewModel_Factory(profileRepositoryProvider);
  }

  public static ManualEntryViewModel newInstance(ProfileRepository profileRepository) {
    return new ManualEntryViewModel(profileRepository);
  }
}
