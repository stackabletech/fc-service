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

@Slf4j
public class ProxyCall {
  public static <T, R> ResponseEntity<T> retrieve(WebClient webClient, HttpServletRequest request, R rqBody) {
    log.debug("Retrieve request method {} by URI {}", request.getMethod(), request.getRequestURI());
    HttpMethod method = HttpMethod.valueOf(request.getMethod());

    final WebClient.RequestBodySpec callBuilder = webClient
        .method(method)
        .uri(builder ->
            builder.path(request.getRequestURI())
                .queryParams(getAllQueryParams(request))
                .build());
    if (!method.equals(HttpMethod.GET) && !method.equals(HttpMethod.DELETE)) {
      callBuilder.bodyValue(rqBody);
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
