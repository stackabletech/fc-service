package eu.gaiax.difs.fc.core.service.sdstore.impl;

import eu.gaiax.difs.fc.core.pojo.ContentAccessor;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityExistsException;
import javax.persistence.LockModeType;
import lombok.extern.slf4j.Slf4j;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("SDStore")
@Slf4j
@Transactional
public class SelfDescriptionStoreImpl implements SelfDescriptionStore {

  private static final String ORDER_BY_HQL = "order by sd.statusTime desc, sd.sdHash";

  /**
   * The fileStore to use. TODO: figure out how to configure the implementation at
   * run time.
   */
  @Autowired
  private FileStoreImpl fileStore;

  @Autowired
  private SessionFactory sessionFactory;

  @Override
  public List<SelfDescriptionMetadata> getAllSelfDescriptions(int offset, int limit) {
    var resultList = sessionFactory
        .getCurrentSession()
        .createQuery("select sd from SdMetaRecord sd where sd.status=?1 " + ORDER_BY_HQL, SdMetaRecord.class)
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
      SdMetaRecord sdmdRecord = sessionFactory.getCurrentSession().byId(SdMetaRecord.class).load(hash);
      if (sdmdRecord == null) {
        throw new NotFoundException("There is no SelfDescription with hash " + hash);
      }
      SelfDescriptionMetadata sdmd = sdmdRecord.asSelfDescriptionMetadata();
      sdmd.setSelfDescription(fileStore.readFile(STORE_NAME, hash));
      return sdmd;
    } catch (IOException ex) {
      log.error("Failed to read SD file with registered hash {}", hash);
      return null;
    }
  }

  @Override
  public ContentAccessor getSDFileByHash(String hash) {
    try {
      return fileStore.readFile(STORE_NAME, hash);
    } catch (IOException ex) {
      log.error("Failed to read SD file with registered hash {}", hash);
      return null;
    }
  }

  private static class FilterQueryBuilder {
    private final SessionFactory sessionFactory;
    private final List<Clause> clauses;
    private int firstResult;
    private int maxResults;

    private static class Clause {
      private static final char PARAMETER_LEAD_IN = '?';
      private final String formalParameterName;
      private final Object actualParameter;
      private final String templateInstance;

      private Clause(final String template, final String formalParameterName, final Object actualParameter) {
        assert actualParameter != null : "value for parameter " + formalParameterName + " is null";
        this.formalParameterName = formalParameterName;
        this.actualParameter = actualParameter;
        final int parameterPosition = template.indexOf(PARAMETER_LEAD_IN);
        assert parameterPosition >= 0
            : "missing parameter placeholder '" + PARAMETER_LEAD_IN + "' in template: " + template;
        templateInstance = template.substring(0, parameterPosition) + ":" + formalParameterName
            + template.substring(parameterPosition + 1);
        assert templateInstance.indexOf(PARAMETER_LEAD_IN, parameterPosition + 1) == -1
            : "multiple parameter placeholders '" + PARAMETER_LEAD_IN + "' in template: " + template;
      }
    }

    private FilterQueryBuilder(final SessionFactory sessionFactory) {
      this.sessionFactory = sessionFactory;
      clauses = new ArrayList<>();
    }

    private void addClause(final String template, final String formalParameterName, final Object actualParameter) {
      clauses.add(new Clause(template, formalParameterName, actualParameter));
    }

    private void setFirstResult(final int firstResult) {
      this.firstResult = firstResult;
    }

    private void setMaxResults(final int maxResults) {
      this.maxResults = maxResults;
    }

    private String buildHqlQuery() {
      final StringBuilder hqlWhere = new StringBuilder();
      clauses.stream().forEach(clause -> {
        if (hqlWhere.length() > 0) {
          hqlWhere.append(" and");
        } else {
          hqlWhere.append(" where");
        }
        hqlWhere.append(" (");
        hqlWhere.append(clause.templateInstance);
        hqlWhere.append(")");
      });
      final String hqlQuery = "select sd from SdMetaRecord sd" + hqlWhere + " " + ORDER_BY_HQL;
      log.debug("hqlQuery=" + hqlQuery);
      return hqlQuery;
    }

    private Query<SdMetaRecord> createQuery() {
      final String hqlQuery = buildHqlQuery();
      final Session currentSession = sessionFactory.getCurrentSession();
      final Query<SdMetaRecord> query = currentSession.createQuery(hqlQuery, SdMetaRecord.class);
      clauses.stream().forEach(clause -> query.setParameter(clause.formalParameterName, clause.actualParameter));
      query.setFirstResult(firstResult);
      if (maxResults != 0) {
        /*
         * As specified, we model maxResults as int rather than as Integer, such that it
         * is not nullable. However, since "query.setMaxResults(0)" will make Hibernate
         * to return an empty result set rather than not imposing any limit, the only
         * way to keep the option not to apply any limit is to reserve a special value
         * of maxResults. We choose the value "0" for this purpose (since this is the
         * default initialization value for int variables in Java). That is, we call
         * "query.setMaxResults(maxResults)" only for values other than 0. For
         * maxResults < 0, Hibernate itself will already throw a runtime exception, such
         * that we do not need to handle negative values here.
         */
        query.setMaxResults(maxResults);
      }
      return query;
    }
  }

  @Override
  public List<SelfDescriptionMetadata> getByFilter(final SdFilter filterParams) {
    final FilterQueryBuilder queryBuilder = new FilterQueryBuilder(sessionFactory);

    final Instant uploadTimeStart = filterParams.getUploadTimeStart();
    if (uploadTimeStart != null) {
      queryBuilder.addClause("uploadtime >= ?", "uploadTimeStart", uploadTimeStart);
      final Instant uploadTimeEnd = filterParams.getUploadTimeEnd();
      queryBuilder.addClause("uploadtime <= ?", "uploadTimeEnd", uploadTimeEnd);
    }

    final Instant statusTimeStart = filterParams.getStatusTimeStart();
    if (statusTimeStart != null) {
      queryBuilder.addClause("statustime >= ?", "statusTimeStart", statusTimeStart);
      final Instant statusTimeEnd = filterParams.getStatusTimeEnd();
      queryBuilder.addClause("statustime <= ?", "statusTimeEnd", statusTimeEnd);
    }

    final String issuer = filterParams.getIssuer();
    if (issuer != null) {
      queryBuilder.addClause("issuer = ?", "issuer", issuer);
    }

    final String validator = filterParams.getValidator();
    if (validator != null) {
      queryBuilder.addClause("? in (select validator from ValidatorRecord where sdHash=sd.sdHash)", "validator",
          validator);
    }

    final SelfDescriptionStatus status = filterParams.getStatus();
    if (status != null) {
      queryBuilder.addClause("status = ?", "status", status);
    }

    final String id = filterParams.getId();
    if (id != null) {
      queryBuilder.addClause("subject = ?", "id", id);
    }

    final String hash = filterParams.getHash();
    if (hash != null) {
      queryBuilder.addClause("sdhash = ?", "hash", hash);
    }

    queryBuilder.setFirstResult(filterParams.getOffset());
    queryBuilder.setMaxResults(filterParams.getLimit());

    final Query<SdMetaRecord> query = queryBuilder.createQuery();
    final Stream<SdMetaRecord> stream = query.stream();
    try {
      return stream.map(SdMetaRecord::asSelfDescriptionMetadata).collect(Collectors.toList());
    } finally {
      stream.close();
    }
  }

  @Override
  public void storeSelfDescription(SelfDescriptionMetadata selfDescription, VerificationResult sdVerificationResults) {
    if (selfDescription.getSelfDescription() == null) {
      throw new IllegalArgumentException("Self-DescriptionMeta must have a content Self-Description");
    }
    if (selfDescription.getSdHash() == null) {
      selfDescription
          .setSdHash(HashUtils.calculateSha256AsHex(selfDescription.getSelfDescription().getContentAsString()));
    }

    final Session currentSession = sessionFactory.getCurrentSession();
    Transaction transaction = currentSession.getTransaction();

    SdMetaRecord existingSd = currentSession
        .createQuery("select sd from SdMetaRecord sd where sd.subject=?1 and sd.status=?2", SdMetaRecord.class)
        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
        .setTimeout(1)
        .setParameter(1, selfDescription.getId())
        .setParameter(2, SelfDescriptionStatus.ACTIVE)
        .uniqueResult();

    SdMetaRecord record = new SdMetaRecord(selfDescription);

    // TODO: Add validators/signatures to the record once the Signature class is
    // clarified.

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
      throw new ConflictException("Can not change status of SD with hash " + hash
          + " because status is not Active but '" + record.getStatus() + "'");
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

    // TODO: Claims from existing SD need to be removed from the GraphDB if existing
    // SD was active.

  }

}
