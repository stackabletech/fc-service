package eu.gaiax.difs.fc.server.controller;

import static eu.gaiax.difs.fc.server.helper.FileReaderHelper.getMockFileDataAsString;
import static eu.gaiax.difs.fc.server.util.CommonConstants.PARTICIPANT_ADMIN_ROLE;
import static eu.gaiax.difs.fc.server.util.TestCommonConstants.CATALOGUE_ADMIN_ROLE_WITH_PREFIX;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.c4_soft.springaddons.security.oauth2.test.annotations.Claims;
import com.c4_soft.springaddons.security.oauth2.test.annotations.OpenIdClaims;
import com.c4_soft.springaddons.security.oauth2.test.annotations.StringClaim;
import com.c4_soft.springaddons.security.oauth2.test.annotations.WithMockJwtAuth;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.gaiax.difs.fc.api.generated.model.Error;
import eu.gaiax.difs.fc.api.generated.model.Participants;
import eu.gaiax.difs.fc.api.generated.model.SelfDescription;
import eu.gaiax.difs.fc.api.generated.model.SelfDescriptions;
import eu.gaiax.difs.fc.api.generated.model.User;
import eu.gaiax.difs.fc.api.generated.model.UserProfile;
import eu.gaiax.difs.fc.api.generated.model.UserProfiles;
import eu.gaiax.difs.fc.core.dao.ParticipantDao;
import eu.gaiax.difs.fc.core.dao.UserDao;
import eu.gaiax.difs.fc.core.exception.NotFoundException;
import eu.gaiax.difs.fc.core.exception.ServerException;
import eu.gaiax.difs.fc.core.pojo.ContentAccessorDirect;
import eu.gaiax.difs.fc.core.pojo.ParticipantMetaData;
import eu.gaiax.difs.fc.core.pojo.SdFilter;
import eu.gaiax.difs.fc.core.pojo.SelfDescriptionMetadata;
import eu.gaiax.difs.fc.core.pojo.VerificationResultParticipant;
import eu.gaiax.difs.fc.core.service.filestore.FileStore;
import eu.gaiax.difs.fc.core.service.sdstore.SelfDescriptionStore;
import eu.gaiax.difs.fc.core.service.verification.VerificationService;
import eu.gaiax.difs.fc.server.controller.common.EmbeddedKeycloakTest;
import eu.gaiax.difs.fc.testsupport.config.EmbeddedNeo4JConfig;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;;
import org.neo4j.harness.Neo4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.event.annotation.BeforeTestClass;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Import(EmbeddedNeo4JConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {"server.port=9092"})
public class ParticipantsControllerTest extends EmbeddedKeycloakTest {
    @Autowired
    private WebApplicationContext context;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    @Qualifier("sdFileStore")
    private FileStore fileStore;

    @Autowired
    private SelfDescriptionStore selfDescriptionStore;

    @SpyBean
    private ParticipantDao participantDao;
    @Autowired
    private Neo4j embeddedDatabaseServer;

    @Autowired
    private VerificationService verificationService;

    @AfterAll
    void closeNeo4j() {
        embeddedDatabaseServer.close();
    }

