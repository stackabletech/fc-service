package eu.gaiax.difs.fc.core.service.graphdb;

import eu.gaiax.difs.fc.core.pojo.GraphQuery;

import java.util.List;
import java.util.Map;

public interface QueryGraph {


    /**
     * Query the graph when  Cypher query is passed in query object and this
     * returns list of Maps with key value pairs as a result.
     *
     * @param sdQuery Query to execute
     * @return List of Maps
     */
    public List<Map<String, String>> queryData(GraphQuery sdQuery);


}

