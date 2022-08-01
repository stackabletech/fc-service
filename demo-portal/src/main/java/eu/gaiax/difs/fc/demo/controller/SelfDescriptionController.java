package eu.gaiax.difs.fc.demo.controller;

import eu.gaiax.difs.fc.api.generated.model.SelfDescription;
import eu.gaiax.difs.fc.demo.proxy.ProxyCall;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

@RestController
@RequestMapping("self-descriptions")
public class SelfDescriptionController {
  @Autowired
  @Qualifier("fcServer")
  private WebClient fcServer;

  @GetMapping
  public ResponseEntity<List<SelfDescription>> readSelfDescriptions(HttpServletRequest request) {
    return ProxyCall.retrieve(fcServer, request, null);
  }

  @GetMapping("/{self_description_hash}")
  public ResponseEntity<Object> readSelfDescriptionByHash(@PathVariable("self_description_hash") String hash,
                                                          HttpServletRequest request) {
    return ProxyCall.retrieve(fcServer, request, null);
  }

  @DeleteMapping("/{self_description_hash}")
  public ResponseEntity<Void> deleteSelfDescription(@PathVariable("self_description_hash") String hash,
                                                    HttpServletRequest request) {
    return ProxyCall.retrieve(fcServer, request, null);
  }

  @PostMapping
  public ResponseEntity<SelfDescription> addSelfDescription(@Valid @RequestBody String selfDescription,
                                                            HttpServletRequest request) {

    return ProxyCall.retrieve(fcServer, request, selfDescription);
  }

  @PostMapping("/{self_description_hash}/revoke")
  public ResponseEntity<SelfDescription> updateSelfDescription(@PathVariable("self_description_hash") String hash,
                                                               @Valid @RequestBody String selfDescription,
                                                               HttpServletRequest request) {
    return ProxyCall.retrieve(fcServer, request, selfDescription);
  }
}
