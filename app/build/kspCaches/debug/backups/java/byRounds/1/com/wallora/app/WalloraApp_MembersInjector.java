package com.wallora.app;

import androidx.hilt.work.HiltWorkerFactory;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
    "cast"
})
public final class WalloraApp_MembersInjector implements MembersInjector<WalloraApp> {
  private final Provider<HiltWorkerFactory> workerFactoryProvider;

  public WalloraApp_MembersInjector(Provider<HiltWorkerFactory> workerFactoryProvider) {
    this.workerFactoryProvider = workerFactoryProvider;
  }

  public static MembersInjector<WalloraApp> create(
      Provider<HiltWorkerFactory> workerFactoryProvider) {
    return new WalloraApp_MembersInjector(workerFactoryProvider);
  }

  @Override
  public void injectMembers(WalloraApp instance) {
    injectWorkerFactory(instance, workerFactoryProvider.get());
  }

  @InjectedFieldSignature("com.wallora.app.WalloraApp.workerFactory")
  public static void injectWorkerFactory(WalloraApp instance, HiltWorkerFactory workerFactory) {
    instance.workerFactory = workerFactory;
  }
}
