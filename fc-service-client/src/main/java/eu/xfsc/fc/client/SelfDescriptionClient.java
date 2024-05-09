package eu.xfsc.fc.client;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.reactive.function.client.WebClient;

import eu.xfsc.fc.api.generated.model.SelfDescription;
import eu.xfsc.fc.api.generated.model.SelfDescriptionResult;

public class SelfDescriptionClient extends ServiceClient {

    public SelfDescriptionClient(String baseUrl, String jwt) {
        super(baseUrl, jwt);
    }

    public SelfDescriptionClient(String baseUrl, WebClient client) {
        super(baseUrl, client);
    }
    
    public List<SelfDescriptionResult> getSelfDescriptions(Instant uploadStart, Instant uploadEnd, Instant statusStart, Instant statusEnd, 
    		Collection<String> issuers, Collection<String> validators, Collection<String> statuses, Collection<String> ids,
            Collection<String> hashes, Boolean withMeta, Boolean withContent, Integer offset, Integer limit) {
    	StringBuilder query = new StringBuilder(baseUrl);
    	query.append("/self-descriptions?");
    	Map<String, Object> params = new HashMap<>();
    	addQuery(query, params, "upload-timerange", addQueryTimeRange(uploadStart, uploadEnd));
    	addQuery(query, params, "status-timerange", addQueryTimeRange(statusStart, statusEnd));
    	addQuery(query, params, "issuers", addQueryList(issuers));
    	addQuery(query, params, "validators", addQueryList(validators));
    	addQuery(query, params, "statuses", addQueryList(statuses));
    	addQuery(query, params, "ids", addQueryList(ids));
    	addQuery(query, params, "hashes", addQueryList(hashes));
    	addQuery(query, params, "withMeta", withMeta);
    	addQuery(query, params, "withContent", withContent);
    	addQuery(query, params, "offset", offset);
    	addQuery(query, params, "limit", limit);
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

    public SelfDescriptionResult getSelfDescriptionByHash(String hash, boolean withMeta, boolean withContent) {
        List<SelfDescriptionResult> sds = getSelfDescriptions(null, null, null, null, null, null, null, null, List.of(hash), withMeta, withContent, null, null);
        return sds.isEmpty() ? null: sds.get(0);
    }
    
    public SelfDescriptionResult getSelfDescriptionById(String id) {
        List<SelfDescriptionResult> sds = getSelfDescriptions(null, null, null, null, null, null, null, List.of(id), null, true, true, null, null);
        return sds.isEmpty() ? null: sds.get(0);
    }
   
    public List<SelfDescriptionResult> getSelfDescriptionsByIds(List<String> ids) {
        return getSelfDescriptions(null, null, null, null, null, null, null, ids, null, true, true, null, null);
    }
    
    private void addQuery(StringBuilder query, Map<String, Object> params, String param, Object value) {
    	if (value != null) {
    		query.append(param).append("={").append(param).append("}&");
    		params.put(param, value);
    	}
    }   
        
    private String addQueryList(Collection<String> list) {
    	if (list == null || list.isEmpty()) {
    		return null;
    	}
    	return String.join(",", list);
    }
    
    private String addQueryTimeRange(Instant start, Instant end) {
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
    
}
