package eu.xfsc.fc.client;

import java.util.List;
import java.util.Map;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.web.reactive.function.client.WebClient;

import eu.xfsc.fc.api.generated.model.Participant;
import eu.xfsc.fc.api.generated.model.Participants;
import eu.xfsc.fc.api.generated.model.UserProfile;

public class ParticipantClient extends ServiceClient {
    
    public ParticipantClient(String baseUrl, String jwt) {
        super(baseUrl, jwt);
    }

    public ParticipantClient(String baseUrl, WebClient client) {
        super(baseUrl, client);
    }
    
    public Participant getParticipant(String participantId) {
        return doGet(baseUrl + "/participants/{participantId}", Map.of("participantId", participantId), Participant.class);
    }
    
    public Participant getParticipant(String participantId, OAuth2AuthorizedClient authorizedClient) {
        return doGet(baseUrl + "/participants/{participantId}", Map.of("participantId", participantId), Participant.class, authorizedClient);
    }
    
    public List<UserProfile> getParticipantUsers(String participantId) {
        Class<List<UserProfile>> reType = (Class<List<UserProfile>>)(Class<?>) List.class;
        return doGet(baseUrl + "/participants/{participantId}/users", Map.of("participantId", participantId), reType);
    }
    
    public List<UserProfile> getParticipantUsers(String participantId, OAuth2AuthorizedClient authorizedClient) {
        Class<List<UserProfile>> reType = (Class<List<UserProfile>>)(Class<?>) List.class;
        return doGet(baseUrl + "/participants/{participantId}/users", Map.of("participantId", participantId), reType, authorizedClient);
    }
    
    public List<Participant> getParticipants(int offset, int limit) {
        Map<String, Object> params = buildPagingParams(offset, limit);
        Class<List<Participant>> reType = (Class<List<Participant>>)(Class<?>) List.class;
        return doGet(baseUrl + "/participants?offset={offset}&limit={limit}", params, reType);
    }

    public Participants getParticipants(int offset, int limit, OAuth2AuthorizedClient authorizedClient) {
        Map<String, Object> params = buildPagingParams(offset, limit);
        return doGet(baseUrl + "/participants?offset={offset}&limit={limit}", params, Participants.class, authorizedClient);
    }

    public Participant addParticipant(String participantSD) {
        return doPost(baseUrl + "/participants", participantSD, Map.of(), Participant.class);
    }
    
    public Participant addParticipant(String participantSD, OAuth2AuthorizedClient authorizedClient) {
        return doPost(baseUrl + "/participants", participantSD, Map.of(), Participant.class, authorizedClient);
    }
    
    public Participant deleteParticipant(String participantId) {
        return doDelete(baseUrl + "/participants/{participantId}", Map.of("participantId", participantId), Participant.class);
    }
    
    public Participant deleteParticipant(String participantId, OAuth2AuthorizedClient authorizedClient) {
        return doDelete(baseUrl + "/participants/{participantId}", Map.of("participantId", participantId), Participant.class, authorizedClient);
    }
    
    public Participant updateParticipant(String participantId, String participantSD) {
        return doPut(baseUrl + "/participants/{participantId}", participantSD, Map.of("participantId", participantId), Participant.class);
    }    

    public Participant updateParticipant(String participantId, String participantSD, OAuth2AuthorizedClient authorizedClient) {
        return doPut(baseUrl + "/participants/{participantId}", participantSD, Map.of("participantId", participantId), Participant.class, authorizedClient);
    }    
    
}
