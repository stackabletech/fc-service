package eu.gaiax.difs.fc.core.service.graphdb.impl;

import eu.gaiax.difs.fc.api.generated.model.QueryLanguage;
import eu.gaiax.difs.fc.core.exception.ServerException;
import eu.gaiax.difs.fc.core.exception.TimeoutException;
import eu.gaiax.difs.fc.core.exception.VerificationException;
import eu.gaiax.difs.fc.core.pojo.GraphQuery;
import eu.gaiax.difs.fc.core.pojo.PaginatedResults;
import eu.gaiax.difs.fc.core.pojo.SdClaim;
import eu.gaiax.difs.fc.core.service.graphdb.GraphStore;
import eu.gaiax.difs.fc.core.util.ClaimValidator;
import eu.gaiax.difs.fc.core.util.ExtendClaims;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Model;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.TransactionConfig;
import org.neo4j.driver.exceptions.DatabaseException;
import org.neo4j.driver.internal.InternalNode;
import org.neo4j.driver.internal.InternalRelationship;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
@Transactional
public class Neo4jGraphStore implements GraphStore {

    private static final String queryInsert = "CALL n10s.rdf.import.inline($payload, \"N-Triples\");"; //\n" +
                                              //"YIELD terminationStatus, triplesLoaded, triplesParsed, namespaces, extraInfo\n" +
                                              //"RETURN terminationStatus, triplesLoaded, triplesParsed, namespaces, extraInfo";
    private static final String queryDelete = "MATCH (n {claimsGraphUri: [$uri]})\n" +
                                              "DETACH DELETE n;";
    private static final String queryUpdate = "MATCH (n) WHERE $uri IN n.claimsGraphUri\n" +
                                              "SET n.claimsGraphUri = [g IN n.claimsGraphUri WHERE g <> $uri];";
    
    @Autowired
    private Driver driver;
    private final ClaimValidator claimValidator;

    /* Any appearances of ORDER BY (each word surrounded by any whitespace)
     * which is not enclosed by quotes
     */
    protected final Pattern orderByRegex = Pattern.compile("ORDER\\sBY(?=(?:[^'\"`]*(['\"`])[^'\"`]*\1)*[^'\"`]*$)", Pattern.CASE_INSENSITIVE);

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
        if (!sdClaimList.isEmpty()) {
            try (Session session = driver.session()) { 
                Model model = claimValidator.validateClaims(sdClaimList);
                String claimsAdded = ExtendClaims.addPropertyGraphUri(model, credentialSubject);
                Set<String> properties = ExtendClaims.getMultivalProp(model);
                if (!properties.isEmpty()) {
                    updateGraphConfig(session, properties);
                }
                Result rs = session.run(queryInsert, Map.of("payload", claimsAdded));
                log.debug("addClaims; inserted: {}", rs.consume());
            } catch (IOException ex) {
            //    log.error("addClaims.error;", ex);
                throw new VerificationException(ex);
            }
        }
        log.debug("addClaims.exit");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteClaims(String credentialSubject) {
        log.debug("deleteClaims.enter; got subject: {}", credentialSubject);
        Map<String, Object> params = Map.of("uri", credentialSubject);
        try (Session session = driver.session()) {
            Result rsDelelte = session.run(queryDelete, params);
            log.debug("deleteClaims; deleted: {}", rsDelelte.consume());
            Result rsUpdate = session.run(queryUpdate, params);
            log.debug("deleteClaims; updated: {}", rsUpdate.consume());
        //} catch (Exception ex) {
        //    log.error("deleteClaims.error;", ex);
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
            //In this method we use read transaction to avoid any Cypher query that modifies data
            return session.readTransaction(tx -> doQuery(tx, sdQuery), transactionConfig);
        } catch (Exception ex) {
            stamp = System.currentTimeMillis() - stamp;
            log.error("queryData.error;", ex);
            if (ex.getMessage() != null && ex.getMessage().contains("dbms.transaction.timeout")) {
                if (stamp > sdQuery.getTimeout() * 1000) {
                    throw new TimeoutException("query timeout (" + sdQuery.getTimeout() + " sec) exceeded)");
                }
            }
            throw new ServerException("error querying data " + ex.getMessage());
        }
    }
    
    private PaginatedResults<Map<String, Object>> doQuery(Transaction tx, GraphQuery query) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        String finalString = getDynamicallyAddedCountClauseQuery(query);
        Result result = tx.run(finalString, query.getParams());
        log.debug("doQuery; got result: {}", result.keys());
        Long totalCount = 0L;
        while (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();
            Map<String, Object> map = record.asMap();
            log.debug("doQuery; record: {}", map);
            Map<String, Object> outputMap = new HashMap<>();
            totalCount = (Long) map.getOrDefault("totalCount", resultList.size());
            for (var entry : map.entrySet()) {
                if (entry.getKey().equals("totalCount"))
                    continue;
                if (entry.getValue() == null) {
                    outputMap.put(entry.getKey(), null);
                } else if (entry.getValue() instanceof InternalNode) {
                    Map<String, Object> nodeMap = ((InternalNode) entry.getValue()).asMap();
                    Map<String, Object> modifiableNodeMap = new HashMap<>(nodeMap);
                    modifiableNodeMap.remove("uri");
                    outputMap.put(entry.getKey(), modifiableNodeMap);
                } else if (entry.getValue() instanceof InternalRelationship) {
                    outputMap.put(entry.getKey(), ((InternalRelationship) entry.getValue()).type());
                } else {
                    outputMap.put(entry.getKey(), entry.getValue());
                }
            }
            resultList.add(outputMap);
        }

        // Shuffle list to guarantee results won't appear in a deterministic order thus giving certain results
        // an advantage over others as they would always be in the top n result entries.
        // However, the shuffling should only be performed if the query does not, by itself, return an ordered result.
        Matcher matcher = orderByRegex.matcher(query.getQuery());
        boolean queryProvidesOrderedResult = matcher.find();
        if (!queryProvidesOrderedResult) {
            Collections.shuffle(resultList);
        }

        log.debug("doQuery.exit; returning: {}", resultList);
        return new PaginatedResults<>(totalCount, resultList);
    }

    private void updateGraphConfig(Session session, Set<String> properties) {
        Result config = session.run("CALL n10s.graphconfig.show");
        while (config.hasNext()) {
            org.neo4j.driver.Record record = config.next();
            Map<String,Object> propMap = record.asMap();
            if (propMap.get("param").equals("multivalPropList")) {
                Collection<String> propList = new HashSet<>((Collection<String>) propMap.get("value"));
                log.debug("updateGraphConfig; got multivalPropList {}", propList);
                int size= propList.size();
                propList.addAll(properties);
                if (propList.size() > size) {
                    log.debug("updateGraphConfig; Adding new properties to graphconfig {}", propList);
                    try {
                        Map<String, Object> params = Map.of("propList", propList, "force", true);
                        session.run("CALL n10s.graphconfig.set({multivalPropList: $propList, force: $force})", params);
                    } catch (Exception e) {
                        log.error("updateGraphConfig.error; Failed to add new properties due to Exception", e);
                    }
                }
                break;
            }
        }
    }

    private String getDynamicallyAddedCountClauseQuery(GraphQuery sdQuery) {
        log.debug("getDynamicallyAddedCountClauseQuery.enter; actual query: {}", sdQuery.getQuery());
        /*get string before statements and append count clause*/
        String statement = "return";
        int indexOf = sdQuery.getQuery().toLowerCase().lastIndexOf(statement);

        if (indexOf == -1) {
            // no need for count if no return
            return sdQuery.getQuery();
        }
        
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
    }


}
