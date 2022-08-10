package eu.gaiax.difs.fc.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.gaiax.difs.fc.api.generated.model.Result;
import eu.gaiax.difs.fc.api.generated.model.Statement;
import eu.gaiax.difs.fc.core.exception.ServerException;
import eu.gaiax.difs.fc.server.generated.controller.QueryApiDelegate;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

/**
 * Service for query the catalogue. Implementation of the {@link QueryApiDelegate} .
 */
@Slf4j
@Service
public class QueryService implements QueryApiDelegate {
  // TODO : We will add actual service implemented by Fraunhofer for getting result back.

  @Autowired
  private ResourceLoader resourceLoader;

  @Autowired
  private ObjectMapper jsonMapper;

  /**
   * Get List of results from catalogue for provided {@link Statement}.
   *
   * @param queryLanguage  (required) Language for query the results like openCypher etc.
   * @param statement JSON object to send queries. Use \&quot;application/json\&quot; for openCypher queries.
   *                   A Catalogue may also support the other content types depending on its supported query languages
   *                   but only \&quot;application/json\&quot; is mandatory. (optional)
   * @return List of {@link Result}
   */
  @Override
  public ResponseEntity<Result> query(String queryLanguage, Statement statement) {
    log.debug("query.enter; got queryLanguage:{}, got statement:{}", queryLanguage, statement);
    Optional<Result> result = Optional.of(new Result());
    log.debug("query.exit; got results:{}", result);
    return new ResponseEntity<>(result.get(), HttpStatus.OK);
  }

  @Override
  public ResponseEntity<String> querywebsite() {
    log.debug("queryPage.enter");

    final Resource resource = resourceLoader.getResource("classpath:static/query.html");
    String page;
    try {
      Reader reader = new InputStreamReader(resource.getInputStream());
      page = FileCopyUtils.copyToString(reader);
    } catch (IOException e) {
      log.error("error in getting file: {}", e);
      throw new ServerException(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
    }
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.set("Content-Type", "text/html");
    log.debug("queryPage.exit; returning page");
    return ResponseEntity.ok()
        .headers(responseHeaders)
        .body(page);
  }
}