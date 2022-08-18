package eu.gaiax.difs.fc.core.service.graphdb.impl;

import eu.gaiax.difs.fc.core.pojo.GraphQuery;
import eu.gaiax.difs.fc.core.pojo.SdClaim;
import eu.gaiax.difs.fc.core.service.graphdb.QueryGraph;
import eu.gaiax.difs.fc.core.service.graphdb.SDGraphStore;
import org.neo4j.driver.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class SDStoreImplement implements AutoCloseable, SDGraphStore, QueryGraph {

    private final Driver driver;


    public SDStoreImplement() {
        this("bolt://localhost:7687", "neo4j", "12345");

    }

    public SDStoreImplement(String uri, String user, String password) {

        driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
        System.out.println("connected");
        initialiseGraph(driver);

    }

    @Override
    public void close() throws Exception {
        driver.close();
    }

    private void initialiseGraph(Driver driver) {
        try (Session session = driver.session()) {
            session.run("CALL n10s.graphconfig.init();"); /// run only when creating a new graph
            session.run("CREATE CONSTRAINT n10s_unique_uri ON (r:Resource) ASSERT r.uri IS UNIQUE");
            System.out.println("n10 procedure and Constraints are loaded successfully");
        } catch (org.neo4j.driver.exceptions.ClientException e) {
            System.out.println("Graph already loaded" + e);
        }
    }


    public String uploadSelfDescription(List<SdClaim> sdClaimList) {
		/* Pass claim as a pojo object with subject, predicate and object. This is in turn passed on to the function which uploads it to the neo4j database.
		  Function returns SUCCESS or FAIL  */

        String payload = "";

        try (Session session = driver.session()) {
            for (SdClaim sdClaim : sdClaimList) {
                payload = payload + sdClaim.getSubject() + " " + sdClaim.getPredicate() + " " + sdClaim.getObject() + "	. \n";

            }

            String query = " WITH '\n" +
                    payload +
                    "' as payload\n" +
                    "CALL n10s.rdf.import.inline(payload,\"N-Triples\") YIELD terminationStatus, triplesLoaded\n" +
                    "RETURN terminationStatus, triplesLoaded";

            System.out.println(query);
            session.run(query);
            return "SUCCESS";
        } catch (Exception e) {
            return "FAIL";
        }
    }


    public List<Map<String, Object>> queryData(GraphQuery sdQuery) {
        /*
         * Given a query object , the method executes the query on the neo4j database
         * and returns the list of maps as output
         */


        try (Session session = driver.session()) {
            List<Map<String, Object>> Result_list = new ArrayList();
            System.out.println("beginning transaction");
            Transaction tx = session.beginTransaction();
            Result result = tx.run(sdQuery.getQuery());
            String SD_query_op = "";
            while (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> map = record.asMap();
                Result_list.add(map);
            }
            System.out.println("Query executed successfully ");
            return Result_list;
        } catch (Exception e) {
            System.out.println("Query unsuccessful " + e);
            return null;
        }
    }

}
