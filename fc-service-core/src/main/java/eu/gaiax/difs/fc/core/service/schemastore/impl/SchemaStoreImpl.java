package eu.gaiax.difs.fc.core.service.schemastore.impl;

import com.google.common.base.Strings;
import eu.gaiax.difs.fc.core.exception.ConflictException;
import eu.gaiax.difs.fc.core.exception.NotFoundException;
import eu.gaiax.difs.fc.core.exception.ServerException;
import eu.gaiax.difs.fc.core.exception.VerificationException;
import eu.gaiax.difs.fc.core.pojo.ContentAccessor;
import eu.gaiax.difs.fc.core.pojo.ContentAccessorDirect;
import eu.gaiax.difs.fc.core.pojo.ContentAccessorFile;
import eu.gaiax.difs.fc.core.service.filestore.FileStore;
import eu.gaiax.difs.fc.core.service.schemastore.SchemaStore;
import eu.gaiax.difs.fc.core.util.HashUtils;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.persistence.EntityExistsException;
import javax.persistence.LockModeType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 */
@Slf4j
@Component
@Transactional
public class SchemaStoreImpl implements SchemaStore {

  @Autowired
  @Qualifier("schemaFileStore")
  private FileStore fileStore;

  @Autowired
  private SessionFactory sessionFactory;

  private static final Map<SchemaType, ContentAccessor> COMPOSITE_SCHEMAS = new ConcurrentHashMap<>();

  @Override
  public void initializeDefaultSchemas() {
    Session currentSession = sessionFactory.getCurrentSession();
    long count = currentSession.createQuery("select count(s) from SchemaRecord s", Long.class).getSingleResult();
    if (count > 0) {
      log.info("Default schemas already initialized.");
      return;
    }
    addSchemasFromDirectory("defaultschema/ontology");
    addSchemasFromDirectory("defaultschema/shacl");
    currentSession.flush();
    count = currentSession.createQuery("select count(s) from SchemaRecord s", Long.class).getSingleResult();
    log.info("Added {} default schemas", count);
  }

