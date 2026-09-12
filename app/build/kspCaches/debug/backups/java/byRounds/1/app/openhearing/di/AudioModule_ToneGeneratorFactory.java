package app.openhearing.di;

import app.openhearing.core.audio.ToneGenerator;
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
public final class AudioModule_ToneGeneratorFactory implements Factory<ToneGenerator> {
  @Override
  public ToneGenerator get() {
    return toneGenerator();
  }

  public static AudioModule_ToneGeneratorFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ToneGenerator toneGenerator() {
    return Preconditions.checkNotNullFromProvides(AudioModule.INSTANCE.toneGenerator());
  }

  private static final class InstanceHolder {
    static final AudioModule_ToneGeneratorFactory INSTANCE = new AudioModule_ToneGeneratorFactory();
  }
}
