package app.openhearing.ui.demo;

import app.openhearing.assist.AssistController;
import app.openhearing.assist.AssistSessionFactory;
import app.openhearing.core.audio.AbPlayer;
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
public final class HearDifferenceViewModel_Factory implements Factory<HearDifferenceViewModel> {
  private final Provider<AssistSessionFactory> sessionFactoryProvider;

  private final Provider<ProfileRepository> profileRepositoryProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<AssistController> controllerProvider;

  private final Provider<AbPlayer> playerProvider;

  public HearDifferenceViewModel_Factory(Provider<AssistSessionFactory> sessionFactoryProvider,
      Provider<ProfileRepository> profileRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<AssistController> controllerProvider, Provider<AbPlayer> playerProvider) {
    this.sessionFactoryProvider = sessionFactoryProvider;
    this.profileRepositoryProvider = profileRepositoryProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.controllerProvider = controllerProvider;
    this.playerProvider = playerProvider;
  }

  @Override
  public HearDifferenceViewModel get() {
    return newInstance(sessionFactoryProvider.get(), profileRepositoryProvider.get(), settingsRepositoryProvider.get(), controllerProvider.get(), playerProvider.get());
  }

  public static HearDifferenceViewModel_Factory create(
      Provider<AssistSessionFactory> sessionFactoryProvider,
      Provider<ProfileRepository> profileRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<AssistController> controllerProvider, Provider<AbPlayer> playerProvider) {
    return new HearDifferenceViewModel_Factory(sessionFactoryProvider, profileRepositoryProvider, settingsRepositoryProvider, controllerProvider, playerProvider);
  }

  public static HearDifferenceViewModel newInstance(AssistSessionFactory sessionFactory,
      ProfileRepository profileRepository, SettingsRepository settingsRepository,
      AssistController controller, AbPlayer player) {
    return new HearDifferenceViewModel(sessionFactory, profileRepository, settingsRepository, controller, player);
  }
}
