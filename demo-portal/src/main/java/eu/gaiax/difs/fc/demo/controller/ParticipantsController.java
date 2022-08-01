package eu.gaiax.difs.fc.demo.controller;

import eu.gaiax.difs.fc.api.generated.model.Participant;
import eu.gaiax.difs.fc.demo.proxy.ProxyCall;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
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

@RestController
@RequestMapping("participants")
public class ParticipantsController {
  @Autowired
  @Qualifier("fcServer")
  private WebClient fcServer;

  @PostMapping
  public ResponseEntity<Participant> addParticipant(HttpServletRequest request, @RequestBody String body) {
    return ProxyCall.retrieve(fcServer, request, body);
  }

  @DeleteMapping("{participantId}")
  public ResponseEntity<Participant> deleteParticipant(HttpServletRequest request,
                                                       @PathVariable("participantId") String id) {
    return ProxyCall.retrieve(fcServer, request, null);
  }

  @GetMapping("/{participantId}")
  public ResponseEntity<Participant> getUser(HttpServletRequest request, @PathVariable("participantId") String id) {
    return ProxyCall.retrieve(fcServer, request, null);
  }

  @GetMapping("/{participantId}/users")
  public ResponseEntity<Participant> getParticipantUsers(HttpServletRequest request,
                                                         @PathVariable("participantId") String id) {
    return ProxyCall.retrieve(fcServer, request, null);
  }

  @GetMapping
  public ResponseEntity<List<Participant>> getParticipants(
      HttpServletRequest request,
      @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset,
      @RequestParam(value = "limit", required = false, defaultValue = "100") Integer limit,
      @RequestParam(value = "orderBy", required = false) String orderBy,
      @RequestParam(value = "ascending", required = false, defaultValue = "true") Boolean ascending) {
    return ProxyCall.retrieve(fcServer, request, null);
  }

  @PutMapping("/{participantId}")
  public ResponseEntity<Participant> updateParticipant(HttpServletRequest request,
                                                       @PathVariable("participantId") String id,
                                                       @RequestBody String body) {
    return ProxyCall.retrieve(fcServer, request, body);
  }
}
