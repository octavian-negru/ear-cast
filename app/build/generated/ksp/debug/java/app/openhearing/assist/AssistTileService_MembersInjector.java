package app.openhearing.assist;

import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;

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
public final class AssistTileService_MembersInjector implements MembersInjector<AssistTileService> {
  private final Provider<AssistController> controllerProvider;

  private final Provider<AssistSessionFactory> sessionFactoryProvider;

  public AssistTileService_MembersInjector(Provider<AssistController> controllerProvider,
      Provider<AssistSessionFactory> sessionFactoryProvider) {
    this.controllerProvider = controllerProvider;
    this.sessionFactoryProvider = sessionFactoryProvider;
  }

  public static MembersInjector<AssistTileService> create(
      Provider<AssistController> controllerProvider,
      Provider<AssistSessionFactory> sessionFactoryProvider) {
    return new AssistTileService_MembersInjector(controllerProvider, sessionFactoryProvider);
  }

  @Override
  public void injectMembers(AssistTileService instance) {
    injectController(instance, controllerProvider.get());
    injectSessionFactory(instance, sessionFactoryProvider.get());
  }

  @InjectedFieldSignature("app.openhearing.assist.AssistTileService.controller")
  public static void injectController(AssistTileService instance, AssistController controller) {
    instance.controller = controller;
  }

  @InjectedFieldSignature("app.openhearing.assist.AssistTileService.sessionFactory")
  public static void injectSessionFactory(AssistTileService instance,
      AssistSessionFactory sessionFactory) {
    instance.sessionFactory = sessionFactory;
  }
}
