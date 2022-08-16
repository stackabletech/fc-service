package eu.gaiax.difs.fc.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import eu.gaiax.difs.fc.api.generated.model.Session;
import eu.gaiax.difs.fc.client.SessionClient;

@RestController
@RequestMapping("ssn")
public class SessionController {

  @Autowired
  private SessionClient ssnClient;
  
  @GetMapping
  public Session getSession() { //@RegisteredOAuth2AuthorizedClient("fc-client-oidc") OAuth2AuthorizedClient authorizedClient) {
      return ssnClient.getCurrentSession();
  }

  @DeleteMapping
  public void dropSession() { //@RegisteredOAuth2AuthorizedClient("fc-client-oidc") OAuth2AuthorizedClient authorizedClient) {
      ssnClient.deleteCurrentSession();
  }

}
