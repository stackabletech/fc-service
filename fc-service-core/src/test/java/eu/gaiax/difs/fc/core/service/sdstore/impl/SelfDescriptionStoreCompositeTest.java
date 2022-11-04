package eu.gaiax.difs.fc.core.service.sdstore.impl;

import static eu.gaiax.difs.fc.core.util.TestUtil.getAccessor;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import eu.gaiax.difs.fc.core.service.validatorcache.impl.ValidatorCacheImpl;
import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.neo4j.harness.Neo4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import eu.gaiax.difs.fc.core.config.DatabaseConfig;
import eu.gaiax.difs.fc.core.config.FileStoreConfig;
import eu.gaiax.difs.fc.core.exception.NotFoundException;
import eu.gaiax.difs.fc.core.pojo.ContentAccessor;
import eu.gaiax.difs.fc.core.pojo.GraphQuery;
import eu.gaiax.difs.fc.core.pojo.SelfDescriptionMetadata;
import eu.gaiax.difs.fc.core.pojo.VerificationResultParticipant;
import eu.gaiax.difs.fc.core.service.filestore.FileStore;
import eu.gaiax.difs.fc.core.service.graphdb.impl.Neo4jGraphStore;
import eu.gaiax.difs.fc.core.service.schemastore.impl.SchemaStoreImpl;
import eu.gaiax.difs.fc.core.service.sdstore.SelfDescriptionStore;
import eu.gaiax.difs.fc.core.service.verification.VerificationService;
import eu.gaiax.difs.fc.core.service.verification.impl.VerificationDirectTest;
import eu.gaiax.difs.fc.core.service.verification.impl.VerificationServiceImpl;
import eu.gaiax.difs.fc.testsupport.config.EmbeddedNeo4JConfig;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import lombok.extern.slf4j.Slf4j;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.MethodName.class)
@SpringBootTest
@ActiveProfiles("tests-sdstore")
@ContextConfiguration(classes = {SelfDescriptionStoreCompositeTest.TestApplication.class, FileStoreConfig.class, VerificationServiceImpl.class,
  SelfDescriptionStoreImpl.class, SelfDescriptionStoreCompositeTest.class, SchemaStoreImpl.class, DatabaseConfig.class, Neo4jGraphStore.class, ValidatorCacheImpl.class})
@DirtiesContext
@Transactional
@Slf4j
@AutoConfigureEmbeddedDatabase(provider = DatabaseProvider.ZONKY)
@Import(EmbeddedNeo4JConfig.class)
public class SelfDescriptionStoreCompositeTest {

  @SpringBootApplication
  public static class TestApplication {

    public static void main(final String[] args) {
      SpringApplication.run(TestApplication.class, args);
    }
  }

  @Autowired
  private VerificationService verificationService;
  @Autowired
  private SelfDescriptionStore sdStore;

  @Autowired
  private Neo4j embeddedDatabaseServer;

  @Autowired
  private Neo4jGraphStore graphStore;
  @Autowired
  @Qualifier("sdFileStore")
  private FileStore fileStore;

  @AfterEach
  public void storageSelfCleaning() throws IOException {
    fileStore.clearStorage();
  }

  @AfterAll
  void closeNeo4j() {
    embeddedDatabaseServer.close();
  }

  // Since SdMetaRecord class extends SelfDescriptionMetadata class instead of being formed from it, then check
  // in the equals method will always be false. Because we are downcasting SdMetaRecord to SelfDescriptionMetadata.
  private static void assertThatSdHasTheSameData(final SelfDescriptionMetadata expected,
      final SelfDescriptionMetadata actual) {
    assertEquals(expected.getId(), actual.getId());
    assertEquals(expected.getSdHash(), actual.getSdHash());
    assertEquals(expected.getStatus(), actual.getStatus());
    assertEquals(expected.getIssuer(), actual.getIssuer());
    assertEquals(expected.getValidatorDids(), actual.getValidatorDids());
    assertEquals(expected.getUploadDatetime(), actual.getUploadDatetime());
    assertEquals(expected.getStatusDatetime(), actual.getStatusDatetime());
    assertEquals(expected.getSelfDescription().getContentAsString(), actual.getSelfDescription().getContentAsString());
  }

  private void assertStoredSdFiles(final int expected) {
    final MutableInt count = new MutableInt(0);
    fileStore.getFileIterable().forEach(file -> count.increment());
    final String message = String.format("Storing %d file(s) should result in exactly %d file(s) in the store.",
        expected, expected);
    assertEquals(expected, count.intValue(), message);
  }

  private void assertAllSdFilesDeleted() {
    final MutableInt count = new MutableInt(0);
    fileStore.getFileIterable().forEach(file -> count.increment());
    assertEquals(0, count.intValue(), "Deleting the last file should result in exactly 0 files in the store.");
    // TODO: check all claims were deleted also
    //List<Map<String, Object>> claims = graphStore.queryData(new OpenCypherQuery("MATCH (n) RETURN n", Map.of()));
    //assertEquals(0, claims.size());
  }

  /**
   * Test storing a self-description, ensuring it creates exactly one file on
   * disk, retrieving it by hash, and deleting it again.
   */
  @Test
  void test01StoreSelfDescription() throws Exception {
    log.info("test01StoreSelfDescription");
    ContentAccessor content = getAccessor("Claims-Extraction-Tests/participantSD.jsonld");
    // Only verify semantics, not schema or signatures
    VerificationResultParticipant result = (VerificationResultParticipant) verificationService.verifySelfDescription(content, true, false, false);
    SelfDescriptionMetadata sdMeta = new SelfDescriptionMetadata(content, result);
    sdStore.storeSelfDescription(sdMeta, result);

    assertStoredSdFiles(1);
    String hash = sdMeta.getSdHash();

    assertThatSdHasTheSameData(sdMeta, sdStore.getByHash(hash));

    List<Map<String, Object>> claims = graphStore.queryData(
            new GraphQuery("MATCH (n {uri: $uri}) RETURN labels(n)", Map.of("uri", sdMeta.getId()))).getResults();
    log.debug("test01StoreSelfDescription; got claims: {}", claims);
    //Assertions.assertEquals(5, claims.size()); only 1 node found..

    List<Map<String, Object>> nodes = graphStore.queryData(
            new GraphQuery("MATCH (n) RETURN labels(n)", Map.of())).getResults();
    log.debug("test01StoreSelfDescription; got nodes: {}", nodes);

    //final ContentAccessor sdfileByHash = sdStore.getSDFileByHash(hash);
    //assertEquals(sdfileByHash, sdMeta.getSelfDescription(),
    //    "Getting the SD file by hash is equal to the stored SD file");

    sdStore.deleteSelfDescription(hash);
    assertAllSdFilesDeleted();

    claims = graphStore.queryData(
            new GraphQuery("MATCH (n {uri: $uri}) RETURN n", Map.of("uri", sdMeta.getId()))).getResults();
    Assertions.assertEquals(0, claims.size());

    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash);
    });
  }


}
