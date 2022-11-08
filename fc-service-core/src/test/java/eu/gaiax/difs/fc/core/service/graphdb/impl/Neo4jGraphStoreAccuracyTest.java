package eu.gaiax.difs.fc.core.service.graphdb.impl;

import eu.gaiax.difs.fc.core.config.DatabaseConfig;
import eu.gaiax.difs.fc.core.config.FileStoreConfig;
import eu.gaiax.difs.fc.core.pojo.ContentAccessorDirect;
import eu.gaiax.difs.fc.core.pojo.GraphQuery;
import eu.gaiax.difs.fc.core.pojo.SdClaim;
import eu.gaiax.difs.fc.core.pojo.SelfDescriptionMetadata;
import eu.gaiax.difs.fc.core.pojo.VerificationResultOffering;
import eu.gaiax.difs.fc.core.service.filestore.FileStore;
import eu.gaiax.difs.fc.core.service.schemastore.impl.SchemaStoreImpl;
import eu.gaiax.difs.fc.core.service.sdstore.impl.SelfDescriptionStoreImpl;
import eu.gaiax.difs.fc.core.service.validatorcache.impl.ValidatorCacheImpl;
import eu.gaiax.difs.fc.core.service.verification.impl.VerificationServiceImpl;
import eu.gaiax.difs.fc.testsupport.config.EmbeddedNeo4JConfig;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.junit.FixMethodOrder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.runners.MethodSorters;
import org.neo4j.harness.Neo4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SpringBootTest
@ActiveProfiles({"tests-sdstore"}) //"test",
@ContextConfiguration(classes = {
    Neo4jGraphStoreAccuracyTest.class, Neo4jGraphStore.class,
    SelfDescriptionStoreImpl.class,
    FileStoreConfig.class,
    VerificationServiceImpl.class, DatabaseConfig.class,
    SchemaStoreImpl.class, ValidatorCacheImpl.class})
@DirtiesContext
@Transactional
@Import(EmbeddedNeo4JConfig.class)
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
public class Neo4jGraphStoreAccuracyTest {

  //TODO:: We need to update SD  file when final implementation of claim parsing is done .
  private final String SERVICE_SD_FILE_NAME = "serviceOfferingSD.jsonld";

  private final String SERVICE_SD_FILE_NAME1 = "serviceOfferingSD1.jsonld";

  private final String SERVICE_SD_FILE_NAME2 = "serviceOfferingSD2.jsonld";

  private final String SERVICE_SD_FILE_NAME3 = "serviceOfferingSD3.jsonld";
  @Autowired
  private Neo4j embeddedDatabaseServer;

  @Autowired
  private Neo4jGraphStore neo4jGraphStore;

  @Autowired
  private SelfDescriptionStoreImpl sdStore;

  @Autowired
  private VerificationServiceImpl verificationService;

  @Autowired
  @Qualifier("sdFileStore")
  private FileStore fileStore;

  @BeforeAll
  void addDBEntries() throws Exception {
    initialiseAllDataBaseWithManuallyAddingSD();
  }

  @AfterAll
  void closeNeo4j() {
    embeddedDatabaseServer.close();
  }

  @AfterEach
  public void storageSelfCleaning() throws IOException {
    fileStore.clearStorage();
  }

  @Test
  void testCypherServiceOfferingAccuracy() throws Exception {

    List<Map<String, String>> resultListExpected = List.of(
        Map.of("n.uri", "http://w3id.org/gaia-x/indiv#serviceMVGPortal.json"));

    GraphQuery queryDelta = new GraphQuery(
        "MATCH (n:ServiceOffering) WHERE n.name = $name RETURN n LIMIT $limit",
        Map.of("name", "Portal","limit", 25));
    List<Map<String, Object>> responseList = neo4jGraphStore.queryData(queryDelta).getResults();

    Assertions.assertTrue(
            CollectionUtils.isEqualCollection(resultListExpected, responseList),
            resultListExpected + " != " + responseList
    );
  }

