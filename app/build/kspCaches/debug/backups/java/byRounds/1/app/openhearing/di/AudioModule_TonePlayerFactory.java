package app.openhearing.di;

import app.openhearing.core.audio.TonePlayer;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AudioModule_TonePlayerFactory implements Factory<TonePlayer> {
  @Override
  public TonePlayer get() {
    return tonePlayer();
  }

  public static AudioModule_TonePlayerFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static TonePlayer tonePlayer() {
    return Preconditions.checkNotNullFromProvides(AudioModule.INSTANCE.tonePlayer());
  }

  private static final class InstanceHolder {
    static final AudioModule_TonePlayerFactory INSTANCE = new AudioModule_TonePlayerFactory();
  }
}
