package app.openhearing.di;

import app.openhearing.audiogram.FittingStrategy;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AudioModule_FittingStrategyFactory implements Factory<FittingStrategy> {
  @Override
  public FittingStrategy get() {
    return fittingStrategy();
  }

  public static AudioModule_FittingStrategyFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static FittingStrategy fittingStrategy() {
    return Preconditions.checkNotNullFromProvides(AudioModule.INSTANCE.fittingStrategy());
  }

  private static final class InstanceHolder {
    static final AudioModule_FittingStrategyFactory INSTANCE = new AudioModule_FittingStrategyFactory();
  }
}
