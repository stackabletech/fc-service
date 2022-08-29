package eu.gaiax.difs.fc.server.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.c4_soft.springaddons.security.oauth2.test.annotations.Claims;
import com.c4_soft.springaddons.security.oauth2.test.annotations.OpenIdClaims;
import com.c4_soft.springaddons.security.oauth2.test.annotations.StringClaim;
import com.c4_soft.springaddons.security.oauth2.test.annotations.WithMockJwtAuth;
import eu.gaiax.difs.fc.core.pojo.ParticipantMetaData;
import eu.gaiax.difs.fc.core.service.filestore.impl.FileStoreImpl;
import eu.gaiax.difs.fc.core.service.sdstore.SelfDescriptionStore;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.event.annotation.BeforeTestClass;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.gaiax.difs.fc.api.generated.model.Participants;
import eu.gaiax.difs.fc.api.generated.model.UserProfiles;
import eu.gaiax.difs.fc.core.dao.impl.ParticipantDaoImpl;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@AutoConfigureEmbeddedDatabase(provider = DatabaseProvider.ZONKY)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ParticipantsControllerTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FileStoreImpl fileStore;

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
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeTestClass
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
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
    @WithMockUser(authorities = {"ROLE_Ro-MU-CA"})
    @Order(10)
    public void addParticipantShouldReturnCreatedResponse() throws Exception {
        
        String json = readFileFromResources("new_participant.json");
        ParticipantMetaData part = new ParticipantMetaData("ebc6f1c2", "did:example:holder", "did:example:holder#key-1", json);
        setupKeycloak(HttpStatus.SC_CREATED, part);
        
        try {
            fileStore.deleteFile(SelfDescriptionStore.STORE_NAME, part.getSdHash());
        } catch(FileNotFoundException ex) {
            // possible..
        }

        String response = mockMvc
            .perform(MockMvcRequestBuilders.post("/participants")
            .contentType("application/json")
            .content(json))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        part = objectMapper.readValue(response, ParticipantMetaData.class);
        assertNotNull(part);
        assertEquals("ebc6f1c2", part.getId());
        assertEquals("did:example:holder", part.getName());
        assertEquals("did:example:holder#key-1", part.getPublicKey());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_Ro-MU-CA"})
    @Order(20)
    public void getParticipantShouldReturnSuccessResponse() throws Exception {

        String json = readFileFromResources("new_participant.json");
        ParticipantMetaData part = new ParticipantMetaData("ebc6f1c2", "did:example:holder", "did:example:holder#key-1", json);
        setupKeycloak(HttpStatus.SC_OK, part);

        mockMvc
            .perform(MockMvcRequestBuilders.get("/participants/{participantId}", part.getId())
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_Ro-MU-CA"})
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
    @WithMockUser(authorities = {"ROLE_Ro-MU-CA"})
    @Order(20)
    public void getParticipantsShouldReturnCorrectNumber() throws Exception {

        String json = readFileFromResources("new_participant.json");
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
        // TODO:
        //assertEquals(1, parts.getTotalCount());
    }
    
    @Test
    @WithMockUser(authorities = {"ROLE_Ro-MU-CA"})
    @Order(20)
    public void getParticipantUsersShouldReturnCorrectNumber() throws Exception {

        ParticipantMetaData part = new ParticipantMetaData("ebc6f1c2", "did:example:holder", "did:example:holder#key-1", "empty SD");
        setupKeycloak(HttpStatus.SC_OK, part);

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
    @WithMockJwtAuth(authorities = {"ROLE_Ro-MU-CA"},
        claims = @OpenIdClaims(otherClaims = @Claims(stringClaims =
            {@StringClaim(name = "participant_id", value = "ebc6f1c2")})))
    @Order(30)
    public void updateParticipantShouldReturnSuccessResponse() throws Exception {
        
        String json = readFileFromResources("update_participant.json");
        ParticipantMetaData part = new ParticipantMetaData("ebc6f1c2", "did:example:holder", "did:example:holder#key-1", json);
        setupKeycloak(HttpStatus.SC_OK, part);
        
        try {
            fileStore.deleteFile(SelfDescriptionStore.STORE_NAME, part.getSdHash());
        } catch(FileNotFoundException ex) {
            // possible..
        }

        String response = mockMvc
            .perform(MockMvcRequestBuilders.put("/participants/{participantId}", part.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        part = objectMapper.readValue(response, ParticipantMetaData.class);
        assertNotNull(part);
        assertEquals("ebc6f1c2", part.getId());
        assertEquals("did:example:updated-holder", part.getName());
        assertEquals("did:example:holder#key-2", part.getPublicKey());
    }
    
    @Test
    @WithMockJwtAuth(authorities = {"ROLE_Ro-MU-CA"},
        claims = @OpenIdClaims(otherClaims = @Claims(stringClaims =
            {@StringClaim(name = "participant_id", value = "ebc6f1c2")})))
    @Order(40)
    public void deleteParticipantShouldReturnSuccessResponse() throws Exception {
        
        String json = readFileFromResources("update_participant.json");
        ParticipantMetaData part = new ParticipantMetaData("ebc6f1c2", "did:example:updated-holder", "did:example:holder#key-2", json);
        setupKeycloak(HttpStatus.SC_OK, part);

        String response = mockMvc
            .perform(MockMvcRequestBuilders.delete("/participants/{participantId}", part.getId()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        part = objectMapper.readValue(response, ParticipantMetaData.class);
        assertNotNull(part);
        assertEquals("ebc6f1c2", part.getId());
        assertEquals("did:example:updated-holder", part.getName());
        assertEquals("did:example:holder#key-2", part.getPublicKey());
    }
    

    private void setupKeycloak(int status, ParticipantMetaData part) {
        when(builder.build()).thenReturn(keycloak);
        when(keycloak.realm("gaia-x")).thenReturn(realmResource);
        when(realmResource.groups()).thenReturn(groupsResource);
        when(groupsResource.add(any())).thenReturn(Response.status(status).build());
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
        }
    }
    
    public static String readFileFromResources(String filename) { 
        URL resource = ParticipantsControllerTest.class.getClassLoader().getResource(filename);
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(Paths.get(resource.toURI()));
            return new String(bytes);  
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }
        
}
