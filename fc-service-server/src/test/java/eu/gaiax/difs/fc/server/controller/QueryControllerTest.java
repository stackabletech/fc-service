package eu.gaiax.difs.fc.server.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.gaiax.difs.fc.api.generated.model.Results;
import eu.gaiax.difs.fc.core.pojo.ContentAccessorDirect;
import eu.gaiax.difs.fc.core.pojo.SdClaim;
import eu.gaiax.difs.fc.core.pojo.SelfDescriptionMetadata;
import eu.gaiax.difs.fc.core.pojo.VerificationResultOffering;
import eu.gaiax.difs.fc.core.service.filestore.FileStore;
import eu.gaiax.difs.fc.core.service.graphdb.impl.Neo4jGraphStore;
import eu.gaiax.difs.fc.core.service.sdstore.SelfDescriptionStore;
import eu.gaiax.difs.fc.core.service.verification.VerificationService;
import eu.gaiax.difs.fc.server.config.EmbeddedNeo4JConfig;
import eu.gaiax.difs.fc.server.helper.FileReaderHelper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.harness.Neo4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Neo4jGraphStore graphStore;

    @Autowired
    private Neo4j embeddedDatabaseServer;

    @Autowired
    private SelfDescriptionStore sdStore;

    @Autowired
    private VerificationService verificationService;

    @Autowired
    @Qualifier("sdFileStore")
    private FileStore fileStore;

    @AfterAll
    public void storageSelfCleaning() throws IOException {
        fileStore.clearStorage();
    }

    @AfterAll
    void closeNeo4j() {
        embeddedDatabaseServer.close();
    }

    @Autowired
    private  ObjectMapper objectMapper;

    private final static String SD_FILE_NAME = "test-provider-sd.json";

    @BeforeAll
    public void addManuallyDBEntries() throws Exception {
        initialiseAllDataBaseWithManuallyAddingSDFromRepository();
    }

    @BeforeTestClass
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    String QUERY_REQUEST_GET="{\"statement\": \"MATCH (n:ns0__ServiceOffering) RETURN n LIMIT 25\", " +
        "\"parameters\": " +
        "null}}";

    String QUERY_REQUEST_GET_WITH_PARAMETERS="{\"statement\": \"MATCH (n:ns0__ServiceOffering) where n.ns0__name = " +
        "$name RETURN n \", \"parameters\": { \"name\": \"EuProGigant Portal\"}}";

    String QUERY_REQUEST_GET_WITH_PARAMETERS_UNKNOWN="{\"statement\": \"MATCH (n:ns0__ServiceOffering) where " +
        "n.ns0__name = $name RETURN n \", \"parameters\": { \"name\": \"notFound\"}}";

    String QUERY_REQUEST_POST="{\"statement\": \"CREATE (m:Address {postal-code: '99999', address : 'test'})\", " +
        "\"parameters\": null}}";

    String QUERY_REQUEST_UPDATE="{\"statement\": \"Match (m:Address) where m.postal-code > 99999 SET m.postal-code = " +
        "88888 RETURN m\", " +
        "\"parameters\": " +
        "null}}";

    String QUERY_REQUEST_DELETE="{\"statement\": \"Match (m:Address) where m.postal-code > 99999 DELETE m\", " +
        "\"parameters\": " +
        "null}}";
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
                .header("Produces","application/json","application/sparql-results+xml", "text/turtle", "text/html")
                .header("Accept","application/json") //,"application/sparql-query","application/sparql*")
                .header("query-language","application/sparql-query"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));

    }

    @Test
    public void postGetQueriesReturnSuccessResponse() throws Exception {

        String response =  mockMvc.perform(MockMvcRequestBuilders.post("/query")
                        .content(QUERY_REQUEST_GET)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Produces","application/json","application/sparql-results+xml", "text/turtle", "text/html")
                        .header("Accept","application/json") //,"application/sparql-query","application/sparql*")
                        .header("query-language","application/sparql-query"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();


        Results result = objectMapper.readValue(response, Results.class);
        assertEquals(1, result.getItems().size());
    }

    @Test
    public void postGetQueriesWithParametersReturnSuccessResponse() throws Exception {

        String response =  mockMvc.perform(MockMvcRequestBuilders.post("/query")
                .content(QUERY_REQUEST_GET_WITH_PARAMETERS)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Produces","application/json","application/sparql-results+xml", "text/turtle", "text/html")
                .header("Accept","application/json") //,"application/sparql-query","application/sparql*")
                .header("query-language","application/sparql-query"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();


        Results result = objectMapper.readValue(response, Results.class);
        assertEquals(1, result.getItems().size());
    }

    @Test
    public void postGetQueriesWithUnKnownParametersResultNotFoundReturnSuccessResponse() throws Exception {

        String response =  mockMvc.perform(MockMvcRequestBuilders.post("/query")
                .content(QUERY_REQUEST_GET_WITH_PARAMETERS_UNKNOWN)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Produces","application/json","application/sparql-results+xml", "text/turtle", "text/html")
                .header("Accept","application/json") //,"application/sparql-query","application/sparql*")
                .header("query-language","application/sparql-query"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();


        Results result = objectMapper.readValue(response, Results.class);
        assertEquals(0, result.getItems().size());
    }
    @Test
    @Disabled("Enable  when FH implementation is done")
    public void postQueryReturnForbiddenResponse() throws Exception {
          mockMvc.perform(MockMvcRequestBuilders.post("/query")
                .content(QUERY_REQUEST_POST)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Produces","application/json","application/sparql-results+xml", "text/turtle", "text/html")
                .header("Accept","application/json") //,"application/sparql-query","application/sparql*")
                .header("query-language","application/sparql-query"))
            .andExpect(status().isForbidden());

    }
    @Test
    @Disabled("Enable  when FH implementation is done")
    public void postQueryForUpdateReturnForbiddenResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/query")
                .content(QUERY_REQUEST_UPDATE)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Produces","application/json","application/sparql-results+xml", "text/turtle", "text/html")
                .header("Accept","application/json") //,"application/sparql-query","application/sparql*")
                .header("query-language","application/sparql-query"))
            .andExpect(status().isForbidden());

    }

    @Test
    @Disabled("Enable  when FH implementation is done")
    public void postQueryForDeleteReturnForbiddenResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/query")
                .content(QUERY_REQUEST_DELETE)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .header("Produces","application/json","application/sparql-results+xml", "text/turtle", "text/html")
                .header("Accept","application/json") //,"application/sparql-query","application/sparql*")
                .header("query-language","application/sparql-query"))
            .andExpect(status().isForbidden());

    }

    private void initialiseAllDataBaseWithManuallyAddingSDFromRepository() throws Exception {
        ContentAccessorDirect contentAccessor = new ContentAccessorDirect(FileReaderHelper.getMockFileDataAsString(SD_FILE_NAME));
        VerificationResultOffering verificationResult = verificationService.verifyOfferingSelfDescription(contentAccessor);

        SdClaim sdClaim = new SdClaim("<http://w3id.org/gaia-x/indiv#serviceMVGPortal.json>",
            "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>",
            "<http://w3id.org/gaia-x/service#ServiceOffering>");

        SdClaim sdClaim1 = new SdClaim("<http://w3id.org/gaia-x/indiv#serviceMVGPortal.json>",
            "<http://w3id.org/gaia-x/service#name> \"EuProGigant Portal\"",
            "");


        List<SdClaim> sdClaimFile = List.of(sdClaim,sdClaim1);

        verificationResult.setClaims(sdClaimFile);
        verificationResult.setId(sdClaimFile.get(0).getSubject());

        //Manually removing extra character from string <>
        String id = verificationResult.getId();
        String verificationIdAsSubject = id.substring(1, id.length() - 1);

        SelfDescriptionMetadata sdMetadata = new SelfDescriptionMetadata(contentAccessor, verificationIdAsSubject,
            verificationResult.getIssuer(), new ArrayList<>());
        sdStore.storeSelfDescription(sdMetadata, verificationResult);

    }

}
