package eu.gaiax.difs.fc.core.service.sdstore.impl;

import eu.gaiax.difs.fc.core.exception.ServerException;
import eu.gaiax.difs.fc.core.pojo.ContentAccessor;
import eu.gaiax.difs.fc.core.service.filestore.impl.FileStoreImpl;
import eu.gaiax.difs.fc.api.generated.model.SelfDescriptionStatus;
import eu.gaiax.difs.fc.core.exception.ConflictException;
import eu.gaiax.difs.fc.core.exception.NotFoundException;
import eu.gaiax.difs.fc.core.exception.ServiceException;
import eu.gaiax.difs.fc.core.pojo.SdFilter;
import eu.gaiax.difs.fc.core.pojo.SelfDescriptionMetadata;
import eu.gaiax.difs.fc.core.pojo.VerificationResult;
import eu.gaiax.difs.fc.core.service.sdstore.SelfDescriptionStore;

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

import org.apache.commons.io.FileExistsException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * File system based implementation of the self-description store interface.
 *
 * @author hylke
 * @author j_reuter
 */
@Component
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
  public ContentAccessor getSDFileByHash(final String hash) {
    try {
      return fileStore.readFile(STORE_NAME, hash);
    } catch (final FileNotFoundException exc) {
      log.error("Error in getSDFileByHash method: ", exc);
      return null;
    } catch (final IOException exc) {
      log.error("failed reading self-description file with hash {}", hash);
      // TODO: Need correct error handling if we get something other than FileNotFoundException
      throw new ServerException("Failed reading self-description file with hash " + hash, exc);
    }
  }

  private void checkNonNull(final SdMetaRecord sdmRecord, final String hash) {
    if (sdmRecord == null) {
      final String message = String.format("no self-description found for hash %s", hash);
      throw new NotFoundException(message);
    }
  }

  @Override
  public SelfDescriptionMetadata getByHash(final String hash) {
    final SdMetaRecord sdmRecord = sessionFactory.getCurrentSession().byId(SdMetaRecord.class).load(hash);
    checkNonNull(sdmRecord, hash);
    // FIXME: Inconsistent exception handling: IOException will be caught and null
    // returned; but NotFoundException will be propagated to caller.
    final ContentAccessor sdFile = getSDFileByHash(hash);
    if (sdFile == null) {
      throw new ServerException("Self-Description with hash " + hash + " not found in the file storage.");
    }
    final SelfDescriptionMetadata sdmData = sdmRecord.asSelfDescriptionMetadata();
    sdmData.setSelfDescription(sdFile);
    return sdmData;
  }

  private static class FilterQueryBuilder {
    private final SessionFactory sessionFactory;
    private final List<Clause> clauses;
    private int firstResult;
    private int maxResults;

    private static class Clause {
      private static final char PLACEHOLDER_SYMBOL = '?';
      private final String formalParameterName;
      private final Object actualParameter;
      private final String templateInstance;

      private Clause(final String template, final String formalParameterName, final Object actualParameter) {
        assert actualParameter != null : "value for parameter " + formalParameterName + " is null";
        this.formalParameterName = formalParameterName;
        this.actualParameter = actualParameter;
        final int placeholderPosition = template.indexOf(PLACEHOLDER_SYMBOL);
        assert placeholderPosition >= 0
            : "missing parameter placeholder '" + PLACEHOLDER_SYMBOL + "' in template: " + template;
        templateInstance = template.substring(0, placeholderPosition) + ":" + formalParameterName
            + template.substring(placeholderPosition + 1);
        assert templateInstance.indexOf(PLACEHOLDER_SYMBOL, placeholderPosition + 1) == -1
            : "multiple parameter placeholders '" + PLACEHOLDER_SYMBOL + "' in template: " + template;
      }
    }

    private FilterQueryBuilder(final SessionFactory sessionFactory) {
      this.sessionFactory = sessionFactory;
      clauses = new ArrayList<>();
    }

    private void addClause(final String template, final String formalParameterName,
        final Object actualParameter) {
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
  public List<SelfDescriptionMetadata> getByFilter(final SdFilter filter) {
    final FilterQueryBuilder queryBuilder = new FilterQueryBuilder(sessionFactory);

    final Instant uploadTimeStart = filter.getUploadTimeStart();
    if (uploadTimeStart != null) {
      queryBuilder.addClause("uploadtime >= ?", "uploadTimeStart", uploadTimeStart);
      final Instant uploadTimeEnd = filter.getUploadTimeEnd();
      queryBuilder.addClause("uploadtime <= ?", "uploadTimeEnd", uploadTimeEnd);
    }

    final Instant statusTimeStart = filter.getStatusTimeStart();
    if (statusTimeStart != null) {
      queryBuilder.addClause("statustime >= ?", "statusTimeStart", statusTimeStart);
      final Instant statusTimeEnd = filter.getStatusTimeEnd();
      queryBuilder.addClause("statustime <= ?", "statusTimeEnd", statusTimeEnd);
    }

    final String issuer = filter.getIssuer();
    if (issuer != null) {
      queryBuilder.addClause("issuer = ?", "issuer", issuer);
    }

    final String validator = filter.getValidator();
    if (validator != null) {
      queryBuilder.addClause("? in (select validator from ValidatorRecord where sdHash=sd.sdHash)", "validator",
          validator);
    }

    final SelfDescriptionStatus status = filter.getStatus();
    if (status != null) {
      queryBuilder.addClause("status = ?", "status", status);
    }

    final String subjectId = filter.getId();
    if (subjectId != null) {
      queryBuilder.addClause("subjectid = ?", "subjectId", subjectId);
    }

    final String hash = filter.getHash();
    if (hash != null) {
      queryBuilder.addClause("sdhash = ?", "hash", hash);
    }

    queryBuilder.setFirstResult(filter.getOffset());
    queryBuilder.setMaxResults(filter.getLimit());

    final Query<SdMetaRecord> query = queryBuilder.createQuery();
    final Stream<SdMetaRecord> stream = query.stream();
    try {
      return stream.map(SdMetaRecord::asSelfDescriptionMetadata).collect(Collectors.toList());
    } finally {
      stream.close();
    }
  }

  @Override
  public void storeSelfDescription(final SelfDescriptionMetadata selfDescription,
      final VerificationResult sdVerificationResults) {
    final Session currentSession = sessionFactory.getCurrentSession();

    final SdMetaRecord existingSd = currentSession
        .createQuery("select sd from SdMetaRecord sd where sd.subjectId=?1 and sd.status=?2", SdMetaRecord.class)
        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
        .setTimeout(1)
        .setParameter(1, selfDescription.getId())
        .setParameter(2, SelfDescriptionStatus.ACTIVE)
        .uniqueResult();

    final SdMetaRecord sdmRecord = new SdMetaRecord(selfDescription);

    // TODO: Add validators/signatures to the record once the Signature class is
    // clarified.

    if (existingSd != null) {
      existingSd.setStatus(SelfDescriptionStatus.DEPRECATED);
      existingSd.setStatusTime(Instant.now());
      currentSession.update(existingSd);
      currentSession.flush();
    }
    try {
      currentSession.persist(sdmRecord);
    } catch (final EntityExistsException exc) {
      final String message = String.format("self-description file with hash %s already exists",
          selfDescription.getSdHash());
      throw new ConflictException(message);
    }
    try {
      fileStore.storeFile(STORE_NAME, selfDescription.getSdHash(), selfDescription.getSelfDescription());
    } catch (FileExistsException e) {
      throw new ConflictException("The SD file with the hash " + selfDescription.getSdHash() + " already exists in the file storage.", e);
    } catch (final IOException exc) {
      throw new ServerException("Error while adding SD to file storage: " + exc.getMessage());
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
  public void changeLifeCycleStatus(final String hash, final SelfDescriptionStatus targetStatus) {
    final Session currentSession = sessionFactory.getCurrentSession();
    // Get a lock on the record.
    // TODO: Investigate lock-less update.
    final SdMetaRecord sdmRecord = currentSession.find(SdMetaRecord.class, hash, LockModeType.PESSIMISTIC_WRITE);
    checkNonNull(sdmRecord, hash);
    final SelfDescriptionStatus status = sdmRecord.getStatus();
    if (status != SelfDescriptionStatus.ACTIVE) {
      final String message = String.format(
          "can not change status of self-description with hash %s: require status %s, but encountered status %s", hash,
          SelfDescriptionStatus.ACTIVE, status);
      throw new ConflictException(message);
    }
    sdmRecord.setStatus(targetStatus);
    sdmRecord.setStatusTime(Instant.now());
    currentSession.update(sdmRecord);
    currentSession.flush();

    // TODO: Claims from existing SD need to be removed from the GraphDB.

  }

  @Override
  public void deleteSelfDescription(final String hash) {
    final Session currentSession = sessionFactory.getCurrentSession();
    // Get a lock on the record.
    final SdMetaRecord sdmRecord = currentSession.find(SdMetaRecord.class, hash);
    checkNonNull(sdmRecord, hash);
    currentSession.delete(sdmRecord);
    currentSession.flush();
    try {
      fileStore.deleteFile(STORE_NAME, hash);
    } catch (final FileNotFoundException exc) {
      log.info("no self-description file with hash {} found for deletion", hash);
    } catch (final IOException exc) {
      log.error("failed to delete self-description file with hash {}", hash, exc);
    }

    // TODO: Claims from existing SD need to be removed from the GraphDB if existing
    // SD was active.

  }

  @Override
  public int invalidateSelfDescriptions() {
    // TODO: Implementation Required
    return 0;
  }
}
