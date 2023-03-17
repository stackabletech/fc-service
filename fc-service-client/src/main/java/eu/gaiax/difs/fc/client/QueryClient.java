package eu.gaiax.difs.fc.client;

import java.util.Map;

import org.springframework.web.reactive.function.client.WebClient;

import eu.gaiax.difs.fc.api.generated.model.QueryLanguage;
import eu.gaiax.difs.fc.api.generated.model.Results;
import eu.gaiax.difs.fc.api.generated.model.Statement;

public class QueryClient extends ServiceClient {

    public QueryClient(String baseUrl, String jwt) {
        super(baseUrl, jwt);
    }

    public QueryClient(String baseUrl, WebClient client) {
        super(baseUrl, client);
    }
    
    public Results query(QueryLanguage queryLanguage, Integer timeout, boolean withTotalCount, Statement statement) { 
        return doPost(baseUrl + "/query", statement, Map.of("queryLanguage", queryLanguage, "timeout", timeout, "withTotalCount", withTotalCount), 
        		Results.class);
    }

}
