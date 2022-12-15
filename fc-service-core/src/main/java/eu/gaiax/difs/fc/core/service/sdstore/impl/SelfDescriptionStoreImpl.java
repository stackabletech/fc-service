package eu.gaiax.difs.fc.core.service.sdstore.impl;

import eu.gaiax.difs.fc.core.exception.ServerException;
import eu.gaiax.difs.fc.core.pojo.ContentAccessor;
import eu.gaiax.difs.fc.core.pojo.PaginatedResults;
import eu.gaiax.difs.fc.core.service.graphdb.GraphStore;
import eu.gaiax.difs.fc.core.service.filestore.FileStore;
import eu.gaiax.difs.fc.api.generated.model.SelfDescriptionStatus;
import eu.gaiax.difs.fc.core.exception.ConflictException;
import eu.gaiax.difs.fc.core.exception.NotFoundException;
import eu.gaiax.difs.fc.core.pojo.SdFilter;
import eu.gaiax.difs.fc.core.pojo.SelfDescriptionMetadata;
import eu.gaiax.difs.fc.core.pojo.Validator;
import eu.gaiax.difs.fc.core.pojo.VerificationResult;
import eu.gaiax.difs.fc.core.service.sdstore.SelfDescriptionStore;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityExistsException;
import javax.persistence.LockModeType;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FileExistsException;
import org.apache.commons.lang3.mutable.MutableInt;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.jpa.TypedParameterValue;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.vladmihalcea.hibernate.type.array.StringArrayType;
import org.hibernate.Transaction;

/**
 * File system based implementation of the self-description store interface.
 *
 * @author hylke
 * @author j_reuter
 */
@Slf4j
@Component
@Transactional
public class SelfDescriptionStoreImpl implements SelfDescriptionStore {

  /**
   * The fileStore to use.
   */
  @Autowired
  @Qualifier("sdFileStore")
  private FileStore fileStore;

  @Autowired
  private SessionFactory sessionFactory;

  @Autowired
  private GraphStore graphDb;

