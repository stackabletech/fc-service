package eu.gaiax.difs.fc.client;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
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
    
    public List<SelfDescription> getSelfDescriptions(Instant uploadStart, Instant uploadEnd, Instant statusStart, Instant statusEnd, 
    		Collection<String> issuers, Collection<String> validators, Collection<String> statuses, Collection<String> ids,
            Collection<String> hashes, Boolean withMeta, Boolean withContent, Integer offset, Integer limit) {
    	StringBuilder query = new StringBuilder(baseUrl);
    	query.append("/self-descriptions?");
    	Map<String, Object> params = new HashMap<>();
    	buildQuery(query, params, "upload-timerange", buildTimeRange(uploadStart, uploadEnd));
    	buildQuery(query, params, "status-timerange", buildTimeRange(statusStart, statusEnd));
    	buildQuery(query, params, "issuers", buildList(issuers));
    	buildQuery(query, params, "validators", buildList(validators));
    	buildQuery(query, params, "statuses", buildList(statuses));
    	buildQuery(query, params, "ids", buildList(ids));
    	buildQuery(query, params, "hashes", buildList(hashes));
    	buildQuery(query, params, "withMeta", withMeta);
    	buildQuery(query, params, "withContent", withContent);
    	buildQuery(query, params, "offset", offset);
    	buildQuery(query, params, "limit", limit);
    	if (query.charAt(query.length() - 1) == '&') {
    		query.deleteCharAt(query.length() - 1);
    	}
    	return doGet(query.toString(), params, List.class);
    }
    
    public SelfDescription addSefDescription(String selfDescription) {
        return doPost(baseUrl + "/self-descriptions", selfDescription, Map.of(), SelfDescription.class);
    }

    public SelfDescription getSelfDescription(String hash) {
        return doGet(baseUrl + "/self-descriptions/{self_description_hash}", Map.of("self_description_hash", hash), SelfDescription.class);
    }

    public void deleteSelfDescription(String hash) {
        doDelete(baseUrl + "/self-descriptions/{self_description_hash}", Map.of("self_description_hash", hash), Void.class);
    }

    public void revokeSelfDescription(String hash) {
        doPost(baseUrl + "/self-descriptions/{self_description_hash}/revoke", null, Map.of("self_description_hash", hash), Void.class);
    }

    public SelfDescription getSelfDescriptionById(String id) {
        List<SelfDescription> sds = getSelfDescriptions(null, null, null, null, null, null, null, List.of(id), null, true, true, null, null);
        return sds.isEmpty() ? null: sds.get(0);
    }
   
    public List<SelfDescription> getSelfDescriptionsByIds(List<String> ids) {
        return getSelfDescriptions(null, null, null, null, null, null, null, ids, null, true, true, null, null);
    }
    
    private String buildTimeRange(Instant start, Instant end) {
    	if (start == null) {
    		if (end == null) {
    			return null;
    		}
    		start = Instant.ofEpochMilli(0);
    	} else {
    		if (end == null) {
    			end = Instant.now().plusSeconds(86400);
    		}
    	}
    	return start.toString() + "/" + end.toEpochMilli();
    }
    
    private String buildList(Collection<String> list) {
    	if (list == null || list.isEmpty()) {
    		return null;
    	}
    	return String.join(",", list);
    }
    
    private void buildQuery(StringBuilder query, Map<String, Object> params, String param, Object value) {
    	if (value != null) {
    		query.append(param).append("={").append(param).append("}&");
    		params.put(param, value);
    	}
    }   
        
}
