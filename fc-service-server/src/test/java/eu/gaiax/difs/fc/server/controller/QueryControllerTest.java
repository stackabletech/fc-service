package eu.gaiax.difs.fc.server.controller;

import static eu.gaiax.difs.fc.server.util.TestUtil.getAccessor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.gaiax.difs.fc.api.generated.model.QueryLanguage;
import eu.gaiax.difs.fc.api.generated.model.Results;
import eu.gaiax.difs.fc.core.pojo.ContentAccessorDirect;
import eu.gaiax.difs.fc.core.pojo.PaginatedResults;
import eu.gaiax.difs.fc.core.pojo.SdFilter;
import eu.gaiax.difs.fc.core.pojo.SelfDescriptionMetadata;
import eu.gaiax.difs.fc.core.pojo.VerificationResultOffering;
import eu.gaiax.difs.fc.core.pojo.VerificationResultParticipant;
import eu.gaiax.difs.fc.core.service.schemastore.SchemaStore;
import eu.gaiax.difs.fc.core.service.sdstore.SelfDescriptionStore;
import eu.gaiax.difs.fc.core.service.verification.VerificationService;
import eu.gaiax.difs.fc.server.helper.FileReaderHelper;
import eu.gaiax.difs.fc.testsupport.config.EmbeddedNeo4JConfig;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.harness.Neo4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.annotation.BeforeTestClass;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension.class)
@AutoConfigureEmbeddedDatabase(provider = DatabaseProvider.ZONKY)
@Import(EmbeddedNeo4JConfig.class)
public class QueryControllerTest {

  private final static String DEFAULT_SERVICE_SD_FILE_NAME = "default-sd-service-offering.json";
  private final static String DEFAULT_PARTICIPANT_SD_FILE_NAME = "default_participant.json";
  private final static String UNIQUE_PARTICIPANT_SD_FILE_NAME = "unique_participant.json";

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private Neo4j embeddedDatabaseServer;

  @Autowired
  private SelfDescriptionStore sdStore;

  @Autowired
  private VerificationService verificationService;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private SchemaStore schemaStore;

  @BeforeAll
  public void setup() throws Exception {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    schemaStore.addSchema(getAccessor("mock-data/gax-test-ontology.ttl"));
    initialiseAllDataBaseWithManuallyAddingSDFromRepository();
  }

  @AfterAll
  void cleanUpStores() {
    schemaStore.clear();
    sdStore.clear();
    embeddedDatabaseServer.close();
  }

  private String QUERY_REQUEST_GET = "{\"statement\": \"MATCH (n:ServiceOffering) RETURN n LIMIT 1\", "
          + "\"parameters\": null}";

  private String QUERY_REQUEST_TIMEOUT = "{\"statement\": \"CALL apoc.util.sleep($duration)\", \"parameters\": {\"duration\": 3000}}";

  private String QUERY_REQUEST_GET_WITH_PARAMETERS = "{\"statement\": \"MATCH (n)-[:hasLegallyBindingAddress]->(m)  " +
      "where m.locality = $locality RETURN n \", \"parameters\": { \"locality\": \"City Name 2\"}}";

  private String QUERY_REQUEST_GET_WITH_PARAMETERS_UNKNOWN = "{\"statement\": \"MATCH (n:ServiceOffering) where "
          + "n.name = $name RETURN n \", \"parameters\": { \"name\": \"notFound\"}}";

  private String QUERY_REQUEST_POST = "{\"statement\": \" CREATE (n:Person {name: 'TestUser', title: 'Developer'})\", "
          + "\"parameters\": null}";

  private String QUERY_REQUEST_UPDATE = "{\"statement\": \"Match (m:Person) where m.name = 'TestUser' SET m.name = "
          + "'TestUserUpdated' RETURN m\", \"parameters\": null}";

  private String QUERY_REQUEST_DELETE = "{\"statement\": \"MATCH (n:LegalPerson) where n.name = 'Fredrik "
          + "DETACH DELETE n\", \"parameters\": null}";

  private String QUERY_REQUEST_GET_SUBJECT_ID = "{\"statement\": \"MATCH (n:ServiceOffering) where n.uri IS NOT NULL RETURN n" +
      ".uri \", \"parameters\": null}";

