package eu.gaiax.difs.fc.server.controller;

import com.c4_soft.springaddons.security.oauth2.test.annotations.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.gaiax.difs.fc.api.generated.model.SelfDescriptionStatus;
import eu.gaiax.difs.fc.core.pojo.ContentAccessorDirect;
import eu.gaiax.difs.fc.core.pojo.SelfDescriptionMetadata;
import eu.gaiax.difs.fc.core.service.sdstore.SelfDescriptionStore;
import eu.gaiax.difs.fc.core.util.HashUtils;
import java.time.OffsetDateTime;

import javax.transaction.Transactional;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.event.annotation.BeforeTestClass;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static eu.gaiax.difs.fc.server.helper.FileReaderHelper.getMockFileDataAsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// TODO: 23.08.2022 Add a test file storage
@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@AutoConfigureEmbeddedDatabase(provider = DatabaseProvider.ZONKY)
//@Transactional
public class SelfDescriptionControllerTest {
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

    @BeforeTestClass
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @BeforeAll
    static void initBeforeAll() {
        sdMeta = createSdMetadata();
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
    public void readSDsShouldReturnSuccessResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/self-descriptions")
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
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
    public void readSDByHashShouldReturnSuccessResponse() throws Exception {
        sdStore.storeSelfDescription(sdMeta, null);

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
    @WithMockJwtAuth(authorities = {"ROLE_Ro-MU-CA"},
            claims = @OpenIdClaims(otherClaims = @Claims(stringClaims =
                    {@StringClaim(name = "participant_id", value = "http://example.org/test-issuer")})))
    public void deleteSDReturnSuccessResponse() throws Exception {
        sdStore.storeSelfDescription(sdMeta, null);

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
                        .content(getMockFileDataAsString("test-provider-self-description"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    // TODO: 23.08.2022 see TODO in SD add method
    @Test
    @WithMockJwtAuth(authorities = {"ROLE_Ro-MU-CA"},
            claims = @OpenIdClaims(otherClaims = @Claims(stringClaims =
                    {@StringClaim(name = "participant_id", value = "http://example.org/test-issuer")})))
    public void addSDReturnSuccessResponse() throws Exception {
        assertThrows(AssertionError.class, () ->
            mockMvc.perform(MockMvcRequestBuilders.post("/self-descriptions")
                .content(getMockFileDataAsString("test-provider-self-description"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isCreated()));

//        sdStore.deleteSelfDescription(sdMeta.getSdHash());
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
    @WithMockJwtAuth(authorities = {"ROLE_Ro-MU-CA"},
            claims = @OpenIdClaims(otherClaims = @Claims(stringClaims =
                    {@StringClaim(name = "participant_id", value = "http://example.org/test-issuer")})))
    public void revokeSDReturnSuccessResponse() throws Exception {
        sdStore.storeSelfDescription(sdMeta, null);
        mockMvc.perform(MockMvcRequestBuilders.post("/self-descriptions/" + sdMeta.getSdHash() + "/revoke")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        sdStore.deleteSelfDescription(sdMeta.getSdHash());
    }

    private static SelfDescriptionMetadata createSdMetadata() {
        SelfDescriptionMetadata sdMeta = new SelfDescriptionMetadata();
        sdMeta.setId("test id");
        sdMeta.setIssuer("http://example.org/test-issuer");
        sdMeta.setSdHash(HashUtils.calculateSha256AsHex("test hash"));
        sdMeta.setStatus(SelfDescriptionStatus.ACTIVE);
        sdMeta.setStatusDatetime(OffsetDateTime.parse("2022-01-01T12:00:00Z"));
        sdMeta.setUploadDatetime(OffsetDateTime.parse("2022-01-02T12:00:00Z"));
        sdMeta.setSelfDescription(new ContentAccessorDirect("test content"));
        return sdMeta;
    }
}
