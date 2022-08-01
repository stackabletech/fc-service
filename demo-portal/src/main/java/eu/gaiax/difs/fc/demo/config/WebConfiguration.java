package eu.gaiax.difs.fc.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebConfiguration {
  @Bean(name = "fcServer")
  public WebClient fcServer(@Value("${services.identity.uri.internal}") final String extURI) {
    return WebClient.builder()
        .baseUrl(extURI)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }
}
