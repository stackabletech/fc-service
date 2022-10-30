package eu.gaiax.difs.fc.core.service.graphdb.impl;

import eu.gaiax.difs.fc.api.generated.model.QueryLanguage;
import eu.gaiax.difs.fc.core.exception.ServerException;
import eu.gaiax.difs.fc.core.exception.TimeoutException;
import eu.gaiax.difs.fc.core.pojo.GraphQuery;
import eu.gaiax.difs.fc.core.pojo.PaginatedResults;
import eu.gaiax.difs.fc.core.pojo.SdClaim;
import eu.gaiax.difs.fc.core.service.graphdb.GraphStore;
import eu.gaiax.difs.fc.core.util.ExtendClaims;
import eu.gaiax.difs.fc.core.util.ClaimValidator;
import liquibase.pro.packaged.L;
import liquibase.pro.packaged.T;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Model;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.TransactionConfig;
import org.neo4j.driver.exceptions.DatabaseException;
import org.neo4j.driver.internal.InternalNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@Component
public class Neo4jGraphStore implements GraphStore {

    @Autowired
    private Driver driver;
    private final ClaimValidator claimValidator;

    public Neo4jGraphStore() {
        super();

        this.claimValidator = new ClaimValidator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addClaims(List<SdClaim> sdClaimList, String credentialSubject) {
        log.debug("addClaims.enter; got claims: {}, subject: {}", sdClaimList, credentialSubject);
        int cnt = 0;
        if (!sdClaimList.isEmpty()) {
            StringBuilder payload = new StringBuilder();
            try (Session session = driver.session()) {
                for (SdClaim sdClaim : sdClaimList) {
                    Model model = claimValidator.validateClaim(sdClaim);
                    String claimsAdded = ExtendClaims.addPropertyGraphUri(model, credentialSubject);
                    payload.append(claimsAdded);
                    cnt++;
                }

                String query = "CALL n10s.rdf.import.inline($payload, \"N-Triples\")\n"
                        + "YIELD terminationStatus, triplesLoaded, triplesParsed, namespaces, extraInfo\n"
                        + "RETURN terminationStatus, triplesLoaded, triplesParsed, namespaces, extraInfo";
                log.debug("addClaims; query: {}", query);
                Result rs = session.run(query, Map.of("payload", payload.toString()));
                log.debug("addClaims; response: {}", rs.list());
            }
        }
        log.debug("addClaims.exit; claims processed: {}", cnt);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteClaims(String credentialSubject) {
        log.debug("deleteClaims.enter; Beginning claims deletion, subject: {}", credentialSubject);
        String queryDelete = "MATCH (n {claimsGraphUri: [$uri]})\n" +
                "DETACH DELETE n;";
        String queryUpdate = "MATCH (n)\n" +
                "WHERE $uri IN n.claimsGraphUri\n" +
                "SET n.claimsGraphUri = [g IN n.claimsGraphUri WHERE g <> $uri];";
        try (Session session = driver.session()) {
            Result rsDelelte = session.run(queryDelete, Map.of("uri", credentialSubject));
            Result rsUpdate = session.run(queryUpdate, Map.of("uri", credentialSubject));
            log.debug("deleteClaims.exit; results: {}", rsDelelte.list());
            log.debug("updateClaims.exit; results: {}", rsUpdate.list());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PaginatedResults<Map<String, Object>> queryData(GraphQuery sdQuery) {
        log.debug("queryData.enter; got query: {}", sdQuery);

        if (sdQuery.getQueryLanguage() != QueryLanguage.OPENCYPHER) {
            throw new UnsupportedOperationException(sdQuery.getQueryLanguage() + " query language is not supported yet");
        }

        TransactionConfig transactionConfig = TransactionConfig.builder()
                .withTimeout(Duration.ofSeconds(sdQuery.getTimeout()))
                .build();

        long stamp = System.currentTimeMillis();
        try (Session session = driver.session()) {
            //In this function we use read transaction to avoid any Cypher query that modifies data

            return session.readTransaction(
                    tx -> {
                        List<Map<String, Object>> resultList = new ArrayList<>();
                        String finalString = getDynamicallyAddedCountClauseQuery(sdQuery);
                        Result result = tx.run(finalString, sdQuery.getParams());
                        log.debug("queryData; got result: {}", result.keys());
                        Long totalCount = 0L;
                        while (result.hasNext()) {
                            org.neo4j.driver.Record record = result.next();
                            Map<String, Object> map = record.asMap();
                            log.debug("queryData; record: {}", map);
                            Map<String, Object> outputMap = new HashMap<>();
                            totalCount = (Long) map.getOrDefault("totalCount", resultList.size());
                            for (var entry : map.entrySet()) {
                                if(entry.getKey().equals("totalCount"))
                                    continue;
                                if (entry.getValue() == null) {
                                    outputMap.put(entry.getKey(), null);
                                } else if (entry.getValue() instanceof InternalNode) {
                                    InternalNode SDNode = (InternalNode) entry.getValue();
                                    outputMap.put("n.uri", SDNode.get("uri").toString().replace("\"", ""));
                                } else {
                                    outputMap.put(entry.getKey(), entry.getValue());
                                }
                            }
                            resultList.add(outputMap);
                        }
                        log.debug("queryData.exit; returning: {}", resultList);
                        return new PaginatedResults<>(totalCount, resultList);
                    },
                    transactionConfig
            );
        } catch (Exception e) {
            stamp = System.currentTimeMillis() - stamp;
            log.error("queryData.error", e);
            if (e instanceof DatabaseException) {
                // TODO: here we must recognize a scenario when we get DatabaseException because of the query timeout
                // if no better solution, when we can check exception stack for the following text:
                // Suppressed: org.neo4j.driver.exceptions.ServiceUnavailableException: Connection to the database terminated.
                // and also check the stamp value > query timeout:
                if (stamp > sdQuery.getTimeout() * 1000) {
                    throw new TimeoutException("query timeout (" + sdQuery.getTimeout() + " sec) exceeded)");
                }
            }
            throw new ServerException("error querying data " + e.getMessage());
        }
    }

    private String getDynamicallyAddedCountClauseQuery(GraphQuery sdQuery) {
        log.debug("getDynamicallyAddedCountClauseQuery.enter; actual query: {}", sdQuery.getQuery());
        /*get string before statements and append count clause*/
        String statement = "return";
        int indexOf = sdQuery.getQuery().toLowerCase().lastIndexOf(statement);

        if (indexOf != -1) {
            /*add totalCount to query to get count*/
            StringBuilder subStringOfCount = new StringBuilder(sdQuery.getQuery().substring(0, indexOf));
            subStringOfCount.append("WITH count(*) as totalCount ");

            /*append totalCount to return statements*/
            StringBuilder actualQuery = new StringBuilder(sdQuery.getQuery());
            int indexOfAfter = actualQuery.toString().toLowerCase().lastIndexOf(statement) + statement.length();
            actualQuery.insert(indexOfAfter + 1, "totalCount, ");

            /*finally combine both string */
            String finalString = subStringOfCount.append(actualQuery).toString();
            log.debug("getDynamicallyAddedCountClauseQuery.exit; count query appended : {}", finalString);
            return finalString;
        } else {
            return sdQuery.getQuery();
        }
    }



}