  private void addSchemasFromDirectory(String path) {
    URL url = getClass().getClassLoader().getResource(path);
    String str = URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8);
    File ontologyDir = new File(str);
    for (File ontology : ontologyDir.listFiles()) {
      addSchema(new ContentAccessorFile(ontology));
    }
  }

  /**
   * Analyse the given schema character content.
   *
   * @param schema The schema to analyse.
   * @return The analysis results.
   */
  private SchemaAnalysisResult analyseSchema(ContentAccessor schema) {
    SchemaAnalysisResult result = new SchemaAnalysisResult();
    List<String> extractedUrls = new ArrayList<>();
    /**
     * TODO FIT: add code that
     *
     * - Analyses the type of the schema
     *
     * - Checks if the schema is valid
     *
     * - Extracts the Identifier from the schema if possible
     *
     * - Extracts the URLs of all entities defined in the schema.
     */
    result.setSchemaType(SchemaType.SHAPE);
    result.setValid(true);
    result.setExtractedUrls(extractedUrls);
    result.setExtractedId(null);
    return result;
  }

  private ContentAccessor createCompositeSchema(SchemaType type) {
    log.debug("createCompositeSchema.enter; got type: {}", type);
    StringBuilder contentBuilder = new StringBuilder();

    /**
     * TODO FIT: Add code that
     *
     * - Fetches the existing schema IDs using getSchemaList
     *
     * - Iterates over these schemas and adds each schema to the composite
     * schema.
     *
     * Example structure listed below.
     */
    Map<SchemaType, List<String>> schemaList = getSchemaList();
    log.debug("createCompositeSchema; got schemaList: {}", schemaList);

    List<String> schemas = schemaList.get(type);
    if (schemas != null) {
      for (String schemaId : schemas) {
        ContentAccessor schemaContent = getSchema(schemaId);
        // Add schema to union schema...
      }
    }

    log.debug("createCompositeSchema.exit; returning: {}", contentBuilder.length());
    try {
      final String compositeSchemaName = "CompositeSchema" + type.name();
      fileStore.replaceFile(compositeSchemaName, new ContentAccessorDirect(contentBuilder.toString()));
      return fileStore.readFile(compositeSchemaName);
    } catch (IOException ex) {
      log.error("Failed to store composite schema", ex);
      return new ContentAccessorDirect(contentBuilder.toString());
    }
  }

  @Override
  public boolean verifySchema(ContentAccessor schema) {
    SchemaAnalysisResult result = analyseSchema(schema);
    return result.isValid();
  }

  @Override
  public String addSchema(ContentAccessor schema) {
    SchemaAnalysisResult result = analyseSchema(schema);
    if (!result.isValid()) {
      throw new VerificationException("Schema is not valid.");
    }
    String schemaId = result.getExtractedId();
    String nameHash;
    if (Strings.isNullOrEmpty(schemaId)) {
      nameHash = HashUtils.calculateSha256AsHex(schema.getContentAsString());
      schemaId = nameHash;
      result.setExtractedId(schemaId);
    } else {
      nameHash = HashUtils.calculateSha256AsHex(schemaId);
    }

    Session currentSession = sessionFactory.getCurrentSession();

    // Check duplicate terms
    List<SchemaTerm> redefines = currentSession.byMultipleIds(SchemaTerm.class)
        .multiLoad(result.getExtractedUrls());
    if (!redefines.isEmpty()) {
      throw new VerificationException("Schema redefines " + redefines.size() + " terms. First: " + redefines.get(0));
    }

    SchemaRecord newRecord = new SchemaRecord(schemaId, nameHash, result.getSchemaType(), schema.getContentAsString(), result.getExtractedUrls());
    try {
      currentSession.persist(newRecord);
    } catch (EntityExistsException ex) {
      throw new ConflictException("A schema with id " + schemaId + " already exists.");
    }

    try {
      fileStore.storeFile(nameHash, schema);
    } catch (IOException ex) {
      throw new RuntimeException("Failed to store schema file", ex);
    }

    currentSession.flush();
    COMPOSITE_SCHEMAS.remove(result.getSchemaType());
    // TODO: Re-Validate SDs in a separate thread.
    return schemaId;
  }

  @Override
  public void updateSchema(String identifier, ContentAccessor schema) {
    SchemaAnalysisResult result = analyseSchema(schema);
    String schemaId = result.getExtractedId();
    if (!result.isValid()) {
      throw new VerificationException("Schema is not valid.");
    }
    if (schemaId != null && !schemaId.equals(identifier)) {
      throw new IllegalArgumentException("Given schema does not have the same Identifier as the old schema: " + identifier + " <> " + schemaId);
    }
    Session currentSession = sessionFactory.getCurrentSession();
    Transaction transaction = currentSession.getTransaction();

    // Find and lock record.
    SchemaRecord existing = currentSession.find(SchemaRecord.class, identifier, LockModeType.PESSIMISTIC_WRITE);

    if (existing == null) {
      currentSession.clear();
      throw new NotFoundException("Schema with id " + identifier + " was not found");
    }

    // Remove old terms
    CriteriaBuilder cb = currentSession.getCriteriaBuilder();
    CriteriaDelete<SchemaTerm> delete = cb.createCriteriaDelete(SchemaTerm.class);
    delete.where(cb.equal(delete.from(SchemaTerm.class).get("schemaId"), identifier));
    currentSession.createQuery(delete).executeUpdate();

    // Check duplicate terms
    List<SchemaTerm> redefines = currentSession.byMultipleIds(SchemaTerm.class)
        .multiLoad(result.getExtractedUrls());
    if (!redefines.isEmpty()) {
      currentSession.clear();
      throw new ConflictException("Schema redefines " + redefines.size() + " terms. First: " + redefines.get(0));
    }

    existing.setUpdateTime(Instant.now());
    existing.replaceTerms(result.getExtractedUrls());
    existing.setContent(schema.getContentAsString());
    try {
      currentSession.update(existing);
    } catch (EntityExistsException ex) {
      transaction.rollback();
      throw new ConflictException("Schema redefines terms.");
    }
    try {
      //Update schema file
      fileStore.replaceFile(existing.getNameHash(), schema);
    } catch (IOException ex) {
      transaction.rollback();
      throw new RuntimeException("Failed to store schema file", ex);
    }
    currentSession.flush();
    COMPOSITE_SCHEMAS.remove(result.getSchemaType());

    // TODO: Re-Validate SDs in a separate thread.
  }

  @Override
  public void deleteSchema(String identifier) {
    Session currentSession = sessionFactory.getCurrentSession();
    // Find and lock record.
    SchemaRecord existing = currentSession.find(SchemaRecord.class, identifier, LockModeType.PESSIMISTIC_WRITE);
    if (existing == null) {
      throw new NotFoundException("Schema with id " + identifier + " was not found");
    }
    currentSession.delete(existing);
    try {
      fileStore.deleteFile(existing.getNameHash());
    } catch (IOException ex) {
      currentSession.clear();
      throw new ServerException("Failed to delete schema from file store. (" + ex.getMessage() + ")");
    }
    currentSession.flush();
    COMPOSITE_SCHEMAS.remove(existing.getType());
  }

  @Override
  public Map<SchemaType, List<String>> getSchemaList() {
    Session currentSession = sessionFactory.getCurrentSession();
    Map<SchemaType, List<String>> result = new HashMap<>();
    currentSession.createQuery("select new eu.gaiax.difs.fc.core.service.schemastore.impl.SchemaTypeIdPair(s.type, s.schemaId) from SchemaRecord s", SchemaTypeIdPair.class)
        .stream().forEach(p -> result.computeIfAbsent(p.getType(), t -> new ArrayList<>()).add(p.getSchemaId()));
    return result;
  }

  @Override
  public ContentAccessor getSchema(String identifier) {
    Session currentSession = sessionFactory.getCurrentSession();
    // Find and lock record.
    SchemaRecord existing = currentSession.find(SchemaRecord.class, identifier);
    if (existing == null) {
      throw new NotFoundException("Schema with id " + identifier + " was not found");
    }
    try {
      return fileStore.readFile(existing.getNameHash());

    } catch (IOException ex) {
      throw new ServerException("File for Schema " + identifier + " does not exist.");
    }
  }

  @Override
  public Map<SchemaType, List<String>> getSchemasForTerm(String entity) {
    Session currentSession = sessionFactory.getCurrentSession();
    Map<SchemaType, List<String>> result = new HashMap<>();
    currentSession.createQuery("select new eu.gaiax.difs.fc.core.service.schemastore.impl.SchemaTypeIdPair(s.type, s.schemaId) from SchemaRecord s join s.terms as t where t.term=?1", SchemaTypeIdPair.class)
        .setParameter(1, entity)
        .stream().forEach(p -> result.computeIfAbsent(p.getType(), t -> new ArrayList<>()).add(p.getSchemaId()));
    return result;
  }

  @Override
  public ContentAccessor getCompositeSchema(SchemaType type) {
    return COMPOSITE_SCHEMAS.computeIfAbsent(type, t -> createCompositeSchema(t));
  }

}
