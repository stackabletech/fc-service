package eu.gaiax.difs.fc.server.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import eu.gaiax.difs.fc.api.generated.model.Role;
import eu.gaiax.difs.fc.api.generated.model.User;
import eu.gaiax.difs.fc.server.dao.UserDao;
import eu.gaiax.difs.fc.server.generated.controller.UsersApiDelegate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class UsersService implements UsersApiDelegate {
    
    @Autowired
    private UserDao userDao;
    
    @Override
    public ResponseEntity<Void> addUser(User user) {
        log.debug("addUser.enter; got user: {}", user);
        userDao.create(user);
        log.debug("addUser.exit; success: {}", true);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }
    
    @Override
    public ResponseEntity<User> updateUser(String userId, User user) {
        return null;
    }
    
    @Override
    public ResponseEntity<User> deleteUser(String userId) {
        return null;
    }
    
    @Override
    public ResponseEntity<User> getUser(String userId) {
        return null;
    }
    
    @Override
    public ResponseEntity<List<User>> getUsers(Integer offset, Integer limit, String orderBy, Boolean ascending) {
        return null;
    }
    
    @Override
    public ResponseEntity<List<Role>> getUserRoles(String userId) {
        return null;
    }
    
    @Override
    public ResponseEntity<Role> updateUserRoles(String userId, List<Role> role) {
        return null;
    }

}
