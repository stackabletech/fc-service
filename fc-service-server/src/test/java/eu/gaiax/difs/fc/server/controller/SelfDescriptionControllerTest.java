package eu.gaiax.difs.fc.server.controller;

import static eu.gaiax.difs.fc.server.helper.FileReaderHelper.getMockFileDataAsString;
import static eu.gaiax.difs.fc.server.util.TestCommonConstants.CATALOGUE_ADMIN_ROLE_WITH_PREFIX;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.c4_soft.springaddons.security.oauth2.test.annotations.Claims;
import com.c4_soft.springaddons.security.oauth2.test.annotations.OpenIdClaims;
import com.c4_soft.springaddons.security.oauth2.test.annotations.StringClaim;
import com.c4_soft.springaddons.security.oauth2.test.annotations.WithMockJwtAuth;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.gaiax.difs.fc.api.generated.model.Error;
import eu.gaiax.difs.fc.api.generated.model.SelfDescription;
import eu.gaiax.difs.fc.api.generated.model.SelfDescriptionStatus;
import eu.gaiax.difs.fc.api.generated.model.SelfDescriptions;
import eu.gaiax.difs.fc.core.exception.NotFoundException;
import eu.gaiax.difs.fc.core.pojo.ContentAccessorDirect;
import eu.gaiax.difs.fc.core.pojo.SelfDescriptionMetadata;
import eu.gaiax.difs.fc.core.pojo.VerificationResult;
import eu.gaiax.difs.fc.core.pojo.VerificationResultOffering;
import eu.gaiax.difs.fc.core.service.filestore.FileStore;
import eu.gaiax.difs.fc.core.service.sdstore.SelfDescriptionStore;
import eu.gaiax.difs.fc.core.service.verification.VerificationService;
import eu.gaiax.difs.fc.core.util.HashUtils;
import eu.gaiax.difs.fc.testsupport.config.EmbeddedNeo4JConfig;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import javax.transaction.Transactional;

import org.junit.jupiter.api.*;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.neo4j.harness.Neo4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.annotation.BeforeTestClass;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

// TODO: 23.08.2022 Add a test Graph storage
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureEmbeddedDatabase(provider = DatabaseProvider.ZONKY)
@Import(EmbeddedNeo4JConfig.class)
//@Transactional
public class SelfDescriptionControllerTest {
    private final static String TEST_ISSUER = "http://example.org/test-issuer";
    private final static String SD_FILE_NAME = "default-sd.json";

    @Autowired
    private Neo4j embeddedDatabaseServer;

    @AfterAll
    void closeNeo4j() {
        embeddedDatabaseServer.close();
    }

    // TODO: 14.07.2022 After adding business logic, need to fix/add tests, taking into account exceptions
    private static SelfDescriptionMetadata sdMeta;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SelfDescriptionStore sdStore;

    @SpyBean(name = "sdFileStore")
    private FileStore fileStore;

    @Autowired
    private VerificationService verificationService;

    @BeforeTestClass
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @BeforeAll
    static void initBeforeAll() throws IOException {
        sdMeta = createSdMetadata();
    }

    @AfterAll
    public void storageSelfCleaning() throws IOException {
        fileStore.clearStorage();
        embeddedDatabaseServer.close();
    }