  @Test
  public void getQueryPageShouldReturnSuccessResponse() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get("/query")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(header().stringValues("Content-Type", "text/html"));
  }

  @Test
  public void postGetQueriesReturnDefaultJsonResponseTypeSuccess() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.post("/query")
            .content(QUERY_REQUEST_GET)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .header("Produces", "application/json")
            .header("Accept", "application/json"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }

  @Test
  public void postUsupportedQueryReturnNotImplementedResponse() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.post("/query")
            .content(QUERY_REQUEST_GET)
            .contentType(MediaType.APPLICATION_JSON)
            .queryParam("queryLanguage", QueryLanguage.SPARQL.getValue())
            .header("Produces", "application/json")
            .header("Accept", "application/json"))
            .andExpect(status().isNotImplemented());
  }

  @Test
  public void postGetQueriesReturnSuccessResponse() throws Exception {
    String response = mockMvc.perform(MockMvcRequestBuilders.post("/query")
            .content(QUERY_REQUEST_GET)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .header("Produces", "application/json")
            .header("Accept", "application/json"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    Results result = objectMapper.readValue(response, Results.class);
    assertEquals(1, result.getItems().size());
    assertEquals(1, result.getTotalCount());
  }


  @Test
  public void postGetSDMetadataCountBySubjectIDSQueriesReturnSuccessResponse() throws Exception {
    String response = mockMvc.perform(MockMvcRequestBuilders.post("/query")
            .content(QUERY_REQUEST_GET_SUBJECT_ID)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .header("Produces", "application/json")
            .header("Accept", "application/json"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();

    Results result = objectMapper.readValue(response, Results.class);

    final List<String> uriId = result.getItems().stream()
        .map(map -> map.get("n.uri").toString())
        .collect(Collectors.toList());

    assertEquals(1, uriId.size());

    final SdFilter filterParams = new SdFilter();
    filterParams.setIds(uriId);

    PaginatedResults<SelfDescriptionMetadata> byFilter = sdStore.getByFilter(filterParams, true, true);
    int matchCount = byFilter.getResults().size();

    assertEquals(uriId.size(), matchCount);
  }

  @Test
  public void postGetQueriesWithParametersReturnSuccessResponse() throws Exception {
    String response = mockMvc.perform(MockMvcRequestBuilders.post("/query")
            .content(QUERY_REQUEST_GET_WITH_PARAMETERS)
            .contentType(MediaType.APPLICATION_JSON)
            .header("Produces", "application/json")
            .header("Accept", "application/json"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    Results result = objectMapper.readValue(response, Results.class);
    assertEquals(2, result.getItems().size());
    assertTrue(result.getItems().size() < 101);
  }

  @Test
  public void postGetQueriesWithUnKnownParametersResultNotFoundReturnSuccessResponse() throws Exception {

    String response = mockMvc.perform(MockMvcRequestBuilders.post("/query")
            .content(QUERY_REQUEST_GET_WITH_PARAMETERS_UNKNOWN)
            .contentType(MediaType.APPLICATION_JSON)
            .header("Produces", "application/json")
            .header("Accept", "application/json"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    Results result = objectMapper.readValue(response, Results.class);
    assertEquals(0, result.getItems().size());
  }

  @Test
  public void postQueryReturnForbiddenResponse() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.post("/query")
            .content(QUERY_REQUEST_POST)
            .contentType(MediaType.APPLICATION_JSON)
            .header("Produces", "application/json")
            .header("Accept", "application/json"))
            .andExpect(status().is5xxServerError());
  }

  @Test
  public void postQueryForUpdateReturnForbiddenResponse() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.post("/query")
            .content(QUERY_REQUEST_UPDATE)
            .contentType(MediaType.APPLICATION_JSON)
            .header("Produces", "application/json")
            .header("Accept", "application/json"))
            .andExpect(status().is5xxServerError());

  }

  @Test
  public void postQueryForDeleteReturnForbiddenResponse() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.post("/query")
            .content(QUERY_REQUEST_DELETE)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .header("Produces", "application/json")
            .header("Accept", "application/json"))
            .andExpect(status().is5xxServerError());
  }

  @Test
  public void tooLongQueryReturnTimeoutResponse() throws Exception {

    String response = mockMvc.perform(MockMvcRequestBuilders.post("/query")
            .content(QUERY_REQUEST_TIMEOUT)
            .contentType(MediaType.APPLICATION_JSON)
            .queryParam("timeout", "1")
            .header("Produces", "application/json")
            .header("Accept", "application/json"))
            .andExpect(status().isGatewayTimeout())
            .andReturn()
            .getResponse()
            .getContentAsString();

    eu.gaiax.difs.fc.api.generated.model.Error result = objectMapper.readValue(response, eu.gaiax.difs.fc.api.generated.model.Error.class);
    assertEquals("timeout_error", result.getCode());
  }

  private void initialiseAllDataBaseWithManuallyAddingSDFromRepository() throws Exception {

    //adding 1st sd
    ContentAccessorDirect contentAccessor =
        new ContentAccessorDirect(FileReaderHelper.getMockFileDataAsString(DEFAULT_PARTICIPANT_SD_FILE_NAME));
    VerificationResultParticipant verificationResult = verificationService.verifyParticipantSelfDescription(contentAccessor);
    SelfDescriptionMetadata sdMetadata = new SelfDescriptionMetadata(verificationResult.getId(),
            verificationResult.getIssuer(), verificationResult.getValidators(), contentAccessor);
    sdStore.storeSelfDescription(sdMetadata, verificationResult);

    //adding second sd
    ContentAccessorDirect contentAccessor2
            = new ContentAccessorDirect(FileReaderHelper.getMockFileDataAsString(DEFAULT_SERVICE_SD_FILE_NAME));
    VerificationResultOffering verificationResult2
            = verificationService.verifyOfferingSelfDescription(contentAccessor2);
    SelfDescriptionMetadata sdMetadata2 = new SelfDescriptionMetadata(verificationResult2.getId(),
            verificationResult2.getIssuer(), verificationResult2.getValidators(), contentAccessor2);
    sdStore.storeSelfDescription(sdMetadata2, verificationResult2);

    //adding sd 3
   ContentAccessorDirect contentAccessorDirect3 =
        new ContentAccessorDirect(FileReaderHelper.getMockFileDataAsString(UNIQUE_PARTICIPANT_SD_FILE_NAME));
    VerificationResultParticipant verificationResult3
        = verificationService.verifyParticipantSelfDescription(contentAccessorDirect3);
    SelfDescriptionMetadata sdMetadata3 = new SelfDescriptionMetadata(verificationResult3.getId(),
        verificationResult3.getIssuer(), verificationResult3.getValidators(), contentAccessorDirect3);
    sdStore.storeSelfDescription(sdMetadata3, verificationResult2);
  }

}
