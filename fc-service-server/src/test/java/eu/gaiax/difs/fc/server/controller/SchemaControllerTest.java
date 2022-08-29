package eu.gaiax.difs.fc.server.controller;

import org.junit.jupiter.api.Disabled;
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

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@AutoConfigureEmbeddedDatabase(provider = DatabaseProvider.ZONKY)
public class SchemaControllerTest {
    // TODO: Need to fix after final implementations

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MockMvc mockMvc;

   // Schema  schema=new Schema();

    String SCHEMA_REQUEST="{\"ontologies\":null,\"shapes\":null,\"vocabularies\":null}";

    @BeforeTestClass
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }


    @Test
    @WithMockUser(roles = {"Ro-MU-CA","Ro-MU-A"})
    public void getSchemasWithIdShouldReturnSuccessResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/schemas/schemaId")
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void getSchemasWithIdShouldReturnUnauthorizedResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/schemas/schemaId")
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
    @Test
    @WithMockUser(roles = {"Ro-MU-CA","Ro-MU-A"})
    public void getSchemasShouldReturnSuccessResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/schemas")
                        .with(csrf())
                        .param("offset","5")
                        .param("limit","10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void getSchemasShouldReturnUnauthorizedResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/schemas")
                        .with(csrf())
                        .param("offset","5")
                        .param("limit","10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }


    @Test
    @WithMockUser(roles = {"Ro-MU-CA","Ro-MU-A"})
    public void getLatestSchemasShouldReturnSuccessResponse() throws Exception {
        // request for latest schemas without params should return 400 BAD_REQUEST
        mockMvc.perform(MockMvcRequestBuilders.get("/schemas/latest")
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void getLatestSchemasShouldReturnUnauthorizedResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/schemas/latest")
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"Ro-MU-CA","Ro-MU-A"})
    public void getLatestSchemasOfTypeShouldReturnSuccessResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/schemas/latest?type=testType")
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void getLatestSchemasOfTypeShouldReturnUnauthorizedResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/schemas/latest?type=testType")
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void addSchemasShouldReturnUnauthorizedResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/schemas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    public void addSchemasReturnForbiddenResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/schemas")
                        .content(SCHEMA_REQUEST)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"Ro-MU-A"})
    public void addSchemaWithDifferentRoleReturnForbiddenResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/schemas")
                        .content(SCHEMA_REQUEST)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"Ro-MU-CA"})
    @Disabled // TODO: fix it later..
    public void addSchemasReturnSuccessResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/schemas")
                        .content(SCHEMA_REQUEST)
                        .with(csrf())
                        .contentType("application/rdf+xml") //MediaType.APPLICATION_XML)
                        .accept("application/rdf+xml"))  //MediaType.APPLICATION_XML))
                .andExpect(status().isCreated());
    }


    @Test
    @WithMockUser(roles = {"Ro-MU-A"})
    public void deleteSchemasWithDifferentRoleReturnForbiddenResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/schemas/schemaID")
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"Ro-MU-CA"} )
    public void deleteSchemasReturnSuccessResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/schemas/{schemaId}","1234")
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
