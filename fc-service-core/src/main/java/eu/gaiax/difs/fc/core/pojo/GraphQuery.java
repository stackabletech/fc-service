package eu.gaiax.difs.fc.core.pojo;

public class GraphQuery {
    String selfDescriptionQuery;


    public GraphQuery(String selfDescriptionQuery) {
        this.selfDescriptionQuery = selfDescriptionQuery;
    }


    public String getQuery() {
        /* Get Neo4j Query to be executed*/

        return selfDescriptionQuery;
    }

}
