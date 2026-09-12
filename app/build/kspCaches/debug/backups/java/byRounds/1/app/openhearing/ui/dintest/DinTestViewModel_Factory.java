package app.openhearing.ui.dintest;

import app.openhearing.core.audio.TonePlayer;
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
public final class DinTestViewModel_Factory implements Factory<DinTestViewModel> {
  private final Provider<DigitCorpus> corpusProvider;

  private final Provider<TonePlayer> tonePlayerProvider;

  public DinTestViewModel_Factory(Provider<DigitCorpus> corpusProvider,
      Provider<TonePlayer> tonePlayerProvider) {
    this.corpusProvider = corpusProvider;
    this.tonePlayerProvider = tonePlayerProvider;
  }

  @Override
  public DinTestViewModel get() {
    return newInstance(corpusProvider.get(), tonePlayerProvider.get());
  }

  public static DinTestViewModel_Factory create(Provider<DigitCorpus> corpusProvider,
      Provider<TonePlayer> tonePlayerProvider) {
    return new DinTestViewModel_Factory(corpusProvider, tonePlayerProvider);
  }

  public static DinTestViewModel newInstance(DigitCorpus corpus, TonePlayer tonePlayer) {
    return new DinTestViewModel(corpus, tonePlayer);
  }
}
