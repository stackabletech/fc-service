package eu.gaiax.difs.fc.server.dao;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import eu.gaiax.difs.fc.api.generated.model.User;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class UserDao {
    
    //private static final String INITIAL_PASSWORD = "changeme";
    private static final String ACT_UPDATE_PASSWORD = "UPDATE_PASSWORD";
    private static final String ACT_VERIFY_EMAIL = "VERIFY_EMAIL";
    
    private static final ObjectMapper jsonMapper = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE_REF = new TypeReference<Map<String, Object>>() {};
    
    @Value("${keycloak.realm}")
    private String realm;
    @Autowired
    private Keycloak keycloak;
    
    public void create(User user) {
        
        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername(user.getUsername());
        //userRep.setFirstName(userDTO.getFirstname());
        //userRep.setLastName(userDTO.getLastName());
        userRep.setEmail(user.getEmail());
        //userRep.setCredentials(Collections.singletonList(createPasswordCredentials(INITIAL_PASSWORD)));
        userRep.setAttributes(Map.of("principalId", List.of(user.getParticipantId())));
        userRep.setEnabled(true);
        userRep.setEmailVerified(false);
        userRep.setRequiredActions(List.of(ACT_UPDATE_PASSWORD, ACT_VERIFY_EMAIL));

        UsersResource instance = keycloak.realm(realm).users();
        Response response = instance.create(userRep);  
        if (response.getStatus() != HttpStatus.SC_CREATED) {
            String message = getErrorMessage(response);
            log.info("create.error; status {}:{}, {}", response.getStatus(), response.getStatusInfo(), message);
            throw new RuntimeException(message);
        }
    }
    
    private String getErrorMessage(Response response) {
        String message;
        try {
            InputStream is = (InputStream) response.getEntity();
            message = CharStreams.toString(new InputStreamReader(is, Charsets.UTF_8));
            Map<String, Object> error = jsonMapper.readValue(message, MAP_TYPE_REF);
            message = (String) error.get("errorMessage");
        } catch (IOException ex) {
            message = ex.getMessage();
        }
        return message;
    }
    
    private CredentialRepresentation createPasswordCredentials(String password) {
        CredentialRepresentation passwordCredentials = new CredentialRepresentation();
        passwordCredentials.setTemporary(false);
        passwordCredentials.setType(CredentialRepresentation.PASSWORD);
        passwordCredentials.setValue(password);
        return passwordCredentials;
    }

}
