package eu.gaiax.difs.fc.server.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@Slf4j
public class SessionUtils {
    public static String getSessionParticipantId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Jwt) {
            String participantId = ((Jwt) principal).getClaim("participant_id");
            log.debug("getSessionParticipantId.exit; got participant id = {} from principal information: {}",
                    participantId, principal);
                return participantId;
        }
        return null;
    }
}
