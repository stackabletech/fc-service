package eu.gaiax.difs.fc.server.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.event.annotation.BeforeTestClass;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.gaiax.difs.fc.api.generated.model.User;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
public class UsersControllerTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private MockMvc mockMvc;

    //@MockBean
    //private KeycloakBuilder builder;
    //@MockBean
    //private Keycloak keycloak;
    //@MockBean
    //private RealmResource realmResource;
    //@MockBean
    //private UsersResource usersResource;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

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
    @WithMockUser(authorities = {"ROLE_Ro-MU-CA"})
    public void addUserShouldReturnCreatedResponse() throws Exception {
        
        User user = new User()
                .email("unit-test.user@test.org")
                .participantId("test-participant-id")
                .firstName("unit-test")
                .lastName("user");

        //setupKeycloak(HttpStatus.SC_CREATED);

        mockMvc
            .perform(MockMvcRequestBuilders.post("/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(user)))
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_Ro-MU-CA"})
    public void getUserShouldReturnSuccessResponse() throws Exception {
        
        //User user = new User()
        //        .email("email@test.org")
        //        .participantId("test-participant-id")
        //        .username("unit-test-user");

        //setupKeycloak(HttpStatus.SC_OK);
        String userId = "f7e47bd2-8ae4-4fd7-8b03-6e2fdcf1c912";

        mockMvc
            .perform(MockMvcRequestBuilders.get("/users/{userId}", userId)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_Ro-MU-CA"})
    public void deleteUserShouldReturnSuccessResponse() throws Exception {
        
        //User user = new User()
        //        .email("email@test.org")
        //        .participantId("test-participant-id")
        //        .username("unit-test-user");

        //setupKeycloak(HttpStatus.SC_OK);
        String userId = "0fb1eb26-4f92-4941-a782-f092e19dedcb";

        mockMvc
            .perform(MockMvcRequestBuilders.delete("/users/{userId}", userId)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
    }
    
    @Test
    @WithMockUser(authorities = {"ROLE_Ro-MU-CA"})
    public void updateUserShouldReturnSuccessResponse() throws Exception {
        
        User user = new User()
                .firstName("unit-test")
                .lastName("last user")
                .email("email_changed@test.org")
                .participantId("test-participant-id");

        //setupKeycloak(HttpStatus.SC_OK);
        String userId = "ae366624-8371-401d-b2c4-518d2f308a15";

        mockMvc
            .perform(MockMvcRequestBuilders.put("/users/{userId}", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(user)))
            .andExpect(status().isOk());
    }
    
    private void setupKeycloak(int status) {
        //when(builder.build()).thenReturn(keycloak);
        //when(keycloak.realm("gaia-x")).thenReturn(realmResource);
        //when(realmResource.users()).thenReturn(usersResource);
        //when(usersResource.create(any())).thenReturn(Response.status(status).build());
    }
    
}
