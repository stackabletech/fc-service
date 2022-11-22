package eu.gaiax.difs.fc.core.service.verification.impl;

import eu.gaiax.difs.fc.api.generated.model.SelfDescriptionStatus;
import static eu.gaiax.difs.fc.core.util.TestUtil.getAccessor;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
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
import eu.gaiax.difs.fc.core.pojo.ContentAccessorFile;
import eu.gaiax.difs.fc.core.pojo.SelfDescriptionMetadata;
import eu.gaiax.difs.fc.core.pojo.VerificationResult;
import eu.gaiax.difs.fc.core.service.graphdb.impl.Neo4jGraphStore;
import eu.gaiax.difs.fc.core.service.schemastore.SchemaStore;
import eu.gaiax.difs.fc.core.service.schemastore.impl.SchemaStoreImpl;
import eu.gaiax.difs.fc.core.service.sdstore.SelfDescriptionStore;
import eu.gaiax.difs.fc.core.service.sdstore.impl.SelfDescriptionStoreImpl;
import eu.gaiax.difs.fc.core.service.validatorcache.impl.ValidatorCacheImpl;
import eu.gaiax.difs.fc.core.service.verification.VerificationService;
import eu.gaiax.difs.fc.testsupport.config.EmbeddedNeo4JConfig;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.neo4j.harness.Neo4j;
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
  private SessionFactory sessionFactory;

  @Autowired
  private Neo4j embeddedDatabaseServer;

  @AfterAll
  void closeNeo4j() {
    embeddedDatabaseServer.close();
  }

  @AfterEach
  public void storageSelfCleaning() throws IOException {
    revalidator.cleanup();
    sdStore.clear();
    schemaStore.clear();
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
    addSelfDescription("VerificationService/syntax/input.vp.jsonld");
    addSelfDescription("Claims-Extraction-Tests/providerTest.jsonld");
    revalidator.startValidating();
    int count = 0;
    while ((revalidator.isWorking() || !allChunksAfter(treshold)) && count < 10) {
      log.debug("Revalidator working...");
      Thread.sleep(1000);
      count++;
    }
    revalidator.cleanup();
    assertTrue(allChunksAfter(treshold), "All chunks should have been revalidated.");
  }

  @Test
  public void testRevalidatorAutostart() throws Exception {
    log.info("testRevalidatorAutostart");
    revalidator.setInstanceCount(1);
    revalidator.setBatchSize(500);
    revalidator.setWorkerCount(Runtime.getRuntime().availableProcessors());
    revalidator.setup();
    addSelfDescription("VerificationService/syntax/input.vp.jsonld");
    addSelfDescription("Claims-Extraction-Tests/providerTest.jsonld");
    //addSdsFromDirectory("GeneratedSds");
    Instant treshold = Instant.now();
    schemaStore.initializeDefaultSchemas();
    int count = 0;
    do {
      log.debug("Revalidator working...");
      Thread.sleep(1000);
      count++;
    } while ((revalidator.isWorking() || !allChunksAfter(treshold)) && count < 60);
    revalidator.cleanup();
    assertTrue(allChunksAfter(treshold), "All chunks should have been revalidated.");
  }

  private void addSdsFromDirectory(final String path) {
    long start = System.currentTimeMillis();
    URL url = getClass().getClassLoader().getResource(path);
    String str = URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8);
    File sdDir = new File(str);
    File[] files = sdDir.listFiles();
    Arrays.sort(files, (var o1, var o2) -> o1.getName().compareTo(o1.getName()));
    ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    for (File sd : sdDir.listFiles()) {
      service.submit(() -> addSelfDescription(new ContentAccessorFile(sd)));
    }
    service.shutdown();
    try {
      service.awaitTermination(2, TimeUnit.MINUTES);
    } catch (InterruptedException ex) {
      log.warn("Interrupted while waiting for SDs to be added.");
    }
    long time = System.currentTimeMillis() - start;
    Object count = sessionFactory.openSession()
        .createNativeQuery("select count (*) from sdfiles where status = :status")
        .setParameter("status", SelfDescriptionStatus.ACTIVE.ordinal())
        .getSingleResult();
    log.debug("added {} Self-Descriptions from {} in {}ms", count, path, time);
  }

  private boolean allChunksAfter(Instant treshold) {
    Object count;
    try ( Session session = sessionFactory.openSession()) {
      count = session.createNativeQuery("select count(chunkid) from revalidatorchunks where lastcheck < :treshold")
          .setParameter("treshold", treshold)
          .getSingleResult();
    }
    log.debug("Open Chunk Count: {}", count);
    return ((Number) count).intValue() == 0;
  }

  private int countChunks() {
    Object count;
    try ( Session session = sessionFactory.openSession()) {
      count = session.createNativeQuery("select count(chunkid) from revalidatorchunks")
          .getSingleResult();
    }
    log.debug("Chunk Count: {}", count);
    return ((Number) count).intValue();
  }

  private String addSelfDescription(String path) throws VerificationException, UnsupportedEncodingException, InterruptedException {
    log.debug("Adding SD from {}", path);
    final ContentAccessor content = getAccessor(path);
    return addSelfDescription(content);
  }

  public String addSelfDescription(final ContentAccessor content) throws VerificationException {
    try {
      final VerificationResult vr = verificationService.verifySelfDescription(content, true, true, false);
      final SelfDescriptionMetadata sd = new SelfDescriptionMetadata(content, vr);
      sdStore.storeSelfDescription(sd, vr);
      return sd.getSdHash();
    } catch (VerificationException exc) {
      log.debug("Failed to add: {}", exc.getMessage());
      return null;
    }
  }

}
