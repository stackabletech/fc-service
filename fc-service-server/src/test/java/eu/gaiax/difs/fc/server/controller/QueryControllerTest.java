package eu.gaiax.difs.fc.server.controller;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@AutoConfigureEmbeddedDatabase(provider = DatabaseProvider.ZONKY)
public class QueryControllerTest {

    // TODO: Need to fix after final implementations

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private MockMvc mockMvc;


    @BeforeTestClass
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }


    String QUERY_REQUEST_DUMMY="{\"statement\": \"Match (m:Movie) where m.released > 2000 RETURN m\", \"parameters\": null}}";

    @Test
    public void getQueryPageShouldReturnSuccessResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/query")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(header().stringValues("Content-Type", "text/html"));
    }

    @Test
    public void postQueriesReturnSuccessResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/query")
                        .content(QUERY_REQUEST_DUMMY)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Produces","application/json","application/sparql-results+xml", "text/turtle", "text/html")
                        .header("Accept","application/json") //,"application/sparql-query","application/sparql*")
                        .header("query-language","application/sparql-query"))
                .andExpect(status().isOk());
    }


}