    @Autowired
    private UserDao userDao;
    @Autowired
    private ObjectMapper objectMapper;

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
        ParticipantMetaData initialMetadata = getDefaultParticipantMetadata();
        String response = mockMvc.perform(MockMvcRequestBuilders.post("/participants")
                .contentType("application/json").content(initialMetadata.getSelfDescription()))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        ParticipantMetaData resultMetadata = objectMapper.readValue(response, ParticipantMetaData.class);
        assertNotNull(initialMetadata);
        assertEquals(initialMetadata.getId(), resultMetadata.getId());
        assertEquals(initialMetadata.getName(), resultMetadata.getName());
        assertEquals(initialMetadata.getPublicKey(), resultMetadata.getPublicKey());
        assertEquals(initialMetadata.getSelfDescription(), resultMetadata.getSelfDescription());
        storagesAssertDoesNotThrow(initialMetadata);
        clearStorages(resultMetadata);
    }

    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    @Order(10)
    public void addDuplicateParticipantShouldReturnKeycloakConflictResponse() throws Exception {
        ParticipantMetaData metadata = getDefaultParticipantMetadata();
        clearStorages(metadata);
        participantDao.create(metadata);
        String response = mockMvc.perform(MockMvcRequestBuilders.post("/participants")
                .contentType("application/json").content(metadata.getSelfDescription()))
            .andExpect(status().isConflict())
            .andReturn().getResponse().getContentAsString();

        Error error = objectMapper.readValue(response, Error.class);
        assertNotNull(error);
        assertEquals("Top level group named '" + metadata.getId() + "' already exists.", error.getMessage());
        assertThrows(FileNotFoundException.class, () -> fileStore.readFile(metadata.getSdHash()));
        assertThrows(NotFoundException.class, () -> selfDescriptionStore.getByHash(metadata.getSdHash()));
        clearStorages(metadata);
    }

    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    @Order(25)
    public void addDuplicateInSdStorageParticipantShouldReturnConflictResponse() throws Exception {
        ParticipantMetaData metadata = getDefaultParticipantMetadata();
        addToSdStorage(metadata.getSelfDescription());
        mockMvc
            .perform(MockMvcRequestBuilders.post("/participants")
                .contentType("application/json")
                .content(metadata.getSelfDescription()))
            .andExpect(status().isConflict());
        assertTrue(participantDao.select(metadata.getId()).isEmpty());
        SdFilter sdFilter = new SdFilter();
        sdFilter.setHash(metadata.getSdHash());
        assertEquals(1, selfDescriptionStore.getByFilter(sdFilter).getTotalCount());
        clearStorages(metadata);
    }

    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    @Order(26)
    public void addParticipantFailWithKeyCloakErrorShouldReturnErrorWithoutDBStore() throws Exception {
        ParticipantMetaData metadata = getDefaultParticipantMetadata();
        doThrow(new ServerException("Some server exception")).when(participantDao).create(any());
        mockMvc
            .perform(MockMvcRequestBuilders.post("/participants")
                .contentType("application/json")
                .content(metadata.getSelfDescription()))
            .andExpect(status().is5xxServerError());
        assertThrows(FileNotFoundException.class, () -> fileStore.readFile(metadata.getSdHash()));
        assertThrows(NotFoundException.class, () -> selfDescriptionStore.getByHash(metadata.getSdHash()));
    }

    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    @Order(20)
    public void getParticipantShouldReturnSuccessResponse() throws Exception {
        ParticipantMetaData metadata = getDefaultParticipantMetadata();
        removeOldParticipantAndAddNew(metadata);
        mockMvc
            .perform(MockMvcRequestBuilders.get("/participants/{participantId}", metadata.getId())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        clearStorages(metadata);
    }

    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    @Order(20)
    public void getAddedParticipantSDShouldReturnSuccessResponseWithSameSDFromSDAPI() throws Exception {
        ParticipantMetaData metadata = getDefaultParticipantMetadata();
        removeOldParticipantAndAddNew(metadata);
        String response = mockMvc.perform(MockMvcRequestBuilders.get("/self-descriptions")
                .contentType(MediaType.APPLICATION_JSON).queryParam("id", metadata.getId()))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        SelfDescriptions selfDescriptions = objectMapper.readValue(response, SelfDescriptions.class);
        List<SelfDescription> selfDescriptionMetadataList = selfDescriptions.getItems();
        String sdHash = selfDescriptionMetadataList.get(0).getSdHash();

        String responseOfSDContent = mockMvc
            .perform(MockMvcRequestBuilders.get("/self-descriptions/" + sdHash)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertEquals(metadata.getSelfDescription(), responseOfSDContent);
        assertEquals(1, selfDescriptionMetadataList.size());
        clearStorages(metadata);
    }

    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    @Order(20)
    public void wrongParticipantShouldReturnNotFoundResponse() throws Exception {
        String partId = "unknown";
        mockMvc
            .perform(MockMvcRequestBuilders.get("/participants/{partId}", partId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    @Order(20)
    public void getParticipantsShouldReturnCorrectNumber() throws Exception {
        ParticipantMetaData metadata = getDefaultParticipantMetadata();
        removeOldParticipantAndAddNew(metadata);

        MvcResult result = mockMvc
            .perform(MockMvcRequestBuilders.get("/participants?offset={offset}&limit={limit}", null, 1)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
        Participants participants = objectMapper.readValue(result.getResponse().getContentAsString(), Participants.class);
        assertNotNull(participants);
        assertEquals(1, participants.getItems().size());
        assertEquals(2, participants.getTotalCount());
        clearStorages(metadata);
    }

    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    @Order(20)
    public void getParticipantUsersShouldReturnCorrectNumber() throws Exception {
        ParticipantMetaData metadata = getDefaultParticipantMetadata();
        removeOldParticipantAndAddNew(metadata);

        MvcResult result = mockMvc
            .perform(MockMvcRequestBuilders.get("/participants/{participantId}/users", metadata.getId())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
        UserProfiles users = objectMapper.readValue(result.getResponse().getContentAsString(), UserProfiles.class);
        assertNotNull(users);
        assertEquals(0, users.getItems().size());
        clearStorages(metadata);
    }

    @Test
    @WithMockJwtAuth(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX},
        claims = @OpenIdClaims(otherClaims = @Claims(stringClaims =
            {@StringClaim(name = "participant_id", value = "0949d1a0")})))
    @Order(30)
    public void updateParticipantShouldReturnSuccessResponse() throws Exception {
        ParticipantMetaData metadata = getDefaultParticipantMetadata();
        removeOldParticipantAndAddNew(metadata);
        String response = mockMvc
            .perform(MockMvcRequestBuilders.put("/participants/{participantId}", metadata.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(metadata.getSelfDescription().replace("did:example:holder", "did:example:updated-holder")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        ParticipantMetaData partResult = objectMapper.readValue(response, ParticipantMetaData.class);
        assertNotNull(metadata);
        assertEquals("0949d1a0", partResult.getId());
        assertEquals("did:example:updated-holder", partResult.getName());
        assertEquals("did:example:updated-holder#key", partResult.getPublicKey());

        storagesAssertDoesNotThrow(metadata);

        SelfDescriptionMetadata sdMetadata = selfDescriptionStore.getByHash(metadata.getSdHash());
        assertEquals(metadata.getSelfDescription(), sdMetadata.getSelfDescription().getContentAsString());
        clearStorages(metadata);
    }

    @Test
    @WithMockJwtAuth(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX},
        claims = @OpenIdClaims(otherClaims = @Claims(stringClaims =
            {@StringClaim(name = "participant_id", value = "0949d1a0")})))
    @Order(30)
    public void updateParticipantWithSameDataShouldReturnConflictResponse() throws Exception {
        ParticipantMetaData metadata = getDefaultParticipantMetadata();
        removeOldParticipantAndAddNew(metadata);
        String response = mockMvc
            .perform(MockMvcRequestBuilders.put("/participants/{participantId}", metadata.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(metadata.getSelfDescription()))
            .andExpect(status().isConflict())
            .andReturn()
            .getResponse()
            .getContentAsString();
        Error error = objectMapper.readValue(response, Error.class);
        assertEquals("self-description file with hash " + metadata.getSdHash() + " already exists", error.getMessage());
        clearStorages(metadata);
    }

    @Test
    @WithMockJwtAuth(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX},
        claims = @OpenIdClaims(otherClaims = @Claims(stringClaims =
            {@StringClaim(name = "participant_id", value = "0949d1a0")})))
    @Order(30)
    public void updateParticipantWithOtherIdShouldReturnBadClientResponse() throws Exception {
        ParticipantMetaData metadata = getDefaultParticipantMetadata();
        removeOldParticipantAndAddNew(metadata);

        String updatedParticipant =  metadata.getSelfDescription().replace("0949d1a0", "20949d1a0")
            .replace("did:example:holder", "did:example:updated-holder");

        mockMvc
            .perform(MockMvcRequestBuilders.put("/participants/{participantId}", metadata.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatedParticipant))
            .andExpect(status().isBadRequest());
        clearStorages(metadata);
    }

    @Test
    @WithMockJwtAuth(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX},
        claims = @OpenIdClaims(otherClaims = @Claims(stringClaims =
            {@StringClaim(name = "participant_id", value = "b49119f8")})))
    @Order(30)
    public void updateParticipantWithKeycloakErrorShouldReturnServerError() throws Exception {
        ParticipantMetaData metadata = getDefaultParticipantMetadata();
        doThrow(new ServerException("Some server exception")).when(participantDao).update(any(), any());
        removeOldParticipantAndAddNew(metadata);

        String response = mockMvc
            .perform(MockMvcRequestBuilders.put("/participants/{participantId}", metadata.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(metadata.getSelfDescription().replace("Ed25519Signature2018", "Ed25519Signature2022")))
            .andExpect(status().is5xxServerError())
            .andReturn().getResponse().getContentAsString();

        Error error = objectMapper.readValue(response, Error.class);
        assertEquals("Some server exception", error.getMessage());
        clearStorages(metadata);
    }

    @Test
    @WithMockJwtAuth(authorities = {"ROLE_unknown"},
        claims = @OpenIdClaims(otherClaims = @Claims(stringClaims =
            {@StringClaim(name = "participant_id", value = "wrongId")})))
    @Order(40)
    public void deleteParticipantWithWrongSessionParticipantIdAndUnknownRoleShouldReturnForbiddenResponse() throws Exception {
        ParticipantMetaData metadata = getDefaultParticipantMetadata();
        removeOldParticipantAndAddNew(metadata);
        mockMvc
            .perform(MockMvcRequestBuilders.delete("/participants/{participantId}", metadata.getId()))
            .andExpect(status().isForbidden());
        storagesAssertDoesNotThrow(metadata);
        clearStorages(metadata);
    }

    @Test
    @WithMockJwtAuth(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    @Order(40)
    public void deleteParticipantWithWrongParticipantIdShouldReturnNotFoundResponse() throws Exception {
        ParticipantMetaData part = new ParticipantMetaData("unknown", "did:example:updated-holder", "did:example" +
            ":holder#key-2", "unknown");
        mockMvc
            .perform(MockMvcRequestBuilders.delete("/participants/{participantId}", part.getId()))
            .andExpect(status().isNotFound());
        assertThrows(FileNotFoundException.class, () -> fileStore.readFile(part.getSdHash()));
        assertThrows(NotFoundException.class, () -> selfDescriptionStore.getByHash(part.getSdHash()));
        clearStorages(part);
    }

    @Test
    @WithMockJwtAuth(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX},
        claims = @OpenIdClaims(otherClaims = @Claims(stringClaims =
            {@StringClaim(name = "participant_id", value = "0949d1a0")})))
    @Order(50)
    public void deleteParticipantShouldReturnSuccessResponse() throws Exception {
        ParticipantMetaData metadata = getDefaultParticipantMetadata();
        removeOldParticipantAndAddNew(metadata);
        assertDoesNotThrow(() -> fileStore.readFile(metadata.getSdHash()));

        String response = mockMvc
            .perform(MockMvcRequestBuilders.delete("/participants/{participantId}", metadata.getId()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        ParticipantMetaData removedMetaData = objectMapper.readValue(response, ParticipantMetaData.class);
        assertNotNull(metadata);
        assertEquals("0949d1a0", removedMetaData.getId());
        assertEquals("did:example:holder", removedMetaData.getName());
        assertEquals("did:example:holder#key", removedMetaData.getPublicKey());

        assertThrows(FileNotFoundException.class, () -> fileStore.readFile(metadata.getSdHash()));
        assertThrows(NotFoundException.class, () -> selfDescriptionStore.getByHash(metadata.getSdHash()));
        clearStorages(metadata);
    }

    @Test
    @WithMockJwtAuth(authorities = {"ROLE_Ro-MU-CA"},
        claims = @OpenIdClaims(otherClaims = @Claims(stringClaims =
            {@StringClaim(name = "participant_id", value = "0949d1a0")})))
    @Order(60)
    public void deleteParticipantWithAllUsersSuccessShouldReturnSuccessResponse() throws Exception {
        ParticipantMetaData metadata = getDefaultParticipantMetadata();
        removeOldParticipantAndAddNew(metadata);
        User userOfParticipant = getUserOfParticipant(metadata.getId());
        UserProfile profile = userDao.create(userOfParticipant);
        userDao.updateRoles(profile.getId(), List.of(PARTICIPANT_ADMIN_ROLE));
        assertDoesNotThrow(() -> fileStore.readFile(metadata.getSdHash()));

        String response = mockMvc
            .perform(MockMvcRequestBuilders.delete("/participants/{participantId}", metadata.getId()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        ParticipantMetaData newParticipant = objectMapper.readValue(response, ParticipantMetaData.class);
        assertNotNull(newParticipant);

        assertThrows(FileNotFoundException.class, () -> fileStore.readFile(metadata.getSdHash()));
        assertThrows(NotFoundException.class, () -> selfDescriptionStore.getByHash(metadata.getSdHash()));

        assertEquals(1, userDao.search( "ae366624-8371-401d-b2c4-518d2f308a15",0,1).getResults().size());
        clearStorages(metadata);
    }

    private void clearStorages(ParticipantMetaData part) {
        try {
            participantDao.delete(part.getId());
        } catch (Exception ignored) {
        }
        try {
            fileStore.deleteFile(part.getSdHash());
            selfDescriptionStore.deleteSelfDescription(part.getSdHash());
            //TODO: graphdb need to add after implementation
        } catch (Exception ignored) {
        }
    }

    private ParticipantMetaData getDefaultParticipantMetadata () throws IOException {
        return new ParticipantMetaData("0949d1a0",
            "did:example:holder",
            "did:example:holder#key",
            getMockFileDataAsString("default_participant.json"));
    }

    public User getUserOfParticipant(String partId) {
        return new User(partId, "testUserName", "testLastName", "test@gmail", List.of());
    }

    private void addToSdStorage(String json) {
        ContentAccessorDirect contentAccessor = new ContentAccessorDirect(json);
        VerificationResultParticipant verResult =
            verificationService.verifyParticipantSelfDescription(contentAccessor);
        SelfDescriptionMetadata sdMetadata = new SelfDescriptionMetadata(contentAccessor, verResult);
        selfDescriptionStore.storeSelfDescription(sdMetadata, verResult);
    }

    private void removeOldParticipantAndAddNew(ParticipantMetaData metadata){
        clearStorages(metadata);
        addToStorages(metadata);
    }
    private void addToStorages(ParticipantMetaData metadata) {
        addToSdStorage(metadata.getSelfDescription());
        participantDao.create(metadata);
    }

    private void storagesAssertDoesNotThrow(ParticipantMetaData metaData) {
        assertDoesNotThrow(() -> fileStore.readFile(metaData.getSdHash()));
        assertDoesNotThrow(() -> selfDescriptionStore.getByHash(metaData.getSdHash()));
    }
}
