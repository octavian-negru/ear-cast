package app.openhearing.ui.dintest;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class DigitCorpus_Factory implements Factory<DigitCorpus> {
  private final Provider<Context> contextProvider;

  public DigitCorpus_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public DigitCorpus get() {
    return newInstance(contextProvider.get());
  }

  public static DigitCorpus_Factory create(Provider<Context> contextProvider) {
    return new DigitCorpus_Factory(contextProvider);
  }

  public static DigitCorpus newInstance(Context context) {
    return new DigitCorpus(context);
  }
}
