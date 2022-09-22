package eu.gaiax.difs.fc.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.gaiax.difs.fc.api.generated.model.Results;
import eu.gaiax.difs.fc.api.generated.model.Statement;
import eu.gaiax.difs.fc.core.exception.ServerException;
import eu.gaiax.difs.fc.core.pojo.OpenCypherQuery;
import eu.gaiax.difs.fc.core.service.graphdb.GraphStore;
import eu.gaiax.difs.fc.server.generated.controller.QueryApiDelegate;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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
  @Autowired
  private ResourceLoader resourceLoader;

  @Autowired
  private ObjectMapper jsonMapper;

  @Autowired
  private GraphStore graphStore;



  /**
   * Get List of results from catalogue for provided {@link Statement}.
   *
   * @param queryLanguage  (required) Language for query the results like openCypher etc.
   * @param statement JSON object to send queries. Use \&quot;application/json\&quot; for openCypher queries.
   *                   A Catalogue may also support the other content types depending on its supported query languages
   *                   but only \&quot;application/json\&quot; is mandatory. (optional)
   * @return List of {@link Results}
   */
  @Override
  public ResponseEntity<Results> query(String queryLanguage, Statement statement) {
    log.debug("query.enter; got queryLanguage: {}, statement: {}", queryLanguage, statement);
    List<Map<String, Object>> queryResultList = graphStore.queryData(new OpenCypherQuery(statement.getStatement(), statement.getParameters()));
    // TODO: fix totalCount!
    Results result = new Results(0, queryResultList);
    log.debug("query.exit; returning results: {}", result);
    return ResponseEntity.ok(result);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ResponseEntity<String> querywebsite() {
    log.debug("queryPage.enter");

    final Resource resource = resourceLoader.getResource("classpath:static/query.html");
    String page;
    try {
      Reader reader = new InputStreamReader(resource.getInputStream());
      page = FileCopyUtils.copyToString(reader);
    } catch (IOException e) {
      log.error("queryPage; error in getting file: {}", e);
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