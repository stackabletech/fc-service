package eu.gaiax.difs.fc.core.dao.impl;

import static eu.gaiax.difs.fc.core.util.KeycloakUtils.getErrorMessage;

import eu.gaiax.difs.fc.core.pojo.PaginatedResults;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.neo4j.driver.exceptions.ClientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.gaiax.difs.fc.api.generated.model.User;
import eu.gaiax.difs.fc.api.generated.model.UserProfile;
import eu.gaiax.difs.fc.core.dao.UserDao;
import eu.gaiax.difs.fc.core.exception.ConflictException;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of the {@link UserDao} interface.
 */
@Slf4j
@Component
public class UserDaoImpl implements UserDao {
  // private static final String INITIAL_PASSWORD = "changeme";
  private static final String ACT_UPDATE_PASSWORD = "UPDATE_PASSWORD";
  private static final String ACT_VERIFY_EMAIL = "VERIFY_EMAIL";
  private static final String ATR_PARTICIPANT_ID = "participantId";

  @Value("${keycloak.realm}")
  private String realm;
  @Autowired
  private Keycloak keycloak;

  /**
   * Implementation of add user to catalogue repository.
   *
   * @param user User entity to be added {@link User}
   * @return UserProfile of created user
   */
  @Override
  public UserProfile create(User user) {
    UserRepresentation userRepo = toUserRepo(user);
    UsersResource instance = keycloak.realm(realm).users();
    Response response = instance.create(userRepo);
    if (response.getStatus() == HttpStatus.SC_CONFLICT) {
      String message = getErrorMessage(response);
      log.info("create.error; status {}:{}, {}", response.getStatus(), response.getStatusInfo(), message);
      throw new ConflictException(message);
    } else if (response.getStatus() != HttpStatus.SC_CREATED){
      String message = "Invalid used data";
      log.info("create.error; status {}:{}, {}", response.getStatus(), response.getStatusInfo(), message);
      throw new ClientException(message);
    }
    userRepo = instance.search(userRepo.getUsername()).get(0);
    return toUserProfile(userRepo, assignRoleToUser(instance.get(userRepo.getId()), user.getRoleIds()));
  }

  /**
   * Implementation of get user for particular userId.
   *
   * @param userId identifier of user
   * @return Found user profile
   */
  @Override
  public UserProfile select(String userId) {
    UsersResource instance = keycloak.realm(realm).users();
    UserResource userResource = instance.get(userId);
    UserRepresentation userRepo = getUserRepresentation(userResource, userId);
    return toUserProfile(userRepo, userResource.roles().realmLevel().listAll());
  }

  /**
   * Implementation of get list of the users.
   *
   * @param participantId Identifier of the participant
   * @param offset        The number of items to skip before starting to collect the result set
   * @param limit         The number of items to return
   * @return List of the user profiles.
   */
  @Override
  public PaginatedResults<UserProfile> search(String participantId, Integer offset, Integer limit) {
    UsersResource instance = keycloak.realm(realm).users();
    List<UserRepresentation> userRepos;
    int totalCount = instance.count();
    if (participantId == null) {
      userRepos = instance.list(offset, limit);
    } else {
      userRepos = instance.searchByAttributes(offset, limit, true, false,
          ATR_PARTICIPANT_ID + " = " + participantId);
      totalCount = instance.searchByAttributes(participantId).size();
    }
    return new PaginatedResults<>(totalCount, userRepos.stream().map(
        user -> toUserProfile(user, instance.get(user.getId()).roles().realmLevel().listAll())
    ).collect(Collectors.toList()));
  }

  /**
   * Implementation of Delete the user.
   *
   * @param userId Identifier of the user
   * @return UserProfile of the user
   */
  @Override
  public UserProfile delete(String userId) {
    UsersResource instance = keycloak.realm(realm).users();
    UserResource userResource = instance.get(userId);
    UserRepresentation userRepo = getUserRepresentation(userResource, userId);
    List<RoleRepresentation> roles = userResource.roles().realmLevel().listAll();

    Response response = instance.delete(userId);
    if (response.getStatus() != HttpStatus.SC_NO_CONTENT) {
      String message = getErrorMessage(response);
      log.info("delete.error; status {}:{}, {}", response.getStatus(), response.getStatusInfo(), message);
      throw new ConflictException(message);
    }
    return toUserProfile(userRepo, roles);
  }

  /**
   * Implementation of update the user.
   *
   * @param userId Identifier of the user
   * @param user   User entity to be added {@link User}
   * @return UserProfile of the user
   */
  @Override
  public UserProfile update(String userId, User user) {
    UsersResource instance = keycloak.realm(realm).users();
    UserResource userResource = instance.get(userId);
    UserRepresentation userRepo = toUserRepo(user);

    userResource.update(userRepo);
    assignRoleToUser(userResource, user.getRoleIds());
    // no Response ?

    userResource = instance.get(userId);
    userRepo = getUserRepresentation(userResource, userId);
    return toUserProfile(userRepo, userResource.roles().realmLevel().listAll());
  }

