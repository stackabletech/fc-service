package eu.gaiax.difs.fc.server.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.annotation.BeforeTestClass;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.gaiax.difs.fc.api.generated.model.VerificationResult;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;

import static eu.gaiax.difs.fc.server.helper.FileReaderHelper.getMockFileDataAsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@AutoConfigureEmbeddedDatabase(provider = DatabaseProvider.ZONKY)
public class VerificationControllerTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;

    @BeforeTestClass
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    public void getVerifyPageShouldReturnSuccessResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/verification")
               .contentType(MediaType.APPLICATION_JSON)
               .accept(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(header().stringValues("Content-Type", "text/html"));
    }

    @Test
    public void verifyParticipantShouldReturnSuccessResponse() throws Exception {
        String json = getMockFileDataAsString("default_participant.json");
        String response = mockMvc.perform(MockMvcRequestBuilders.post("/verification")
               .contentType(MediaType.APPLICATION_JSON)
               .accept(MediaType.APPLICATION_JSON)
               .content(json))
               .andExpect(status().isOk())
               .andReturn()
               .getResponse()
               .getContentAsString();
        VerificationResult partResult = objectMapper.readValue(response, VerificationResult.class);
        assertEquals("did:example:issuer", partResult.getIssuer());
        assertEquals(OffsetDateTime.of(2010, 1, 1, 19, 23, 24, 0, ZoneOffset.UTC), partResult.getIssuedDateTime()); 
    }
 
    @Test
    public void verifyNoProofsShouldReturnClientError() throws Exception {
        String json = getMockFileDataAsString("participant_without_proofs.json");
        mockMvc.perform(MockMvcRequestBuilders.post("/verification")
               .contentType(MediaType.APPLICATION_JSON)
               .accept(MediaType.APPLICATION_JSON)
               .content(json))
               .andExpect(status().isBadRequest());
    }
    
    @Test
    public void verifyNoProofsNoSignsShouldReturnSuccessResponse() throws Exception {
        String json = getMockFileDataAsString("participant_without_proofs.json");
        mockMvc.perform(MockMvcRequestBuilders.post("/verification")
               .queryParam("verifySemantics", "false")
               .queryParam("verifySignatures", "false")
               .contentType(MediaType.APPLICATION_JSON)
               .accept(MediaType.APPLICATION_JSON)
               .content(json))
               .andExpect(status().isOk());
    }

    @Test
    public void verifySDNoCSShouldReturnClientError() throws Exception {
        String json = getMockFileDataAsString("sd-without-credential-subject.json");
        String response = mockMvc.perform(MockMvcRequestBuilders.post("/verification")
               .contentType(MediaType.APPLICATION_JSON)
               .accept(MediaType.APPLICATION_JSON)
               .content(json))
               .andExpect(status().isBadRequest())
               .andReturn()
               .getResponse()
               .getContentAsString();
        System.out.println("response: " + response);
    }

    @Test
    public void verifySDNoCSNoSemanticsShouldReturnSuccessResponse() throws Exception {
        String json = getMockFileDataAsString("sd-without-credential-subject.json");
        mockMvc.perform(MockMvcRequestBuilders.post("/verification")
               .queryParam("verifySemantics", "false")
               .contentType(MediaType.APPLICATION_JSON)
               .accept(MediaType.APPLICATION_JSON)
               .content(json))
               .andExpect(status().isOk());
    }
    
}
