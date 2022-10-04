package eu.gaiax.difs.fc.testsupport.config;

import static org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters.*;

import eu.gaiax.difs.fc.testsupport.config.properties.KeycloakServerProperties;
import eu.gaiax.difs.fc.testsupport.config.provider.SimplePlatformProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.naming.CompositeName;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;
import javax.naming.spi.NamingManager;
import javax.sql.DataSource;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.keycloak.platform.Platform;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Slf4j
@Configuration
@Import(SecurityTestConfig.class)
@ConditionalOnProperty(value = "federated-catalogue.scope", havingValue = "test")
@EnableConfigurationProperties({KeycloakServerProperties.class})
public class EmbeddedKeycloakConfig {

  @Bean
  @ConditionalOnMissingBean(name = "keycloakJaxRsApplication")
  ServletRegistrationBean<HttpServlet30Dispatcher> keycloakJaxRsApplication(KeycloakServerProperties properties,
                                                                            DataSource dataSource) throws Exception {
    mockJndiEnvironment(dataSource);
    EmbeddedKeycloakApplication.properties = properties;
    ServletRegistrationBean<HttpServlet30Dispatcher> servlet =
        new ServletRegistrationBean<>(new HttpServlet30Dispatcher());
    servlet.addInitParameter("javax.ws.rs.Application", EmbeddedKeycloakApplication.class.getName());
    servlet.addInitParameter(RESTEASY_SERVLET_MAPPING_PREFIX, properties.getServer().getContextPath());
    servlet.addInitParameter(RESTEASY_USE_CONTAINER_FORM_PARAMS, "true");
    servlet.addUrlMappings(properties.getServer().getContextPath() + "/*");
    servlet.setLoadOnStartup(1);
    servlet.setAsyncSupported(true);
    return servlet;
  }

  @Bean
  FilterRegistrationBean<EmbeddedKeycloakRequestFilter> keycloakSessionManagement(KeycloakServerProperties properties) {
    FilterRegistrationBean<EmbeddedKeycloakRequestFilter> filter = new FilterRegistrationBean<>();
    filter.setName("Keycloak Session Management");
    filter.setFilter(new EmbeddedKeycloakRequestFilter());
    filter.addUrlPatterns(properties.getServer().getContextPath() + "/*");
    return filter;
  }

  private void mockJndiEnvironment(DataSource dataSource) throws NamingException {
    if (!NamingManager.hasInitialContextFactoryBuilder()) {
      NamingManager.setInitialContextFactoryBuilder((env) -> (environment) -> new InitialContext() {
        @Override
        public Object lookup(Name name) {
          return lookup(name.toString());
        }

        @Override
        public Object lookup(String name) {
          log.debug("lookup.enter; name: " + name);
          if ("spring/datasource".equals(name)) {
            return dataSource;
          } else if (name.startsWith("java:jboss/ee/concurrency/executor/")) {
            return fixedThreadPool();
          }
          return null;
        }

        @Override
        public NameParser getNameParser(String name) {
          return CompositeName::new;
        }

        @Override
        public void close() {
          log.debug("CLOSING CONTEXT");
        }
      });
    }
  }

  @Bean("fixedThreadPool")
  public ExecutorService fixedThreadPool() {
    return Executors.newFixedThreadPool(5);
  }

  @Bean
  @ConditionalOnMissingBean(name = "springBootPlatform")
  protected SimplePlatformProvider springBootPlatform() {
    return (SimplePlatformProvider) Platform.getPlatform();
  }
}
