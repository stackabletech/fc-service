package eu.gaiax.difs.fc.client;

import java.util.List;
import java.util.Map;

import org.springframework.web.reactive.function.client.WebClient;

public class RoleClient extends ServiceClient {

    public RoleClient(String baseUrl, String jwt) {
        super(baseUrl, jwt);
    }

    public RoleClient(String baseUrl, WebClient client) {
        super(baseUrl, client);
    }
    
    public List<String> getAllRoles(int offset, int limit) {
        if (limit == 0) {
            limit = 50;
        }
        return doGet(baseUrl + "/roles?offset={offset}&limit={limit}", Map.of("offset", offset, "limit", limit), List.class);
    }
    
}