    @Test
    public void readSDsShouldReturnUnauthorizedResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/self-descriptions")
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    public void readSDsShouldReturnBadRequestResponse() throws Exception {
      mockMvc.perform(MockMvcRequestBuilders.get("/self-descriptions?status=123")
              .with(csrf())
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    public void readSDsShouldReturnSuccessResponse() throws Exception {
        sdStore.storeSelfDescription(sdMeta, getStaticVerificationResult());
        MvcResult result =  mockMvc.perform(MockMvcRequestBuilders.get("/self-descriptions")
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
            .andReturn();

        SelfDescriptions selfDescriptions = objectMapper.readValue(result.getResponse().getContentAsString(), SelfDescriptions.class);
        assertNotNull(selfDescriptions);
        assertEquals(1, selfDescriptions.getItems().size());
        assertEquals(1, selfDescriptions.getTotalCount());
        sdStore.deleteSelfDescription(sdMeta.getSdHash());
    }

    @Test
    public void readSDByHashShouldReturnUnauthorizedResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/self-descriptions/" + sdMeta.getSdHash())
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    public void readSDByHashShouldReturnNotFoundResponse() throws Exception {
      mockMvc.perform(MockMvcRequestBuilders.get("/self-descriptions/123").with(csrf()))
          .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    public void readSDByHashShouldReturnSuccessResponse() throws Exception {
        sdStore.storeSelfDescription(sdMeta, getStaticVerificationResult());

        mockMvc.perform(MockMvcRequestBuilders.get("/self-descriptions/" + sdMeta.getSdHash())
                .with(csrf()))
                //.accept("application/ld+json"))
            .andExpect(status().isOk());
        sdStore.deleteSelfDescription(sdMeta.getSdHash());
    }

    @Test
    public void deleteSDhShouldReturnUnauthorizedResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/self-descriptions/" + sdMeta.getSdHash())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    public void deleteSDReturnForbiddenResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/self-descriptions/" + sdMeta.getSdHash())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockJwtAuth(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX}, claims = @OpenIdClaims(otherClaims = @Claims(stringClaims = {
        @StringClaim(name = "participant_id", value = "")})))
    public void deleteSdWithoutIssuerReturnForbiddenResponse() throws Exception {
      sdStore.storeSelfDescription(sdMeta, getStaticVerificationResult());
      mockMvc.perform(MockMvcRequestBuilders.delete("/self-descriptions/" + sdMeta.getSdHash())
              .with(csrf())
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden());
      sdStore.deleteSelfDescription(sdMeta.getSdHash());
    }

    @Test
    @WithMockJwtAuth(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX}, claims = @OpenIdClaims(otherClaims = @Claims(stringClaims = {
        @StringClaim(name = "participant_id", value = TEST_ISSUER)})))
    public void deleteSDReturnNotFoundResponse() throws Exception {
      mockMvc.perform(MockMvcRequestBuilders.delete("/self-descriptions/" + sdMeta.getSdHash())
              .with(csrf())
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound());
    }

    @Test
    @WithMockJwtAuth(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX}, claims = @OpenIdClaims(otherClaims = @Claims(stringClaims = {
        @StringClaim(name = "participant_id", value = TEST_ISSUER)})))
    public void deleteSDReturnSuccessResponse() throws Exception {
        sdStore.storeSelfDescription(sdMeta, getStaticVerificationResult());

        mockMvc.perform(MockMvcRequestBuilders.delete("/self-descriptions/" + sdMeta.getSdHash())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void addSDShouldReturnUnauthorizedResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/self-descriptions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    public void addSDReturnForbiddenResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/self-descriptions")
                        .content(getMockFileDataAsString(SD_FILE_NAME))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockJwtAuth(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX}, claims = @OpenIdClaims(otherClaims = @Claims(stringClaims = {
        @StringClaim(name = "participant_id", value = TEST_ISSUER)})))
    public void addSDWithoutIssuerReturnForbiddenResponse() throws Exception {
      mockMvc.perform(MockMvcRequestBuilders.post("/self-descriptions")
              .content(getMockFileDataAsString("sd-without-credential-subject.json"))
              .with(csrf())
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isForbidden());
    }

    @Test
    @WithMockJwtAuth(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX}, claims = @OpenIdClaims(otherClaims = @Claims(stringClaims = {
        @StringClaim(name = "participant_id", value = TEST_ISSUER)})))
    public void addSDReturnSuccessResponse() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/self-descriptions")
                .content(getMockFileDataAsString(SD_FILE_NAME))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated())
            .andReturn();

        SelfDescription sd = objectMapper.readValue(result.getResponse().getContentAsString(), SelfDescription.class);
        sdStore.deleteSelfDescription(sd.getSdHash());
    }

    @Test
    @WithMockJwtAuth(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX}, claims = @OpenIdClaims(otherClaims = @Claims(stringClaims = {
        @StringClaim(name = "participant_id", value = TEST_ISSUER)})))
    public void addDuplicateSDReturnConflictWithSdStorage() throws Exception {
      String sd = getMockFileDataAsString(SD_FILE_NAME);
      ContentAccessorDirect contentAccessor = new ContentAccessorDirect(sd);

      SelfDescriptionMetadata sdMetadata =
          new SelfDescriptionMetadata(contentAccessor, "id123", TEST_ISSUER, new ArrayList<>());

      sdStore.storeSelfDescription(sdMetadata, getStaticVerificationResult());
      mockMvc.perform(MockMvcRequestBuilders
              .post("/self-descriptions")
              .content(sd)
              .with(csrf())
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isConflict());
      sdStore.deleteSelfDescription(sdMetadata.getSdHash());
      assertThrows(NotFoundException.class, () -> sdStore.getByHash(sdMetadata.getSdHash()));
    }

    // TODO: 05.09.2022 Need to add a test to check the correct scenario with graph storage when it is added
    @Test
    @WithMockJwtAuth(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX}, claims = @OpenIdClaims(otherClaims = @Claims(stringClaims = {
        @StringClaim(name = "participant_id", value = TEST_ISSUER)})))
    public void addSDFailedThanAllTransactionsRolledBack() throws Exception {
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        doThrow((new IOException("Some server exception")))
            .when(fileStore).storeFile(hashCaptor.capture(), any());

        mockMvc.perform(MockMvcRequestBuilders.post("/self-descriptions")
                .content(getMockFileDataAsString(SD_FILE_NAME))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isInternalServerError());

        String hash = hashCaptor.getValue();

        assertThrowsExactly(FileNotFoundException.class,
            () -> fileStore.readFile(hash));
        assertThrows(NotFoundException.class, () -> sdStore.getByHash(hash));
    }

    @Test
    public void revokeSDhShouldReturnUnauthorizedResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/self-descriptions/123/revoke")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    public void revokeSDReturnForbiddenResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/self-descriptions/123/revoke")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockJwtAuth(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX}, claims = @OpenIdClaims(otherClaims = @Claims(stringClaims = {
        @StringClaim(name = "participant_id", value = TEST_ISSUER)})))
    public void revokeSDReturnNotFound() throws Exception {
      mockMvc.perform(MockMvcRequestBuilders.post("/self-descriptions/" + sdMeta.getSdHash() + "/revoke")
              .with(csrf())
              .contentType(MediaType.APPLICATION_JSON)
              .accept(MediaType.APPLICATION_JSON))
          .andExpect(status().isNotFound());
    }

    @Test
    @WithMockJwtAuth(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX}, claims = @OpenIdClaims(otherClaims = @Claims(stringClaims = {
        @StringClaim(name = "participant_id", value = TEST_ISSUER)})))
    public void revokeSDReturnSuccessResponse() throws Exception {
        final VerificationResult vr = new VerificationResult("vhash", new ArrayList<>(), new ArrayList<>(),
            OffsetDateTime.now(), "lifecyclestatus", "issuer", LocalDate.now());
        sdStore.storeSelfDescription(sdMeta, vr);
//        sdStore.storeSelfDescription(sdMeta, getStaticVerificationResult());
        mockMvc.perform(MockMvcRequestBuilders.post("/self-descriptions/" + sdMeta.getSdHash() + "/revoke")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        sdStore.deleteSelfDescription(sdMeta.getSdHash());
    }

    @Test
    @WithMockJwtAuth(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX}, claims = @OpenIdClaims(otherClaims = @Claims(stringClaims = {
        @StringClaim(name = "participant_id", value = TEST_ISSUER)})))
    public void revokeSdWithNotActiveStatusReturnConflictResponse() throws Exception {
        final VerificationResult vr = new VerificationResult("vhash", new ArrayList<>(), new ArrayList<>(),
            OffsetDateTime.now(), "lifecyclestatus", "issuer", LocalDate.now());
        SelfDescriptionMetadata metadata = sdMeta;
        metadata.setStatus(SelfDescriptionStatus.DEPRECATED);
        sdStore.storeSelfDescription(metadata, vr);
        MvcResult result = mockMvc
            .perform(MockMvcRequestBuilders.post("/self-descriptions/" + sdMeta.getSdHash() + "/revoke")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isConflict()).andReturn();
        Error error = objectMapper.readValue(result.getResponse().getContentAsString(), Error.class);
        assertEquals("The SD status cannot be changed because the SD Metadata status is deprecated", error.getMessage());
        sdStore.deleteSelfDescription(metadata.getSdHash());
    }

    private static SelfDescriptionMetadata createSdMetadata() throws IOException {
        SelfDescriptionMetadata sdMeta = new SelfDescriptionMetadata();
        sdMeta.setId("test id");
        sdMeta.setIssuer(TEST_ISSUER);
        sdMeta.setSdHash(HashUtils.calculateSha256AsHex("test hash"));
        sdMeta.setStatus(SelfDescriptionStatus.ACTIVE);
        sdMeta.setStatusDatetime(OffsetDateTime.parse("2022-01-01T12:00:00Z"));
        sdMeta.setUploadDatetime(OffsetDateTime.parse("2022-01-02T12:00:00Z"));
        sdMeta.setSelfDescription(new ContentAccessorDirect(getMockFileDataAsString(SD_FILE_NAME)));
        return sdMeta;
    }

    private VerificationResultOffering getStaticVerificationResult() {
        return verificationService.verifyOfferingSelfDescription(sdMeta.getSelfDescription());
    }
}
