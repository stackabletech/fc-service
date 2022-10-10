package eu.gaiax.difs.fc.server.controller;

import static eu.gaiax.difs.fc.core.dao.impl.UserDaoImpl.getUsername;
import static eu.gaiax.difs.fc.core.dao.impl.UserDaoImpl.toUserRepo;
import static eu.gaiax.difs.fc.server.util.CommonConstants.PARTICIPANT_ADMIN_ROLE;
import static eu.gaiax.difs.fc.server.util.CommonConstants.SD_ADMIN_ROLE;
import static eu.gaiax.difs.fc.server.util.TestCommonConstants.CATALOGUE_ADMIN_ROLE_WITH_PREFIX;
import static eu.gaiax.difs.fc.server.util.TestCommonConstants.DEFAULT_PARTICIPANT_ID;
import static eu.gaiax.difs.fc.server.helper.UserServiceHelper.getAllRoles;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.gaiax.difs.fc.api.generated.model.Error;
import eu.gaiax.difs.fc.core.dao.UserDao;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
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
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
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
    private RolesResource rolesResource;
    @MockBean
    private RoleMappingResource roleMappingResource;
    @MockBean
    private RoleScopeResource roleScopeResource;
    @MockBean
    private UserResource userResource;
    @Autowired
    private UserDao userDao;
    @Autowired
    private ObjectMapper objectMapper;

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
        User user = getTestUser("name1", "surname2");
        String userId = UUID.randomUUID().toString();
        setupKeycloak(SC_CREATED, user, userId);
        String response = mockMvc
            .perform(MockMvcRequestBuilders.post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        UserProfile profile = objectMapper.readValue(response, UserProfile.class);
        assertThatResponseUserHasValidData(user, profile);
    }

    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    public void addDuplicateSDReturnConflictWithKeycloak() throws Exception {
        User user = getTestUser("name2", "surname2");
        setupKeycloak(HttpStatus.SC_CREATED, user, UUID.randomUUID().toString());
        userDao.create(user);
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
        User user = getTestUser("name3", "surname3");
        String userId = UUID.randomUUID().toString();
        setupKeycloak(HttpStatus.SC_OK, user, userId);
        UserProfile existed = userDao.create(user);
        mockMvc
            .perform(MockMvcRequestBuilders.get("/users/{userId}", existed.getId())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    public void wrongUserShouldReturnNotFoundResponse() throws Exception {
        setupKeycloak(HttpStatus.SC_NOT_FOUND, null, "123");

        mockMvc
            .perform(MockMvcRequestBuilders.get("/users/{userId}", "123")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    public void getUsersShouldReturnCorrectNumber() throws Exception {
        User user = getTestUser("name4", "surname4");
        setupKeycloak(HttpStatus.SC_OK, user, null);

        MvcResult result = mockMvc
            .perform(MockMvcRequestBuilders.get("/users?offset={offset}&limit={limit}", null, null)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
        UserProfiles users = objectMapper.readValue(result.getResponse().getContentAsString(), UserProfiles.class);
        assertNotNull(users);
        // Counts with catalogue administrator user
        assertEquals(1, users.getItems().size());
    }

    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    public void deleteUserShouldReturnSuccessResponse() throws Exception {
        User user = getTestUser("name5", "surname5");
        String userId = UUID.randomUUID().toString();
        setupKeycloak(HttpStatus.SC_NO_CONTENT, user, userId);
        UserProfile existed = userDao.create(user);
        String response = mockMvc
            .perform(MockMvcRequestBuilders.delete("/users/{userId}", existed.getId()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        UserProfile profile = objectMapper.readValue(response, UserProfile.class);
        assertThatResponseUserHasValidData(user, profile);
    }

    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    public void updateUserShouldReturnSuccessResponse() throws Exception {
        User user = getTestUser("name6", "surname6");
        String userId = UUID.randomUUID().toString();
        setupKeycloak(HttpStatus.SC_OK, user, userId);
        UserProfile existed = userDao.create(user);
        user = getTestUser("changed name", "changed surname");
        mockMvc
            .perform(MockMvcRequestBuilders.put("/users/{userId}", existed.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
            .andExpect(status().isOk());
    }
    
    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    public void updateUserRolesShouldReturnSuccessResponse() throws Exception {
        User user = getTestUser("name7", "surname7");
        String userId = UUID.randomUUID().toString();
        setupKeycloak(HttpStatus.SC_OK, user, userId);
        UserProfile existed = userDao.create(user);
        mockMvc
            .perform(MockMvcRequestBuilders.put("/users/{userId}/roles", existed.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(SD_ADMIN_ROLE))))
            .andExpect(status().isOk());
    }


    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    public void changeUserPermissionShouldReturnSuccessResponse() throws Exception {
        User user = getTestUser("new_user", "new_user");
        String userId = UUID.randomUUID().toString();
        setupKeycloak(HttpStatus.SC_OK, user, userId);

        UserProfile existed = userDao.create(user);
        assertEquals(1, existed.getRoleIds().size());
        assertTrue( existed.getRoleIds().contains(SD_ADMIN_ROLE));

        when(roleScopeResource.listAll())
            .thenReturn(List.of(new RoleRepresentation(PARTICIPANT_ADMIN_ROLE, PARTICIPANT_ADMIN_ROLE, false)));

        String response = mockMvc
            .perform(MockMvcRequestBuilders.put("/users/{userId}/roles", existed.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(PARTICIPANT_ADMIN_ROLE))))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();

        UserProfile updated = objectMapper.readValue(response, UserProfile.class);
        assertEquals(1, updated.getRoleIds().size());
        assertTrue(updated.getRoleIds().containsAll(List.of(PARTICIPANT_ADMIN_ROLE)));
    }


    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    public void updateDuplicatedUserRoleShouldReturnSuccessResponse() throws Exception {
        User user = getTestUser("name8", "surname8").addRoleIdsItem(PARTICIPANT_ADMIN_ROLE);
        String userId = UUID.randomUUID().toString();
        setupKeycloak(HttpStatus.SC_OK, user, userId);
        UserProfile existed = userDao.create(user);

        mockMvc.perform(MockMvcRequestBuilders.put("/users/{userId}/roles", existed.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(PARTICIPANT_ADMIN_ROLE))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        UserProfile newProfile = userDao.select(existed.getId());
        assertNotNull(newProfile);
        assertEquals(2, newProfile.getRoleIds().size());
        assertTrue(newProfile.getRoleIds().containsAll(List.of(PARTICIPANT_ADMIN_ROLE, SD_ADMIN_ROLE)));
    }

    private void setupKeycloak(int status, User user, String id) {
        when(builder.build()).thenReturn(keycloak);
        when(keycloak.realm("gaia-x")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(realmResource.roles()).thenReturn(rolesResource);
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
            when(rolesResource.list()).thenReturn(getAllRoles());
            when(userResource.roles()).thenReturn(roleMappingResource);
            when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
            List<RoleRepresentation> roleRepresentations =  new ArrayList<>();
            user.getRoleIds().forEach(roleId-> roleRepresentations.add(new RoleRepresentation(roleId, roleId, false)));
            when(roleScopeResource.listAll()).thenReturn(roleRepresentations);
        }
    }

    private User getTestUser(String firstName, String lastName) {
        return new User()
            .email(firstName + "." + lastName + "@test.org")
            .participantId(DEFAULT_PARTICIPANT_ID)
            .firstName(firstName)
            .lastName(lastName)
            .addRoleIdsItem(SD_ADMIN_ROLE);
    }

    private static void assertThatResponseUserHasValidData(final User excepted, final UserProfile actual) {
        assertNotNull(actual);
        assertNotNull(actual.getId());
        assertNotNull(actual.getUsername());
        assertEquals(excepted.getEmail(), actual.getEmail());
        assertEquals(excepted.getFirstName(), actual.getFirstName());
        assertEquals(excepted.getLastName(), actual.getLastName());
        assertEquals(excepted.getParticipantId(), actual.getParticipantId());
    }
}
