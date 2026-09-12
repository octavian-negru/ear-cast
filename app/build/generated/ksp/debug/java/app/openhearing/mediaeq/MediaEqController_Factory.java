package app.openhearing.mediaeq;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class MediaEqController_Factory implements Factory<MediaEqController> {
  @Override
  public MediaEqController get() {
    return newInstance();
  }

  public static MediaEqController_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static MediaEqController newInstance() {
    return new MediaEqController();
  }

  private static final class InstanceHolder {
    static final MediaEqController_Factory INSTANCE = new MediaEqController_Factory();
  }
}
