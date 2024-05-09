package eu.xfsc.fc.client;

import java.util.List;
import java.util.Map;

import org.springframework.web.reactive.function.client.WebClient;

import eu.xfsc.fc.api.generated.model.OntologySchema;

public class SchemaClient extends ServiceClient {

    public SchemaClient(String baseUrl, String jwt) {
        super(baseUrl, jwt);
    }

    public SchemaClient(String baseUrl, WebClient client) {
        super(baseUrl, client);
    }
    
    public List<OntologySchema> getSchemas(int offset, int limit) {
        Map<String, Object> params = buildPagingParams(offset, limit);
        return doGet(baseUrl + "/schemas?offset={offset}&limit={limit}", params, List.class);
    }
    
    public void addSchema(OntologySchema schema) {
        doPost(baseUrl + "/schemas", schema, Map.of(), Void.class);
    }

    public OntologySchema getSchema(String schemaId) {
        return doGet(baseUrl + "/schemas/{schemaId}", Map.of("schemaId", schemaId), OntologySchema.class);
    }

    public void deleteSchema(String schemaId) {
        doDelete(baseUrl + "/schemas/{schemaId}", Map.of("schemaId", schemaId), Void.class);
    }

    public List<OntologySchema> getLatestSchemas() {
        return doGet(baseUrl + "/schemas/latest", Map.of(), List.class);
    }
    
    public OntologySchema getLatestSchemaOfType(String type) {
        return doGet(baseUrl + "/schemas/latest/{type}", Map.of("type", type), OntologySchema.class);
    }
    
}
