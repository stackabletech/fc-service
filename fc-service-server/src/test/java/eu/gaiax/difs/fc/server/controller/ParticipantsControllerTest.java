package eu.gaiax.difs.fc.server.controller;

import static eu.gaiax.difs.fc.server.helper.FileReaderHelper.getMockFileDataAsString;
import static eu.gaiax.difs.fc.server.util.CommonConstants.CATALOGUE_ADMIN_ROLE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.c4_soft.springaddons.security.oauth2.test.annotations.Claims;
import com.c4_soft.springaddons.security.oauth2.test.annotations.OpenIdClaims;
import com.c4_soft.springaddons.security.oauth2.test.annotations.StringClaim;
import com.c4_soft.springaddons.security.oauth2.test.annotations.WithMockJwtAuth;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.gaiax.difs.fc.api.generated.model.Participants;
import eu.gaiax.difs.fc.api.generated.model.SelfDescription;
import eu.gaiax.difs.fc.api.generated.model.SelfDescriptions;
import eu.gaiax.difs.fc.api.generated.model.User;
import eu.gaiax.difs.fc.api.generated.model.UserProfiles;
import eu.gaiax.difs.fc.core.dao.impl.ParticipantDaoImpl;
import eu.gaiax.difs.fc.core.dao.impl.UserDaoImpl;
import eu.gaiax.difs.fc.core.exception.NotFoundException;
import eu.gaiax.difs.fc.core.exception.ServerException;
import eu.gaiax.difs.fc.core.pojo.ContentAccessorDirect;
import eu.gaiax.difs.fc.core.pojo.ParticipantMetaData;
import eu.gaiax.difs.fc.core.pojo.SelfDescriptionMetadata;
import eu.gaiax.difs.fc.core.pojo.VerificationResultParticipant;
import eu.gaiax.difs.fc.core.service.filestore.FileStore;
import eu.gaiax.difs.fc.core.service.sdstore.impl.SelfDescriptionStoreImpl;
import eu.gaiax.difs.fc.core.service.verification.VerificationService;
import eu.gaiax.difs.fc.server.config.EmbeddedNeo4JConfig;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.neo4j.harness.Neo4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.annotation.BeforeTestClass;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureEmbeddedDatabase(provider = DatabaseProvider.ZONKY)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Import(EmbeddedNeo4JConfig.class)
@DirtiesContext
public class ParticipantsControllerTest {
    private final String CATALOGUE_ADMIN_ROLE_WITH_PREFIX = "ROLE_" + CATALOGUE_ADMIN_ROLE;

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    @Qualifier("sdFileStore")
    private FileStore fileStore;

    @Autowired
    private SelfDescriptionStoreImpl selfDescriptionStore;

    @Autowired
    private Neo4j embeddedDatabaseServer;

    @Autowired
    private VerificationService verificationService;

    @AfterAll
    void closeNeo4j() {
        embeddedDatabaseServer.close();
    }

    @MockBean
    private KeycloakBuilder builder;
    @MockBean
    private Keycloak keycloak;
    @MockBean
    private RealmResource realmResource;
    @MockBean
    private GroupsResource groupsResource;
    @MockBean
    private GroupResource groupResource;
    @MockBean
    private UsersResource usersResource;
    @MockBean
    private UserResource userResource;
    @Autowired
    private UserDaoImpl userDao;
    @Autowired
    private ObjectMapper objectMapper;

    private String userId = "ae366624-8371-401d-b2c4-518d2f308a15";

    @BeforeTestClass
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterAll
    public void storageSelfCleaning() throws IOException {
        fileStore.clearStorage();
    }

