package eu.gaiax.difs.fc.testsupport.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakServerProperties {
  @Value("${keycloak.realm}")
  private String realm;

  @Value("${keycloak.resource}")
  private String resource;

  @Value("${keycloak.credentials.secret}")
  private String secret;

  private ServerConfig server;

  @Getter
  @Setter
  public static class ServerConfig {
    @Value("${keycloak.server.context-path}")
    private String contextPath;

    @Value("${keycloak.server.realm-import-file}")
    private String realmImportFile;

    private AdminUser admin;

    @Getter
    @Setter
    public static class AdminUser {
      @Value("${keycloak.server.admin.username}")
      private String username;
      @Value("${keycloak.server.admin.password}")
      private String password;
    }
  }
}