package eu.gaiax.difs.fc.server.service;

import eu.gaiax.difs.fc.api.generated.model.User;
import eu.gaiax.difs.fc.api.generated.model.UserProfile;
import eu.gaiax.difs.fc.api.generated.model.UserProfiles;
import eu.gaiax.difs.fc.core.dao.UserDao;
import eu.gaiax.difs.fc.server.generated.controller.UsersApiDelegate;
import java.net.URI;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Service for the crud operation of the user . Implementation of the {@link UsersApiDelegate }.
 */
@Slf4j
@Service
public class UsersService implements UsersApiDelegate {

  @Autowired
  private UserDao userDao;

  /**
   * Service method for  register a new user to the associated participant in the catalogue.
   *
   * @param user User entity to be added {@link User}
   * @return Created User profile (status code 201)
   *        or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *        or Forbidden. The user does not have the permission to execute this request. (status code 403)
   *        or HTTP Conflict 409 (status code 409)
   *        or May contain hints how to solve the error or indicate what went wrong at the server. Must not outline
   *        any information about the internal structure of the server. (status code 500)
   */
  @Override
  public ResponseEntity<UserProfile> addUser(User user) {
    log.debug("addUser.enter; got user: {}", user);
    UserProfile profile = userDao.create(user);
    log.debug("addUser.exit; returning: {}", profile);
    return ResponseEntity.created(URI.create("/users/" + profile.getId())).body(profile);
  }

  /**
   * Service method for update the user profile.
   *
   * @param userId user id of the user(required)
   * @param user   User entity to be added {@link User}
   * @return Updated user profile (status code 200)
   *        or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *        or Forbidden. The user does not have the permission to execute this request. (status code 403)
   *        or The specified resource was not found (status code 404)
   *        or May contain hints how to solve the error or indicate what went wrong at the server. Must not outline
   *        any information about the internal structure of the server. (status code 500)
   */
  @Override
  public ResponseEntity<UserProfile> updateUser(String userId, User user) {
    log.debug("updateUser.enter; got userId: {}", userId);
    UserProfile profile = userDao.update(userId, user);
    log.debug("updateUser.exit; returning: {}", profile);
    return ResponseEntity.ok(profile);
  }

  /**
   * Service method for delete a user.
   *
   * @param userId user id of the user(required)
   * @return Deleted user profile (status code 200)
   *        or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *        or Forbidden. The user does not have the permission to execute this request. (status code 403)
   *        or The specified resource was not found (status code 404)
   *        or HTTP Conflict 409 (status code 409)
   *        or May contain hints how to solve the error or indicate what went wrong at the server. Must not outline
   *        any information about the internal structure of the server. (status code 500)
   */
  @Override
  public ResponseEntity<UserProfile> deleteUser(String userId) {
    log.debug("deleteUser.enter; got userId: {}", userId);
    UserProfile profile = userDao.delete(userId);
    log.debug("deleteUser.exit; returning: {}", profile);
    return ResponseEntity.ok(profile);
  }

  /**
   * Service method for get the user profile.
   *
   * @param userId user id of the user(required)
   * @return User profile (status code 200)
   *        or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *        or Forbidden. The user does not have the permission to execute this request. (status code 403)
   *        or The specified resource was not found (status code 404)
   *        or May contain hints how to solve the error or indicate what went wrong at the server. Must not outline
   *        any information about the internal structure of the server. (status code 500)
   */
  @Override
  public ResponseEntity<UserProfile> getUser(String userId) {
    log.debug("getUser.enter; got userId: {}", userId);
    UserProfile profile = userDao.select(userId);
    log.debug("getUser.exit; returning: {}", profile);
    return ResponseEntity.ok(profile);
  }

  /**
   * Service method for list the registered users.
   *
   * @param offset    The number of items to skip before starting to collect the result set. (optional, default to 0)
   * @param limit     The number of items to return. (optional, default to 100)
   * @param orderBy   Results will be sorted by this field. (optional)
   * @param ascending Ascending/Descending ordering. (optional, default to true)
   * @return List of usernames (status code 200)
   *        or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *        or May contain hints how to solve the error or indicate what went wrong at the server. Must not outline
   *        any information about the internal structure of the server. (status code 500)
   */
  @Override
  public ResponseEntity<UserProfiles> getUsers(Integer offset, Integer limit) { //String orderBy, Boolean ascending) {
    // sorting is not supported yet by keycloak admin API
    log.debug("getUsers.enter; got offset: {}, limit: {}", offset, limit);
    List<UserProfile> profiles = userDao.search(null, offset, limit);
    log.debug("getUsers.exit; returning: {}", profiles.size());
    // TODO: set total count
    return ResponseEntity.ok(new UserProfiles(0, profiles));
  }

  /**
   * Service method for the  get roles of the user.
   *
   * @param userId user id of the user(required)
   * @return User roles (status code 200)
   *        or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *        or Forbidden. The user does not have the permission to execute this request. (status code 403)
   *        or The specified resource was not found (status code 404)
   *        or May contain hints how to solve the error or indicate what went wrong at the server. Must not outline
   *        any information about the internal structure of the server. (status code 500)
   */
  @Override
  public ResponseEntity<List<String>> getUserRoles(String userId) {
    log.debug("getUserRoles.enter; got userId: {}", userId);
    UserProfile profile = userDao.select(userId);
    log.debug("getUserRoles.exit; returning: {}", profile.getRoleIds());
    return ResponseEntity.ok(profile.getRoleIds());
  }

  /**
   * Service method for the update roles of the user.
   *
   * @param userId user id of the user (required)
   * @param roles  List of roles which should be assigned to the user
   * @return All assigned roles of the user (status code 200)
   *        or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *        or Forbidden. The user does not have the permission to execute this request. (status code 403)
   *        or The specified resource was not found (status code 404)
   *        or May contain hints how to solve the error or indicate what went wrong at the server. Must not outline
   *        any information about the internal structure of the server. (status code 500)
   */
  @Override
  public ResponseEntity<UserProfile> updateUserRoles(String userId, List<String> roles) {
    log.debug("updateUserRoles.enter; got userId: {}, roles: {}", userId, roles);
    UserProfile profile = userDao.updateRoles(userId, roles);
    log.debug("updateUserRoles.exit; returning: {}", profile);
    return ResponseEntity.ok(profile);
  }
}
