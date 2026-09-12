package app.openhearing.assist;

import app.openhearing.audiogram.FittingStrategy;
import app.openhearing.data.ProfileRepository;
import app.openhearing.data.SettingsRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class AssistSessionFactory_Factory implements Factory<AssistSessionFactory> {
  private final Provider<AssistController> controllerProvider;

  private final Provider<ProfileRepository> profileRepositoryProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<FittingStrategy> fittingStrategyProvider;

  public AssistSessionFactory_Factory(Provider<AssistController> controllerProvider,
      Provider<ProfileRepository> profileRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<FittingStrategy> fittingStrategyProvider) {
    this.controllerProvider = controllerProvider;
    this.profileRepositoryProvider = profileRepositoryProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.fittingStrategyProvider = fittingStrategyProvider;
  }

  @Override
  public AssistSessionFactory get() {
    return newInstance(controllerProvider.get(), profileRepositoryProvider.get(), settingsRepositoryProvider.get(), fittingStrategyProvider.get());
  }

  public static AssistSessionFactory_Factory create(Provider<AssistController> controllerProvider,
      Provider<ProfileRepository> profileRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<FittingStrategy> fittingStrategyProvider) {
    return new AssistSessionFactory_Factory(controllerProvider, profileRepositoryProvider, settingsRepositoryProvider, fittingStrategyProvider);
  }

  public static AssistSessionFactory newInstance(AssistController controller,
      ProfileRepository profileRepository, SettingsRepository settingsRepository,
      FittingStrategy fittingStrategy) {
    return new AssistSessionFactory(controller, profileRepository, settingsRepository, fittingStrategy);
  }
}
