package eu.gaiax.difs.fc.core.dao;

import java.util.List;

import eu.gaiax.difs.fc.api.generated.model.User;
import eu.gaiax.difs.fc.api.generated.model.UserProfile;

/**
 * User repository interface.
 */
public interface UserDao {
  /**
   * Add user to catalogue repository.
   *
   * @param user User entity to be added {@link User}
   * @return UserProfile of created user
   */
  UserProfile create(User user);

  /**
   * Get a User Profile by id.
   *
   * @param userId identifier of user
   * @return Found user profile
   */
  UserProfile select(String userId);

  /**
   * Get list of the user profiles by filtered params.
   *
   * @param participantId Identifier of the participant
   * @param offset        The number of items to skip before starting to collect the result set
   * @param limit         The number of items to return
   * @return List of the user profiles.
   */
  List<UserProfile> search(String participantId, Integer offset, Integer limit);

  /**
   * Delete the user.
   *
   * @param userId Identifier of the user
   * @return UserProfile  of the user
   */
  UserProfile delete(String userId);

  /**
   * Update the user.
   *
   * @param userId Identifier of the user
   * @param user   User entity to be added {@link User}
   * @return UserProfile of the user
   */
  UserProfile update(String userId, User user);

  /**
   * Update user roles.
   *
   * @param userId Identifier of the user
   * @param roles  list of roles to be updated
   * @return Updated user profile
   */
  UserProfile updateRoles(String userId, List<String> roles);

  /**
   * Get all roles.
   *
   * @return List of all user roles
   */
  List<String> getAllRoles();
}
