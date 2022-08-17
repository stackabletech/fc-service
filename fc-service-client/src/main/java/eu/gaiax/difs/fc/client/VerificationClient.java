package eu.gaiax.difs.fc.client;

import java.util.Map;

import org.springframework.web.reactive.function.client.WebClient;

import eu.gaiax.difs.fc.api.generated.model.VerificationResult;

public class VerificationClient extends ServiceClient {

    public VerificationClient(String baseUrl, String jwt) {
        super(baseUrl, jwt);
    }

    public VerificationClient(String baseUrl, WebClient client) {
        super(baseUrl, client);
    }
    
    public VerificationResult verify(String selfDescription) {
        return doPost(baseUrl + "/verification", selfDescription, Map.of(), VerificationResult.class);
    }

}
