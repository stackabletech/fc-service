package eu.gaiax.difs.fc.server.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

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
    

}
