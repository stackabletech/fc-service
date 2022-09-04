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
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    }

    @AfterAll
    void closeNeo4j() {
        embeddedDatabaseServer.close();
    }

    /**
     * Given set of credentials connect to graph and upload self description.
     * Instantiate list of claims with subject predicate and object in N-triples
     * form and upload to graph. Verify if the claim has been uploaded using
     * query service
     */

    @Test
    void testCypherQueriesFull() throws Exception {

        List<SdClaim> sdClaimFile = loadTestClaims("Databases/neo4j/data/Triples/claim.nt");
        List<Map<String, String>> resultListFull = new ArrayList<Map<String, String>>();
        Map<String, String> mapFull = new HashMap<String, String>();
        mapFull.put("n.uri", "http://w3id.org/gaia-x/indiv#serviceMVGPortal.json");
        resultListFull.add(mapFull);
        Map<String, String> mapFullES = new HashMap<String, String>();
        mapFullES.put("n.uri", "http://w3id.org/gaia-x/indiv#serviceElasticSearch.json");
        resultListFull.add(mapFullES);
        for (SdClaim sdClaim : sdClaimFile) {
            List<SdClaim> sdClaimList = new ArrayList<>();
            sdClaimList.add(sdClaim);
            String credentialSubject = sdClaimList.get(0).getSubject();
            graphGaia.addClaims(sdClaimList, credentialSubject.substring(1, credentialSubject.length() - 1));
        }
        OpenCypherQuery queryFull = new OpenCypherQuery(
                "MATCH (n:ns0__ServiceOffering) RETURN n LIMIT 25");
        List<Map<String, String>> responseFull = graphGaia.queryData(queryFull);
        Assertions.assertEquals(resultListFull, responseFull);
    }

    /**
     * Given set of credentials connect to graph and upload self description.
     * Instantiate list of claims with subject predicate and object in N-triples
     * form along with literals and upload to graph. Verify if the claim has been uploaded using
     * query service
     */

    @Test
    void testCypherDelta() throws Exception {

        List<SdClaim> sdClaimFile = loadTestClaims("Databases/neo4j/data/Triples/claim.nt");
        List<Map<String, String>> resultListDelta = new ArrayList<Map<String, String>>();
        Map<String, String> mapDelta = new HashMap<String, String>();
        mapDelta.put("n.uri", "https://delta-dao.com/.well-known/participant.json");
        resultListDelta.add(mapDelta);
        for (SdClaim sdClaim : sdClaimFile) {
            List<SdClaim> sdClaimList = new ArrayList<>();
            sdClaimList.add(sdClaim);
            String credentialSubject = sdClaimList.get(0).getSubject();
            graphGaia.addClaims(sdClaimList, credentialSubject.substring(1, credentialSubject.length() - 1));
        }
        OpenCypherQuery queryDelta = new OpenCypherQuery(
                "MATCH (n:ns1__LegalPerson) WHERE n.ns1__name = \"deltaDAO AG\" RETURN n LIMIT 25");
        List<Map<String, String>> responseDelta = graphGaia.queryData(queryDelta);
        Assertions.assertEquals(resultListDelta, responseDelta);
    }


    private List<SdClaim> loadTestClaims(String Path) throws Exception {
        List credentialSubjectList = new ArrayList();
        try (InputStream is = new ClassPathResource(Path)
                .getInputStream()) {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String strLine;
            List<SdClaim> sdClaimList = new ArrayList<>();
            while ((strLine = br.readLine()) != null) {
                String[] split = strLine.split("\\s+");
                Pattern regex = Pattern.compile("[^\\s\"']+|\"[^\"]*\"|'[^']*'");
                Matcher regexMatcher = regex.matcher(strLine);
                int i = 0;
                String subject = "";
                String predicate = "";
                String object = "";
                while (regexMatcher.find()) {
                    if (i == 0) {
                        subject = regexMatcher.group().toString();
                    } else if (i == 1) {
                        predicate = regexMatcher.group().toString();
                    } else if (i == 2) {
                        object = regexMatcher.group().toString();
                    }
                    i++;
                }
                SdClaim sdClaim = new SdClaim(subject, predicate, object);
                sdClaimList.add(sdClaim);
            }
            return sdClaimList;
        } catch (Exception e) {
            throw e;
        }
    }


}