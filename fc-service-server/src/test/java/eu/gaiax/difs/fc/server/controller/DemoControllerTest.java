package eu.gaiax.difs.fc.server.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.event.annotation.BeforeTestClass;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
public class DemoControllerTest {
    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MockMvc mockMvc;

    @BeforeTestClass
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    public void demoShouldReturnSuccessResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/demo")).andExpect(status().isOk());
    }

    @Test
    public void demoAuthShouldReturnUnauthorizedResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/demo/authorized")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    public void demoAuthShouldReturnSuccessResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/demo/authorized")).andExpect(status().isOk());
    }

    @Test
    public void demoAdminShouldReturnUnauthorizedResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/demo/admin")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    public void demoAdminShouldReturnForbiddenResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/demo/admin")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_Ro-MU-CA", "SCOPE_gaia-x"})
    public void demoAdminShouldReturnSuccessResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/demo/admin")).andExpect(status().isOk());
    }
}