  @Test
  void testCypherServiceOfferingByURIAccuracy() throws Exception {
    /*expected only one node as previous added claims with same ID deleted from code*/
    List<Map<String, String>> resultListExpected = List.of(
        Map.of("n.name", "Portal3"));

    GraphQuery queryDelta = new GraphQuery(
        "MATCH (n:ServiceOffering) WHERE n.uri = $uri RETURN n.name LIMIT $limit",
        Map.of("uri", "http://w3id.org/gaia-x/indiv#serviceMVGPortal3.json","limit", 25));
    List<Map<String, Object>> responseList = neo4jGraphStore.queryData(queryDelta).getResults();

    Assertions.assertTrue(
            CollectionUtils.isEqualCollection(resultListExpected, responseList),
            resultListExpected + " != " + responseList
    );
  }

  @Test
  void testCypherAllServiceOfferingAccuracy() throws Exception {

    List<Map<String, String>> resultListExpected = List.of(
        Map.of("n.uri", "http://w3id.org/gaia-x/indiv#serviceMVGPortal.json"),
        Map.of("n.uri", "http://w3id.org/gaia-x/indiv#serviceMVGPortal2.json"),
        Map.of("n.uri", "http://w3id.org/gaia-x/indiv#serviceMVGPortal3.json"),
        Map.of("n.uri", "http://w3id.org/gaia-x/indiv#serviceMVGPortal4.json"));

    GraphQuery queryDelta = new GraphQuery(
        "MATCH (n:ServiceOffering)  RETURN n LIMIT $limit", Map.of("limit", 25));
    List<Map<String, Object>> responseList = neo4jGraphStore.queryData(queryDelta).getResults();

    Assertions.assertTrue(
            CollectionUtils.isEqualCollection(resultListExpected, responseList),
            resultListExpected + " != " + responseList
    );
  }


  @Test
  void testCypherAllServiceOfferingWithNameAndURI_IN_ClauseAccuracy() throws Exception {

    List<Map<String, String>> resultListExpected = List.of(
        Map.of("name", "Portal2" ,"uri", "http://w3id.org/gaia-x/indiv#serviceMVGPortal2.json"),
        Map.of("name", "Portal2" ,"uri", "http://w3id.org/gaia-x/indiv#serviceMVGPortal4.json"));

    GraphQuery queryDelta = new GraphQuery(
        "CALL {MATCH (n:ServiceOffering) WHERE n.name = $name RETURN n.uri as urlList} MATCH " +
            "(n:ServiceOffering) WHERE n.uri IN [urlList] RETURN n.uri as uri, n.name as name LIMIT $limit",
        Map.of("name", "Portal2","limit", 25));
    List<Map<String, Object>> responseList = neo4jGraphStore.queryData(queryDelta).getResults();

    Assertions.assertTrue(
            CollectionUtils.isEqualCollection(responseList, resultListExpected),
            resultListExpected + " != " + responseList
    );
  }
  @Test
  void testCypherTotalCount() throws Exception {

    GraphQuery queryDelta = new GraphQuery(
        "MATCH (n)  RETURN n LIMIT $limit", Map.of("limit", 25));
    List<Map<String, Object>> responseList = neo4jGraphStore.queryData(queryDelta).getResults();

    Assertions.assertEquals(5, responseList.size());
  }

