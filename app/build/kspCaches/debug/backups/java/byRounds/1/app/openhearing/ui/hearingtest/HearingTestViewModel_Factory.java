package app.openhearing.ui.hearingtest;

import app.openhearing.audiogram.FittingStrategy;
import app.openhearing.core.audio.ToneGenerator;
import app.openhearing.core.audio.TonePlayer;
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
public final class HearingTestViewModel_Factory implements Factory<HearingTestViewModel> {
  private final Provider<ToneGenerator> toneGeneratorProvider;

  private final Provider<TonePlayer> tonePlayerProvider;

  private final Provider<FittingStrategy> fittingStrategyProvider;

  private final Provider<ProfileRepository> profileRepositoryProvider;

  public HearingTestViewModel_Factory(Provider<ToneGenerator> toneGeneratorProvider,
      Provider<TonePlayer> tonePlayerProvider, Provider<FittingStrategy> fittingStrategyProvider,
      Provider<ProfileRepository> profileRepositoryProvider) {
    this.toneGeneratorProvider = toneGeneratorProvider;
    this.tonePlayerProvider = tonePlayerProvider;
    this.fittingStrategyProvider = fittingStrategyProvider;
    this.profileRepositoryProvider = profileRepositoryProvider;
  }

  @Override
  public HearingTestViewModel get() {
    return newInstance(toneGeneratorProvider.get(), tonePlayerProvider.get(), fittingStrategyProvider.get(), profileRepositoryProvider.get());
  }

  public static HearingTestViewModel_Factory create(Provider<ToneGenerator> toneGeneratorProvider,
      Provider<TonePlayer> tonePlayerProvider, Provider<FittingStrategy> fittingStrategyProvider,
      Provider<ProfileRepository> profileRepositoryProvider) {
    return new HearingTestViewModel_Factory(toneGeneratorProvider, tonePlayerProvider, fittingStrategyProvider, profileRepositoryProvider);
  }

  public static HearingTestViewModel newInstance(ToneGenerator toneGenerator, TonePlayer tonePlayer,
      FittingStrategy fittingStrategy, ProfileRepository profileRepository) {
    return new HearingTestViewModel(toneGenerator, tonePlayer, fittingStrategy, profileRepository);
  }
}