    @Test
    public void participantAuthShouldReturnUnauthorizedResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/participants")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    public void participantAuthShouldReturnForbiddenResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/participants")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    @Order(10)
    public void addParticipantShouldReturnCreatedResponse() throws Exception {
        String json = getMockFileDataAsString("new_participant.json");
        ParticipantMetaData part = new ParticipantMetaData("ebc6f1c2", "did:example:holder", "did:example:holder#key-1", json);
        setupKeycloak(HttpStatus.SC_CREATED, part);

        initialiseWithParticipantDeleteFromAllDB(part);

        String response = mockMvc
            .perform(MockMvcRequestBuilders.post("/participants")
            .contentType("application/json")
            .content(json))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        ParticipantMetaData partResult = objectMapper.readValue(response, ParticipantMetaData.class);
        assertNotNull(part);
        assertEquals("ebc6f1c2", partResult.getId());
        assertEquals("did:example:holder", partResult.getName());
        assertEquals("did:example:holder#key-1", partResult.getPublicKey());
        assertEquals(part.getSelfDescription(), partResult.getSelfDescription());

        assertDoesNotThrow(() -> fileStore.readFile(part.getSdHash()));
        assertDoesNotThrow(() -> selfDescriptionStore.getByHash(part.getSdHash()));
    }

    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    @Order(10)
    public void addDuplicateParticipantShouldReturnConflictResponse() throws Exception {
        String json = getMockFileDataAsString("new_participant.json");
        ParticipantMetaData part = new ParticipantMetaData("ebc6f1c2", "did:example:holder", "did:example:holder#key-1", json);
        setupKeycloak(HttpStatus.SC_CREATED, part);

        initialiseWithParticipantDeleteFromAllDB(part);

        ContentAccessorDirect contentAccessor = new ContentAccessorDirect(json);
        VerificationResultParticipant verResult = verificationService.verifyParticipantSelfDescription(contentAccessor);
        SelfDescriptionMetadata sdMetadata = new SelfDescriptionMetadata(contentAccessor, verResult);
        selfDescriptionStore.storeSelfDescription(sdMetadata, verResult);

        mockMvc
            .perform(MockMvcRequestBuilders.post("/participants")
                .contentType("application/json")
                .content(json))
            .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    @Order(25)
    public void addParticipantFailWithSameSDShouldReturnConflictFromKeyCloakWithoutDBStore() throws Exception {

        String json = getMockFileDataAsString("new_participant_same_sd.json");
        ParticipantMetaData partNew = new ParticipantMetaData("ebc6f1c2new", "did:example:holder-new", "did:example" +
            ":holder#key-1", json);
        setupKeycloak(HttpStatus.SC_CONFLICT, partNew);

         mockMvc
            .perform(MockMvcRequestBuilders.post("/participants")
                .contentType("application/json")
                .content(json))
            .andExpect(status().isConflict());

        FileNotFoundException exception = assertThrows(FileNotFoundException.class,
            () -> fileStore.readFile(partNew.getSdHash()));
        assertEquals(FileNotFoundException.class, exception.getClass());

        NotFoundException exceptionSDStore = assertThrows(NotFoundException.class,
            () -> selfDescriptionStore.getByHash(partNew.getSdHash()));
        assertEquals(NotFoundException.class, exceptionSDStore.getClass());
    }

    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    @Order(26)
    public void addParticipantFailWithKeyCloakErrorShouldReturnErrorWithoutDBStore() throws Exception {

        String json = getMockFileDataAsString("participant_not_added.json");
        ParticipantMetaData part = new ParticipantMetaData("ebc6f1c3", "did:example:holder",
            "did:example:holder#key-1", json);
        setupKeycloak(HttpStatus.SC_INTERNAL_SERVER_ERROR, part);

        mockMvc
            .perform(MockMvcRequestBuilders.post("/participants")
                .contentType("application/json")
                .content(json))
            .andExpect(status().is5xxServerError());

        FileNotFoundException exception = assertThrows(FileNotFoundException.class,
            () -> fileStore.readFile(part.getSdHash()));
        assertEquals(FileNotFoundException.class, exception.getClass());

        Throwable exceptionSD = assertThrows(Throwable.class,
            () -> selfDescriptionStore.getByHash(part.getSdHash()));
        assertEquals(NotFoundException.class, exceptionSD.getClass());
    }

    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    @Order(20)
    public void getParticipantShouldReturnSuccessResponse() throws Exception {

        String json = getMockFileDataAsString("new_participant.json");
        ParticipantMetaData part = new ParticipantMetaData("ebc6f1c2", "did:example:holder", "did:example:holder#key-1", json);
        setupKeycloak(HttpStatus.SC_OK, part);

        mockMvc
            .perform(MockMvcRequestBuilders.get("/participants/{participantId}", part.getId())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    @Order(20)
    public void getAddedParticipantSDShouldReturnSuccessResponseWithSameSDFromSDAPI() throws Exception {

        String json = getMockFileDataAsString("new_participant.json");
        ParticipantMetaData part = new ParticipantMetaData("ebc6f1c2", "did:example:holder", "did:example:holder#key-1", json);
        setupKeycloak(HttpStatus.SC_OK, part);

        String response = mockMvc
            .perform(MockMvcRequestBuilders.get("/self-descriptions")
            .contentType(MediaType.APPLICATION_JSON).queryParam("id",part.getId()))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        SelfDescriptions selfDescriptions =  objectMapper.readValue(response, SelfDescriptions.class);
        List<SelfDescription> selfDescriptionMetadataList =  (List)selfDescriptions.getItems();
        String  sdHash = selfDescriptionMetadataList.get(0).getSdHash();

        String responseOfSDContent = mockMvc
            .perform(MockMvcRequestBuilders.get("/self-descriptions/" + sdHash)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertEquals(part.getSelfDescription(), responseOfSDContent);
        assertEquals(1, selfDescriptionMetadataList.size());

    }
    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    @Order(20)
    public void wrongParticipantShouldReturnNotFoundResponse() throws Exception {
        
        String partId = "unknown";
        setupKeycloak(HttpStatus.SC_OK, null);

        mockMvc
            .perform(MockMvcRequestBuilders.get("/participants/{partId}", partId)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }
    
    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    @Order(20)
    public void getParticipantsShouldReturnCorrectNumber() throws Exception {
        String json = getMockFileDataAsString("new_participant.json");
        ParticipantMetaData part = new ParticipantMetaData("ebc6f1c2", "did:example:holder", "did:example:holder#key-1", json);
        setupKeycloak(HttpStatus.SC_OK, part);

        MvcResult result = mockMvc
            .perform(MockMvcRequestBuilders.get("/participants?offset={offset}&limit={limit}", null, 1)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
        Participants parts = objectMapper.readValue(result.getResponse().getContentAsString(), Participants.class);
        assertNotNull(parts);
        assertEquals(1, parts.getItems().size());
        assertEquals(1, parts.getTotalCount());
    }
    
    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    @Order(20)
    public void getParticipantUsersShouldReturnCorrectNumber() throws Exception {

        ParticipantMetaData part = new ParticipantMetaData("ebc6f1c2", "did:example:holder", "did:example:holder#key-1", "empty SD");
        setupKeycloak(HttpStatus.SC_OK, part);
        setupKeycloakForUsers(HttpStatus.SC_NO_CONTENT, null, userId);
        MvcResult result = mockMvc
            .perform(MockMvcRequestBuilders.get("/participants/{participantId}/users", part.getId())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
        UserProfiles users = objectMapper.readValue(result.getResponse().getContentAsString(), UserProfiles.class);
        assertNotNull(users);
        assertEquals(0, users.getItems().size());
    }

    @Test
    @WithMockJwtAuth(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX},
        claims = @OpenIdClaims(otherClaims = @Claims(stringClaims =
            {@StringClaim(name = "participant_id", value = "ebc6f1c2")})))
    @Order(30)
    public void updateParticipantShouldReturnSuccessResponse() throws Exception {

        String json = getMockFileDataAsString("update_participant.json");
        ParticipantMetaData part = new ParticipantMetaData("ebc6f1c2", "did:example:holder", "did:example:holder#key-1", json);
        setupKeycloak(HttpStatus.SC_OK, part);

        String response = mockMvc
            .perform(MockMvcRequestBuilders.put("/participants/{participantId}", part.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
       ParticipantMetaData partResult = objectMapper.readValue(response, ParticipantMetaData.class);
        assertNotNull(part);
        assertEquals("ebc6f1c2", partResult.getId());
        assertEquals("did:example:updated-holder", partResult.getName());
        assertEquals("did:example:holder#key-2", partResult.getPublicKey());

        assertDoesNotThrow(() -> fileStore.readFile(part.getSdHash()));
        assertDoesNotThrow(() -> selfDescriptionStore.getByHash(part.getSdHash()));

        SelfDescriptionMetadata metadata=selfDescriptionStore.getByHash(part.getSdHash());
        assertEquals(part.getSelfDescription(), metadata.getSelfDescription().getContentAsString());
    }

    @Test
    @WithMockJwtAuth(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX},
        claims = @OpenIdClaims(otherClaims = @Claims(stringClaims =
            {@StringClaim(name = "participant_id", value = "ebc6f1c2")})))
    @Order(30)
    public void updateParticipantWithOtherIdShouldReturnBadClientResponse() throws Exception {
        String json = getMockFileDataAsString("update_participant_2.json");
        ParticipantMetaData part = new ParticipantMetaData("2ebc6f1c2", "2ebc6f1c2name", "2ebc6f1c2pubKey", json);
        setupKeycloak(HttpStatus.SC_OK, part);

        ContentAccessorDirect contentAccessor = new ContentAccessorDirect(json);
        VerificationResultParticipant verResult = verificationService.verifyParticipantSelfDescription(contentAccessor);
        SelfDescriptionMetadata sdMetadata = new SelfDescriptionMetadata(contentAccessor, verResult);
        selfDescriptionStore.storeSelfDescription(sdMetadata, verResult);

        String updatedParticipant = getMockFileDataAsString("update_participant.json");

        mockMvc
            .perform(MockMvcRequestBuilders.put("/participants/{participantId}", part.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatedParticipant))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockJwtAuth(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX},
        claims = @OpenIdClaims(otherClaims = @Claims(stringClaims =
            {@StringClaim(name = "participant_id", value = "ebc6f1c2")})))
    @Order(30)
    public void updateParticipantFailWithKeycloakErrorShouldReturnErrorWithoutDBStore() throws Exception {

        String json = getMockFileDataAsString("update_participant_error.json");
        ParticipantMetaData part = new ParticipantMetaData("ebc6f1c5", "did:example:updated-holder-error", "did" +
            ":example" +
            ":holder#key-2-error", json);
        setupKeycloak(HttpStatus.SC_INTERNAL_SERVER_ERROR, part);

         mockMvc
            .perform(MockMvcRequestBuilders.put("/participants/{participantId}", part.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().is5xxServerError());


        Throwable exception = assertThrows(Throwable.class,
            () -> fileStore.readFile(part.getSdHash()));
        assertEquals(FileNotFoundException.class, exception.getClass());

        Throwable exceptionSD = assertThrows(Throwable.class,
            () -> selfDescriptionStore.getByHash(part.getSdHash()));
        assertEquals(NotFoundException.class, exceptionSD.getClass());
    }


    @Test
    @WithMockJwtAuth(authorities = {"ROLE_unknown"},
        claims = @OpenIdClaims(otherClaims = @Claims(stringClaims =
            {@StringClaim(name = "participant_id", value = "wrongId")})))
    @Order(40)
    public void deleteParticipantFailWithWrongSessionParticipantIdAndUnknownRoleShouldReturnForbiddenResponse() throws Exception {

        String json = getMockFileDataAsString("update_participant.json");
        ParticipantMetaData part = new ParticipantMetaData("ebc6f1c2", "did:example:updated-holder", "did:example:holder#key-2", json);
        setupKeycloak(HttpStatus.SC_OK, part);

        mockMvc
            .perform(MockMvcRequestBuilders.delete("/participants/{participantId}", part.getId()))
            .andExpect(status().isForbidden());

         assertDoesNotThrow(() -> fileStore.readFile(part.getSdHash()));
         assertDoesNotThrow(() -> selfDescriptionStore.getByHash(part.getSdHash()));
    }

    @Test
    @WithMockJwtAuth(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX},
        claims = @OpenIdClaims(otherClaims = @Claims(stringClaims =
            {@StringClaim(name = "participant_id", value = "ebc6f1c2")})))
    @Order(40)
    public void deleteParticipantFailWithWrongParticipantIdShouldReturnNotFoundResponse() throws Exception {


        String json = getMockFileDataAsString("unknownId_participant.json");
        ParticipantMetaData part = new ParticipantMetaData("unknown", "did:example:updated-holder", "did:example" +
            ":holder#key-2", json);
        setupKeycloak(HttpStatus.SC_OK, part);

        mockMvc
            .perform(MockMvcRequestBuilders.delete("/participants/{participantId}", part.getId()))
            .andExpect(status().isNotFound());


        Throwable exception = assertThrows(Throwable.class,
            () -> fileStore.readFile(part.getSdHash()));
        assertEquals(FileNotFoundException.class, exception.getClass());

        Throwable exceptionSD = assertThrows(Throwable.class,
            () -> selfDescriptionStore.getByHash(part.getSdHash()));
        assertEquals(NotFoundException.class, exceptionSD.getClass());

    }

    @Test
    @WithMockJwtAuth(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX},
        claims = @OpenIdClaims(otherClaims = @Claims(stringClaims =
            {@StringClaim(name = "participant_id", value = "ebc6f1c2")})))
    @Order(50)
    public void deleteParticipantSuccessShouldReturnSuccessResponse() throws Exception {

        String json = getMockFileDataAsString("update_participant.json");
        ParticipantMetaData part = new ParticipantMetaData("ebc6f1c2", "did:example:updated-holder", "did:example:holder#key-2", json);
        setupKeycloak(HttpStatus.SC_OK, part);

        assertDoesNotThrow(() -> fileStore.readFile(part.getSdHash()));

        String response = mockMvc
            .perform(MockMvcRequestBuilders.delete("/participants/{participantId}", part.getId()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        ParticipantMetaData  participantMetaData = objectMapper.readValue(response, ParticipantMetaData.class);
        assertNotNull(part);
        assertEquals("ebc6f1c2", participantMetaData.getId());
        assertEquals("did:example:updated-holder", participantMetaData.getName());
        assertEquals("did:example:holder#key-2", participantMetaData.getPublicKey());

        Throwable exception = assertThrows(Throwable.class,
            () -> fileStore.readFile(part.getSdHash()));
        assertEquals(FileNotFoundException.class, exception.getClass());

        Throwable exceptionSD = assertThrows(Throwable.class,
            () -> selfDescriptionStore.getByHash(part.getSdHash()));
        assertEquals(NotFoundException.class, exceptionSD.getClass());
    }

    @Test
    @WithMockJwtAuth(authorities = {"ROLE_Ro-MU-CA"},
        claims = @OpenIdClaims(otherClaims = @Claims(stringClaims =
            {@StringClaim(name = "participant_id", value = "ebc6f1c2")})))
    @Order(60)
    public void deleteParticipantWithAllUsersSuccessShouldReturnSuccessResponse() throws Exception {
        //Initially adding user
        addParticipantShouldReturnCreatedResponse();

        String json = getMockFileDataAsString("new_participant.json");
        ParticipantMetaData part = new ParticipantMetaData("ebc6f1c2", "did:example:holder", "did:example:holder#key-1", json);

        User userOfParticipant = getUserOfParticipant(part.getId());
        setupKeycloakForUsers(HttpStatus.SC_CREATED, userOfParticipant, userId);
        userDao.create(userOfParticipant);

        setupKeycloak(HttpStatus.SC_OK, part);

        setupKeycloakForUsers(HttpStatus.SC_NO_CONTENT, userOfParticipant, userId);

        assertDoesNotThrow(() -> fileStore.readFile(part.getSdHash()));

        String response = mockMvc
            .perform(MockMvcRequestBuilders.delete("/participants/{participantId}", part.getId()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        ParticipantMetaData  participantMetaData = objectMapper.readValue(response, ParticipantMetaData.class);
        assertNotNull(part);

        Throwable exception = assertThrows(Throwable.class,
            () -> fileStore.readFile(part.getSdHash()));
        assertEquals(FileNotFoundException.class, exception.getClass());

        Throwable exceptionSD = assertThrows(Throwable.class,
            () -> selfDescriptionStore.getByHash(part.getSdHash()));
        assertEquals(NotFoundException.class, exceptionSD.getClass());

        assertEquals(0, userDao.search(userId,0,1).size());
    }
    
    private void setupKeycloak(int status, ParticipantMetaData part) {
        when(builder.build()).thenReturn(keycloak);
        when(keycloak.realm("gaia-x")).thenReturn(realmResource);
        when(realmResource.groups()).thenReturn(groupsResource);
        if (status == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
            when(groupsResource.add(any())).thenThrow(new ServerException("500 Server Error"));
            doThrow(new ServerException("500 Server Error")).when(groupResource).update(any());
        } else if (status == HttpStatus.SC_CONFLICT) {
            String message = "{\"errorMessage\": \"User already exists\"}";
            String charset = "UTF-8";
            ByteArrayInputStream byteArrayInputStream;
            try {
                 byteArrayInputStream = new ByteArrayInputStream(message.getBytes(charset));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            when(groupsResource.add(any())).thenReturn(Response.status(status).type(javax.ws.rs.core.MediaType.APPLICATION_JSON).entity(byteArrayInputStream).build());
        } else {
            when(groupsResource.add(any())).thenReturn(Response.status(status).build());
        }
        
        if (part == null) {
            when(groupsResource.group(any())).thenThrow(new NotFoundException("404 NOT FOUND"));
            when(groupResource.members()).thenReturn(List.of()); 
            when(groupsResource.groups()).thenReturn(List.of());
            when(groupsResource.groups(any(), any())).thenReturn(List.of());
            when(groupsResource.groups(any(), any(), any())).thenReturn(List.of());
            when(groupsResource.groups(any(), any(), any(), anyBoolean())).thenReturn(List.of());
        } else {
            GroupRepresentation groupRepo = ParticipantDaoImpl.toGroupRepo(part);
            when(groupsResource.group(any())).thenReturn(groupResource);
            when(groupResource.members()).thenReturn(List.of());
            when(groupsResource.groups()).thenReturn(List.of(groupRepo));
            when(groupsResource.groups(any(), any())).thenReturn(List.of(groupRepo));
            when(groupsResource.groups(eq(part.getId()), any(), any())).thenReturn(List.of(groupRepo));
            when(groupsResource.groups(any(), any(), any(), anyBoolean())).thenReturn(List.of(groupRepo));
            when(groupsResource.count()).thenReturn(Map.of("count", 1L));
        }
    }

    private void setupKeycloakForUsers(int status, User user, String id) {
        when(builder.build()).thenReturn(keycloak);
        when(keycloak.realm("gaia-x")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.create(any())).thenReturn(Response.status(status).build());
        if (user == null) {
            when(usersResource.search(any())).thenReturn(List.of());
        } else {
            when(usersResource.delete(any())).thenReturn(Response.status(status).build());
            UserRepresentation userRepo = UserDaoImpl.toUserRepo(user);
            userRepo.setId(id);
            when(groupResource.members()).thenReturn(List.of());
            when(usersResource.list(any(), any())).thenReturn(List.of(userRepo));
            when(usersResource.search(userRepo.getUsername())).thenReturn(List.of(userRepo));
            when(usersResource.get(any())).thenReturn(userResource);
            when(userResource.toRepresentation()).thenReturn(userRepo);
        }
    }

    private void initialiseWithParticipantDeleteFromAllDB(ParticipantMetaData part){
        try {
            fileStore.deleteFile(part.getSdHash());
            selfDescriptionStore.deleteSelfDescription(part.getSdHash());
            //TODO: graphdb need to add after implementation
        } catch(Exception ex) {

        }
    }

    public User getUserOfParticipant(String partId){
        return new  User(partId,"testUserName","testLastName","test@gmail",List.of());
    }
}
