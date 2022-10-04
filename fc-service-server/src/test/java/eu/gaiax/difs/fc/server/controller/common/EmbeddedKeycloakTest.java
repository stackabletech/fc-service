package eu.gaiax.difs.fc.server.controller.common;

import static eu.gaiax.difs.fc.server.util.CommonConstants.CATALOGUE_ADMIN_ROLE;
import static eu.gaiax.difs.fc.server.util.TestCommonConstants.CATALOGUE_ADMIN_USERNAME;
import static eu.gaiax.difs.fc.server.util.TestCommonConstants.DEFAULT_PARTICIPANT_ID;

import eu.gaiax.difs.fc.testsupport.config.EmbeddedKeycloakApplication;
import eu.gaiax.difs.fc.testsupport.config.EmbeddedKeycloakConfig;
import eu.gaiax.difs.fc.testsupport.config.properties.KeycloakServerProperties;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.RealmManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Slf4j
@ActiveProfiles("test")
@Import(value = {EmbeddedKeycloakConfig.class})
@ExtendWith(SpringExtension.class)
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnableConfigurationProperties({KeycloakServerProperties.class})
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
public class EmbeddedKeycloakTest {
  @Value("${keycloak.realm}")
  private String realmName;

  @BeforeAll
  void setupKeycloak() {
    KeycloakSession session = EmbeddedKeycloakApplication.getSessionFactory().create();
    try {
      session.getTransactionManager().begin();
      RealmManager manager = new RealmManager(session);
      RealmModel realm = manager.getRealm(realmName);
      GroupModel group = session.groups().getGroupById(realm, DEFAULT_PARTICIPANT_ID);
      if (group == null) {
        group = session.groups().createGroup(realm, UUID.randomUUID().toString(), DEFAULT_PARTICIPANT_ID);
      }
      RoleModel catalogAdminRole = realm.getRole(CATALOGUE_ADMIN_ROLE);
      UserModel catalogAdmin = session.users().getUserByUsername(realm, CATALOGUE_ADMIN_USERNAME);
      if (catalogAdmin == null) {
        catalogAdmin =
            session.users().addUser(realm, UUID.randomUUID().toString(), CATALOGUE_ADMIN_USERNAME, true, true);
        catalogAdmin.grantRole(catalogAdminRole);
        catalogAdmin.joinGroup(group);
      }
      session.getTransactionManager().commit();
    } catch (Exception ex) {
      log.error("Failed to setup keycloak for tests: ", ex);
      session.getTransactionManager().rollback();
    }
    session.close();
  }
}
