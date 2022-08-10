package eu.gaiax.difs.fc.demo.controller;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient;

import eu.gaiax.difs.fc.api.generated.model.Participant;
import eu.gaiax.difs.fc.api.generated.model.UserProfile;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Participants API Controller.
 */
@RestController
@RequestMapping("parts")
public class ParticipantsController {

  private static final ParameterizedTypeReference<List<Participant>> PART_LIST_TYPE_REF = new ParameterizedTypeReference<List<Participant>>() {};
  private static final ParameterizedTypeReference<List<UserProfile>> USER_LIST_TYPE_REF = new ParameterizedTypeReference<List<UserProfile>>() {};

  @Value("${federated-catalogue.base-uri}")
  private String baseUri;
  
  @Autowired
  @Qualifier("fcServer")
  private WebClient fcServer;
  
  /**
   * POST /participants : Register a new participant in the catalogue.
   *
   * @param body Participant Self-Description (required)
   * @return Created Participant (status code 201)
   *         or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *         or Forbidden. The user does not have the permission to execute this request. (status code 403)
   *         or HTTP Conflict 409 (status code 409)
   *         or May contain hints how to solve the error or indicate what went wrong at the server. (status code 500)
   */
  @PostMapping
  public Participant addParticipant(
          @RegisteredOAuth2AuthorizedClient("fc-client-oidc") OAuth2AuthorizedClient authorizedClient,
          @RequestBody String body) {
      return this.fcServer
                .post()
                .uri(baseUri + "/participants")
                .attributes(oauth2AuthorizedClient(authorizedClient))
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Participant.class)
                .block();
  }

  /**
   * DELETE /participants/{participantId} : Delete a participant in the catalogue.
   *
   * @param id The participant to delete. (required)
   * @return Deleted Participant (status code 200)
   *         or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *         or Forbidden. The user does not have the permission to execute this request. (status code 403)
   *         or The specified resource was not found (status code 404)
   *         or HTTP Conflict 409 (status code 409)
   *         or May contain hints how to solve the error or indicate what went wrong at the server. (status code 500)
   */
  @DeleteMapping("{id}")
  public Participant deleteParticipant(
          @RegisteredOAuth2AuthorizedClient("fc-client-oidc") OAuth2AuthorizedClient authorizedClient,
          @PathVariable("id") String id) {
      return this.fcServer
              .delete()
              .uri(baseUri + "/participants/" + id)
              .attributes(oauth2AuthorizedClient(authorizedClient))
              .retrieve()
              .bodyToMono(Participant.class)
              .block();
  }

  /**
   * GET /participants/{participantId} : Get the registered participant.
   *
   * @param id The participantId to get. (required)
   * @return The requested participant (status code 200)
   *         or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *         or Forbidden. The user does not have the permission to execute this request. (status code 403)
   *         or The specified resource was not found (status code 404)
   *         or May contain hints how to solve the error or indicate what went wrong at the server. (status code 500)
   */
  @GetMapping("/{id}")
  public Participant getParticipant(
          @RegisteredOAuth2AuthorizedClient("fc-client-oidc") OAuth2AuthorizedClient authorizedClient,
          @PathVariable("id") String id) {
      return this.fcServer
              .get()
              .uri(baseUri + "/participants/" + id)
              .attributes(oauth2AuthorizedClient(authorizedClient))
              .retrieve()
              .bodyToMono(Participant.class)
              .block();
  }

  /**
   * GET /participants/{participantId}/users : Get all users of the registered participant.
   *
   * @param id The participant Id (required)
   * @return Users of the participant (status code 200)
   *         or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *         or Forbidden. The user does not have the permission to execute this request. (status code 403)
   *         or The specified resource was not found (status code 404)
   *         or May contain hints how to solve the error or indicate what went wrong at the server. (status code 500)
   */
  @GetMapping("/{id}/users")
  public List<UserProfile> getParticipantUsers(
          @RegisteredOAuth2AuthorizedClient("fc-client-oidc") OAuth2AuthorizedClient authorizedClient,
          @PathVariable("id") String id) {
      return this.fcServer
              .get()
              .uri(baseUri + "/participants/" + id + "/users")
              .attributes(oauth2AuthorizedClient(authorizedClient))
              .retrieve()
              .bodyToMono(USER_LIST_TYPE_REF)
              .block();
  }

  /**
   * GET /participants : Get the registered participants.
   *
   * @return List of registered participants (status code 200)
   *         or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *         or May contain hints how to solve the error or indicate what went wrong at the server. (status code 500)
   */
  @GetMapping
  public List<Participant> getParticipants(
    @RegisteredOAuth2AuthorizedClient("fc-client-oidc") OAuth2AuthorizedClient authorizedClient
  ) {
      return this.fcServer
        .get()
        .uri(baseUri + "/participants?offset=0&limit=50")
        .attributes(oauth2AuthorizedClient(authorizedClient))
        .retrieve()
        .bodyToMono(PART_LIST_TYPE_REF)
        .block();
  }
  
  /**
   * PUT /participants/{participantId} : Update a participant in the catalogue.
   *
   * @param id The participant to update. (required)
   * @param body Participant Self-Description (required)
   * @return Updated Participant (status code 200)
   *         or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *         or Forbidden. The user does not have the permission to execute this request. (status code 403)
   *         or The specified resource was not found (status code 404)
   *         or May contain hints how to solve the error or indicate what went wrong at the server. (status code 500)
   */
  @PutMapping("/{participantId}")
  public Participant updateParticipant(
          @RegisteredOAuth2AuthorizedClient("fc-client-oidc") OAuth2AuthorizedClient authorizedClient,
          @PathVariable("id") String id,
          @RequestBody String body) {
      return this.fcServer
              .put()
              .uri(baseUri + "/participants/" + id)
              .attributes(oauth2AuthorizedClient(authorizedClient))
              .retrieve()
              .bodyToMono(Participant.class)
              .block();
  }
}
