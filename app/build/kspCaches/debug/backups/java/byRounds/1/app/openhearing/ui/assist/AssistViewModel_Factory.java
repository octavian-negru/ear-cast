package app.openhearing.ui.assist;

import app.openhearing.assist.AssistController;
import app.openhearing.assist.AssistSessionFactory;
import app.openhearing.data.ProfileRepository;
import app.openhearing.data.SettingsRepository;
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
public final class AssistViewModel_Factory implements Factory<AssistViewModel> {
  private final Provider<AssistController> controllerProvider;

  private final Provider<ProfileRepository> profileRepositoryProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<AssistSessionFactory> sessionFactoryProvider;

  public AssistViewModel_Factory(Provider<AssistController> controllerProvider,
      Provider<ProfileRepository> profileRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<AssistSessionFactory> sessionFactoryProvider) {
    this.controllerProvider = controllerProvider;
    this.profileRepositoryProvider = profileRepositoryProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  @Override
  public AssistViewModel get() {
    return newInstance(controllerProvider.get(), profileRepositoryProvider.get(), settingsRepositoryProvider.get(), sessionFactoryProvider.get());
  }

  public static AssistViewModel_Factory create(Provider<AssistController> controllerProvider,
      Provider<ProfileRepository> profileRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<AssistSessionFactory> sessionFactoryProvider) {
    return new AssistViewModel_Factory(controllerProvider, profileRepositoryProvider, settingsRepositoryProvider, sessionFactoryProvider);
  }

  public static AssistViewModel newInstance(AssistController controller,
      ProfileRepository profileRepository, SettingsRepository settingsRepository,
      AssistSessionFactory sessionFactory) {
    return new AssistViewModel(controller, profileRepository, settingsRepository, sessionFactory);
  }
}
