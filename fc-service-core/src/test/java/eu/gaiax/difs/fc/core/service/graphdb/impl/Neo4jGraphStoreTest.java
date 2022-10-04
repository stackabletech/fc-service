package eu.gaiax.difs.fc.core.service.graphdb.impl;

import eu.gaiax.difs.fc.testsupport.config.EmbeddedNeo4JConfig;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.gaiax.difs.fc.core.exception.QueryException;
import org.junit.FixMethodOrder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.runners.MethodSorters;
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

import eu.gaiax.difs.fc.core.pojo.OpenCypherQuery;
import eu.gaiax.difs.fc.core.pojo.SdClaim;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@EnableAutoConfiguration(exclude = {LiquibaseAutoConfiguration.class, DataSourceAutoConfiguration.class})
@SpringBootTest
@ActiveProfiles({"test", "tests-sdstore"})
@ContextConfiguration(classes = {Neo4jGraphStore.class})
@Import(EmbeddedNeo4JConfig.class)
public class Neo4jGraphStoreTest {
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
            graphGaia.addClaims(
                    sdClaimList,
                    credentialSubject.substring(1, credentialSubject.length() - 1));
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

    /**
     * Given set of credentials connect to graph and upload self description.
     * Instantiate list of claims with subject predicate and object in N-triples
     * form along with literals and upload to graph.
     */
    @Test
    void testAddClaims() throws Exception {
        List<SdClaim> sdClaimList = new ArrayList<>();
        SdClaim sdClaim = new SdClaim("<http://w3id.org/gaia-x/indiv#serviceElasticSearch.json>", "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>", "<http://w3id.org/gaia-x/service#ServiceOffering>");
        sdClaimList.add(sdClaim);
        SdClaim sdClaimSecond = new SdClaim("<http://w3id.org/gaia-x/indiv#serviceElasticSearch.json>", "<http://w3id.org/gaia-x/service#providedBy>", "<https://delta-dao.com/.well-known/participant.json>");
        sdClaimList.add(sdClaimSecond);
        graphGaia.addClaims(sdClaimList, "http://w3id.org/gaia-x/indiv#serviceElasticSearch.json");
    }

