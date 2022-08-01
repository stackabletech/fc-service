package eu.gaiax.difs.fc.demo.controller;

import eu.gaiax.difs.fc.demo.proxy.ProxyCall;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

@RestController
@RequestMapping("roles")
public class RoleController {
  @Autowired
  @Qualifier("fcServer")
  private WebClient fcServer;

  @GetMapping
  public ResponseEntity<List<String>> getAllRoles(HttpServletRequest request) {
    return ProxyCall.retrieve(fcServer, request, null);
  }
}
