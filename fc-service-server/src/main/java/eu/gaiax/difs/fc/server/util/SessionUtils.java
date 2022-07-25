package eu.gaiax.difs.fc.server.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Utility class with static methods for getting data from the active user session.
 */
@Slf4j
public class SessionUtils {
  /**
   * Public static method to get the Participant ID from the active user session.
   *
   * @return Returns either the Participant ID or null if the user session doesn't contain Participant ID attribute.
   */
  public static String getSessionParticipantId() {
    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (principal instanceof Jwt) {
      String participantId = ((Jwt) principal).getClaim("participant_id");
      log.debug("getSessionParticipantId.exit; got participant id = {} from principal information: {}", participantId,
          principal);
      return participantId;
    }
    return null;
  }
}
