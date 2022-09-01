package eu.gaiax.difs.fc.core.service.graphdb;

import eu.gaiax.difs.fc.core.config.GraphDbConfig;
import eu.gaiax.difs.fc.core.pojo.OpenCypherQuery;
import eu.gaiax.difs.fc.core.pojo.SdClaim;
import eu.gaiax.difs.fc.core.service.graphdb.impl.Neo4jGraphStore;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import n10s.graphconfig.GraphConfigProcedures;
import n10s.rdf.load.RDFLoadProcedures;
import org.junit.FixMethodOrder;
import org.junit.jupiter.api.*;
import org.junit.runners.MethodSorters;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.configuration.connectors.BoltConnector;
import org.neo4j.configuration.helpers.SocketAddress;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.springframework.boot.test.autoconfigure.Neo4jTestHarnessAutoConfiguration;
import org.neo4j.gds.catalog.GraphExistsProc;
import org.neo4j.gds.catalog.GraphListProc;
import org.neo4j.gds.catalog.GraphProjectProc;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
@EnableAutoConfiguration(exclude = {Neo4jTestHarnessAutoConfiguration.class})
public class GraphTest {
    private static Neo4j embeddedDatabaseServer;
    private Neo4jGraphStore graphGaia;
    private Driver driver;

    @BeforeAll
    void initializeNeo4j() throws Exception {
        embeddedDatabaseServer = Neo4jBuilders.newInProcessBuilder()
                .withDisabledServer()
                .withConfig(GraphDatabaseSettings.procedure_allowlist, List.of("gds.*", "n10s.*"))
                .withConfig(BoltConnector.listen_address, new SocketAddress(7687))
                .withConfig(GraphDatabaseSettings.procedure_unrestricted, List.of("gds.*", "n10s.*"))
                // will be user for gds procedure
                .withProcedure(GraphExistsProc.class) // gds.graph.exists procedure
                .withProcedure(GraphListProc.class)
                .withProcedure(GraphProjectProc.class)
                // will be used for neo-semantics
                .withProcedure(GraphConfigProcedures.class) // n10s.graphconfig.*
                .withProcedure(RDFLoadProcedures.class)
                .build();

        GraphDbConfig graphDbConfig = new GraphDbConfig();
        graphDbConfig.setUri(embeddedDatabaseServer.boltURI().toString());
        graphDbConfig.setUser("neo4j");
        graphDbConfig.setPassword("");
        graphGaia = new Neo4jGraphStore(graphDbConfig);
        graphGaia.initialiseGraph();
        this.driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI(), Config.builder().withoutEncryption().build());
    }

    @AfterAll
    void closeNeo4j() {
        embeddedDatabaseServer.close();
    }


    /**
     * Data hardcoded for claims and upload to Graph . Given set of credentials,
     * connect to graph and upload self description. Instantiate list of claims
     * with subject predicate and object in N-triples form and upload to graph.
     * Verify if the claim has been uploaded using query service
     */
    @Test
    void testLiteralGraphUpload() {

        List<SdClaim> sdClaimList = new ArrayList<>();
        SdClaim sdClaim = new SdClaim("<https://delta-dao.com/.well-known/participantCompany.json>",
                "<https://www.w3.org/2018/credentials#credentialSubject>",
                "\"410 Terry Avenue North\"^^<http://www.w3.org/2001/XMLSchema#string>");
        sdClaimList.add(sdClaim);
        List<Map<String, String>> resultList = new ArrayList<Map<String, String>>();
        Map<String, String> map = new HashMap<String, String>();
        map.put("n.uri", "https://delta-dao.com/.well-known/participantCompany.json");
        map.put("n.ns0__credentialSubject", "410 Terry Avenue North");
        resultList.add(map);
        graphGaia.addClaims(sdClaimList, "https://delta-dao.com/.well-known/participantCompany.json");
        OpenCypherQuery query = new OpenCypherQuery(
                "match(n) where n.uri ='https://delta-dao.com/.well-known/participantCompany.json' return n.uri, n.ns0__credentialSubject;");
        List<Map<String, String>> response = graphGaia.queryData(query);
        Assertions.assertEquals(resultList, response);
    }

    /**
     * Instantiate a query without specifying property. Function should return
     * uri of claim subject
     */
    @Test
    void testNoPropertyQuery() {
        List<SdClaim> sdClaimList = new ArrayList<>();
        SdClaim sdClaim = new SdClaim("<https://delta-dao.com/.well-known/participantCompany.json>",
                "<https://www.w3.org/2018/credentials#credentialSubject>",
                "\"410 Terry Avenue North\"^^<http://www.w3.org/2001/XMLSchema#string>");
        sdClaimList.add(sdClaim);
        List<Map<String, String>> resultList = new ArrayList<Map<String, String>>();
        Map<String, String> map = new HashMap<String, String>();
        map.put("n.uri", "https://delta-dao.com/.well-known/participantCompany.json");
        resultList.add(map);
        graphGaia.addClaims(sdClaimList, "https://delta-dao.com/.well-known/participantCompany.json");
        OpenCypherQuery query = new OpenCypherQuery(
                "match(n) where n.uri ='https://delta-dao.com/.well-known/participantCompany.json' return n;");
        List<Map<String, String>> response = graphGaia.queryData(query);
        Assertions.assertEquals(resultList, response);
    }

    @Test
    void testDeleteClaim() {
        List<SdClaim> sdClaimList = new ArrayList<>();
        SdClaim sdClaim = new SdClaim("<https://delta-dao.com/.well-known/participantCompany.json>",
                "<https://www.w3.org/2018/credentials#credentialSubject>",
                "\"410 Terry Avenue North\"^^<http://www.w3.org/2001/XMLSchema#string>");
        sdClaimList.add(sdClaim);
        graphGaia.addClaims(sdClaimList, "https://delta-dao.com/.well-known/participantCompany.json");
        List<Map<String, String>> resultList = new ArrayList<Map<String, String>>();
        graphGaia.deleteClaims("https://delta-dao.com/.well-known/participantCompany.json");
        OpenCypherQuery query = new OpenCypherQuery(
                "match(n) where n.uri ='https://delta-dao.com/.well-known/participantCompany.json' return n;");
        List<Map<String, String>> response = graphGaia.queryData(query);
        Assertions.assertEquals(resultList, response);

    }


}