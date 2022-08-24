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
     * Query the graph with the openCypher query passed as parameter and this
     * returns list of Maps with key value pairs as a result.
     *
     * @param query An openCypher query to be executed
     * @return List of Maps containing the query results
     */
    List<Map<String, String>> queryData(OpenCypherQuery query);


}

