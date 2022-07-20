package eu.gaiax.difs.fc.server.dao;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RolesResource;
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
import eu.gaiax.difs.fc.api.generated.model.UserProfile;
import eu.gaiax.difs.fc.server.exception.ConflictException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class UserDao {

    // private static final String INITIAL_PASSWORD = "changeme";
    private static final String ACT_UPDATE_PASSWORD = "UPDATE_PASSWORD";
    private static final String ACT_VERIFY_EMAIL = "VERIFY_EMAIL";
    private static final String ATR_PARTICIPANT_ID = "participantId";

    private static final ObjectMapper jsonMapper = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE_REF = new TypeReference<Map<String, Object>>() {
    };

    @Value("${keycloak.realm}")
    private String realm;
    @Autowired
    private Keycloak keycloak;

    public UserProfile create(User user) {

        UserRepresentation userRepo = toUserRepo(user);
        UsersResource instance = keycloak.realm(realm).users();
        Response response = instance.create(userRepo);
        if (response.getStatus() != HttpStatus.SC_CREATED) {
            String message = getErrorMessage(response);
            log.info("create.error; status {}:{}, {}", response.getStatus(), response.getStatusInfo(), message);
            throw new ConflictException(message);
        }

        userRepo = instance.search(userRepo.getUsername()).get(0);
        return toUserProfile(userRepo);
    }

    public UserProfile select(String userId) {

        UsersResource instance = keycloak.realm(realm).users();
        UserResource userResource = instance.get(userId);
        UserRepresentation userRepo = userResource.toRepresentation();
        return toUserProfile(userRepo);
    }

    public List<UserProfile> search(Integer offset, Integer limit) {

        UsersResource instance = keycloak.realm(realm).users();
        List<UserRepresentation> userRepos = instance.list(offset, limit);
        return userRepos.stream().map(u -> toUserProfile(u)).collect(Collectors.toList());
    }

    public UserProfile delete(String userId) {

        UsersResource instance = keycloak.realm(realm).users();
        UserResource userResource = instance.get(userId);
        UserRepresentation userRepo = userResource.toRepresentation();

        Response response = instance.delete(userId);
        if (response.getStatus() != HttpStatus.SC_NO_CONTENT) {
            String message = getErrorMessage(response);
            log.info("delete.error; status {}:{}, {}", response.getStatus(), response.getStatusInfo(), message);
            throw new ConflictException(message);
        }

        UserProfile userPro = toUserProfile(userRepo);
        return userPro;
    }

    public UserProfile update(String userId, User user) {

        UsersResource instance = keycloak.realm(realm).users();
        UserResource userResource = instance.get(userId);
        UserRepresentation userRepo = toUserRepo(user);
        userResource.update(userRepo);
        // no Response ?

        userResource = instance.get(userId);
        userRepo = userResource.toRepresentation();
        return toUserProfile(userRepo);
    }

    public UserProfile updateRoles(String userId, List<String> roles) {

        UsersResource instance = keycloak.realm(realm).users();
        UserResource userResource = instance.get(userId);
        UserRepresentation userRepo = userResource.toRepresentation();
        userRepo.setRealmRoles(roles);
        userResource.update(userRepo);
        // no Response ?

        userResource = instance.get(userId);
        userRepo = userResource.toRepresentation();
        return toUserProfile(userRepo);
    }
    
    public List<String> getAllRoles() {
        
        RolesResource instance = keycloak.realm(realm).roles();
        return instance.list(true).stream().map(r -> r.getName()).collect(Collectors.toList());
    }

    public static UserRepresentation toUserRepo(User user) {

        UserRepresentation userRepo = new UserRepresentation();
        // userRepo.setCredentials(Collections.singletonList(createPasswordCredentials(INITIAL_PASSWORD)));
        userRepo.setFirstName(user.getFirstName());
        userRepo.setLastName(user.getLastName());
        userRepo.setEmail(user.getEmail());
        userRepo.setUsername(getUsername(user)); // use email instead?
        userRepo.setAttributes(Map.of(ATR_PARTICIPANT_ID, List.of(user.getParticipantId())));
        userRepo.setEnabled(true);
        userRepo.setEmailVerified(false);
        userRepo.setRequiredActions(List.of(ACT_UPDATE_PASSWORD, ACT_VERIFY_EMAIL));
        if (user.getRoleIds() != null) {
            userRepo.setRealmRoles(user.getRoleIds());
        }
        return userRepo;
    }

    public static UserProfile toUserProfile(UserRepresentation userRepo) {
        List<String> partIds = userRepo.getAttributes() == null ? null
                : userRepo.getAttributes().get(ATR_PARTICIPANT_ID);
        String participantId = partIds == null ? null : partIds.get(0);
        return new UserProfile(participantId, userRepo.getFirstName(), userRepo.getLastName(), userRepo.getEmail(),
                userRepo.getRealmRoles(), userRepo.getId(), userRepo.getFirstName() + " " + userRepo.getLastName());
    }

    private static String getUsername(User user) {
        return user.getParticipantId() + "{" + user.getFirstName() + " " + user.getLastName() + "}";
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
