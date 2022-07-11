package eu.gaiax.difs.fc.server.controller;

import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/demo")
public class DemoController {

    @GetMapping
    public String demonstrate(HttpServletRequest request) {
        return "Hi from demo app: " + getIdentity(request);
    }

    @GetMapping("/authorized")
    public String demonstrateAuthorizedAccess(HttpServletRequest request) {
        return "Hi from demo authorized with read grants: " + getIdentity(request);
    }

    @GetMapping("/admin")
    public String demonstrateAuthorizedAdminAccess(HttpServletRequest request) {
        return "Hi from demo admin with read grants: " + getIdentity(request);
    }

    private String getIdentity(HttpServletRequest request) {
        return request.getUserPrincipal() == null ? null : request.getUserPrincipal().toString();
    }
}
