package eu.gaiax.difs.fc.core.service.graphdb;

import eu.gaiax.difs.fc.core.pojo.OpenCypherQuery;

import java.util.List;
import java.util.Map;

/**
 * Defines the required functions to query the Graph DB
 * @author nacharya
 */
public interface QueryGraph {


    /**
     * Query the graph when Cypher query is passed in query object and this
     * returns list of Maps with key value pairs as a result.
     *
     * @param sdQuery is the query to be executed
     * @return List of Maps
     */
    public List<Map<String, String>> queryData(OpenCypherQuery sdQuery);


}
