package eu.gaiax.difs.fc.server.controller;

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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
public class SelfDescriptionControllerTest {
    // TODO: 14.07.2022 After adding business logic, need to fix/add tests, taking into account exceptions

    private final String ADD_SD_REQUEST_BODY = "{\n" +
            " \"@context\": \"https://json-ld.org/contexts/gaia-x.jsonld\",\n" +
            " \"@id\": \"http://dbpedia.org/resource/MyService\",\n" +
            " \"provider\": \"http://dbpedia.org/resource/MyProvider\",\n" +
            " \"name\": \"https://gaia-x.catalogue.com\"\n" +
            " }";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MockMvc mockMvc;

    @BeforeTestClass
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
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
        mockMvc.perform(MockMvcRequestBuilders.get("/self-descriptions/123")
                .with(csrf())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    public void readSDByHashShouldReturnSuccessResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/self-descriptions/123")
                        .with(csrf())
                        .accept("application/ld+json"))
                .andExpect(status().isOk());
    }

    @Test
    public void deleteSDhShouldReturnUnauthorizedResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/self-descriptions/123")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    public void deleteSDReturnForbiddenResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/self-descriptions/123")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"Ro-MU-CA"})
    public void deleteSDReturnSuccessResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/self-descriptions/123")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void addSDhShouldReturnUnauthorizedResponse() throws Exception {
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
                        .content(ADD_SD_REQUEST_BODY)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"Ro-MU-CA"})
    public void addSDReturnSuccessResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/self-descriptions")
                        .content(ADD_SD_REQUEST_BODY)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());
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
    @WithMockUser(roles = {"Ro-MU-CA"})
    public void revokeSDReturnSuccessResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/self-descriptions/123/revoke")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
