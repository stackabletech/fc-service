package eu.gaiax.difs.fc.server.service;

import eu.gaiax.difs.fc.server.dao.UserDao;
import eu.gaiax.difs.fc.server.generated.controller.RolesApiDelegate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RolesService implements RolesApiDelegate {
    
    @Autowired
    private UserDao userDao;
        
    @Override
    public ResponseEntity<List<String>> getAllRoles() {
        List<String> roles = userDao.getAllRoles();
        log.debug("getAllRoles; returning {} roles", roles.size());
        return ResponseEntity.ok(roles);
    }

}
