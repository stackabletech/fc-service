package eu.gaiax.difs.fc.core.dao.impl;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.gaiax.difs.fc.api.generated.model.Session;
import eu.gaiax.difs.fc.core.dao.SessionDao;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SessionDaoImpl implements SessionDao {
    
    @Value("${keycloak.realm}")
    private String realm;
    @Autowired
    private Keycloak keycloak;
    

    @Override
    public Session select(String id) {
        UserResource user = keycloak.realm(realm).users().get(id);
        List<UserSessionRepresentation> sessions = user.getUserSessions();
        log.debug("select; got sessions: {}", sessions);
        if (sessions != null && sessions.size() > 0) {
            UserSessionRepresentation ssn = sessions.get(0);
            log.debug("select; session {} of {}, started at: {}, last accessed at: {}, from: {}", 
                    ssn.getId(), ssn.getUsername(), ssn.getStart(), ssn.getLastAccess(), ssn.getIpAddress());
            java.util.Date start = new java.util.Date(ssn.getStart());
            OffsetDateTime started = start.toInstant().atOffset(ZoneOffset.UTC); 
            return new Session(ssn.getUserId(), started, "ACTIVE", // what it should be??
                    user.roles().getAll().getRealmMappings().stream().map(rr -> rr.getName()).collect(Collectors.toList()));
        }
        return null;
    }

    @Override
    public void delete(String id) {
        // keycloak.realm(realm).deleteSession(id);
         keycloak.realm(realm).users().get(id).logout();
    }

}
