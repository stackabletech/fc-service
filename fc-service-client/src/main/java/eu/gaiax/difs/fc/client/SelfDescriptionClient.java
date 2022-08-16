package eu.gaiax.difs.fc.client;

import java.util.List;
import java.util.Map;

import org.springframework.web.reactive.function.client.WebClient;

import eu.gaiax.difs.fc.api.generated.model.SelfDescription;

public class SelfDescriptionClient extends ServiceClient {

    public SelfDescriptionClient(String baseUrl, String jwt) {
        super(baseUrl, jwt);
    }

    public SelfDescriptionClient(String baseUrl, WebClient client) {
        super(baseUrl, client);
    }

    public List<SelfDescription> getSelfDescriptions(String uploadTimeRange, String statusTimeRange, String issuer, String validator, String status, String id, String hash) {
        return doGet(baseUrl + "/self-descriptions?upload-timerange={upload-timerange}&status-timerange={status-timerange}&issuer={issuer}&validator={validator}&" +
            "status={status}&id={id}&hash={hash}", Map.of("upload-timerange", uploadTimeRange, "status-timerange", statusTimeRange, "issuer", issuer, "validator", validator,
                    "status", status, "id", id, "hash", hash));
    }
    
    public SelfDescription addSefDescription(String selfDescription) {
        return doPost(baseUrl + "/self-descriptions", selfDescription, Map.of());
    }

    public SelfDescription getSelfDescription(String hash) {
        return doGet(baseUrl + "/self-descriptions/{self_description_hash}", Map.of("self_description_hash", hash));
    }

    public void deleteSelfDescription(String hash) {
        doDelete(baseUrl + "/self-descriptions/{self_description_hash}", Map.of("self_description_hash", hash));
    }

    public void revokeSelfDescription(String hash) {
        doPost(baseUrl + "/self-descriptions/{self_description_hash}/revoke", null, Map.of("self_description_hash", hash));
    }
        
}
