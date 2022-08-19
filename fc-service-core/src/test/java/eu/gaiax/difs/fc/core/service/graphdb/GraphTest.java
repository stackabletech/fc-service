package eu.gaiax.difs.fc.core.service.graphdb;

import apoc.ApocConfig;
import eu.gaiax.difs.fc.core.pojo.GraphQuery;
import eu.gaiax.difs.fc.core.pojo.SdClaim;
import eu.gaiax.difs.fc.core.service.graphdb.impl.GraphConnect;
import org.apache.jena.sparql.pfunction.library.container;
import org.junit.FixMethodOrder;
import org.junit.jupiter.api.*;
import org.junit.runners.MethodSorters;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.harness.ServerControls;
import org.neo4j.harness.TestServerBuilders;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GraphTest {

    GraphConnect graphGaia;

    private static Neo4j neo4j;
    private static Driver driver;

    @BeforeAll
    static void initializeNeo4j() {
        // Make sure that the plugins folder is listed in -cp
        Path pluginDirContainingApocJar = Paths.get("src/main/resources/neo4j-plugins/");

        if (!Files.exists(pluginDirContainingApocJar)) {
            throw new IllegalArgumentException("Invalid path to plugins directory");
        }

        neo4j = Neo4jBuilders
                .newInProcessBuilder()
                .withDisabledServer()
                .withFixture("CREATE (p1:Person)-[:knows]->(p2:Person)-[:knows]->(p3:Person)")
                .withConfig(GraphDatabaseSettings.plugin_dir, pluginDirContainingApocJar)
                .withConfig(GraphDatabaseSettings.procedure_unrestricted, List.of("apoc.*"))
                .build();
        driver = GraphDatabase.driver(neo4j.boltURI(), AuthTokens.none());
        graphGaia = new GraphConnect(url, user, password);
    }

    @AfterAll
    static void stopNeo4j() {
        driver.close();
        neo4j.close();
    }



    @Test
    void GraphUploadSimulate() {
        /*
        Simulate Data from file upload on SD storage graph. Given set of credentials, connect to graph and upload self description.
        Instantiate list of claims from file with subject predicate and object and upload to graph.
        * */

        String url = container.getBoltUrl();
        String user = "neo4j";
        String password = "12345";
        //GraphConnect graphGaia = new GraphConnect(url,user,password);
        try {
            //graphGaia = new GraphConnect(url,user,password);
            File rootDirectory = new File("./");
            String rootDirectoryPath = rootDirectory.getCanonicalPath();
            String path = "/src/test/resources/Databases/neo4j/data/Triples/testData2.nt";
            FileInputStream fstream = new FileInputStream(rootDirectoryPath + path);
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String strLine;
            List<SdClaim> sdClaimList = new ArrayList<>();
            while ((strLine = br.readLine()) != null) {
                String[] split = strLine.split("\\s+");
                SdClaim sdClaim = new SdClaim(split[0], split[1], split[2]);
                sdClaimList.add(sdClaim);

            }
            Assertions.assertEquals("SUCCESS", graphGaia.uploadSelfDescription(sdClaimList));
            fstream.close();

        } catch (Exception e) {
            System.out.println("error " + e);
        }
    }


    @Test
    void GraphUploadSimple() {
        /*
         Data hardcoded for claims and upload to Graph . Given set of credentials, connect to graph and upload self description.
        Instantiate list of claims from file with subject predicate and object in N-triples form and upload to graph.
        * */


        try {
            List<SdClaim> sdClaimList = new ArrayList<>();
            SdClaim sdClaim = new SdClaim("<https://delta-dao.com/.well-known/participantAmazon.json>", "<https://www.w3.org/2018/credentials#credentialSubject>", "<https://delta-dao.com/.well-known/participantAmazon.json>");
            sdClaimList.add(sdClaim);
            Assertions.assertEquals("SUCCESS", graphGaia.uploadSelfDescription(sdClaimList));
        } catch (Exception e) {
            System.out.println(" failed");
        }

    }


    @Test
    void GraphUploadLiteral() {
        /*
         Data hardcoded for claims and upload to Graph . Given set of credentials, connect to graph and upload self description.
        Instantiate list of claims from file with subject predicate and object in N-triples form and upload to graph.
        * */


        try {
            String triple_String = "<https://delta-dao.com/.well-known/participantAmazon.json> <gx-participant:registrationNumber> \"LURCSL.B186284\"^^<http://www.w3.org/2001/XMLSchema#string>";

            String testurl = container.getHttpUrl();
            List<SdClaim> sdClaimList = new ArrayList<>();
            SdClaim sdClaim = new SdClaim("<https://delta-dao.com/.well-known/participantAmazon.json>", "<https://www.w3.org/2018/credentials#credentialSubject>", "\"410 Terry Avenue North\"^^<http://www.w3.org/2001/XMLSchema#string>");
            sdClaimList.add(sdClaim);
            Assertions.assertEquals("SUCCESS", graphGaia.uploadSelfDescription(sdClaimList));
            System.out.println("Completed");
        } catch (Exception e) {
            System.out.println(" failed");
        }

    }


    @Test
    @DisplayName("Test for QueryData")
    void testQueryTransactionEndpoint() {
        /*
         * Query to graph using Query endpoint by instantiating query object and passing query string as parameter. THe result is a list of maps */
        try {
            String url = container.getBoltUrl();
            System.out.println(url);
            File rootDirectory = new File("./");
            String rootDirectoryPath = rootDirectory.getCanonicalPath();
            String path = "/src/test/resources/Databases/neo4j/data/Triples/testData2.nt";
            FileInputStream fstream = new FileInputStream(rootDirectoryPath + path);
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String strLine;
            List<SdClaim> sdClaimList = new ArrayList<>();
            while ((strLine = br.readLine()) != null) {

                System.out.println(strLine);
                String[] split = strLine.split("\\s+");
                SdClaim sdClaim = new SdClaim(split[0], split[1], split[2]);
                sdClaimList.add(sdClaim);

            }
            Assertions.assertEquals("SUCCESS", graphGaia.uploadSelfDescription(sdClaimList));
            fstream.close();

        } catch (Exception e) {
            System.out.println("error " + e);
        }


        List<Map<String, Object>> Result_list = new ArrayList<Map<String, Object>>();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("n.ns0__country", null);
        map.put("n.ns0__legalName", "AmazonWebServicesEMEASARL");
        Result_list.add(map);
        GraphQuery query = new GraphQuery("match(n{ns0__legalName: 'AmazonWebServicesEMEASARL'}) return n.ns0__country, n.ns0__legalName;");
        List<Map<String, Object>> response = graphGaia.queryData(query);
        Assertions.assertEquals(Result_list, response);
        System.out.println(response);

    }

}