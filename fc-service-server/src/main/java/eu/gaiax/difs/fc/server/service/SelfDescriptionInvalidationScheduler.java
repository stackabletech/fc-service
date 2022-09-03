package eu.gaiax.difs.fc.server.service;

import eu.gaiax.difs.fc.core.service.sdstore.SelfDescriptionStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SelfDescriptionInvalidationScheduler {
  @Autowired
  private SelfDescriptionStore sdStore;

  @Scheduled(cron="${scheduler.sd.cron.expression}")
  public void scheduleSdInvalidationTask() {
    log.debug("scheduleSdInvalidationTask.enter; Launched scheduler to invalidate expired SDs in store.");
    int numberOfExpiredSd = sdStore.invalidateSelfDescriptions();
    log.debug("scheduleSdInvalidationTask.exit; {} expired SDs were found and invalidated.", numberOfExpiredSd);
  }
}