  private void initialiseAllDataBaseWithManuallyAddingSD() throws Exception {

    fileStore.clearStorage();

    ContentAccessorDirect contentAccessor = new ContentAccessorDirect(getMockFileDataAsString(SERVICE_SD_FILE_NAME));
    VerificationResultOffering verificationResult =
            (VerificationResultOffering) verificationService.verifySelfDescription(contentAccessor, true, false, false);

    //TODO:: adding manually claims, after final implementation we will remove it and change the query according to sd

    SdClaim sdClaim = new SdClaim("<http://w3id.org/gaia-x/indiv#serviceMVGPortal.json>",
        "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>",
        "<http://w3id.org/gaia-x/service#ServiceOffering>");

    SdClaim sdClaimName = new SdClaim("<http://w3id.org/gaia-x/indiv#serviceMVGPortal.json>",
        "<http://w3id.org/gaia-x/service#name>",
        "\"Portal\"");

    List<SdClaim> sdClaimFile = List.of(sdClaim,sdClaimName);

    verificationResult.setClaims(sdClaimFile);
    verificationResult.setId(sdClaimFile.get(0).stripSubject());

    SelfDescriptionMetadata sdMetadata = new SelfDescriptionMetadata( verificationResult.getId(),
        verificationResult.getIssuer(), new ArrayList<>(),contentAccessor);
    sdStore.storeSelfDescription(sdMetadata, verificationResult);

    //adding 2 sd skipping sd validation as we don't have full implementation
    ContentAccessorDirect contentAccessorDirect2 =
        new ContentAccessorDirect(getMockFileDataAsString(SERVICE_SD_FILE_NAME1));
    VerificationResultOffering verificationResult2 =
            (VerificationResultOffering) verificationService.verifySelfDescription(contentAccessor, true, false, false);

    SdClaim sdClaim1 = new SdClaim("<http://w3id.org/gaia-x/indiv#serviceMVGPortal2.json>",
        "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>",
        "<http://w3id.org/gaia-x/service#ServiceOffering>");

    SdClaim sdClaimName1 = new SdClaim("<http://w3id.org/gaia-x/indiv#serviceMVGPortal2.json>",
        "<http://w3id.org/gaia-x/service#name>",
        "\"Portal2\"");

    List<SdClaim> sdClaimFile1 = List.of(sdClaim1,sdClaimName1);

    verificationResult2.setClaims(sdClaimFile1);
    verificationResult2.setId(sdClaimFile1.get(0).stripSubject());

    SelfDescriptionMetadata sdMetadata2 = new SelfDescriptionMetadata(
        verificationResult2.getId(),
        verificationResult2.getIssuer(), new ArrayList<>(),contentAccessorDirect2);
    sdStore.storeSelfDescription(sdMetadata2, verificationResult2);

    //adding sd 3
    ContentAccessorDirect contentAccessorDirect3 =
        new ContentAccessorDirect(getMockFileDataAsString(SERVICE_SD_FILE_NAME2));
    VerificationResultOffering verificationResult3 =
            (VerificationResultOffering) verificationService.verifySelfDescription(contentAccessor, true, false, false);

    SdClaim sdClaim3 = new SdClaim("<http://w3id.org/gaia-x/indiv#serviceMVGPortal3.json>",
        "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>",
        "<http://w3id.org/gaia-x/service#ServiceOffering>");

    SdClaim sdClaimName3 = new SdClaim("<http://w3id.org/gaia-x/indiv#serviceMVGPortal3.json>",
        "<http://w3id.org/gaia-x/service#name>",
        " \"Portal3\"");

    List<SdClaim> sdClaimFile3 = List.of(sdClaim3,sdClaimName3);

    verificationResult3.setClaims(sdClaimFile3);
    verificationResult3.setId(sdClaimFile3.get(0).stripSubject());

    SelfDescriptionMetadata sdMetadata3 = new SelfDescriptionMetadata(
        verificationResult3.getId(),
        verificationResult3.getIssuer(), new ArrayList<>(),contentAccessorDirect3);
    sdStore.storeSelfDescription(sdMetadata3, verificationResult3);


    //adding sd 3
    ContentAccessorDirect contentAccessorDirect4 =
        new ContentAccessorDirect(getMockFileDataAsString(SERVICE_SD_FILE_NAME3));
    VerificationResultOffering verificationResult4 =
            (VerificationResultOffering) verificationService.verifySelfDescription(contentAccessor, true, false, false);


    SdClaim sdClaim4 = new SdClaim("<http://w3id.org/gaia-x/indiv#serviceMVGPortal4.json>",
        "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>",
        "<http://w3id.org/gaia-x/service#ServiceOffering>");

    SdClaim sdClaimName4 = new SdClaim("<http://w3id.org/gaia-x/indiv#serviceMVGPortal4.json>",
        "<http://w3id.org/gaia-x/service#name>",
        "\"Portal2\"");


    List<SdClaim> sdClaimFile4 = List.of(sdClaim4,sdClaimName4);

    verificationResult4.setClaims(sdClaimFile4);
    verificationResult4.setId(sdClaimFile4.get(0).stripSubject());

    SelfDescriptionMetadata sdMetadata4 = new SelfDescriptionMetadata(
        verificationResult4.getId(),
        verificationResult4.getIssuer(), new ArrayList<>(),contentAccessorDirect4);
    sdStore.storeSelfDescription(sdMetadata4, verificationResult4);
  }

  public static String getMockFileDataAsString(String filename) throws IOException {
    Path resourceDirectory = Paths.get("src", "test", "resources", "Query-Tests");
    String absolutePath = resourceDirectory.toFile().getAbsolutePath();
    return new String(Files.readAllBytes(Paths.get(absolutePath + "/" + filename )));
  }
}
