package eu.gaiax.difs.fc.demo.controller;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import eu.gaiax.difs.fc.api.generated.model.Session;

@RestController
@RequestMapping("ssn")
public class SessionController {

  @Value("${federated-catalogue.base-uri}")
  private String baseUri;
  
  @Autowired
  @Qualifier("fcServer")
  private WebClient fcServer;
  
  @GetMapping
  public Session getSession(@RegisteredOAuth2AuthorizedClient("fc-client-oidc") OAuth2AuthorizedClient authorizedClient) {
      return this.fcServer
                .get()
                .uri(baseUri + "/session")
                .attributes(oauth2AuthorizedClient(authorizedClient))
                .retrieve()
                .bodyToMono(Session.class)
                .block();
  }

  @DeleteMapping
  public void dropSession(@RegisteredOAuth2AuthorizedClient("fc-client-oidc") OAuth2AuthorizedClient authorizedClient) {
      this.fcServer
                .post()
                .uri(baseUri + "/session")
                .attributes(oauth2AuthorizedClient(authorizedClient))
                .retrieve();
                //.bodyToMono(Session.class)
                //.block();
  }

}
