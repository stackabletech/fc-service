package eu.gaiax.difs.fc.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.gaiax.difs.fc.core.pojo.ContentAccessorDirect;
import eu.gaiax.difs.fc.core.service.schemastore.SchemaStore;
import java.util.Objects;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;

import static eu.gaiax.difs.fc.server.helper.FileReaderHelper.getMockFileDataAsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@AutoConfigureEmbeddedDatabase(provider = DatabaseProvider.ZONKY)
public class SchemaControllerTest {
  @Autowired
  private WebApplicationContext context;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private SchemaStore schemaStore;

  @Autowired
  private ObjectMapper objectMapper;

  String SCHEMA_REQUEST = "{\"ontologies\":null,\"shapes\":null,\"vocabularies\":null}";

  @BeforeTestClass
  public void setup() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }


  @Test
  @WithMockUser(roles = {"Ro-MU-CA", "Ro-MU-A"})
  public void getSchemaByIdShouldReturnSuccessResponse() throws Exception {
    String id = schemaStore.addSchema(new ContentAccessorDirect(getMockFileDataAsString("test-schema.ttl")));
    mockMvc.perform(MockMvcRequestBuilders.get("/schemas/" + id)
            .with(csrf())
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
    schemaStore.deleteSchema(id);
  }

  @Test
  public void getSchemaByIdShouldReturnUnauthorizedResponse() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get("/schemas/schemaId")
            .with(csrf())
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = {"Ro-MU-CA", "Ro-MU-A"})
  public void getSchemasShouldReturnSuccessResponse() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get("/schemas")
            .with(csrf())
            .param("offset", "5")
            .param("limit", "10")
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
  }

  @Test
  public void getSchemasShouldReturnUnauthorizedResponse() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get("/schemas")
            .with(csrf())
            .param("offset", "5")
            .param("limit", "10")
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = {"Ro-MU-CA", "Ro-MU-A"})
  public void getLatestSchemaShouldReturnSuccessResponse() throws Exception {
    String id = schemaStore.addSchema(new ContentAccessorDirect(getMockFileDataAsString("test-schema.ttl")));
    mockMvc.perform(MockMvcRequestBuilders.get("/schemas/latest?type=SHAPE")
            .with(csrf())
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
    schemaStore.deleteSchema(id);
  }

  @Test
  public void getLatestSchemaShouldReturnUnauthorizedResponse() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get("/schemas/latest")
            .with(csrf())
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = {"Ro-MU-CA", "Ro-MU-A"})
  public void getLatestSchemaWithoutTypeShouldReturnBadRequest() throws Exception {
    String id = schemaStore.addSchema(new ContentAccessorDirect(getMockFileDataAsString("test-schema.ttl")));
    mockMvc.perform(MockMvcRequestBuilders.get("/schemas/latest")
            .with(csrf())
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
    schemaStore.deleteSchema(id);
  }

  @Test
  @WithMockUser(roles = {"Ro-MU-CA", "Ro-MU-A"})
  public void getLatestSchemaWithUncorrectedTypeShouldReturnBadRequest() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get("/schemas/latest?type=testType")
            .with(csrf())
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void addSchemaShouldReturnUnauthorizedResponse() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.post("/schemas")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  public void addSchemaWithoutRoleShouldReturnForbiddenResponse() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.post("/schemas")
            .content(SCHEMA_REQUEST)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = {"Ro-MU-A"})
  public void addSchemaWithoutRoleAccessShouldReturnForbiddenResponse() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.post("/schemas")
            .content(SCHEMA_REQUEST)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = {"Ro-MU-CA"})
  public void addSchemaShouldReturnSuccessResponse() throws Exception {
    ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.post("/schemas")
            .content(getMockFileDataAsString("test-schema.ttl"))
            .with(csrf())
            .contentType("application/rdf+xml")
    ).andExpect(status().isCreated());

    MvcResult result = resultActions.andReturn();
    String id = Objects.requireNonNull(result.getResponse().getHeader("location"))
        .replace("/schemas/", "");
    schemaStore.deleteSchema(id);
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
  @WithMockUser(roles = {"Ro-MU-CA"})
  public void deleteSchemasReturnSuccessResponse() throws Exception {
    String id = schemaStore.addSchema(new ContentAccessorDirect(getMockFileDataAsString("test-schema.ttl")));
    mockMvc.perform(MockMvcRequestBuilders.delete("/schemas/{schemaId}", id)
            .with(csrf())
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
  }
}
