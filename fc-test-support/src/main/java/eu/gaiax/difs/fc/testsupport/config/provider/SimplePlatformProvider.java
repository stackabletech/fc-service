package eu.gaiax.difs.fc.testsupport.config.provider;

import java.io.File;
import org.keycloak.platform.PlatformProvider;
import org.keycloak.services.ServicesLogger;

public class SimplePlatformProvider implements PlatformProvider {
  Runnable shutdownHook;

  @Override
  public void onStartup(Runnable startupHook) {
    startupHook.run();
  }

  @Override
  public void onShutdown(Runnable shutdownHook) {
    this.shutdownHook = shutdownHook;
  }

  @Override
  public void exit(Throwable cause) {
    ServicesLogger.LOGGER.fatal(cause);
    exit();
  }

  private void exit() {
    new Thread(() -> System.exit(1)).start();
  }

  @Override
  public File getTmpDirectory() {
    return new File(System.getProperty("java.io.tmpdir"));
  }
}