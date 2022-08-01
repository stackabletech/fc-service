package eu.gaiax.difs.fc.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Defines additional configuration fot the web demo-application.
 * */
@Configuration
public class WebConfiguration {
  /**
   * Defines the web client bean used to connect to the Federated Catalogue Server.
   *
   * @param externalUri Federated directory URI.
   */
  @Bean(name = "fcServer")
  public WebClient fcServer(@Value("${services.identity.uri.internal}") final String externalUri) {
    return WebClient.builder()
        .baseUrl(externalUri)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }
}
