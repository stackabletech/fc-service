package eu.gaiax.difs.fc.core.service.graphdb;

import eu.gaiax.difs.fc.core.pojo.OpenCypherQuery;
import eu.gaiax.difs.fc.core.pojo.SdClaim;

import java.util.List;
import java.util.Map;

/**
 * Defines the required functions to add, query, update and delete active claims extracted from self-descriptions
 * @author nacharya
 */
public interface GraphStore {


    /**
     * Pushes set of claims to the Graph DB. The set of claims are list of claim
     * objects containing subject, predicate and object similar to the form of n-triples
     * format stored in individual strings.
     *
     * @param sdClaimList List of claims to be added to the Graph DB.
     * @param credentialSubject contains a self-description unique identifier
     */
    void addClaims(List<SdClaim> sdClaimList, String credentialSubject);

    /**
     * Deletes all claims in the Graph DB of a given self-description
     * @param credentialSubject contains a self-description unique identifier
     */
    void deleteClaims(String credentialSubject);

    /**
     * Query the graph when Cypher query is passed in query object and this
     * returns list of Maps with key value pairs as a result.
     *
     * @param sdQuery is the query to be executed
     * @return List of Maps
     */
    List<Map<String, Object>> queryData(OpenCypherQuery sdQuery);

}

