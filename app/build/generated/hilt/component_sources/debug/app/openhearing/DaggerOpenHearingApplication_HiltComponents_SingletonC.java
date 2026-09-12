package app.openhearing;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import app.openhearing.assist.AssistController;
import app.openhearing.assist.AssistService;
import app.openhearing.assist.AssistService_MembersInjector;
import app.openhearing.assist.AssistSessionFactory;
import app.openhearing.assist.AssistTileService;
import app.openhearing.assist.AssistTileService_MembersInjector;
import app.openhearing.audiogram.FittingStrategy;
import app.openhearing.core.audio.ToneGenerator;
import app.openhearing.data.ProfileRepository;
import app.openhearing.data.SettingsRepository;
import app.openhearing.di.AudioModule_AbPlayerFactory;
import app.openhearing.di.AudioModule_FittingStrategyFactory;
import app.openhearing.di.AudioModule_ToneGeneratorFactory;
import app.openhearing.di.AudioModule_TonePlayerFactory;
import app.openhearing.di.DataModule_DataStoreFactory;
import app.openhearing.di.DataModule_ProfileRepositoryFactory;
import app.openhearing.di.DataModule_SettingsRepositoryFactory;
import app.openhearing.mediaeq.MediaEqController;
import app.openhearing.ui.RootViewModel;
import app.openhearing.ui.RootViewModel_HiltModules;
import app.openhearing.ui.RootViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import app.openhearing.ui.RootViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import app.openhearing.ui.assist.AssistViewModel;
import app.openhearing.ui.assist.AssistViewModel_HiltModules;
import app.openhearing.ui.assist.AssistViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import app.openhearing.ui.assist.AssistViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import app.openhearing.ui.demo.HearDifferenceViewModel;
import app.openhearing.ui.demo.HearDifferenceViewModel_HiltModules;
import app.openhearing.ui.demo.HearDifferenceViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import app.openhearing.ui.demo.HearDifferenceViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import app.openhearing.ui.dintest.DigitCorpus;
import app.openhearing.ui.dintest.DinTestViewModel;
import app.openhearing.ui.dintest.DinTestViewModel_HiltModules;
import app.openhearing.ui.dintest.DinTestViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import app.openhearing.ui.dintest.DinTestViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import app.openhearing.ui.hearingtest.HearingTestViewModel;
import app.openhearing.ui.hearingtest.HearingTestViewModel_HiltModules;
import app.openhearing.ui.hearingtest.HearingTestViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import app.openhearing.ui.hearingtest.HearingTestViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import app.openhearing.ui.manualentry.ManualEntryViewModel;
import app.openhearing.ui.manualentry.ManualEntryViewModel_HiltModules;
import app.openhearing.ui.manualentry.ManualEntryViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import app.openhearing.ui.manualentry.ManualEntryViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.MapBuilder;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

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
public final class DaggerOpenHearingApplication_HiltComponents_SingletonC {
  private DaggerOpenHearingApplication_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public OpenHearingApplication_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements OpenHearingApplication_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public OpenHearingApplication_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements OpenHearingApplication_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public OpenHearingApplication_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements OpenHearingApplication_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public OpenHearingApplication_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements OpenHearingApplication_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public OpenHearingApplication_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements OpenHearingApplication_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public OpenHearingApplication_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements OpenHearingApplication_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public OpenHearingApplication_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements OpenHearingApplication_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public OpenHearingApplication_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends OpenHearingApplication_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends OpenHearingApplication_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    FragmentCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends OpenHearingApplication_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends OpenHearingApplication_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    ActivityCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity arg0) {
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(MapBuilder.<String, Boolean>newMapBuilder(6).put(AssistViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, AssistViewModel_HiltModules.KeyModule.provide()).put(DinTestViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, DinTestViewModel_HiltModules.KeyModule.provide()).put(HearDifferenceViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, HearDifferenceViewModel_HiltModules.KeyModule.provide()).put(HearingTestViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, HearingTestViewModel_HiltModules.KeyModule.provide()).put(ManualEntryViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, ManualEntryViewModel_HiltModules.KeyModule.provide()).put(RootViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, RootViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }
  }

  private static final class ViewModelCImpl extends OpenHearingApplication_HiltComponents.ViewModelC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    Provider<AssistViewModel> assistViewModelProvider;

    Provider<DinTestViewModel> dinTestViewModelProvider;

    Provider<HearDifferenceViewModel> hearDifferenceViewModelProvider;

    Provider<HearingTestViewModel> hearingTestViewModelProvider;

    Provider<ManualEntryViewModel> manualEntryViewModelProvider;

    Provider<RootViewModel> rootViewModelProvider;

    ViewModelCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        SavedStateHandle savedStateHandleParam, ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;

      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.assistViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.dinTestViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.hearDifferenceViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.hearingTestViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.manualEntryViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.rootViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(MapBuilder.<String, javax.inject.Provider<ViewModel>>newMapBuilder(6).put(AssistViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) (assistViewModelProvider))).put(DinTestViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) (dinTestViewModelProvider))).put(HearDifferenceViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) (hearDifferenceViewModelProvider))).put(HearingTestViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) (hearingTestViewModelProvider))).put(ManualEntryViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) (manualEntryViewModelProvider))).put(RootViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) (rootViewModelProvider))).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return Collections.<Class<?>, Object>emptyMap();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // app.openhearing.ui.assist.AssistViewModel
          return (T) new AssistViewModel(singletonCImpl.assistControllerProvider.get(), singletonCImpl.profileRepositoryProvider.get(), singletonCImpl.settingsRepositoryProvider.get(), singletonCImpl.assistSessionFactoryProvider.get());

          case 1: // app.openhearing.ui.dintest.DinTestViewModel
          return (T) new DinTestViewModel(singletonCImpl.digitCorpusProvider.get(), AudioModule_TonePlayerFactory.tonePlayer());

          case 2: // app.openhearing.ui.demo.HearDifferenceViewModel
          return (T) new HearDifferenceViewModel(singletonCImpl.assistSessionFactoryProvider.get(), singletonCImpl.profileRepositoryProvider.get(), singletonCImpl.settingsRepositoryProvider.get(), singletonCImpl.assistControllerProvider.get(), AudioModule_AbPlayerFactory.abPlayer());

          case 3: // app.openhearing.ui.hearingtest.HearingTestViewModel
          return (T) new HearingTestViewModel(singletonCImpl.toneGeneratorProvider.get(), AudioModule_TonePlayerFactory.tonePlayer(), singletonCImpl.fittingStrategyProvider.get(), singletonCImpl.profileRepositoryProvider.get());

          case 4: // app.openhearing.ui.manualentry.ManualEntryViewModel
          return (T) new ManualEntryViewModel(singletonCImpl.profileRepositoryProvider.get());

          case 5: // app.openhearing.ui.RootViewModel
          return (T) new RootViewModel(singletonCImpl.settingsRepositoryProvider.get(), singletonCImpl.profileRepositoryProvider.get(), singletonCImpl.assistSessionFactoryProvider.get(), singletonCImpl.mediaEqControllerProvider.get(), singletonCImpl.toneGeneratorProvider.get(), AudioModule_TonePlayerFactory.tonePlayer(), singletonCImpl.digitCorpusProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends OpenHearingApplication_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends OpenHearingApplication_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }

    @Override
    public void injectAssistService(AssistService arg0) {
      injectAssistService2(arg0);
    }

    @Override
    public void injectAssistTileService(AssistTileService arg0) {
      injectAssistTileService2(arg0);
    }

    private AssistService injectAssistService2(AssistService instance) {
      AssistService_MembersInjector.injectController(instance, singletonCImpl.assistControllerProvider.get());
      AssistService_MembersInjector.injectSettingsRepository(instance, singletonCImpl.settingsRepositoryProvider.get());
      return instance;
    }

    private AssistTileService injectAssistTileService2(AssistTileService instance2) {
      AssistTileService_MembersInjector.injectController(instance2, singletonCImpl.assistControllerProvider.get());
      AssistTileService_MembersInjector.injectSessionFactory(instance2, singletonCImpl.assistSessionFactoryProvider.get());
      return instance2;
    }
  }

  private static final class SingletonCImpl extends OpenHearingApplication_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    Provider<AssistController> assistControllerProvider;

    Provider<DataStore<Preferences>> dataStoreProvider;

    Provider<ProfileRepository> profileRepositoryProvider;

    Provider<SettingsRepository> settingsRepositoryProvider;

    Provider<FittingStrategy> fittingStrategyProvider;

    Provider<AssistSessionFactory> assistSessionFactoryProvider;

    Provider<DigitCorpus> digitCorpusProvider;

    Provider<ToneGenerator> toneGeneratorProvider;

    Provider<MediaEqController> mediaEqControllerProvider;

    SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.assistControllerProvider = DoubleCheck.provider(new SwitchingProvider<AssistController>(singletonCImpl, 0));
      this.dataStoreProvider = DoubleCheck.provider(new SwitchingProvider<DataStore<Preferences>>(singletonCImpl, 2));
      this.profileRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<ProfileRepository>(singletonCImpl, 1));
      this.settingsRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<SettingsRepository>(singletonCImpl, 3));
      this.fittingStrategyProvider = DoubleCheck.provider(new SwitchingProvider<FittingStrategy>(singletonCImpl, 5));
      this.assistSessionFactoryProvider = DoubleCheck.provider(new SwitchingProvider<AssistSessionFactory>(singletonCImpl, 4));
      this.digitCorpusProvider = DoubleCheck.provider(new SwitchingProvider<DigitCorpus>(singletonCImpl, 6));
      this.toneGeneratorProvider = DoubleCheck.provider(new SwitchingProvider<ToneGenerator>(singletonCImpl, 7));
      this.mediaEqControllerProvider = DoubleCheck.provider(new SwitchingProvider<MediaEqController>(singletonCImpl, 8));
    }

    @Override
    public void injectOpenHearingApplication(OpenHearingApplication arg0) {
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return Collections.<Boolean>emptySet();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // app.openhearing.assist.AssistController
          return (T) new AssistController(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 1: // app.openhearing.data.ProfileRepository
          return (T) DataModule_ProfileRepositoryFactory.profileRepository(singletonCImpl.dataStoreProvider.get());

          case 2: // androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
          return (T) DataModule_DataStoreFactory.dataStore(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 3: // app.openhearing.data.SettingsRepository
          return (T) DataModule_SettingsRepositoryFactory.settingsRepository(singletonCImpl.dataStoreProvider.get());

          case 4: // app.openhearing.assist.AssistSessionFactory
          return (T) new AssistSessionFactory(singletonCImpl.assistControllerProvider.get(), singletonCImpl.profileRepositoryProvider.get(), singletonCImpl.settingsRepositoryProvider.get(), singletonCImpl.fittingStrategyProvider.get());

          case 5: // app.openhearing.audiogram.FittingStrategy
          return (T) AudioModule_FittingStrategyFactory.fittingStrategy();

          case 6: // app.openhearing.ui.dintest.DigitCorpus
          return (T) new DigitCorpus(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 7: // app.openhearing.core.audio.ToneGenerator
          return (T) AudioModule_ToneGeneratorFactory.toneGenerator();

          case 8: // app.openhearing.mediaeq.MediaEqController
          return (T) new MediaEqController();

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
