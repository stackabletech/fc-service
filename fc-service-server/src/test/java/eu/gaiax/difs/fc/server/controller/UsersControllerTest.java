package eu.gaiax.difs.fc.server.controller;

import static eu.gaiax.difs.fc.server.util.TestCommonConstants.CATALOGUE_ADMIN_ROLE_WITH_PREFIX;
import static eu.gaiax.difs.fc.server.util.TestCommonConstants.CATALOGUE_PARTICIPANT_ADMIN_ROLE_ID;
import static eu.gaiax.difs.fc.server.util.TestCommonConstants.DEFAULT_PARTICIPANT_ID;
import static eu.gaiax.difs.fc.server.util.TestCommonConstants.SD_ADMIN_ROLE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.gaiax.difs.fc.api.generated.model.Error;
import eu.gaiax.difs.fc.core.dao.UserDao;

import eu.gaiax.difs.fc.server.controller.common.EmbeddedKeycloakTest;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.event.annotation.BeforeTestClass;
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

@Slf4j
@AutoConfigureMockMvc
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT, properties = {"server.port=9091"})
public class UsersControllerTest extends EmbeddedKeycloakTest {
    @Autowired
    private WebApplicationContext context;
    @Autowired
    private MockMvc mockMvc;
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
        userDao.delete(profile.getId());
    }

    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    public void addDuplicateSDReturnConflictWithKeycloak() throws Exception {
        User user = getTestUser("name2", "surname2");
        UserProfile existed = userDao.create(user);
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
        userDao.delete(existed.getId());
    }

    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX},
        username = "catalog_admin", password = "catalog_admin")
    public void getUserShouldReturnSuccessResponse() throws Exception {
        User user = getTestUser("name3", "surname3");
        UserProfile existed = userDao.create(user);
        mockMvc
            .perform(MockMvcRequestBuilders.get("/users/{userId}", existed.getId())
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        userDao.delete(existed.getId());
    }

    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    public void wrongUserShouldReturnNotFoundResponse() throws Exception {
        mockMvc
            .perform(MockMvcRequestBuilders.get("/users/{userId}", "123")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX},
        username = "catalog_admin", password = "catalog_admin")
    public void getUsersShouldReturnSuccessResponse() throws Exception {
        User user = getTestUser("name4", "surname4");
        UserProfile existed = userDao.create(user);
        MvcResult result = mockMvc
            .perform(MockMvcRequestBuilders.get("/users?offset={offset}&limit={limit}", null, null)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();
        UserProfiles users = objectMapper.readValue(result.getResponse().getContentAsString(), UserProfiles.class);
        assertNotNull(users);
        // Counts with catalogue administrator user
        assertEquals(2, users.getItems().size());
        userDao.delete(existed.getId());
    }

    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX},
        username = "catalog_admin", password = "catalog_admin")
    public void getUsersWithTotalCountShouldReturnSuccessResponse() throws Exception {
        User user = getTestUser("name4", "surname4");
        UserProfile existed = userDao.create(user);
        MvcResult result = mockMvc
            .perform(MockMvcRequestBuilders.get("/users", null, null)
                .contentType(MediaType.APPLICATION_JSON)
                .param("offset","1")
                .param("limit","1"))
            .andExpect(status().isOk())
            .andReturn();
        UserProfiles users = objectMapper.readValue(result.getResponse().getContentAsString(), UserProfiles.class);
        assertNotNull(users);
        // Counts with catalogue administrator user
        assertEquals(2, users.getTotalCount());
        assertEquals(1, users.getItems().size());
        userDao.delete(existed.getId());
    }

    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    public void deleteUserShouldReturnSuccessResponse() throws Exception {
        User user = getTestUser("name5", "surname5");
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
        UserProfile existed = userDao.create(user);
        user = getTestUser("changed name", "changed surname");
        mockMvc
            .perform(MockMvcRequestBuilders.put("/users/{userId}", existed.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
            .andExpect(status().isOk());
        userDao.delete(existed.getId());
    }
    
    @Test
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    public void updateUserRolesShouldReturnSuccessResponse() throws Exception {
        User user = getTestUser("name7", "surname7");
        UserProfile existed = userDao.create(user);
        mockMvc
            .perform(MockMvcRequestBuilders.put("/users/{userId}/roles", existed.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(SD_ADMIN_ROLE_ID))))
            .andExpect(status().isOk());
        userDao.delete(existed.getId());
    }

    @Test
    @Disabled("Need to fix a bug with roles when creating a user in Keycloak")
    @WithMockUser(authorities = {CATALOGUE_ADMIN_ROLE_WITH_PREFIX})
    public void updateDuplicatedUserRoleShouldReturnSuccessResponse() throws Exception {
        User user = getTestUser("name8", "surname8");
        UserProfile existed = userDao.create(user);
        mockMvc.perform(MockMvcRequestBuilders.put("/users/{userId}/roles", existed.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(CATALOGUE_PARTICIPANT_ADMIN_ROLE_ID))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        UserProfile newProfile = userDao.select(existed.getId());
        assertNotNull(newProfile);
        assertEquals(1, newProfile.getRoleIds().size());
        assertEquals(List.of(CATALOGUE_PARTICIPANT_ADMIN_ROLE_ID), newProfile.getRoleIds());
    }

    private User getTestUser(String firstName, String lastName) {
        return new User()
            .email(firstName + "." + lastName + "@test.org")
            .participantId(DEFAULT_PARTICIPANT_ID)
            .firstName(firstName)
            .lastName(lastName)
            .addRoleIdsItem("Ro-SD-A");
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
