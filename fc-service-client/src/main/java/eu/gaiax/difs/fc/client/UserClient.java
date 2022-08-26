package eu.gaiax.difs.fc.client;

import eu.gaiax.difs.fc.api.generated.model.UserProfiles;
import java.util.List;
import java.util.Map;

import org.springframework.web.reactive.function.client.WebClient;

import eu.gaiax.difs.fc.api.generated.model.User;
import eu.gaiax.difs.fc.api.generated.model.UserProfile;

public class UserClient extends ServiceClient {

    public UserClient(String baseUrl, String jwt) {
        super(baseUrl, jwt);
    }

    public UserClient(String baseUrl, WebClient client) {
        super(baseUrl, client);
    }
    
    public UserProfile getUser(String userId) {
        return doGet(baseUrl + "/users/{userId}", Map.of("userId", userId), UserProfile.class);
    }
    
    public UserProfiles getUsers(int offset, int limit) {
        if (limit == 0) {
            limit = 50;
        }
        return doGet(baseUrl + "/users?offset={offset}&limit={limit}", Map.of("offset", offset, "limit", limit), UserProfiles.class);
    }

    public List<String> getUserRoles(String userId) {
        return doGet(baseUrl + "/users/{userId}/roles", Map.of("userId", userId), List.class);
    }
    
    public UserProfile addUser(User user) {
        return doPost(baseUrl + "/users", user, Map.of(), UserProfile.class);
    }
    
    public UserProfile deleteUser(String userId) {
        return doDelete(baseUrl + "/users/{userId}", Map.of("userId", userId), UserProfile.class);
    }

    public UserProfile updateUser(String userId, User user) {
        return doPut(baseUrl + "/users/{userId}", user, Map.of("userId", userId), UserProfile.class);
    }
    
    public List<String> updateUserRoles(String userId, List<String> roles) {
        return doPut(baseUrl + "/users/{userId}/roles", roles, Map.of("userId", userId), List.class);
    }
    
}
