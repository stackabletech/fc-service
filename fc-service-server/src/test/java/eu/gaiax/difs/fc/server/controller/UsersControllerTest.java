package eu.gaiax.difs.fc.server.controller;

import static eu.gaiax.difs.fc.core.dao.impl.UserDaoImpl.getUsername;
import static eu.gaiax.difs.fc.core.dao.impl.UserDaoImpl.toUserRepo;
import static eu.gaiax.difs.fc.server.util.CommonConstants.PARTICIPANT_ADMIN_ROLE;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.gaiax.difs.fc.api.generated.model.Error;
import eu.gaiax.difs.fc.core.dao.UserDao;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.gaiax.difs.fc.api.generated.model.User;
import eu.gaiax.difs.fc.api.generated.model.UserProfile;
import eu.gaiax.difs.fc.api.generated.model.UserProfiles;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@AutoConfigureEmbeddedDatabase(provider = DatabaseProvider.ZONKY)
public class UsersControllerTest {
    private final String CATALOGUE_ADMIN_ROLE_WITH_PREFIX = "ROLE_" + PARTICIPANT_ADMIN_ROLE;

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KeycloakBuilder builder;
    @MockBean
    private Keycloak keycloak;
    @MockBean
    private RealmResource realmResource;
    @MockBean
    private UsersResource usersResource;
    @MockBean
    private UserResource userResource;
    @Autowired
    private UserDao userDao;
    @Autowired
    private  ObjectMapper objectMapper;