  /**
   * Implementation of update user roles.
   *
   * @param userId Identifier of the user
   * @param roles  list of roles to be updated
   * @return Updated user profile
   */
  @Override
  public UserProfile updateRoles(String userId, List<String> roles) {
    UsersResource instance = keycloak.realm(realm).users();
    UserResource userResource = instance.get(userId);
    UserRepresentation userRepo = getUserRepresentation(userResource, userId);
    userRepo.setRealmRoles(roles);
    userResource.update(userRepo);
    assignRoleToUser(userResource, roles);
    // no Response ?

    userResource = instance.get(userId);
    userRepo = getUserRepresentation(userResource, userId);
    return toUserProfile(userRepo, userResource.roles().realmLevel().listAll());
  }

  /**
   * Implementation of Get all roles.
   *
   * @return List of all user roles
   */
  @Override
  public List<String> getAllRoles() {
    RolesResource instance = keycloak.realm(realm).roles();
    return instance.list(true).stream().map(RoleRepresentation::getName).collect(Collectors.toList());
  }

  /**
   * Utility method for Convert user to UserRepresentation.
   *
   * @param user User entity to be added {@link User}
   * @return UserRepresentation of the user
   */
  public static UserRepresentation toUserRepo(User user) {
    UserRepresentation userRepo = new UserRepresentation();
    // userRepo.setCredentials(Collections.singletonList(createPasswordCredentials(INITIAL_PASSWORD)));
    userRepo.setFirstName(user.getFirstName());
    userRepo.setLastName(user.getLastName());
    userRepo.setEmail(user.getEmail());
    userRepo.setUsername(user.getEmail());
    userRepo.setAttributes(Map.of(ATR_PARTICIPANT_ID, List.of(user.getParticipantId())));
    userRepo.setEnabled(true);
    userRepo.setEmailVerified(false);
    userRepo.setGroups(List.of(user.getParticipantId()));
    userRepo.setRequiredActions(List.of(ACT_UPDATE_PASSWORD, ACT_VERIFY_EMAIL));
    return userRepo;
  }

  private List<RoleRepresentation> assignRoleToUser(UserResource userResource, List<String> roles) {
    List<RoleRepresentation> existedRoles = keycloak.realm(realm).roles().list();

    List<RoleRepresentation> roleRepresentations = existedRoles.stream()
        .filter(role -> roles.contains(role.getName()))
        .collect(Collectors.toList());
    //if added role is valid role then  delete old roles and update new one
    if ((!roleRepresentations.isEmpty() && roles.size()==roleRepresentations.size()) || roles.isEmpty()) {
      userResource.roles().realmLevel().remove(existedRoles);
      userResource.roles().realmLevel().add(roleRepresentations);
    } else {
      throw new ClientException("Please check that the sent roles are valid.");
    }
    return roleRepresentations;
  }

  private static List<String> toRoleIds(List<RoleRepresentation> roleRepresentations) {
    return roleRepresentations.stream().map(RoleRepresentation::getName).collect(Collectors.toList());
  }

  private UserRepresentation getUserRepresentation(UserResource userResource, String id) {
    try {
      return userResource.toRepresentation();
    } catch (NotFoundException exception) {
      throw new eu.gaiax.difs.fc.core.exception.NotFoundException("User with id " + id + " not found");
    }
  }

  /**
   * Utility method for Convert UserRepresentation to UserProfile.
   *
   * @param userRepo UserRepresentation of user
   * @return UserProfile of the user
   */
  public static UserProfile toUserProfile(UserRepresentation userRepo, List<RoleRepresentation> roles) {
    List<String> partIds = userRepo.getAttributes() == null ? null
        : userRepo.getAttributes().get(ATR_PARTICIPANT_ID);
    String participantId = partIds == null ? null : partIds.get(0);
    return new UserProfile(participantId, userRepo.getFirstName(), userRepo.getLastName(), userRepo.getEmail(),
        toRoleIds(roles), userRepo.getId(), userRepo.getFirstName() + " " + userRepo.getLastName());
  }

  /**
   * Utility method for create passwordCredentials.
   *
   * @param password String password
   * @return CredentialRepresentation of password
   */
  private CredentialRepresentation createPasswordCredentials(String password) {
    CredentialRepresentation passwordCredentials = new CredentialRepresentation();
    passwordCredentials.setTemporary(false);
    passwordCredentials.setType(CredentialRepresentation.PASSWORD);
    passwordCredentials.setValue(password);
    return passwordCredentials;
  }
}