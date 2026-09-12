package app.openhearing.ui;

import app.openhearing.assist.AssistSessionFactory;
import app.openhearing.core.audio.ToneGenerator;
import app.openhearing.core.audio.TonePlayer;
import app.openhearing.data.ProfileRepository;
import app.openhearing.data.SettingsRepository;
import app.openhearing.mediaeq.MediaEqController;
import app.openhearing.ui.dintest.DigitCorpus;
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
public final class RootViewModel_Factory implements Factory<RootViewModel> {
  private final Provider<SettingsRepository> settingsProvider;

  private final Provider<ProfileRepository> profileRepositoryProvider;

  private final Provider<AssistSessionFactory> sessionFactoryProvider;

  private final Provider<MediaEqController> mediaEqProvider;

  private final Provider<ToneGenerator> toneGeneratorProvider;

  private final Provider<TonePlayer> tonePlayerProvider;

  private final Provider<DigitCorpus> digitCorpusProvider;

  public RootViewModel_Factory(Provider<SettingsRepository> settingsProvider,
      Provider<ProfileRepository> profileRepositoryProvider,
      Provider<AssistSessionFactory> sessionFactoryProvider,
      Provider<MediaEqController> mediaEqProvider, Provider<ToneGenerator> toneGeneratorProvider,
      Provider<TonePlayer> tonePlayerProvider, Provider<DigitCorpus> digitCorpusProvider) {
    this.settingsProvider = settingsProvider;
    this.profileRepositoryProvider = profileRepositoryProvider;
    this.sessionFactoryProvider = sessionFactoryProvider;
    this.mediaEqProvider = mediaEqProvider;
    this.toneGeneratorProvider = toneGeneratorProvider;
    this.tonePlayerProvider = tonePlayerProvider;
    this.digitCorpusProvider = digitCorpusProvider;
  }

  @Override
  public RootViewModel get() {
    return newInstance(settingsProvider.get(), profileRepositoryProvider.get(), sessionFactoryProvider.get(), mediaEqProvider.get(), toneGeneratorProvider.get(), tonePlayerProvider.get(), digitCorpusProvider.get());
  }

  public static RootViewModel_Factory create(Provider<SettingsRepository> settingsProvider,
      Provider<ProfileRepository> profileRepositoryProvider,
      Provider<AssistSessionFactory> sessionFactoryProvider,
      Provider<MediaEqController> mediaEqProvider, Provider<ToneGenerator> toneGeneratorProvider,
      Provider<TonePlayer> tonePlayerProvider, Provider<DigitCorpus> digitCorpusProvider) {
    return new RootViewModel_Factory(settingsProvider, profileRepositoryProvider, sessionFactoryProvider, mediaEqProvider, toneGeneratorProvider, tonePlayerProvider, digitCorpusProvider);
  }

  public static RootViewModel newInstance(SettingsRepository settings,
      ProfileRepository profileRepository, AssistSessionFactory sessionFactory,
      MediaEqController mediaEq, ToneGenerator toneGenerator, TonePlayer tonePlayer,
      DigitCorpus digitCorpus) {
    return new RootViewModel(settings, profileRepository, sessionFactory, mediaEq, toneGenerator, tonePlayer, digitCorpus);
  }
}
