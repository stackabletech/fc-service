package eu.gaiax.difs.fc.demo.controller;

import eu.gaiax.difs.fc.api.generated.model.Participant;
import eu.gaiax.difs.fc.demo.proxy.ProxyCall;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Participants API Controller.
 */
@RestController
@RequestMapping("participants")
public class ParticipantsController {
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
  public ResponseEntity<Participant> addParticipant(HttpServletRequest request, @RequestBody String body) {
    return ProxyCall.retrieve(fcServer, request, body);
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
  @DeleteMapping("{participantId}")
  public ResponseEntity<Participant> deleteParticipant(HttpServletRequest request,
                                                       @PathVariable("participantId") String id) {
    return ProxyCall.retrieve(fcServer, request, null);
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
  @GetMapping("/{participantId}")
  public ResponseEntity<Participant> getParticipant(HttpServletRequest request,
                                                    @PathVariable("participantId") String id) {
    return ProxyCall.retrieve(fcServer, request, null);
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
  @GetMapping("/{participantId}/users")
  public ResponseEntity<Participant> getParticipantUsers(HttpServletRequest request,
                                                         @PathVariable("participantId") String id) {
    return ProxyCall.retrieve(fcServer, request, null);
  }

  /**
   * GET /participants : Get the registered participants.
   *
   * @param offset The number of items to skip before starting to collect the result set. (optional, default to 0)
   * @param limit The number of items to return. (optional, default to 100)
   * @param orderBy Results will be sorted by this field. (optional)
   * @param ascending Ascending/Descending ordering. (optional, default to true)
   * @return List of registered participants (status code 200)
   *         or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *         or May contain hints how to solve the error or indicate what went wrong at the server. (status code 500)
   */
  @GetMapping
  public ResponseEntity<List<Participant>> getParticipants(
      @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset,
      @RequestParam(value = "limit", required = false, defaultValue = "100") Integer limit,
      @RequestParam(value = "orderBy", required = false) String orderBy,
      @RequestParam(value = "ascending", required = false, defaultValue = "true") Boolean ascending,
      HttpServletRequest request) {
    return ProxyCall.retrieve(fcServer, request, null);
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
  public ResponseEntity<Participant> updateParticipant(HttpServletRequest request,
                                                       @PathVariable("participantId") String id,
                                                       @RequestBody String body) {
    return ProxyCall.retrieve(fcServer, request, body);
  }
}
