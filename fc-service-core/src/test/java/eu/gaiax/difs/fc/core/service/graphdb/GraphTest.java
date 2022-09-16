package eu.gaiax.difs.fc.core.service.graphdb;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.FixMethodOrder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.runners.MethodSorters;
import org.neo4j.driver.springframework.boot.test.autoconfigure.Neo4jTestHarnessAutoConfiguration;
import org.neo4j.harness.Neo4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import eu.gaiax.difs.fc.core.config.EmbeddedNeo4JConfig;
import eu.gaiax.difs.fc.core.pojo.OpenCypherQuery;
import eu.gaiax.difs.fc.core.pojo.SdClaim;
import eu.gaiax.difs.fc.core.service.graphdb.impl.Neo4jGraphStore;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@EnableAutoConfiguration(exclude = {LiquibaseAutoConfiguration.class, DataSourceAutoConfiguration.class, Neo4jTestHarnessAutoConfiguration.class})
@SpringBootTest
@ActiveProfiles("tests-sdstore")
@ContextConfiguration(classes = {Neo4jGraphStore.class})
@Import(EmbeddedNeo4JConfig.class)
public class GraphTest {
    
    @Autowired
    private Neo4j embeddedDatabaseServer;
    
    @Autowired
    private Neo4jGraphStore graphGaia;

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

        List<SdClaim> sdClaimFile = loadTestClaims("Claims-Tests/claimsForQuery.nt");
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
                "MATCH (n:ns0__ServiceOffering) RETURN n LIMIT 25", Map.of());
        List<Map<String, Object>> responseFull = graphGaia.queryData(queryFull);
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

        List<SdClaim> sdClaimFile = loadTestClaims("Claims-Tests/claimsForQuery.nt");
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
                "MATCH (n:ns1__LegalPerson) WHERE n.ns1__name = $name RETURN n LIMIT $limit", Map.of("name", "deltaDAO AG", "limit", 25));
        List<Map<String, Object>> responseDelta = graphGaia.queryData(queryDelta);
        Assertions.assertEquals(resultListDelta, responseDelta);
    }


    private List<SdClaim> loadTestClaims(String path) throws Exception {
        try (InputStream is = new ClassPathResource(path).getInputStream()) {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String strLine;
            List<SdClaim> sdClaimList = new ArrayList<>();
            while ((strLine = br.readLine()) != null) {
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
        }
    }

}