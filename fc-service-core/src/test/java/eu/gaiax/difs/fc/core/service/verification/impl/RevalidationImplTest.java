package eu.gaiax.difs.fc.core.service.verification.impl;

import static eu.gaiax.difs.fc.core.util.TestUtil.getAccessor;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import eu.gaiax.difs.fc.core.config.DatabaseConfig;
import eu.gaiax.difs.fc.core.config.FileStoreConfig;
import eu.gaiax.difs.fc.core.exception.VerificationException;
import eu.gaiax.difs.fc.core.pojo.ContentAccessor;
import eu.gaiax.difs.fc.core.pojo.SelfDescriptionMetadata;
import eu.gaiax.difs.fc.core.pojo.VerificationResult;
import eu.gaiax.difs.fc.core.service.filestore.FileStore;
import eu.gaiax.difs.fc.core.service.graphdb.impl.Neo4jGraphStore;
import eu.gaiax.difs.fc.core.service.schemastore.SchemaStore;
import eu.gaiax.difs.fc.core.service.schemastore.impl.SchemaStoreImpl;
import eu.gaiax.difs.fc.core.service.sdstore.SelfDescriptionStore;
import eu.gaiax.difs.fc.core.service.sdstore.impl.SelfDescriptionStoreImpl;
import eu.gaiax.difs.fc.core.service.validatorcache.impl.ValidatorCacheImpl;
import eu.gaiax.difs.fc.core.service.verification.VerificationService;
import eu.gaiax.difs.fc.testsupport.config.EmbeddedNeo4JConfig;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.neo4j.harness.Neo4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;

@Slf4j
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("tests-sdstore")
@ContextConfiguration(classes = {RevalidationImplTest.TestApplication.class, RevalidationServiceImpl.class, FileStoreConfig.class, Neo4jGraphStore.class,
  VerificationServiceImpl.class, SchemaStoreImpl.class, DatabaseConfig.class, ValidatorCacheImpl.class, SelfDescriptionStoreImpl.class})
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
@Import(EmbeddedNeo4JConfig.class)
public class RevalidationImplTest {

  @SpringBootApplication
  public static class TestApplication {

    public static void main(final String[] args) {
      SpringApplication.run(TestApplication.class, args);
    }
  }

  @Autowired
  private RevalidationServiceImpl revalidator;

  @Autowired
  private VerificationService verificationService;

  @Autowired
  private SelfDescriptionStore sdStore;

  @Autowired
  private SchemaStore schemaStore;

  @Autowired
  @Qualifier("sdFileStore")
  private FileStore fileStore;

  @Autowired
  private SessionFactory sessionFactory;

  @Autowired
  private Neo4j embeddedDatabaseServer;

  @AfterAll
  void closeNeo4j() {
    embeddedDatabaseServer.close();
  }

  @AfterEach
  public void storageSelfCleaning() throws IOException {
    Map<SchemaStore.SchemaType, List<String>> schemaList = schemaStore.getSchemaList();
    for (List<String> typeList : schemaList.values()) {
      for (String schema : typeList) {
        schemaStore.deleteSchema(schema);
      }
    }
    final Session session = sessionFactory.openSession();
    Transaction t = session.beginTransaction();
    session.createNativeQuery("delete from sdfiles").executeUpdate();
    t.commit();
    fileStore.clearStorage();
  }

  @Test
  void testRevalidatorSetup() throws Exception {
    log.info("testRevalidatorSetup");
    int origCount = revalidator.getInstanceCount();

    revalidator.setInstanceCount(5);
    revalidator.setup();
    Assertions.assertEquals(5, countChunks(), "Unexpected number of chunks created.");
    revalidator.cleanup();

    revalidator.setInstanceCount(2);
    revalidator.setup();
    Assertions.assertEquals(2, countChunks(), "Unexpected number of chunks created.");
    revalidator.cleanup();

    revalidator.setInstanceCount(3);
    revalidator.setup();
    Assertions.assertEquals(3, countChunks(), "Unexpected number of chunks created.");
    revalidator.cleanup();

    revalidator.setInstanceCount(10);
    revalidator.setup();
    Assertions.assertEquals(10, countChunks(), "Unexpected number of chunks created.");
    revalidator.cleanup();

    revalidator.setInstanceCount(origCount);
    revalidator.setup();
    Assertions.assertEquals(origCount, countChunks(), "Unexpected number of chunks created.");
    revalidator.cleanup();
  }

  @Test
  void testRevalidatorManualStart() throws Exception {
    log.info("testRevalidatorManualStart");
    schemaStore.initializeDefaultSchemas();
    revalidator.setup();
    Instant treshold = Instant.now();
    List<String> hashes = new ArrayList<>();
    hashes.add(addSelfDescription("VerificationService/syntax/input.vp.jsonld"));
    hashes.add(addSelfDescription("Query-Tests/serviceOfferingSD.jsonld"));

    revalidator.startValidating();
    int count = 0;
    while ((revalidator.isWorking() || !allChunksAfter(treshold)) && count < 10) {
      log.debug("Revalidator working...");
      Thread.sleep(1000);
      count++;
    }
    revalidator.cleanup();
    for (String hash : hashes) {
      sdStore.deleteSelfDescription(hash);
    }
    assertTrue(allChunksAfter(treshold), "All chunks should have been revalidated.");
  }

  @Test
  void testRevalidatorAutostart() throws Exception {
    log.info("testRevalidatorAutostart");
    revalidator.setup();
    List<String> hashes = new ArrayList<>();
    hashes.add(addSelfDescription("VerificationService/syntax/input.vp.jsonld"));
    hashes.add(addSelfDescription("Query-Tests/serviceOfferingSD.jsonld"));
    Instant treshold = Instant.now();
    schemaStore.initializeDefaultSchemas();
    int count = 0;
    while ((revalidator.isWorking() || !allChunksAfter(treshold)) && count < 10) {
      log.debug("Revalidator working...");
      Thread.sleep(1000);
      count++;
    }
    revalidator.cleanup();
    for (String hash : hashes) {
      sdStore.deleteSelfDescription(hash);
    }
    assertTrue(allChunksAfter(treshold), "All chunks should have been revalidated.");
  }

  private boolean allChunksAfter(Instant treshold) {
    Session session = sessionFactory.openSession();
    Object count = session.createNativeQuery("select count(chunkid) from revalidatorchunks where lastcheck < :treshold")
        .setParameter("treshold", treshold)
        .getSingleResult();
    log.debug("Open Chunk Count: {}", count);
    return ((Number) count).intValue() == 0;
  }

  private int countChunks() {
    Session session = sessionFactory.openSession();
    Object count = session.createNativeQuery("select count(chunkid) from revalidatorchunks")
        .getSingleResult();
    log.debug("Chunk Count: {}", count);
    return ((Number) count).intValue();
  }

  private String addSelfDescription(String path) throws VerificationException, UnsupportedEncodingException, InterruptedException {
    final ContentAccessor content = getAccessor(path);
    final VerificationResult vr = verificationService.verifySelfDescription(content, true, true, false);
    final SelfDescriptionMetadata sd = new SelfDescriptionMetadata(content, vr);
    sdStore.storeSelfDescription(sd, vr);
    return sd.getSdHash();
  }

}
