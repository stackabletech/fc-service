package eu.xfsc.fc.client;

import java.util.List;
import java.util.Map;

import org.springframework.web.reactive.function.client.WebClient;

import eu.xfsc.fc.api.generated.model.AnnotatedStatement;
import eu.xfsc.fc.api.generated.model.QueryLanguage;
import eu.xfsc.fc.api.generated.model.Results;
import eu.xfsc.fc.api.generated.model.Statement;
import reactor.core.publisher.Mono;

public class QueryClient extends ServiceClient {

    public QueryClient(String baseUrl, String jwt) {
        super(baseUrl, jwt);
    }

    public QueryClient(String baseUrl, WebClient client) {
        super(baseUrl, client);
    }
    
    public List<Map<String, Object>> query(String query, Map<String, Object> params) {
    	Statement stmt = new Statement();
    	stmt = stmt.parameters(params).statement(query);
        Results results = doPost(baseUrl + "/query", stmt, Map.of(), Results.class);
        return results.getItems();
    }

    public Results query(QueryLanguage queryLanguage, Integer timeout, boolean withTotalCount, Statement statement) {
    	Map<String, Object> params = Map.of("queryLanguage", queryLanguage, "timeout", timeout, "withTotalCount", withTotalCount);
    	String query = super.buildQuery(params);
        return doPost(baseUrl + "/query" + query, statement, params, Results.class);
    }

    public Mono<Results> queryAsync(QueryLanguage queryLanguage, Integer timeout, boolean withTotalCount, Statement statement) { 
    	Map<String, Object> params = Map.of("queryLanguage", queryLanguage, "timeout", timeout, "withTotalCount", withTotalCount);
    	String query = super.buildQuery(params);
        return doPostAsync(baseUrl + "/query" + query, statement, params, Results.class);
    }

    public Results search(AnnotatedStatement statement) { 
        return doPost(baseUrl + "/query/search", statement, Map.of(), Results.class);
    }

    public Mono<Results> searchAsync(AnnotatedStatement statement) { 
        return doPostAsync(baseUrl + "/query/search", statement, Map.of(), Results.class);
    }
    
}
