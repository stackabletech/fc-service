package eu.gaiax.difs.fc.demo.controller;

import eu.gaiax.difs.fc.demo.proxy.RequestCall;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Roles API Controller.
 */
@RestController
@RequestMapping("roles")
public class RoleController {
  @Autowired
  @Qualifier("fcServer")
  private WebClient fcServer;

  /**
   * GET /roles : Get all registered roles in the catalogue.
   *
   * @return All roles (status code 200)
   *         or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *         or May contain hints how to solve the error or indicate what went wrong at the server. (status code 500)
   */
  @GetMapping
  public ResponseEntity<List<String>> getAllRoles(HttpServletRequest request) {
    return RequestCall.doGet(fcServer, request);
  }
}
