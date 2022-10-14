package eu.gaiax.difs.fc.server.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import eu.gaiax.difs.fc.core.service.schemastore.SchemaStore;
import eu.gaiax.difs.fc.core.service.sdstore.SelfDescriptionStore;
import eu.gaiax.difs.fc.core.service.verification.VerificationService;
import eu.gaiax.difs.fc.server.helper.FileReaderHelper;
import eu.gaiax.difs.fc.testsupport.config.EmbeddedNeo4JConfig;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
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

    private final static String SD_FILE_NAME = "default-sd.json"; //"default_participant.json"; //

    private final static String DEFAULT_SERVICE_SD_FILE_NAME = "default-sd-service-offering.json";

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
    @Qualifier("sdFileStore")
    private FileStore fileStore;
    @Autowired
    private  ObjectMapper objectMapper;
    //@Autowired
    //private  SchemaStore schemaStore;

    
    @BeforeAll
    public void addManuallyDBEntries() throws Exception {
        initialiseAllDataBaseWithManuallyAddingSDFromRepository();
    }

    @BeforeTestClass
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterEach
    public void storageSelfCleaning() throws IOException {
        fileStore.clearStorage();
    }

    @AfterAll
    void closeNeo4j() {
        embeddedDatabaseServer.close();
    }

    
    String QUERY_REQUEST_GET="{\"statement\": \"MATCH (n:ns0__ServiceOffering) RETURN n LIMIT 1\", " +
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
        assertEquals(3, result.getTotalCount());
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
        assertTrue(result.getItems().size() < 101);
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

        fileStore.clearStorage();

        ContentAccessorDirect contentAccessor = new ContentAccessorDirect(FileReaderHelper.getMockFileDataAsString(SD_FILE_NAME));
        VerificationResultOffering verificationResult = verificationService.verifyOfferingSelfDescription(contentAccessor);

        SdClaim sdClaim = new SdClaim("<http://w3id.org/gaia-x/indiv#serviceMVGPortal.json>",
            "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>",
            "<http://w3id.org/gaia-x/service#ServiceOffering>");

        SdClaim sdClaim1 = new SdClaim("<http://w3id.org/gaia-x/indiv#serviceMVGPortal.json>",
            "<http://w3id.org/gaia-x/service#name>",
                "\"EuProGigant Portal\"");

        SdClaim sdClaim2 = new SdClaim("<http://w3id.org/gaia-x/indiv#serviceMVGPortal2.json>",
            "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>",
            "<http://w3id.org/gaia-x/service#ServiceOffering>");

        SdClaim sdClaim3 = new SdClaim("<http://w3id.org/gaia-x/indiv#serviceMVGPortal2.json>",
            "<http://w3id.org/gaia-x/service#name>",
                "\"EuProGigant Portal2\"");

        SdClaim sdClaim4 = new SdClaim("<http://w3id.org/gaia-x/indiv#serviceMVGPortal4.json>",
            "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>",
            "<http://w3id.org/gaia-x/service#ServiceOffering>");

        List<SdClaim> sdClaimFile = List.of(sdClaim,sdClaim1,sdClaim2,sdClaim3,sdClaim4);

        verificationResult.setClaims(sdClaimFile);
        verificationResult.setId(sdClaimFile.get(0).getSubject());

        //Manually removing extra character from string <>
        String id = verificationResult.getId();
        String verificationIdAsSubject = id.substring(1, id.length() - 1);

        SelfDescriptionMetadata sdMetadata = new SelfDescriptionMetadata(verificationIdAsSubject,
            verificationResult.getIssuer(), new ArrayList<>(), contentAccessor);
        sdStore.storeSelfDescription(sdMetadata, verificationResult);

        //adding second sd
        ContentAccessorDirect contentAccessor2 =
            new ContentAccessorDirect(FileReaderHelper.getMockFileDataAsString(DEFAULT_SERVICE_SD_FILE_NAME));
        VerificationResultOffering verificationResult2 =
            verificationService.verifyOfferingSelfDescription(contentAccessor2);

        verificationResult2.setId(sdClaimFile.get(2).getSubject());

        //Manually removing extra character from string <>
        String id2 = verificationResult2.getId();
        String verificationIdAsSubject2 = id2.substring(1, id.length() - 1);

        SelfDescriptionMetadata sdMetadata2 = new SelfDescriptionMetadata(verificationIdAsSubject2,
            verificationResult2.getIssuer(), new ArrayList<>(), contentAccessor2);
        sdStore.storeSelfDescription(sdMetadata2, verificationResult2);

        //adding sd 3
        ContentAccessorDirect contentAccessorDirect3 = new ContentAccessorDirect("test sd3");

        SelfDescriptionMetadata sdMetadata3 = new SelfDescriptionMetadata("http://w3id.org/gaia-x/indiv#serviceMVGPortal4.json",
            "http://example.org/test-issuer", new ArrayList<>(), contentAccessorDirect3);
        sdStore.storeSelfDescription(sdMetadata3, verificationResult2);
    }

}
