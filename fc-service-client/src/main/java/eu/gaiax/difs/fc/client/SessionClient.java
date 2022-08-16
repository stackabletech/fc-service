package eu.gaiax.difs.fc.client;

import java.util.Map;

import org.springframework.web.reactive.function.client.WebClient;

import eu.gaiax.difs.fc.api.generated.model.Session;

public class SessionClient extends ServiceClient {

    public SessionClient(String baseUrl, String jwt) {
        super(baseUrl, jwt);
    }

    public SessionClient(String baseUrl, WebClient client) {
        super(baseUrl, client);
    }
    
    public Session getCurrentSession() {
        return doGet(baseUrl + "/session", Map.of());
    }

    public void deleteCurrentSession() {
        doDelete(baseUrl + "/session", Map.of());
    }

}
