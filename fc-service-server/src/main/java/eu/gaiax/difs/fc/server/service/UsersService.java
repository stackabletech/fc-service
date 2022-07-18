package eu.gaiax.difs.fc.server.service;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import eu.gaiax.difs.fc.api.generated.model.Role;
import eu.gaiax.difs.fc.api.generated.model.User;
import eu.gaiax.difs.fc.api.generated.model.UserProfile;
import eu.gaiax.difs.fc.server.dao.UserDao;
import eu.gaiax.difs.fc.server.generated.controller.UsersApiDelegate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UsersService implements UsersApiDelegate {
    
    @Autowired
    private UserDao userDao;
    
    @Override
    public ResponseEntity<UserProfile> addUser(User user) {
        log.debug("addUser.enter; got user: {}", user);
        UserProfile profile = userDao.create(user);
        log.debug("addUser.exit; returning: {}", profile);
        return ResponseEntity.created(URI.create("/users/" + profile.getId())).body(profile);
    }
    
    @Override
    public ResponseEntity<UserProfile> updateUser(String userId, User user) {
        log.debug("updateUser.enter; got userId: {}", userId);
        UserProfile profile = userDao.update(userId, user);
        log.debug("updateUser.exit; returning: {}", profile);
        return ResponseEntity.ok(profile);
    }
    
    @Override
    public ResponseEntity<UserProfile> deleteUser(String userId) {
        log.debug("deleteUser.enter; got userId: {}", userId);
        UserProfile profile = userDao.delete(userId);
        log.debug("deleteUser.exit; returning: {}", profile);
        return ResponseEntity.ok(profile);
    }
    
    @Override
    public ResponseEntity<UserProfile> getUser(String userId) {
        log.debug("getUser.enter; got userId: {}", userId);
        UserProfile profile = userDao.select(userId);
        log.debug("getUser.exit; returning: {}", profile);
        return ResponseEntity.ok(profile);
    }
    
    @Override
    public ResponseEntity<List<UserProfile>> getUsers(Integer offset, Integer limit, String orderBy, Boolean ascending) {
        // sorting is not supported yet by keycloak admin API
        log.debug("getUsers.enter; got offset: {}, limit: {}", offset, limit);
        List<UserProfile> profiles = userDao.search(offset, limit);
        log.debug("getUser.exit; returning: {}", profiles.size());
        return ResponseEntity.ok(profiles);
    }
    
    @Override
    public ResponseEntity<List<Role>> getUserRoles(String userId) {
        log.debug("getUserRoles.enter; got userId: {}", userId);
        UserProfile profile = userDao.select(userId);
        log.debug("getUserRoles.exit; returning: {}", profile.getRoles());
        return ResponseEntity.ok(profile.getRoles());
    }
    
    @Override
    public ResponseEntity<UserProfile> updateUserRoles(String userId, List<Role> roles) {
        log.debug("updateUserRoles.enter; got userId: {}, roles: {}", userId, roles);
        UserProfile profile = userDao.updateRoles(userId, roles);
        log.debug("updateUserRoles.exit; returning: {}", profile);
        return ResponseEntity.ok(profile);
    }

}
