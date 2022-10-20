package eu.gaiax.difs.fc.server.util;

import java.util.Collection;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.enums.TokenStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

/**
 * Utility class with static methods for getting data from the active user session.
 */
@Slf4j
public class SessionUtils {
  /**
   * Public static method to get Participant ID from the active user session.
   *
   * @return Returns either the Participant ID or null if the user session doesn't contain Participant ID attribute.
   */
  public static String getSessionParticipantId() {
    String participantId = null;
    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (principal instanceof Jwt) {
      participantId = ((Jwt) principal).getClaim("participant_id");
    }
    log.debug("getSessionParticipantId.exit; got participant id {} from principal: {}", participantId, principal);
    return participantId;
  }

  /**
   * Public static method to get User ID from the active user session.
   *
   *
   * @return String user Id.
   */
  public static String getSessionUserId() {
    String userId = null;
    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (principal instanceof Jwt) {
      userId = ((Jwt) principal).getSubject();
    }
    log.debug("getSessionUserId.exit; got user id {} from principal: {}", userId, principal);
    return userId;
  }

  /**
   * Checks user session role.
   *
   * @param role role to be checked.
   * @return boolean status.
   */
  public static boolean sessionUserHasRole(String role) {
    Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>)
        SecurityContextHolder.getContext().getAuthentication().getAuthorities();
    boolean hasRole = false;
    for (GrantedAuthority authority : authorities) {
      hasRole = authority.getAuthority().equals(role);
      if (hasRole) {
        break;
      }
    }
    return hasRole;
  }

  /**
   * Clear spring security context.
   *
   */
  public static void logoutSessionUser() {
    SecurityContextHolder.getContext().setAuthentication(null);
    SecurityContextHolder.clearContext();
    log.debug("logoutSessionUser.exit;");
  }

}
