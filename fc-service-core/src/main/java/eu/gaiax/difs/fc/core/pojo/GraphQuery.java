package eu.gaiax.difs.fc.core.pojo;

import java.util.Map;

import eu.gaiax.difs.fc.api.generated.model.QueryLanguage;

/**
 * POJO Class for holding a Cypher Query.
 */
@lombok.AllArgsConstructor
@lombok.EqualsAndHashCode
@lombok.Getter
@lombok.ToString
public class GraphQuery {
    
    private static final int QUERY_TIMEOUT = 5;
	
	private final String query;
	private Map<String, Object> params;
	private QueryLanguage queryLanguage;
    private int timeout;

    public GraphQuery(String query, Map<String, Object> params) {
      this(query, params, QueryLanguage.OPENCYPHER, QUERY_TIMEOUT);  
    }
    
}
