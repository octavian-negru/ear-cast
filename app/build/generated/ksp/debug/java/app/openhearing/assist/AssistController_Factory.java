package app.openhearing.assist;

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
public final class AssistController_Factory implements Factory<AssistController> {
  private final Provider<Context> contextProvider;

  public AssistController_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public AssistController get() {
    return newInstance(contextProvider.get());
  }

  public static AssistController_Factory create(Provider<Context> contextProvider) {
    return new AssistController_Factory(contextProvider);
  }

  public static AssistController newInstance(Context context) {
    return new AssistController(context);
  }
}
