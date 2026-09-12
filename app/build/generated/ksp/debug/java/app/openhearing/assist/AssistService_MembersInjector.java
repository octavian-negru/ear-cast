package app.openhearing.assist;

import app.openhearing.data.SettingsRepository;
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
public final class AssistService_MembersInjector implements MembersInjector<AssistService> {
  private final Provider<AssistController> controllerProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  public AssistService_MembersInjector(Provider<AssistController> controllerProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    this.controllerProvider = controllerProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  public static MembersInjector<AssistService> create(Provider<AssistController> controllerProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    return new AssistService_MembersInjector(controllerProvider, settingsRepositoryProvider);
  }

  @Override
  public void injectMembers(AssistService instance) {
    injectController(instance, controllerProvider.get());
    injectSettingsRepository(instance, settingsRepositoryProvider.get());
  }

  @InjectedFieldSignature("app.openhearing.assist.AssistService.controller")
  public static void injectController(AssistService instance, AssistController controller) {
    instance.controller = controller;
  }

  @InjectedFieldSignature("app.openhearing.assist.AssistService.settingsRepository")
  public static void injectSettingsRepository(AssistService instance,
      SettingsRepository settingsRepository) {
    instance.settingsRepository = settingsRepository;
  }
}