  @Override
  public ContentAccessor getSDFileByHash(final String hash) {
    try {
      return fileStore.readFile(hash);
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
    final Session currentSession = sessionFactory.getCurrentSession();
    final SdMetaRecord sdmRecord = currentSession.byId(SdMetaRecord.class).load(hash);
    checkNonNull(sdmRecord, hash);
    currentSession.detach(sdmRecord);
    // FIXME: Inconsistent exception handling: IOException will be caught and null
    //  returned; but NotFoundException will be propagated to caller.
    final ContentAccessor sdFile = getSDFileByHash(hash);
    if (sdFile == null) {
      throw new ServerException("Self-Description with hash " + hash + " not found in the file storage.");
    }
    sdmRecord.setSelfDescription(sdFile);
    return sdmRecord;
  }

  private static class FilterQueryBuilder {

    private final Session currentSession;
    private final List<Clause> clauses;
    private final boolean fullMeta;
    private int firstResult;
    private int maxResults;

    private static class Clause {

      private static final char PLACEHOLDER_SYMBOL = '?';
      private final String formalParameterName;
      private final Object actualParameter;
      private final String templateInstance;

      private Clause(final String template, final String formalParameterName, final Object actualParameter) {
        if (actualParameter == null) {
          throw new IllegalArgumentException("value for parameter " + formalParameterName + " is null");
        }
        this.formalParameterName = formalParameterName;
        this.actualParameter = actualParameter;
        final int placeholderPosition = template.indexOf(PLACEHOLDER_SYMBOL);
        if (placeholderPosition < 0) {
          throw new IllegalArgumentException("missing parameter placeholder '" + PLACEHOLDER_SYMBOL + "' in template: " + template);
        }
        templateInstance = template.substring(0, placeholderPosition) + ":" + formalParameterName + template.substring(placeholderPosition + 1);
        if (templateInstance.indexOf(PLACEHOLDER_SYMBOL, placeholderPosition + 1) != -1) {
          throw new IllegalArgumentException("multiple parameter placeholders '" + PLACEHOLDER_SYMBOL + "' in template: " + template);
        }
      }
    }

    private FilterQueryBuilder(final Session currentSession, final boolean fullMeta) {
      this.currentSession = currentSession;
      this.fullMeta = fullMeta;
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

    private String buildCountQuery() {
      final StringBuilder hqlQuery = new StringBuilder("select count(*) from sdfiles where 1=1");
      for (Clause cls : clauses) {
        hqlQuery.append(" and (");
        hqlQuery.append(cls.templateInstance);
        hqlQuery.append(")");
      }
      log.debug("buildCountQuery; Query: {}", hqlQuery.toString());
      return hqlQuery.toString();
    }

    private String buildHqlQuery() {
      final StringBuilder hqlQuery;
      if (fullMeta) {
        hqlQuery = new StringBuilder("select sdhash, subjectid, status, issuer, uploadtime, statustime, expirationtime, validators, null as content");
      } else {
        hqlQuery = new StringBuilder("select sdhash, null as subjectid, null as status, null as issuer, null as uploadtime, null as statustime, null as expirationtime, null as validators, null as content");
      }
      hqlQuery.append(" from sdfiles");
      hqlQuery.append(" where 1=1");
      for (Clause cls : clauses) {
        hqlQuery.append(" and (");
        hqlQuery.append(cls.templateInstance);
        hqlQuery.append(")");
      }
      hqlQuery.append(" ").append("order by statustime desc, sdhash");
      log.debug("buildHqlQuery; Query: {}", hqlQuery.toString());
      return hqlQuery.toString();
    }

    private Query<SdMetaRecord> createQuery() {
      final String hqlQuery = buildHqlQuery();
      Query<SdMetaRecord> query = currentSession.createNativeQuery(hqlQuery, SdMetaRecord.class);
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

    private Query<?> createCountQuery() {
      final String hqlQuery = buildCountQuery();
      final Query<?> query = currentSession.createNativeQuery(hqlQuery);
      clauses.stream().forEach(clause -> query.setParameter(clause.formalParameterName, clause.actualParameter));
      return query;
    }
  }

  @Override
  public PaginatedResults<SelfDescriptionMetadata> getByFilter(final SdFilter filter, final boolean withMeta, final boolean withContent) {
    log.debug("getByFilter.enter; got filter: {}, withMeta: {}, withContent: {}", filter, withMeta, withContent);
    final Session currentSession = sessionFactory.getCurrentSession();
    final FilterQueryBuilder queryBuilder = new FilterQueryBuilder(currentSession, withMeta);

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

    final List<String> issuers = filter.getIssuers();
    if (issuers != null) {
      queryBuilder.addClause("issuer in (?)", "issuers", issuers);
    }

    final List<String> validators = filter.getValidators();
    if (validators != null) {
      queryBuilder.addClause("validators && cast(? as varchar[])", "validators",
          new TypedParameterValue(StringArrayType.INSTANCE, validators));
    }

    final List<SelfDescriptionStatus> statuses = filter.getStatuses();
    if (statuses != null) {
      List<Integer> ords = statuses.stream().map(s -> s.ordinal()).collect(Collectors.toList());
      queryBuilder.addClause("status in (?)", "statuses", ords);
    }

    final List<String> subjectIds = filter.getIds();
    if (subjectIds != null) {
      queryBuilder.addClause("subjectid in (?)", "subjectIds", subjectIds);
    }

    final List<String> hashes = filter.getHashes();
    if (hashes != null) {
      queryBuilder.addClause("sdhash in (?)", "hashes", hashes);
    }

    BigInteger totalCount = (BigInteger) queryBuilder.createCountQuery().uniqueResult();

    queryBuilder.setFirstResult(filter.getOffset());
    queryBuilder.setMaxResults(filter.getLimit());
    Stream<SdMetaRecord> sdStream = queryBuilder.createQuery().stream()
        .peek(t -> currentSession.detach(t));
    if (withContent) {
      sdStream = sdStream.peek(t -> t.setSelfDescription(getSDFileByHash(t.getSdHash())));
    }
    final List<SelfDescriptionMetadata> sdList = sdStream.collect(Collectors.toList());
    log.debug("getByFilter.exit; returning records: {}, total: {}", sdList.size(), totalCount.longValue());
    return new PaginatedResults<>(totalCount.longValue(), sdList);
  }

  @Override
  public void storeSelfDescription(final SelfDescriptionMetadata sdMetadata, final VerificationResult verificationResult) {
    if (verificationResult == null) {
      throw new IllegalArgumentException("verification result must not be null");
    }
    final Session currentSession = sessionFactory.getCurrentSession();

    final SdMetaRecord existingSd = currentSession
        .createQuery("select sd from SdMetaRecord sd where sd.subjectId=?1 and sd.status=?2", SdMetaRecord.class)
        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
        .setTimeout(1)
        .setParameter(1, sdMetadata.getId())
        .setParameter(2, SelfDescriptionStatus.ACTIVE)
        .uniqueResult();

    final SdMetaRecord sdmRecord = new SdMetaRecord(sdMetadata);
    final List<Validator> validators = verificationResult.getValidators();
    final boolean registerValidators = sdMetadata.getValidatorDids() == null || sdMetadata.getValidatorDids().isEmpty();
    if (validators != null) {
      Instant expDateFirst = null;
      for (Validator validator : validators) {
        Instant expDate = validator.getExpirationDate();
        if (expDateFirst == null || expDate.isBefore(expDateFirst)) {
          expDateFirst = expDate;
        }
        if (registerValidators) {
          sdmRecord.addValidatorDidsItem(validator.getDidURI());
        }
      }
      sdmRecord.setExpirationTime(expDateFirst);
    }

    if (existingSd != null) {
      existingSd.setStatus(SelfDescriptionStatus.DEPRECATED);
      existingSd.setStatusDatetime(Instant.now());
      currentSession.update(existingSd);
      currentSession.flush();
//      graphDb.deleteClaims(existingSd.getSubjectId());
    }
    try {
//      graphDb.addClaims(verificationResult.getClaims(), sdmRecord.getSubjectId());
      currentSession.persist(sdmRecord);
    } catch (final EntityExistsException exc) {
      log.error("storeSelfDescription.error 1: {}", sdMetadata.getSdHash(), exc);
      final String message = String.format("self-description with hash %s already exists", sdMetadata.getSdHash());
      throw new ConflictException(message);
    } catch (Exception ex) {
      log.error("storeSelfDescription.error 2", ex);
      throw ex;
    }

    try {
      fileStore.storeFile(sdMetadata.getSdHash(), sdMetadata.getSelfDescription());
      currentSession.flush();
    } catch (FileExistsException e) {
      throw new ConflictException("The SD file with the hash " + sdMetadata.getSdHash() + " already exists in the file storage.", e);
    } catch (final IOException exc) {
      throw new ServerException("Error while adding SD to file storage: " + exc.getMessage());
    } catch (Exception ex) {
      log.error("storeSelfDescription.error 3", ex);
      throw ex;
    }

    if (existingSd != null) {
      graphDb.deleteClaims(existingSd.getSubjectId());
    }
    graphDb.addClaims(verificationResult.getClaims(), sdmRecord.getSubjectId());
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
    sdmRecord.setStatusDatetime(Instant.now());
    currentSession.update(sdmRecord);

    graphDb.deleteClaims(sdmRecord.getSubjectId());
    currentSession.flush();
  }

  @Override
  public void deleteSelfDescription(final String hash) {
    final Session currentSession = sessionFactory.getCurrentSession();
    // Get a lock on the record.
    final SdMetaRecord sdmRecord = currentSession.find(SdMetaRecord.class, hash);
    checkNonNull(sdmRecord, hash);
    final SelfDescriptionStatus status = sdmRecord.getStatus();
    currentSession.delete(sdmRecord);
    currentSession.flush();
    try {
      fileStore.deleteFile(hash);
    } catch (final FileNotFoundException exc) {
      log.info("no self-description file with hash {} found for deletion", hash);
    } catch (final IOException exc) {
      log.error("failed to delete self-description file with hash {}", hash, exc);
    }

    if (status == SelfDescriptionStatus.ACTIVE) {
      graphDb.deleteClaims(sdmRecord.getSubjectId());
    }
  }

  @Override
  public int invalidateSelfDescriptions() {
    final Session currentSession = sessionFactory.getCurrentSession();
    // A possible Performance optimisation may be required here to limit the number
    // of SDs that are expired in one run, to limit the size of the Transaction.
    Stream<SdMetaRecord> existingSds = currentSession
        .createQuery("select sd from SdMetaRecord sd where sd.expirationTime < ?1 and sd.status = ?2", SdMetaRecord.class)
        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
        .setTimeout(1)
        .setParameter(1, Instant.now())
        .setParameter(2, SelfDescriptionStatus.ACTIVE)
        .getResultStream();
    final MutableInt count = new MutableInt();
    existingSds.forEach((sd) -> {
      try {
        count.increment();
        changeLifeCycleStatus(sd.getSdHash(), SelfDescriptionStatus.EOL);
      } catch (ConflictException exc) {
        log.info("SD was set non-active before we could expire it. Hash: {}", sd.getSdHash());
      }
    });
    return count.intValue();
  }

  @Override
  public List<String> getActiveSdHashes(String afterHash, int count, int chunks, int chunkId) {
    final Session currentSession = sessionFactory.getCurrentSession();
    final Query<String> query;
    if (afterHash == null) {
      String select = "select sdhash from sdfiles where status = :status and abs(hashtext(sdhash) % :chunks) = :chunkid order by sdhash asc limit :limit";
      query = currentSession.createNativeQuery(select);
    } else {
      String select = "select sdhash from sdfiles where sdhash > :lastSdHash and status = :status and abs(hashtext(sdhash) % :chunks) = :chunkid order by sdhash asc limit :limit";
      query = currentSession.createNativeQuery(select);
      query.setParameter("lastSdHash", afterHash);
    }
    query.setParameter("status", SelfDescriptionStatus.ACTIVE.ordinal());
    query.setParameter("chunks", chunks);
    query.setParameter("chunkid", chunkId);
    query.setParameter("limit", count);
    final List<String> resultList = query.getResultList();
    return resultList;
  }

  @Override
  public void clear() {
    try ( Session session = sessionFactory.openSession()) {
      Transaction transaction = session.getTransaction();
      // Transaction is sometimes not active. For instance when called from an @AfterAll Test method
      if (transaction == null || !transaction.isActive()) {
        transaction = session.beginTransaction();
        session.createNativeQuery("delete from sdfiles").executeUpdate();
        transaction.commit();
      } else {
        session.createNativeQuery("delete from sdfiles").executeUpdate();
      }
    }
    try {
      fileStore.clearStorage();
    } catch (IOException ex) {
      log.error("SelfDescriptionStoreImpl: Exception while clearing FileStore: {}.", ex.getMessage());
    }
  }

}
