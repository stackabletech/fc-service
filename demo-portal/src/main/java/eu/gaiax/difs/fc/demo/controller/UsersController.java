package eu.gaiax.difs.fc.demo.controller;

import eu.gaiax.difs.fc.api.generated.model.User;
import eu.gaiax.difs.fc.api.generated.model.UserProfile;
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
@RequestMapping("users")
public class UsersController {
  @Autowired
  @Qualifier("fcServer")
  private WebClient fcServer;

  @PostMapping
  public ResponseEntity<UserProfile> addUser(HttpServletRequest request, @RequestBody(required = false) User user) {
    return ProxyCall.retrieve(fcServer, request, user);
  }

  @PutMapping("/{userId}")
  public ResponseEntity<UserProfile> updateUser(HttpServletRequest request, @PathVariable("userId") String id,
                                                @RequestBody(required = false) User user) {
    return ProxyCall.retrieve(fcServer, request, user);
  }

  @DeleteMapping("/{userId}")
  public ResponseEntity<UserProfile> deleteUser(HttpServletRequest request, @PathVariable("userId") String id) {
    return ProxyCall.retrieve(fcServer, request, null);
  }

  @GetMapping("/{userId}")
  public ResponseEntity<UserProfile> getUser(HttpServletRequest request, @PathVariable("userId") String id) {
    return ProxyCall.retrieve(fcServer, request, null);
  }

  @GetMapping
  public ResponseEntity<List<UserProfile>> getUsers(
      HttpServletRequest request,
      @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset,
      @RequestParam(value = "limit", required = false, defaultValue = "100") Integer limit,
      @RequestParam(value = "orderBy", required = false) String orderBy,
      @RequestParam(value = "ascending", required = false, defaultValue = "true") Boolean ascending) {
    return ProxyCall.retrieve(fcServer, request, null);
  }

  @GetMapping("/{userId}/roles")
  public ResponseEntity<List<String>> getUserRoles(HttpServletRequest request, @PathVariable("userId") String id) {
    return ProxyCall.retrieve(fcServer, request, null);
  }

  @PutMapping("/{userId}/roles")
  public ResponseEntity<List<String>> updateUserRoles(HttpServletRequest request, @PathVariable("userId") String id,
                                                      @RequestBody(required = false) List<String> roles) {
    return ProxyCall.retrieve(fcServer, request, roles);
  }
}
