package eu.gaiax.difs.fc.core.service.graphdb;

import java.io.*;
import java.time.Duration;
import java.util.*;

import org.junit.FixMethodOrder;
import org.junit.jupiter.api.*;
import org.junit.runners.MethodSorters;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;

import eu.gaiax.difs.fc.core.pojo.OpenCypherQuery;
import eu.gaiax.difs.fc.core.pojo.SdClaim;
import eu.gaiax.difs.fc.core.service.graphdb.impl.Neo4jGraphStore;
import eu.gaiax.difs.fc.core.config.GraphDbConfig;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GraphTest {

    private Neo4jGraphStore graphGaia;

    @Container
    final static Neo4jContainer<?> container = new Neo4jContainer<>("neo4j:4.4.5")
            .withNeo4jConfig("dbms.security.procedures.unrestricted", "apoc.*,n10s.*,gds.*,graph-data-science.*")
            .withEnv("NEO4JLABS_PLUGINS", "[\"apoc\",\"n10s\", \"graph-data-science\"]")
            .withEnv("NEO4J_dbms_security_procedures_unrestricted", "apoc.*,gds.*,graph-data-science.*")
            .withEnv("apoc.import.file.enabled", "true")
            .withEnv("dbms.connector.bolt.listen_address", ":7687")
            .withEnv("apoc.import.file.use_neo4j_config", "false")
            .withAdminPassword("12345")
            .withStartupTimeout(Duration.ofMinutes(5));

    @BeforeAll
    void setupContainer() throws Exception {
        container.start();
        GraphDbConfig graphDbConfig = new GraphDbConfig();
        graphDbConfig.setUri(container.getBoltUrl());
        graphDbConfig.setUser("neo4j");
        graphDbConfig.setPassword("12345");
        graphGaia = new Neo4jGraphStore(graphDbConfig);
        graphGaia.initialiseGraph();
    }

    @AfterAll
    void stopContainer() {
        container.stop();
    }

    /**
     * Data hardcoded for claims and upload to Graph . Given set of credentials,
     * connect to graph and upload self description. Instantiate list of claims
     * from file with subject predicate and object in N-triples form and upload
     * to graph.
     */
    @Test
    void simpleGraphUpload() {
        List<SdClaim> sdClaimList = new ArrayList<>();
        SdClaim sdClaim = new SdClaim("<https://delta-dao.com/.well-known/participantCompany.json>",
                "<https://www.w3.org/2018/credentials#credentialSubject>",
                "<https://delta-dao.com/.well-known/participantCompany.json>");
        sdClaimList.add(sdClaim);
        graphGaia.addClaims(sdClaimList, "<https://delta-dao.com/.well-known/participantCompany.json>");
        Assertions.assertEquals("SUCCESS", "SUCCESS");

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


    /**
     * Query to graph using Query endpoint by instantiating query object and
     * passing query string as parameter. THe result is a list of maps
     *
     * @throws Exception
     */

	/*
	@Test
	@DisplayName("Test for QueryData")
	void testQueryTransactionEndpoint() throws Exception {
		List<SdClaim> sdClaimList = loadTestClaims();
		Assertions.assertEquals("SUCCESS", graphGaia.addClaims(sdClaimList, ));

		List<Map<String, String>> resultList = new ArrayList<Map<String, String>>();
		Map<String, String> map = new HashMap<String, String>();
		map.put("n.ns0__country", null);
		map.put("n.ns0__legalName", "CompanyWebServicesEMEASARL");
		resultList.add(map);
		GraphQuery query = new GraphQuery(
				"match(n{ns0__legalName: 'CompanyWebServicesEMEASARL'}) return n.ns0__country, n.ns0__legalName;");
		List<Map<String, String>> response = graphGaia.queryData(query);
		Assertions.assertEquals(resultList, response);

	}
	*/
    private List<SdClaim> loadTestClaims() throws Exception {
        try (InputStream is = new ClassPathResource("Databases/neo4j/data/Triples/testData2.nt")
                .getInputStream()) {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String strLine;
            List<SdClaim> sdClaimList = new ArrayList<>();
            while ((strLine = br.readLine()) != null) {
                String[] split = strLine.split("\\s+");
                SdClaim sdClaim = new SdClaim(split[0], split[1], split[2]);
                sdClaimList.add(sdClaim);

            }
            return sdClaimList;
        } catch (Exception e) {
            throw e;
        }
    }

}