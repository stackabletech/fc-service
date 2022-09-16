package eu.gaiax.difs.fc.core.pojo;

import java.util.Map;

/**
 * POJO Class for holding a Cypher Query.
 */
@lombok.AllArgsConstructor
@lombok.EqualsAndHashCode
@lombok.Getter
@lombok.ToString
public class OpenCypherQuery {
	
	private final String query;
	private Map<String, Object> params;
	
}
