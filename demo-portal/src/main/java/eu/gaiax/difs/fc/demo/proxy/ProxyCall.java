package eu.gaiax.difs.fc.demo.proxy;

import java.util.Enumeration;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Utils class for request proxying.
 */
@Slf4j
public class ProxyCall {
  /**
   * Proxies outgoing requests for the required Server.
   *
   * @param webClient Client-side component used to connect to the required Server.
   * @param request Incoming request.
   * @param requestBody Incoming request body (optional).
   * @param <T> Type of the output parameter.
   * @param <R> Request body type.
   *
   * @return Returns the response entity received from the server.
   */
  public static <T, R> ResponseEntity<T> retrieve(WebClient webClient, HttpServletRequest request, R requestBody) {
    log.debug("Retrieve request method {} by URI {}", request.getMethod(), request.getRequestURI());
    HttpMethod method = HttpMethod.valueOf(request.getMethod());

    final WebClient.RequestBodySpec callBuilder = webClient
        .method(method)
        .uri(builder ->
            builder.path(request.getRequestURI())
                .queryParams(getAllQueryParams(request))
                .build());
    if (!method.equals(HttpMethod.GET) && !method.equals(HttpMethod.DELETE)) {
      callBuilder.bodyValue(requestBody);
    }
    final Enumeration<String> headerNames = request.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      final String hn = headerNames.nextElement();
      callBuilder.header(hn, request.getHeader(hn));
    }
    return callBuilder
        .retrieve()
        .toEntity(new ParameterizedTypeReference<T>() {
        }).block();
  }

  private static MultiValueMap<String, String> getAllQueryParams(HttpServletRequest request) {
    final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    request.getParameterMap().forEach((s, strings) -> queryParams.addAll(s, List.of(strings)));
    return queryParams;
  }
}
