package eu.gaiax.difs.fc.server.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import eu.gaiax.difs.fc.api.generated.model.Role;
import eu.gaiax.difs.fc.server.dao.UserDao;
import eu.gaiax.difs.fc.server.generated.controller.RolesApiDelegate;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RolesService implements RolesApiDelegate {
    
    @Autowired
    private UserDao userDao;
        
    @Override
    public ResponseEntity<List<Role>> getAllRoles() {
        List<Role> roles = userDao.getAllRoles();
        log.debug("getAllRoles; returning {} roles", roles.size());
        return ResponseEntity.ok(roles);
    }

}
