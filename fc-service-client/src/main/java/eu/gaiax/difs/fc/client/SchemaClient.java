package eu.gaiax.difs.fc.client;

import java.util.List;
import java.util.Map;

import org.springframework.web.reactive.function.client.WebClient;

import eu.gaiax.difs.fc.api.generated.model.OntologySchema;

public class SchemaClient extends ServiceClient {

    public SchemaClient(String baseUrl, String jwt) {
        super(baseUrl, jwt);
    }

    public SchemaClient(String baseUrl, WebClient client) {
        super(baseUrl, client);
    }
    
    public List<OntologySchema> getSchemas(int offset, int limit) {
        if (limit == 0) {
            limit = 100;
        }
        return doGet(baseUrl + "/schemas?offset={offset}&limit={limit}", Map.of("offset", offset, "limit", limit));
    }
    
    public void addSchema(OntologySchema schema) {
        doPost(baseUrl + "/schemas", schema, Map.of());
    }

    public OntologySchema getSchema(String schemaId) {
        return doGet(baseUrl + "/schemas/{schemaId}", Map.of("schemaId", schemaId));
    }

    public void deleteSchema(String schemaId) {
        doDelete(baseUrl + "/schemas/{schemaId}", Map.of("schemaId", schemaId));
    }

    public List<OntologySchema> getLatestSchemas() {
        return doGet(baseUrl + "/schemas/latest", Map.of());
    }
    
    public OntologySchema getLatestSchemaOfType(String type) {
        return doGet(baseUrl + "/schemas/latest/{type}", Map.of("type", type));
    }
    
}
