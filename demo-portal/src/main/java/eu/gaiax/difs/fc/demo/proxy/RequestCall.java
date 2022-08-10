package eu.gaiax.difs.fc.demo.proxy;

import java.util.Enumeration;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Utils class for request proxying.
 */
@Slf4j
public class RequestCall {
  /**
   * Proxies GET outgoing requests for the required Server.
   *
   * @param webClient   Client-side component used to connect to the required Server.
   * @param request     Incoming request.
   * @param <T>         Type of the output parameter.
   * @return Returns the response entity received from the server.
   */
  public static <T> ResponseEntity<T> doGet(WebClient webClient, HttpServletRequest request) {
    log.debug("doGet.enter; GET request method by URI {}", request.getRequestURI());

    final WebClient.RequestHeadersSpec<?> callBuilder = webClient
        .get()
        .uri(builder ->
            builder.path(request.getRequestURI())
                .queryParams(getAllQueryParams(request))
                .build());
    addHeaderToClientCall(request, callBuilder);
    return callBuilder
        .retrieve()
        .toEntity(new ParameterizedTypeReference<T>() {
        }).block();
  }

  /**
   * Proxies POST outgoing requests for the required Server.
   *
   * @param webClient   Client-side component used to connect to the required Server.
   * @param request     Incoming request.
   * @param requestBody Incoming request body (optional).
   * @param <T>         Type of the output parameter.
   * @param <R>         Request body type.
   * @return Returns the response entity received from the server.
   */
  public static <T, R> ResponseEntity<T> doPost(WebClient webClient, HttpServletRequest request, R requestBody) {
    log.debug("POST request method by URI {}",  request.getRequestURI());

    final WebClient.RequestHeadersSpec<?> callBuilder = webClient
        .post()
        .uri(builder ->
            builder.path(request.getRequestURI())
                .queryParams(getAllQueryParams(request))
                .build())
        .bodyValue(requestBody);
    addHeaderToClientCall(request, callBuilder);
    return callBuilder
        .retrieve()
        .toEntity(new ParameterizedTypeReference<T>() {
        }).block();
  }

  /**
   * Proxies PUT outgoing requests for the required Server.
   *
   * @param webClient   Client-side component used to connect to the required Server.
   * @param request     Incoming request.
   * @param requestBody Incoming request body (optional).
   * @param <T>         Type of the output parameter.
   * @param <R>         Request body type.
   * @return Returns the response entity received from the server.
   */
  public static <T, R> ResponseEntity<T> doPut(WebClient webClient, HttpServletRequest request, R requestBody) {
    log.debug("PUT request method by URI {}",  request.getRequestURI());

    final WebClient.RequestHeadersSpec<?> callBuilder = webClient
        .put()
        .uri(builder ->
            builder.path(request.getRequestURI())
                .queryParams(getAllQueryParams(request))
                .build())
        .bodyValue(requestBody);
    addHeaderToClientCall(request, callBuilder);
    return callBuilder
        .retrieve()
        .toEntity(new ParameterizedTypeReference<T>() {
        }).block();
  }

  /**
   * Proxies DELETE outgoing requests for the required Server.
   *
   * @param webClient   Client-side component used to connect to the required Server.
   * @param request     Incoming request.
   * @param <T>         Type of the output parameter.
   * @return Returns the response entity received from the server.
   */
  public static <T> ResponseEntity<T> doDelete(WebClient webClient, HttpServletRequest request) {
    log.debug("DELETE request method by URI {}", request.getRequestURI());

    final WebClient.RequestHeadersSpec<?> callBuilder = webClient
        .delete()
        .uri(builder ->
            builder.path(request.getRequestURI())
                .queryParams(getAllQueryParams(request))
                .build());
    addHeaderToClientCall(request, callBuilder);
    return callBuilder
        .retrieve()
        .toEntity(new ParameterizedTypeReference<T>() {
        }).block();
  }

  private static void addHeaderToClientCall(HttpServletRequest request, WebClient.RequestHeadersSpec<?> callBuilder) {
    final Enumeration<String> headerNames = request.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      final String hn = headerNames.nextElement();
      callBuilder.header(hn, request.getHeader(hn));
    }
  }

  private static MultiValueMap<String, String> getAllQueryParams(HttpServletRequest request) {
    final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    request.getParameterMap().forEach((s, strings) -> queryParams.addAll(s, List.of(strings)));
    return queryParams;
  }
}
