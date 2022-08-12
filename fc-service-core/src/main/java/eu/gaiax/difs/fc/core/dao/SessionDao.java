package eu.gaiax.difs.fc.core.dao;

import eu.gaiax.difs.fc.api.generated.model.Session;

/**
 * Session repository interface.
 */
public interface SessionDao {
    
    Session select(String id);
    
    void delete(String id);

}
