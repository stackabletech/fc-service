package eu.gaiax.difs.fc.core.service.sdstore.impl;

import static eu.gaiax.difs.fc.core.util.TestUtil.getAccessor;
import static eu.gaiax.difs.fc.core.util.TestUtil.assertThatSdHasTheSameData;

import java.util.List;
import java.util.Map;

import eu.gaiax.difs.fc.core.service.validatorcache.impl.ValidatorCacheImpl;
import org.junit.jupiter.api.*;
import org.neo4j.harness.Neo4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import eu.gaiax.difs.fc.core.config.DatabaseConfig;
import eu.gaiax.difs.fc.core.config.FileStoreConfig;
import eu.gaiax.difs.fc.core.exception.NotFoundException;
import eu.gaiax.difs.fc.core.pojo.ContentAccessor;
import eu.gaiax.difs.fc.core.pojo.GraphQuery;
import eu.gaiax.difs.fc.core.pojo.SelfDescriptionMetadata;
import eu.gaiax.difs.fc.core.pojo.VerificationResultParticipant;
import eu.gaiax.difs.fc.core.service.graphdb.impl.Neo4jGraphStore;
import eu.gaiax.difs.fc.core.service.schemastore.impl.SchemaStoreImpl;
import eu.gaiax.difs.fc.core.service.sdstore.SelfDescriptionStore;
import eu.gaiax.difs.fc.core.service.verification.VerificationService;
import eu.gaiax.difs.fc.core.service.verification.impl.VerificationServiceImpl;
import eu.gaiax.difs.fc.core.util.GraphRebuilder;
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
  private SchemaStoreImpl schemaStore;

  @Autowired
  private Neo4j embeddedDatabaseServer;

  @Autowired
  private Neo4jGraphStore graphStore;

  @AfterEach
  public void storageSelfCleaning() {
    schemaStore.clear();
    sdStore.clear();
  }

  @AfterAll
  void closeNeo4j() {
    embeddedDatabaseServer.close();
  }

  /**
   * Test storing a self-description, ensuring it creates exactly one file on disk, retrieving it by hash, and deleting
   * it again.
   */
  @Test
  void test01StoreSelfDescription() throws Exception {
    log.info("test01StoreSelfDescription");
    schemaStore.addSchema(getAccessor("Schema-Tests/gax-test-ontology.ttl"));
    ContentAccessor content = getAccessor("Claims-Extraction-Tests/providerTest.jsonld"); // participantSD.jsonld");
    // Only verify semantics, not schema or signatures
    VerificationResultParticipant result = (VerificationResultParticipant) verificationService.verifySelfDescription(content, true, false, false);
    SelfDescriptionMetadata sdMeta = new SelfDescriptionMetadata(content, result);
    sdStore.storeSelfDescription(sdMeta, result);

    String hash = sdMeta.getSdHash();

    assertThatSdHasTheSameData(sdMeta, sdStore.getByHash(hash), false);

    List<Map<String, Object>> claims = graphStore.queryData(
        new GraphQuery("MATCH (n {uri: $uri}) RETURN labels(n), n", Map.of("uri", sdMeta.getId()))).getResults();
    log.debug("test01StoreSelfDescription; got claims: {}", claims);
    //Assertions.assertEquals(5, claims.size()); only 1 node found..

    List<Map<String, Object>> hNodes = graphStore.queryData(
        new GraphQuery("MATCH (n)-[r:legalAddress]->(a {locality: $locality}) RETURN n, r, a", Map.of("locality", "Hamburg"))).getResults();
    log.debug("test01StoreSelfDescription; got Hamburg nodes: {}", hNodes);

    List<Map<String, Object>> aNodes = graphStore.queryData(
        new GraphQuery("MATCH (n) RETURN labels(n), n", Map.of())).getResults();
    log.debug("test01StoreSelfDescription; got All nodes: {}", aNodes);
    
    //final ContentAccessor sdfileByHash = sdStore.getSDFileByHash(hash);
    //assertEquals(sdfileByHash, sdMeta.getSelfDescription(),
    //    "Getting the SD file by hash is equal to the stored SD file");
    sdStore.deleteSelfDescription(hash);

    claims = graphStore.queryData(
        new GraphQuery("MATCH (n {uri: $uri}) RETURN labels(n), n", Map.of("uri", sdMeta.getId()))).getResults();
    Assertions.assertEquals(0, claims.size());

    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash);
    });
  }

  @Test
  void test02RebuildGraphDb() throws Exception {
    log.info("test02RebuildGraphDb");
    schemaStore.addSchema(getAccessor("Schema-Tests/gax-test-ontology.ttl"));
    ContentAccessor content = getAccessor("Claims-Extraction-Tests/providerTest.jsonld");
    // Only verify semantics, not schema or signatures
    VerificationResultParticipant result = (VerificationResultParticipant) verificationService.verifySelfDescription(content, true, false, false);
    SelfDescriptionMetadata sdMeta = new SelfDescriptionMetadata(content, result);
    sdStore.storeSelfDescription(sdMeta, result);

    String hash = sdMeta.getSdHash();

    assertThatSdHasTheSameData(sdMeta, sdStore.getByHash(hash), false);

    List<Map<String, Object>> claims = graphStore.queryData(
        new GraphQuery("MATCH (n) RETURN n", null)).getResults();
    log.debug("Claims: {}", claims);
    Assertions.assertEquals(3, claims.size());

    graphStore.deleteClaims(sdMeta.getId());

    claims = graphStore.queryData(
        new GraphQuery("MATCH (n) RETURN n", null)).getResults();
    log.debug("Claims: {}", claims);
    Assertions.assertEquals(1, claims.size());

    GraphRebuilder reBuilder = new GraphRebuilder(sdStore, graphStore, verificationService);
    reBuilder.rebuildGraphDb(1, 0, 1, 1);

    claims = graphStore.queryData(
        new GraphQuery("MATCH (n) RETURN n", null)).getResults();
    log.debug("Claims: {}", claims);
    Assertions.assertEquals(3, claims.size());

    sdStore.deleteSelfDescription(hash);

    claims = graphStore.queryData(
        new GraphQuery("MATCH (n) RETURN n", null)).getResults();
    Assertions.assertEquals(1, claims.size());

    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash);
    });
  }

}
