package eu.gaiax.difs.fc.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Defines additional configuration fot the web demo-application.
 * */
@Configuration
public class WebClientConfig {
  /**
   * Defines the web client bean used to connect to the Federated Catalogue Server.
   *
   * @param fcUri Federated Catalog URI.
   */ 
  @Bean(name = "fcServer")
  public WebClient webClient(OAuth2AuthorizedClientManager authorizedClientManager,
          @Value("${federated-catalogue.base-uri}") final String fcUri) {
      ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2Client =
        new ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
      return WebClient.builder()
        .apply(oauth2Client.oauth2Configuration())
        .baseUrl(fcUri)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }  
  
  @Bean
  OAuth2AuthorizedClientManager authorizedClientManager(ClientRegistrationRepository clientRegistrationRepository,
          OAuth2AuthorizedClientRepository authorizedClientRepository) {

      OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
          .authorizationCode()
          .refreshToken()
          .build();
      DefaultOAuth2AuthorizedClientManager authorizedClientManager = new DefaultOAuth2AuthorizedClientManager(
        clientRegistrationRepository, authorizedClientRepository);
      authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

      return authorizedClientManager;
  }  
}
