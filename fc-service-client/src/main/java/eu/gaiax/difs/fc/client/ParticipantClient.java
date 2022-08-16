package eu.gaiax.difs.fc.client;

import java.util.List;
import java.util.Map;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.web.reactive.function.client.WebClient;

import eu.gaiax.difs.fc.api.generated.model.Participant;
import eu.gaiax.difs.fc.api.generated.model.UserProfile;

public class ParticipantClient extends ServiceClient {
    
    public ParticipantClient(String baseUrl, String jwt) {
        super(baseUrl, jwt);
    }

    public ParticipantClient(String baseUrl, WebClient client) {
        super(baseUrl, client);
    }
    
    public Participant getParticipant(String participantId) {
        return doGet(baseUrl + "/participants/{participantId}", Map.of("participantId", participantId));
    }
    
    public Participant getParticipant(String participantId, OAuth2AuthorizedClient authorizedClient) {
        return doGet(baseUrl + "/participants/{participantId}", Map.of("participantId", participantId), authorizedClient);
    }
    
    public List<UserProfile> getParticipantUsers(String participantId) {
        return doGet(baseUrl + "/participants/{participantId}/users", Map.of("participantId", participantId));
    }
    
    public List<UserProfile> getParticipantUsers(String participantId, OAuth2AuthorizedClient authorizedClient) {
        return doGet(baseUrl + "/participants/{participantId}/users", Map.of("participantId", participantId), authorizedClient);
    }
    
    public List<Participant> getParticipants(int offset, int limit) {
        if (limit == 0) {
            limit = 50;
        }
        return doGet(baseUrl + "/participants?offset={offset}&limit={limit}", Map.of("offset", offset, "limit", limit));
    }
    
    public List<Participant> getParticipants(int offset, int limit, OAuth2AuthorizedClient authorizedClient) {
        if (limit == 0) {
            limit = 50;
        }
        return doGet(baseUrl + "/participants?offset={offset}&limit={limit}", Map.of("offset", offset, "limit", limit), authorizedClient);
    }
    
    public Participant addParticipant(String participantSD) {
        return doPost(baseUrl + "/participants", participantSD, Map.of());
    }
    
    public Participant addParticipant(String participantSD, OAuth2AuthorizedClient authorizedClient) {
        return doPost(baseUrl + "/participants", participantSD, Map.of(), authorizedClient);
    }
    
    public Participant deleteParticipant(String participantId) {
        return doDelete(baseUrl + "/participants/{participantId}", Map.of("participantId", participantId));
    }
    
    public Participant deleteParticipant(String participantId, OAuth2AuthorizedClient authorizedClient) {
        return doDelete(baseUrl + "/participants/{participantId}", Map.of("participantId", participantId), authorizedClient);
    }
    
    public Participant updateParticipant(String participantId, String participantSD) {
        return doPut(baseUrl + "/participants/{participantId}", participantSD, Map.of("participantId", participantId));
    }    

    public Participant updateParticipant(String participantId, String participantSD, OAuth2AuthorizedClient authorizedClient) {
        return doPut(baseUrl + "/participants/{participantId}", participantSD, Map.of("participantId", participantId), authorizedClient);
    }    
    
}
