package eu.xfsc.fc.client;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public abstract class ServiceClient {
    
    protected final String baseUrl; 
    protected final ObjectMapper mapper;
    protected final WebClient client;

    public ServiceClient(String baseUrl, String jwt) {
        this.baseUrl = baseUrl;
        mapper = new ObjectMapper()
            .findAndRegisterModules()   // .registerModule(new ParanamerModule()) .registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        WebClient.Builder builder = WebClient.builder()
            .baseUrl(baseUrl)
            .codecs(configurer -> {
                configurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(mapper, MediaType.APPLICATION_JSON));
                configurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(mapper, MediaType.APPLICATION_JSON));
            })
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .filter(ExchangeFilterFunction.ofResponseProcessor(response -> {
            	if (response.statusCode().isError()) {
            		return response.bodyToMono(Map.class)
            				.flatMap(map -> Mono.error(new ExternalServiceException(response.statusCode(), map)));
            	}
            	return Mono.just(response);
            }));
        if (jwt != null) {
            builder = builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
        }
        this.client = builder.build();
//      this.template.setErrorHandler(new ErrorHandler(mapper));
    }

    public ServiceClient(String baseUrl, WebClient client) {
        this.baseUrl = baseUrl;
        this.client = client;
        this.mapper = new ObjectMapper()
                .findAndRegisterModules()   // .registerModule(new ParanamerModule()) .registerModule(new JavaTimeModule())
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
    
    public String getUrl() {
    	return this.baseUrl;
    }
    
    private String buildQuery(Map<String, Object> params) {
    	String query = "";
    	if (params.size() > 0) {
    		int idx = 0;
    		for (String param: params.keySet()) {
    			if (idx == 0) {
    				query += "?";
    			} else {
    				query += "&";
    			}
    			query += param + "={" + param + "}";
    			idx++;
    		}
    	}
    	return query;
    }

    protected <T> T doGet(String path, Map<String, Object> params, Class<T> reType) {
    	log.debug("doGet.enter; params: {}", params);
    	String query = buildQuery(params);
        return client
            .get()
            .uri(path + query, builder -> builder.build(params))
            .retrieve()
            .bodyToMono(reType)
            .block();
    }

    protected <T> T doGet(String path, Map<String, Object> params, Class<T> reType, OAuth2AuthorizedClient authorizedClient) {
    	String query = buildQuery(params);
        return client
            .get()
            .uri(path + query, builder -> builder.build(params))
            .attributes(oauth2AuthorizedClient(authorizedClient))
            .retrieve()
            .bodyToMono(reType) 
            .block();
    }
    
    // TODO: add asynch GET methods also

    protected <T> T doPost(String path, Object body, Map<String, Object> params, Class<T> reType) {
    	String query = buildQuery(params);
        return client
            .post()
            .uri(path + query, builder -> builder.build(params))
            .bodyValue(body)
            .retrieve()
            .bodyToMono(reType) 
            .block();
    }

    protected <T> T doPost(String path, Map<String, Object> params, Class<T> reType) {
    	String query = buildQuery(params);
        return client
            .post()
            .uri(path + query, builder -> builder.build(params))
            .retrieve()
            .bodyToMono(reType)
            .block();
    }
    
    protected <T> T doPost(String path, Object body, Map<String, Object> params, Class<T> reType, OAuth2AuthorizedClient authorizedClient) {
    	String query = buildQuery(params);
        return client
            .post()
            .uri(path + query, builder -> builder.build(params))
            .bodyValue(body)
            .attributes(oauth2AuthorizedClient(authorizedClient))
            .retrieve()
            .bodyToMono(reType) 
            .block();
    }

    protected <T> Mono<T> doPostAsync(String path, Object body, Map<String, Object> params, Class<T> reType) {
    	String query = buildQuery(params);
        return client
            .post()
            .uri(path + query, builder -> builder.build(params))
            .bodyValue(body)
            .retrieve()
            .bodyToMono(reType); 
    }
    
    protected <T> Mono<T> doPostAsync(String path, Map<String, Object> params, Class<T> reType) {
    	String query = buildQuery(params);
        return client
            .post()
            .uri(path + query, builder -> builder.build(params))
            .retrieve()
            .bodyToMono(reType);
    }
    
    protected <T> Mono<T> doPostAsync(String path, Object body, Map<String, Object> params, Class<T> reType, OAuth2AuthorizedClient authorizedClient) {
    	String query = buildQuery(params);
        return client
            .post()
            .uri(path+ query, builder -> builder.build(params))
            .bodyValue(body)
            .attributes(oauth2AuthorizedClient(authorizedClient))
            .retrieve()
            .bodyToMono(reType);
    }
    
    protected <T> T doPut(String path, Object body, Map<String, Object> params, Class<T> reType) {
    	String query = buildQuery(params);
        return client
            .put()
            .uri(path + query, builder -> builder.build(params))
            .bodyValue(body)
            .retrieve()
            .bodyToMono(reType) 
            .block();
    }

    protected <T> T doPut(String path, Object body, Map<String, Object> params, Class<T> reType, OAuth2AuthorizedClient authorizedClient) {
    	String query = buildQuery(params);
        return client
            .put()
            .uri(path + query, builder -> builder.build(params))
            .bodyValue(body)
            .attributes(oauth2AuthorizedClient(authorizedClient))
            .retrieve()
            .bodyToMono(reType) 
            .block();
    }
    
    protected <T> T doDelete(String path, Map<String, Object> params, Class<T> reType) {
        return client
            .delete()
            .uri(path, builder -> builder.build(params))
            .retrieve()
            .bodyToMono(reType) 
            .block();
    }

    protected <T> T doDelete(String path, Map<String, Object> params, Class<T> reType, OAuth2AuthorizedClient authorizedClient) {
        return client
            .delete()
            .uri(path, builder -> builder.build(params))
            .attributes(oauth2AuthorizedClient(authorizedClient))
            .retrieve()
            .bodyToMono(reType)
            .block();
    }
    
}