    @BeforeTestClass
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    public void userAuthShouldReturnUnauthorizedResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/users")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    public void userAuthShouldReturnForbiddenResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/users")).andExpect(status().isForbidden());
    }
    
    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    public void addUserShouldReturnCreatedResponse() throws Exception {
        
        User user = getTestUser("unit-test", "user224", "did:example:holder");
        setupKeycloak(SC_CREATED, user, UUID.randomUUID().toString());

        String response = mockMvc
            .perform(MockMvcRequestBuilders.post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(user)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        UserProfile profile = objectMapper.readValue(response, UserProfile.class);
        assertNotNull(profile);
        assertEquals(user.getEmail(), profile.getEmail());
        assertEquals(user.getFirstName(), profile.getFirstName());
        assertEquals(user.getLastName(), profile.getLastName());
        assertEquals(user.getParticipantId(), profile.getParticipantId());
        assertNotNull(profile.getId());
        assertNotNull(profile.getUsername());
    }

    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    public void addDuplicateSDReturnConflictWithKeycloak() throws Exception {
        User user = getTestUser("user", "test224", "groupId224");
        setupKeycloak(HttpStatus.SC_CREATED, user, UUID.randomUUID().toString());
        when(usersResource.search(getUsername(user))).thenReturn(List.of(toUserRepo(user)));
        when(usersResource.create(any()))
            .thenReturn(Response.status(HttpStatus.SC_CONFLICT, "Conflict")
                    .entity(new ByteArrayInputStream("{ \"errorMessage\" : \"User exists with same username\"}".getBytes()))
                .build());

        String response = mockMvc.perform(MockMvcRequestBuilders.post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
            .andExpect(status().isConflict())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Error error = objectMapper.readValue(response, Error.class);
        assertNotNull(error);
        assertEquals("User exists with same username", error.getMessage());
    }

    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    public void getUserShouldReturnSuccessResponse() throws Exception {
        
        User user = getTestUser("unit-test", "user22", "participant one");
        String userId = "f7e47bd2-8ae4-4fd7-8b03-6e2fdcf1c912";
        setupKeycloak(HttpStatus.SC_OK, user, userId);

        mockMvc
            .perform(MockMvcRequestBuilders.get("/users/{userId}", userId)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    public void wrongUserShouldReturnNotFoundResponse() throws Exception {
        
        String userId = "unknown";
        setupKeycloak(HttpStatus.SC_NOT_FOUND, null, userId);

        mockMvc
            .perform(MockMvcRequestBuilders.get("/users/{userId}", userId)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }
    
    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    public void getUsersShouldReturnCorrectNumber() throws Exception {
        
        User user = getTestUser("unit-test", "user33", "participant one");
        setupKeycloak(HttpStatus.SC_OK, user, null);

        MvcResult result = mockMvc
            .perform(MockMvcRequestBuilders.get("/users?offset={offset}&limit={limit}", null, null)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
        UserProfiles users = objectMapper.readValue(result.getResponse().getContentAsString(), UserProfiles.class);
        assertNotNull(users);
        assertEquals(1, users.getItems().size());
    }
    
    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    public void deleteUserShouldReturnSuccessResponse() throws Exception {
        
        User user = getTestUser("unit-test", "user11", "ebc6f1c2");
        String userId = "0fb1eb26-4f92-4941-a782-f092e19dedcb";
        setupKeycloak(HttpStatus.SC_NO_CONTENT, user, userId);

        String response = mockMvc
            .perform(MockMvcRequestBuilders.delete("/users/{userId}", userId))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        UserProfile profile = objectMapper.readValue(response, UserProfile.class);
        assertNotNull(profile);
        assertEquals(user.getEmail(), profile.getEmail());
        assertEquals(user.getFirstName(), profile.getFirstName());
        assertEquals(user.getLastName(), profile.getLastName());
        assertEquals(user.getParticipantId(), profile.getParticipantId());
        assertNotNull(profile.getId());
        assertNotNull(profile.getUsername());       
    }
    
    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    public void updateUserShouldReturnSuccessResponse() throws Exception {
        
        User user = getTestUser("unit-test", "user", "participant one");
        String userId = "ae366624-8371-401d-b2c4-518d2f308a15";
        setupKeycloak(HttpStatus.SC_OK, user, userId);
        user = getTestUser("unit-test", "changed user", "participant one");
        
        mockMvc
            .perform(MockMvcRequestBuilders.put("/users/{userId}", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(user)))
            .andExpect(status().isOk());
    }
    
    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    public void updateUserRolesShouldReturnSuccessResponse() throws Exception {
        
        User user = getTestUser("unit-test", "user", "ebc6f1c2");
        String userId = "ae366624-8371-401d-b2c4-518d2f308a15";
        setupKeycloak(HttpStatus.SC_OK, user, userId);

        mockMvc
            .perform(MockMvcRequestBuilders.put("/users/{userId}/roles", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(List.of("ROLE_test1", "ROLE_test2"))))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    public void updateDuplicatedUserRoleShouldReturnConflictError() throws Exception {
        User user = getTestUser("unit-test", "user", "ebc6f1c2");
        String id = "ae366624-8371-401d-b2c4-518d2f308a15";
        setupKeycloak(HttpStatus.SC_OK, user, id);

        userDao.create(user);

        mockMvc.perform(MockMvcRequestBuilders.put("/users/{userId}/roles", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of("ROLE_test"))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        UserProfile userProfile = userDao.select(id);
        assertNotNull(userProfile);
        assertEquals(1, userProfile.getRoleIds().size());
        assertEquals(List.of("ROLE_test"), userProfile.getRoleIds());
    }

    private void setupKeycloak(int status, User user, String id) {
        when(builder.build()).thenReturn(keycloak);
        when(keycloak.realm("gaia-x")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        if (user == null) {
            when(usersResource.create(any())).thenReturn(Response.status(status).build());
            when(usersResource.delete(any())).thenThrow(new NotFoundException("404 NOT FOUND"));
            when(usersResource.list(any(), any())).thenReturn(List.of());
            when(usersResource.search(any())).thenReturn(List.of());
            when(usersResource.get(any())).thenThrow(new NotFoundException("404 NOT FOUND"));
        } else {
            when(usersResource.create(any())).thenReturn(Response.status(SC_CREATED).entity(user).build());
            when(usersResource.delete(any())).thenReturn(Response.status(status).build());
            UserRepresentation userRepo = toUserRepo(user);
            userRepo.setId(id);
            when(usersResource.list(any(), any())).thenReturn(List.of(userRepo));
            when(usersResource.search(userRepo.getUsername())).thenReturn(List.of(userRepo));
            when(usersResource.get(any())).thenReturn(userResource);
            when(userResource.toRepresentation()).thenReturn(userRepo);
        }
    }
    
    private User getTestUser(String firstName, String lastName, String groupId) {
        return new User()
            .email(firstName + "." + lastName + "@test.org")
            .participantId(groupId)
            .firstName(firstName)
            .lastName(lastName)
            .addRoleIdsItem("ROLE_test");
    }
    
}
