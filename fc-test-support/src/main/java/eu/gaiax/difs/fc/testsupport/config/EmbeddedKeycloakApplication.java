package eu.gaiax.difs.fc.testsupport.config;

import static org.keycloak.util.JsonSerialization.readValue;

import eu.gaiax.difs.fc.testsupport.config.properties.KeycloakServerProperties;
import java.io.File;
import java.nio.file.Paths;
import java.util.NoSuchElementException;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.Config;
import org.keycloak.exportimport.ExportImportManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.services.util.JsonConfigProviderFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

@Slf4j
public class EmbeddedKeycloakApplication extends KeycloakApplication {
  static KeycloakServerProperties properties;

  @Bean(name = "keycloakSessionFactory")
  public KeycloakSessionFactory keycloakSessionFactory() {
    return EmbeddedKeycloakApplication.getSessionFactory();
  }

  protected void loadConfig() {
    JsonConfigProviderFactory factory = new RegularJsonConfigProviderFactory();
    Config.init(factory.create().orElseThrow(() -> new NoSuchElementException("No value present")));
  }

  @Override
  protected ExportImportManager bootstrap() {
    final ExportImportManager exportImportManager = super.bootstrap();
    createMasterRealm();
    createGaiaXRealm();
    return exportImportManager;
  }

  private void createMasterRealm() {
    KeycloakSession session = KeycloakApplication.getSessionFactory().create();
    ApplianceBootstrap applianceBootstrap = new ApplianceBootstrap(session);
    try {
      session.getTransactionManager().begin();
      KeycloakServerProperties.ServerConfig.AdminUser admin = properties.getServer().getAdmin();
      applianceBootstrap.createMasterRealmUser(admin.getUsername(), admin.getPassword());
      session.getTransactionManager().commit();
    } catch (Exception ex) {
      log.error("Couldn't create keycloak master admin user", ex);
      session.getTransactionManager().rollback();
    }
    session.close();
  }

  private void createGaiaXRealm() {
    KeycloakSession session = KeycloakApplication.getSessionFactory().create();
    session.setAttribute("ALLOW_CREATE_POLICY", true);
    try {
      session.getTransactionManager().begin();
      RealmManager manager = new RealmManager(session);
      Resource realmImportFile = new FileSystemResource(getKeycloakDirPath() + properties.getServer().getRealmImportFile());
      manager.importRealm(readValue(realmImportFile.getInputStream(), RealmRepresentation.class));
      manager.getRealm(properties.getRealm())
          .getClientByClientId(properties.getResource())
          .setSecret(properties.getSecret());
      session.getTransactionManager().commit();
    } catch (Exception ex) {
      log.error("Failed to import Realm json file: ", ex);
      session.getTransactionManager().rollback();
    }
    session.close();
  }

  private String getKeycloakDirPath() {
    String moduleDir = Paths.get("").toAbsolutePath().toString();
    return moduleDir.substring(0, moduleDir.lastIndexOf(File.separator));
  }

  public static class RegularJsonConfigProviderFactory extends JsonConfigProviderFactory {

  }
}
