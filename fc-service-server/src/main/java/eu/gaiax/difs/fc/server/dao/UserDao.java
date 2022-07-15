package eu.gaiax.difs.fc.server.dao;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
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
        
        UserRepresentation userRepo = new UserRepresentation();
        userRepo.setUsername(user.getUsername());
        //userRepo.setFirstName(userDTO.getFirstname());
        //userRepo.setLastName(userDTO.getLastName());
        userRepo.setEmail(user.getEmail());
        //userRepo.setCredentials(Collections.singletonList(createPasswordCredentials(INITIAL_PASSWORD)));
        userRepo.setAttributes(Map.of("principalId", List.of(user.getParticipantId())));
        userRepo.setEnabled(true);
        userRepo.setEmailVerified(false);
        userRepo.setRequiredActions(List.of(ACT_UPDATE_PASSWORD, ACT_VERIFY_EMAIL));

        UsersResource instance = keycloak.realm(realm).users();
        Response response = instance.create(userRepo);  
        if (response.getStatus() != HttpStatus.SC_CREATED) {
            String message = getErrorMessage(response);
            log.info("create.error; status {}:{}, {}", response.getStatus(), response.getStatusInfo(), message);
            throw new RuntimeException(message);
        }
    }

    public User select(String userId) {
        
        UsersResource instance = keycloak.realm(realm).users();
        UserResource userResource = instance.get(userId);
        UserRepresentation userRepo = userResource.toRepresentation();
        User user = new User();
        return user;
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
