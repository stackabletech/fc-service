package eu.gaiax.difs.fc.core.service.sdstore.impl;

import eu.gaiax.difs.fc.core.service.filestore.impl.FileStoreImpl;
import eu.gaiax.difs.fc.api.generated.model.SelfDescriptionStatus;
import eu.gaiax.difs.fc.core.exception.ConflictException;
import eu.gaiax.difs.fc.core.exception.NotFoundException;
import eu.gaiax.difs.fc.core.pojo.SdFilter;
import eu.gaiax.difs.fc.core.pojo.SelfDescriptionMetadata;
import eu.gaiax.difs.fc.core.pojo.VerificationResult;
import eu.gaiax.difs.fc.core.service.sdstore.SelfDescriptionStore;
import eu.gaiax.difs.fc.core.util.HashUtils;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityExistsException;
import javax.persistence.LockModeType;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@Transactional
public class SelfDescriptionStoreImpl implements SelfDescriptionStore {

  /**
   * The fileStore to use. TODO: figure out how to configure the implementation
   * at run time.
   */
  @Autowired
  private FileStoreImpl fileStore;

  @Autowired
  private SessionFactory sessionFactory;

  @Override
  public List<SelfDescriptionMetadata> getAllSelfDescriptions(int offset, int limit) {
    var resultList = sessionFactory.getCurrentSession().createQuery("select sd from SdMetaRecord sd where sd.status=?1 order by sd.statusTime desc, sd.sdHash", SdMetaRecord.class)
            .setParameter(1, SelfDescriptionStatus.ACTIVE)
            .setFirstResult(offset)
            .setMaxResults(limit)
            .getResultList();
    List<SelfDescriptionMetadata> result = new ArrayList<>(resultList.size());
    for (SdMetaRecord item : resultList) {
      result.add(item.asSelfDescriptionMetadata());
    }
    return result;
  }

  @Override
  public SelfDescriptionMetadata getByHash(String hash) {
    try {
      SdMetaRecord record = sessionFactory.getCurrentSession().byId(SdMetaRecord.class).load(hash);
      if (record == null) {
        throw new NotFoundException("There is no SelfDescription with hash " + hash);
      }
      SelfDescriptionMetadata sdmd = record.asSelfDescriptionMetadata();
      sdmd.setSelfDescription(fileStore.readFile(STORE_NAME, hash));
      return sdmd;
    } catch (IOException ex) {
      log.error("Failed to read SD file with registerd hash {}", hash);
      return null;
    }
  }

  @Override
  public List<SelfDescriptionMetadata> getByFilter(SdFilter filterParams) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void storeSelfDescription(SelfDescriptionMetadata selfDescription, VerificationResult sdVerificationResults) {
    if (selfDescription.getSelfDescription() == null) {
      throw new IllegalArgumentException("Self-DescriptionMeta must have a content Self-Description");
    }
    if (selfDescription.getSdHash() == null) {
      selfDescription.setSdHash(HashUtils.calculateSha256AsHex(selfDescription.getSelfDescription().getContentAsString()));
    }

    final Session currentSession = sessionFactory.getCurrentSession();
    Transaction transaction = currentSession.getTransaction();

    SdMetaRecord existingSd = currentSession.createQuery("select sd from SdMetaRecord sd where sd.subject=?1 and sd.status=?2", SdMetaRecord.class)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .setTimeout(1)
            .setParameter(1, selfDescription.getId())
            .setParameter(2, SelfDescriptionStatus.ACTIVE)
            .uniqueResult();

    SdMetaRecord record = new SdMetaRecord(selfDescription);

    // TODO: Add validators/signatures to the record once the Signature class is clarified.

    if (existingSd != null) {
      existingSd.setStatus(SelfDescriptionStatus.DEPRECATED);
      existingSd.setStatusTime(Instant.now());
      currentSession.update(existingSd);
      currentSession.flush();
    }
    try {
      currentSession.persist(record);
    } catch (EntityExistsException ex) {
      transaction.rollback();
      throw new ConflictException("An SD file with the hash " + selfDescription.getSdHash() + " already exists.");
    }
    try {
      fileStore.storeFile(STORE_NAME, selfDescription.getSdHash(), selfDescription.getSelfDescription());
    } catch (IOException ex) {
      transaction.rollback();
      throw new RuntimeException(ex);
    }

    if (existingSd != null) {
      existingSd.setStatus(SelfDescriptionStatus.DEPRECATED);
      existingSd.setStatusTime(Instant.now());

      // TODO: Claims from existing SD need to be removed from the GraphDB.

      currentSession.update(existingSd);
    }

    // TODO: Send sdVerificationResults.getClaims() to the GraphDB

    currentSession.flush();
  }

  @Override
  public void changeLifeCycleStatus(String hash, SelfDescriptionStatus targetStatus) {
    final Session currentSession = sessionFactory.getCurrentSession();
    // Get a lock on the record.
    // TODO: Investigate lock-less update.
    SdMetaRecord record = currentSession.find(SdMetaRecord.class, hash, LockModeType.PESSIMISTIC_WRITE);
    if (record == null) {
      throw new NotFoundException("There is no SelfDescription with hash " + hash);
    }
    if (record.getStatus() != SelfDescriptionStatus.ACTIVE) {
      throw new ConflictException("Can not change status of SD with hash " + hash + " because status is not Active but '" + record.getStatus() + "'");
    }
    record.setStatus(targetStatus);
    record.setStatusTime(Instant.now());
    currentSession.update(record);
    currentSession.flush();

    // TODO: Claims from existing SD need to be removed from the GraphDB.

  }

  @Override
  public void deleteSelfDescription(String hash) {
    final Session currentSession = sessionFactory.getCurrentSession();
    // Get a lock on the record.
    SdMetaRecord record = currentSession.find(SdMetaRecord.class, hash);
    if (record == null) {
      throw new NotFoundException("No Self-Description found with hash " + hash);
    }
    currentSession.delete(record);
    currentSession.flush();
    try {
      fileStore.deleteFile(STORE_NAME, hash);
    } catch (FileNotFoundException ex) {
      log.info("SD File with hash {} was not found for deletion.", hash);
    } catch (IOException ex) {
      log.error("Failed to delete self description file with hash {}", hash, ex);
    }

    // TODO: Claims from existing SD need to be removed from the GraphDB if existing SD was active.

  }

}
