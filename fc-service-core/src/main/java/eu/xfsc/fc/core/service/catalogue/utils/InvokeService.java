package eu.xfsc.fc.core.service.catalogue.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Objects;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class InvokeService {

    public static String executeRequest(String url, HttpMethod method) {
        return executeRequest(url, method, null, null);
    }

    public static String executeRequest(String url, HttpMethod method, Object body, MultiValueMap<String, String> queryParams) {
        WebClient.RequestBodyUriSpec webClient = WebClient.create().method(method);
        webClient.uri(url);
        if (!CollectionUtils.isEmpty(queryParams)) {
            webClient.uri(url, u -> u.queryParams(queryParams).build());
        }
        if (Objects.nonNull(body)) {
            webClient.bodyValue(body);
        }
        return webClient.retrieve()
                .onStatus(HttpStatus.INTERNAL_SERVER_ERROR::equals, error -> {
                    log.error("Error occur while fetch retrieve the credential with url {} and statusCode {}", url, HttpStatus.INTERNAL_SERVER_ERROR);
                    return error.bodyToMono(String.class).map(Exception::new);
                }).bodyToMono(String.class)
                .block();
    }
}
