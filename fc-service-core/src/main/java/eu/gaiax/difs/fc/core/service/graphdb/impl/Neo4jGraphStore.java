package eu.gaiax.difs.fc.core.service.graphdb.impl;

import eu.gaiax.difs.fc.core.config.GraphDbConfig;
import eu.gaiax.difs.fc.core.pojo.OpenCypherQuery;
import eu.gaiax.difs.fc.core.pojo.SdClaim;
import eu.gaiax.difs.fc.core.service.graphdb.GraphStore;
import eu.gaiax.difs.fc.core.service.graphdb.QueryGraph;
import liquibase.repackaged.org.apache.commons.lang3.StringUtils;
import lombok.extern.log4j.Log4j2;
import org.neo4j.cypherdsl.core.*;
import org.neo4j.cypherdsl.parser.ExpressionCreatedEventType;
import org.neo4j.cypherdsl.parser.Options;
import org.neo4j.driver.*;
import org.neo4j.driver.internal.InternalNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.neo4j.cypherdsl.parser.CypherParser;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

@Log4j2
@Component
public class Neo4jGraphStore implements GraphStore, QueryGraph {

    @Autowired
    private Driver driver;

    //@Override
    //public void close() throws Exception {
        // not sure we must close it here
    //    driver.close();
    //}
/*
    private void initialiseGraph() {
        try {
            if (databaseExists()) {
                log.info("Graph already loaded");
            } else {
                initGraph();
            }
        }catch (Exception e) {
            // Temporal solution to avoid exceptions during bean initialization from other components, e.g., unit tests
            // TODO review how to avoid bean initialization problem
            log.error("Error while trying to initialise graph.");
        }

    }

*/
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void addClaims(List<SdClaim> sdClaimList, String credentialSubject) {
        String payload = "";

        try (Session session = driver.session()) {
            for (SdClaim sdClaim : sdClaimList) {
                String subject = sdClaim.getSubject().substring(1, sdClaim.getSubject().length() - 1);
                if (subject.equals(credentialSubject)) {
                    payload = payload + sdClaim.getSubject() + " " + sdClaim.getPredicate() + " " + sdClaim.getObject()
                            + "	. \n";
                }
            }

            String query = " WITH '\n" + payload + "' as payload\n"
                    + "CALL n10s.rdf.import.inline(payload,\"N-Triples\") YIELD terminationStatus, triplesLoaded\n"
                    + "RETURN terminationStatus, triplesLoaded";

            log.debug("Query; " + query);
            session.run(query);
        } catch (Exception e) {
            log.error("Could not update list of self description claims");
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteClaims(String credentialSubject) {
        log.debug("Beginning claims deletion");
        String query = "MATCH (n{uri: '" + credentialSubject + "'})\n" +
                "DELETE n";
        String checkClaim = "match(n) where n.uri ='" + credentialSubject + "' return n;";
        OpenCypherQuery checkQuery = new OpenCypherQuery(checkClaim);
        List<Map<String, String>> result = queryData(checkQuery);
        Map<String, String> resultCheck = result.get(0);
        String claim = resultCheck.entrySet().iterator().next().getValue();

        try (Session session = driver.session()) {
            if (claim.equals(credentialSubject)) {
                session.run(query);
                log.debug("Deleting executed successfully ");
            } else {
                log.debug("Claim doe not exist in GraphDB ");
            }

        }
    }


    public boolean validateQuery(String query) {
        query=query.toLowerCase();
        if (StringUtils.containsIgnoreCase(query,"delete") || StringUtils.containsIgnoreCase(query,"create") || StringUtils.containsIgnoreCase(query,"set") || StringUtils.containsIgnoreCase(query,"remove") || StringUtils.containsIgnoreCase(query,"merge")  ) {
            throw new RuntimeException("Not allowed to remove or add nodes!");
        }
        try {
            var userStatement = CypherParser.parse(query);
        }catch (Exception e) {
            throw e;
        }
        return true;
    }

    public List<Map<String, String>> processResults(Result result)
    {
        List<Map<String, String>> resultList = new ArrayList<>();
        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();
            Map<String, Object> map = record.asMap();
            Map<String, String> outputMap = new HashMap<String, String>();
            for (var entry : map.entrySet()) {
                if (entry.getValue() instanceof String) {
                    outputMap.put(entry.getKey(), entry.getValue().toString());
                } else if (entry.getValue() == null) {
                    outputMap.put(entry.getKey(), null);
                } else if (entry.getValue() instanceof InternalNode) {
                    InternalNode SDNode = (InternalNode) entry.getValue();
                    outputMap.put("n.uri", SDNode.get("uri").toString().replace("\"", ""));
                }
            }
            resultList.add(outputMap);
        }
        return resultList;
    }
    /**
     * {@inheritDoc}
     */
    @Override
    public List<Map<String, String>> queryData(OpenCypherQuery sdQuery) {
        try (Session session = driver.session(); Transaction tx = session.beginTransaction()) {
            List<Map<String, String>> resultList = new ArrayList<>();
            log.debug("Beginning transaction");
            String cypherQuery = sdQuery.getQuery();
            boolean validQuery = validateQuery(cypherQuery);
            if (validQuery) {
                Result result = tx.run(sdQuery.getQuery());
                resultList = processResults(result);
                log.debug("Query executed successfully ");
                return resultList;
            } else {
                return null;
            }
        } catch (Exception e) {
            log.debug("Query unsuccessful " + e);
            throw e;
        }
    }
}
