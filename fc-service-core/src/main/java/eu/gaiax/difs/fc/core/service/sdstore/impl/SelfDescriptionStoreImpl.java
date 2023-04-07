package eu.gaiax.difs.fc.core.service.sdstore.impl;

import eu.gaiax.difs.fc.core.pojo.ContentAccessor;
import eu.gaiax.difs.fc.core.pojo.PaginatedResults;
import eu.gaiax.difs.fc.core.service.graphdb.GraphStore;
import eu.gaiax.difs.fc.api.generated.model.SelfDescriptionStatus;
import eu.gaiax.difs.fc.core.exception.ConflictException;
import eu.gaiax.difs.fc.core.exception.NotFoundException;
import eu.gaiax.difs.fc.core.exception.ServerException;
import eu.gaiax.difs.fc.core.pojo.SdFilter;
import eu.gaiax.difs.fc.core.pojo.SelfDescriptionMetadata;
import eu.gaiax.difs.fc.core.pojo.Validator;
import eu.gaiax.difs.fc.core.pojo.VerificationResult;
import eu.gaiax.difs.fc.core.service.sdstore.SelfDescriptionStore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.LockModeType;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.mutable.MutableInt;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.vladmihalcea.hibernate.type.array.StringArrayType;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;

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

  @Autowired
  private SessionFactory sessionFactory;

  @Autowired
  private GraphStore graphDb;

  @Override
  public ContentAccessor getSDFileByHash(final String hash) {
    SdMetaRecord meta = (SdMetaRecord) getByHash(hash);
    return meta.getSelfDescription();
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
    return sdmRecord;
  }

  private static class FilterQueryBuilder {

    private final Session currentSession;
    private final List<Clause> clauses;
    private final boolean fullMeta;
    private final boolean returnContent;
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

    private FilterQueryBuilder(final Session currentSession, final boolean fullMeta, final boolean returnContent) {
      this.currentSession = currentSession;
      this.fullMeta = fullMeta;
      this.returnContent = returnContent;
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
        hqlQuery = new StringBuilder("select sdhash, subjectid, status, issuer, uploadtime, statustime, expirationtime, validators");
      } else {
        hqlQuery = new StringBuilder("select sdhash, null as subjectid, null as status, null as issuer, null as uploadtime, null as statustime, null as expirationtime, null as validators");
      }
      if (returnContent) {
        hqlQuery.append(", content");
      } else {
        hqlQuery.append(", null as content");  
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

    private Query<Long> createCountQuery() {
      final String hqlQuery = buildCountQuery();
      final Query<Long> query = currentSession.createNativeQuery(hqlQuery, Long.class);
      clauses.stream().forEach(clause -> query.setParameter(clause.formalParameterName, clause.actualParameter));
      return query;
    }
  }

  @Override
  public PaginatedResults<SelfDescriptionMetadata> getByFilter(final SdFilter filter, final boolean withMeta, final boolean withContent) {
    log.debug("getByFilter.enter; got filter: {}, withMeta: {}, withContent: {}", filter, withMeta, withContent);
    final Session currentSession = sessionFactory.getCurrentSession();
    final FilterQueryBuilder queryBuilder = new FilterQueryBuilder(currentSession, withMeta, withContent);

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
      queryBuilder.addClause("validators && cast(? as varchar[])", "validators", validators.toArray(new String[validators.size()]));
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

    Long totalCount = queryBuilder.createCountQuery().uniqueResult();

    queryBuilder.setFirstResult(filter.getOffset());
    queryBuilder.setMaxResults(filter.getLimit());
    Stream<SdMetaRecord> sdStream = queryBuilder.createQuery().stream()
        .peek(t -> currentSession.detach(t));
    final List<SelfDescriptionMetadata> sdList = sdStream.collect(Collectors.toList());
    log.debug("getByFilter.exit; returning records: {}, total: {}", sdList.size(), totalCount.longValue());
    return new PaginatedResults<>(totalCount.longValue(), sdList);
  }

  @Override
  public void storeSelfDescription(final SelfDescriptionMetadata sdMetadata, final VerificationResult verificationResult) {
    if (verificationResult == null) {
      throw new IllegalArgumentException("verification result must not be null");
    }
    log.trace("storeSelfDescription.enter; got meta: {}", sdMetadata);
    final Session currentSession = sessionFactory.getCurrentSession();
    String upsert = """
	  with u as (update sdfiles set status = ?1, statustime = ?2
      where subjectid = ?3 and status = 0 returning ?4 sdhash, subjectid),
      i as (insert into sdfiles(sdhash, subjectid, issuer, uploadtime, statustime, expirationtime, status, content, validators)
      values (?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13)
      returning sdhash)
      select u.subjectid from i full join u on u.sdhash = i.sdhash""";
    Query<String> q = currentSession.createNativeQuery(upsert, String.class); 
    q.setParameter(1, SelfDescriptionStatus.DEPRECATED.ordinal());
    q.setParameter(2, Instant.now());
    q.setParameter(3, sdMetadata.getId());
    q.setParameter(4, sdMetadata.getSdHash());
    q.setParameter(5, sdMetadata.getSdHash());
    q.setParameter(6, sdMetadata.getId());
    q.setParameter(7, sdMetadata.getIssuer());
    q.setParameter(8, sdMetadata.getUploadDatetime());
    q.setParameter(9, sdMetadata.getStatusDatetime());
    final List<Validator> validators = verificationResult.getValidators();
    Instant expDateFirst = null;
    List<String> vaDids = sdMetadata.getValidatorDids();
    if ((vaDids == null || vaDids.isEmpty()) && validators != null) {
      Set<String> dids = new HashSet<>(validators.size());
      for (Validator validator : validators) {
        Instant expDate = validator.getExpirationDate();
        if (expDateFirst == null || expDate.isBefore(expDateFirst)) {
          expDateFirst = expDate;
        }
        dids.add(validator.getDidURI());
      }
      if (!dids.isEmpty()) {
        vaDids = new ArrayList<>(dids);
      }
    }
    q.setParameter(10, expDateFirst); 
    q.setParameter(11, sdMetadata.getStatus().ordinal());
    q.setParameter(12, sdMetadata.getSelfDescription().getContentAsString());
    q.setParameter(13, (vaDids == null ? null : vaDids.toArray(new String[vaDids.size()])), StringArrayType.INSTANCE);

    List<String> result = null;
    try {
      result = q.list();
    } catch (ConstraintViolationException ex) {
      // TODO: check for "sdfiles_pkey"
      throw new ConflictException(String.format("self-description with hash %s already exists", sdMetadata.getSdHash()));
    } catch (Exception ex) {
      if (ex.getCause() instanceof ConstraintViolationException) {
        // TODO: check for "idx_sd_file_is_active"
        throw new ConflictException(String.format("active self-description with subjectId %s already exists", sdMetadata.getId()));
      }
      log.error("storeSelfDescription.error 2", ex);
      throw new ServerException(ex);
    }
    log.trace("storeSelfDescription; upsert result: {}", result);
    
    String subjectId = result.get(0);
    if (subjectId != null) {
      graphDb.deleteClaims(subjectId);
    }
    graphDb.addClaims(verificationResult.getClaims(), sdMetadata.getId());
    currentSession.flush();
  }

  @Override
  public void changeLifeCycleStatus(final String hash, final SelfDescriptionStatus targetStatus) {
    final Session currentSession = sessionFactory.getCurrentSession();
    Query<SubjectStatusRecord> q = currentSession.createNativeQuery("""
      with u as (update sdfiles set status = :status, statustime = :status_dt
      where sdhash = :hash and status = 0 returning sdhash, subjectid),
      o as (select sdhash, status from sdfiles where sdhash = :hash and status > 0)
      select u.subjectid, o.status from u full join o on o.sdhash = u.sdhash""", 
      "SubjectStatusRecordMapping", SubjectStatusRecord.class);
    q.setParameter("hash", hash);
    q.setParameter("status", targetStatus.ordinal());
    q.setParameter("status_dt", Instant.now());
    SubjectStatusRecord ssr = q.getSingleResultOrNull();
    log.debug("changeLifeCycleStatus; update result: {}", ssr);
    if (ssr == null) {
      throw new NotFoundException("no self-description found for hash " + hash);
    }

    if (ssr.subjectId() == null) {
      final String message = String.format(
            "can not change status of self-description with hash %s: require status %s, but encountered status %s", hash,
            SelfDescriptionStatus.ACTIVE, ssr.getSdStatus());
      throw new ConflictException(message);
    }
    graphDb.deleteClaims(ssr.subjectId());
    currentSession.flush();
  }

  @Override
  public void deleteSelfDescription(final String hash) {
    final Session currentSession = sessionFactory.getCurrentSession();
    Query<SubjectStatusRecord> q = currentSession.createNativeQuery("delete from sdfiles where sdhash = :hash returning subjectid, status",
      "SubjectStatusRecordMapping", SubjectStatusRecord.class);
    q.setParameter("hash", hash);
    SubjectStatusRecord ssr = q.getSingleResultOrNull();
    log.debug("deleteSelfDescription; delete result: {}", ssr);
    if (ssr == null) {
      throw new NotFoundException("no self-description found for hash " + hash);
    }
    
    if (ssr.getSdStatus() == SelfDescriptionStatus.ACTIVE) {
      graphDb.deleteClaims(ssr.subjectId());
    }
    currentSession.flush();
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
        changeLifeCycleStatus(sd.getSdHash(), SelfDescriptionStatus.EOL);
        count.increment();
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
      query = currentSession.createNativeQuery(select, String.class);
    } else {
      String select = "select sdhash from sdfiles where sdhash > :lastSdHash and status = :status and abs(hashtext(sdhash) % :chunks) = :chunkid order by sdhash asc limit :limit";
      query = currentSession.createNativeQuery(select, String.class);
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
    try (Session session = sessionFactory.openSession()) {
      Transaction transaction = session.getTransaction();
      // Transaction is sometimes not active. For instance when called from an @AfterAll Test method
      if (transaction == null || !transaction.isActive()) {
        transaction = session.beginTransaction();
        session.createMutationQuery("delete from SdMetaRecord").executeUpdate();
        transaction.commit();
      } else {
        session.createMutationQuery("delete from SdMetaRecord").executeUpdate();
      }
    }
  }

}
