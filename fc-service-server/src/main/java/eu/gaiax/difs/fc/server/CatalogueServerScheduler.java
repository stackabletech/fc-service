package eu.gaiax.difs.fc.server;

//import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Propagation;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.transaction.PlatformTransactionManager;
//import org.springframework.transaction.TransactionStatus;
//import org.springframework.transaction.support.DefaultTransactionDefinition;

import eu.gaiax.difs.fc.core.service.schemastore.SchemaStore;
import eu.gaiax.difs.fc.core.service.sdstore.SelfDescriptionStore;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CatalogueServerScheduler {
    
    @Autowired
    private SelfDescriptionStore sdStore;
    
    @Autowired
    private SchemaStore smStore;
    
    /**
     * Scheduler for invalidating expired SDs in store.
     */
    @Scheduled(cron = "${scheduler.sd.cron.expression}")
    public void scheduleSdInvalidationTask() {
      log.debug("scheduleSdInvalidationTask.enter; Launched scheduler to invalidate expired SDs in store.");
      int numberOfExpiredSd = sdStore.invalidateSelfDescriptions();
      log.debug("scheduleSdInvalidationTask.exit; {} expired SDs were found and invalidated.", numberOfExpiredSd);
    }
    
    @Scheduled(initialDelayString = "${scheduler.schema.init-delay}", fixedDelay = Long.MAX_VALUE) 
    public void scheduleSchemaInitialization() {
      smStore.initializeDefaultSchemas();
    }    

}
