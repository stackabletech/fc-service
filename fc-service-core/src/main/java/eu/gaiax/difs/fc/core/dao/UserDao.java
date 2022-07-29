package eu.gaiax.difs.fc.core.dao;

import java.util.List;

import eu.gaiax.difs.fc.api.generated.model.User;
import eu.gaiax.difs.fc.api.generated.model.UserProfile;

public interface UserDao {
    
    UserProfile create(User user);
    UserProfile select(String userId);
    List<UserProfile> search(String participantId, Integer offset, Integer limit);
    UserProfile delete(String userId);
    UserProfile update(String userId, User user);
    UserProfile updateRoles(String userId, List<String> roles);
    List<String> getAllRoles();

}