    /**
     * Given set of credentials connect to graph and upload self description.
     * Instantiate list of claims with subject predicate and object in N-triples
     * form which is invalid and try uploading to graphDB
     */
    @Test
    void testAddClaimsException() throws Exception {
        List<SdClaim> sdClaimList = new ArrayList<>();

        String credentialSubject = "http://w3id.org/gaia-x/indiv#serviceElasticSearch.json";
        String wrongCredentialSubject = "http://w3id.org/gaia-x/indiv#serviceElasticSearch";


        SdClaim syntacticallyCorrectClaim = new SdClaim(
                "<http://w3id.org/gaia-x/indiv#serviceElasticSearch.json>",
                "<http://w3.org/1999/02/22-rdf-syntax-ns#type>",
                "<http://w3id.org/gaia-x/service#ServiceOffering>"
        );

        SdClaim claimWBrokenSubject = new SdClaim(
                "<htw3id.org/gaia-x/indiv#serviceElasticSearch.json>",
                "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>",
                "<http://w3id.org/gaia-x/service#ServiceOffering>"
        );

        SdClaim claimWBrokenPredicate = new SdClaim(
                "<http://w3id.org/gaia-x/indiv#serviceElasticSearch.json>",
                "<httw3.org/1999/02/22-rdf-syntax-ns#type>",
                "<http://w3id.org/gaia-x/service#ServiceOffering>"
        );

        SdClaim claimWBrokenObjectIRI = new SdClaim(
                "<http://w3id.org/gaia-x/indiv#serviceElasticSearch.json>",
                "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>",
                "<htw3id.org/gaia-x/service#ServiceOffering>"
        );

        SdClaim claimWBrokenLiteral01 = new SdClaim(
                "<http://w3id.org/gaia-x/indiv#serviceElasticSearch.json>",
                "<http://www.w3.org/2000/01/rdf-schema#label>",
                "\"Fourty two\"^^<http://www.w3.org/2001/XMLSchema#int>"
        );

        SdClaim claimWBrokenLiteral02 = new SdClaim(
                "<http://w3id.org/gaia-x/indiv#serviceElasticSearch.json>",
                "<http://www.w3.org/2000/01/rdf-schema#label>",
                "\"Missing quotes^^<http://www.w3.org/2001/XMLSchema#string>"
        );

        SdClaim claimWBlankNodeObject = new SdClaim(
                "<http://w3id.org/gaia-x/indiv#serviceElasticSearch.json>",
                "<http://ex.com/some_property>",
                "_:23"
        );

        // Everything should work well with the syntactically correct claim
        // and the correct credential subject
        Assertions.assertDoesNotThrow(
                () -> graphGaia.addClaims(
                    Collections.singletonList(syntacticallyCorrectClaim),
                    credentialSubject
                )
        );

        // If a claim with a broken subject was passed it should be rejected
        // with a server exception
        Exception exception = Assertions.assertThrows(
                QueryException.class,
                () -> graphGaia.addClaims(
                        Collections.singletonList(claimWBrokenSubject),
                        credentialSubject
                )
        );
        Assertions.assertTrue(
                exception.getMessage().contains("Subject in triple"),
                "Syntax error should have been found for the triple " +
                        "subject, but wasn't");

        // If a claim with a broken predicate was passed it should be rejected
        // with a server exception
        exception = Assertions.assertThrows(
                QueryException.class,
                () -> graphGaia.addClaims(
                        Collections.singletonList(claimWBrokenPredicate),
                        credentialSubject
                )
        );
        Assertions.assertTrue(
                exception.getMessage().contains("Predicate in triple"),
                "A syntax error should have been found for the " +
                        "triple predicate, but wasn't");

        // If a claim with a resource on object position was passed and the URI
        // of the resource was broken, the claim should be rejected with a
        // server error
        exception = Assertions.assertThrows(
                QueryException.class,
                () -> graphGaia.addClaims(
                        Collections.singletonList(claimWBrokenObjectIRI),
                        credentialSubject
                )
        );
        Assertions.assertTrue(
                exception.getMessage().contains("Object in triple"),
                "A syntax error should have been found for the " +
                        "triple object, but wasn't"
        );

        // If a claim with a literal on object position was passed and the
        // literal was broken, the claim should be rejected with a server error.
        // 1) Wrong datatype
        exception = Assertions.assertThrows(
                QueryException.class,
                () -> graphGaia.addClaims(
                        Collections.singletonList(claimWBrokenLiteral01),
                        credentialSubject
                )
        );
        Assertions.assertTrue(
                exception.getMessage().contains("Object in triple"),
                "A syntax error should have been found for the " +
                        "triple object, but wasn't"
        );
        // 2) Syntax error
        exception = Assertions.assertThrows(
                QueryException.class,
                () -> graphGaia.addClaims(
                        Collections.singletonList(claimWBrokenLiteral02),
                        credentialSubject
                )
        );
        Assertions.assertTrue(
                exception.getMessage().contains("Object in triple"),
                "A syntax error should have been found for the " +
                        "triple object, but wasn't"
        );

        // Blank nodes
        // ===========
        // As far as it was communicated, there should be no blank nodes in a
        // claim. We explicitly check this for objects. Blank nodes on a
        // triple's subject position won't make much sense as the credential
        // subject must not be blank node. Blank nodes on predicate position
        // don't make sense either and are assumed to not occur.
        exception = Assertions.assertThrows(
                QueryException.class,
                () -> graphGaia.addClaims(
                        Collections.singletonList(claimWBlankNodeObject),
                        credentialSubject
                )
        );
        Assertions.assertTrue(
                exception.getMessage().contains("Object in triple"),
                "A syntax error should have been found for the " +
                        "triple object, but wasn't"
        );
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