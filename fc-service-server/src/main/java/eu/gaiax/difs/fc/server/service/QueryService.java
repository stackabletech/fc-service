package eu.gaiax.difs.fc.server.service;

import eu.gaiax.difs.fc.api.generated.model.Results;
import eu.gaiax.difs.fc.api.generated.model.Statements;
import eu.gaiax.difs.fc.server.generated.controller.QueryApiDelegate;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Service for query the catalogue. Implementation of the {@link QueryApiDelegate} .
 */
@Slf4j
@Service
public class QueryService implements QueryApiDelegate {
  // TODO : We will add actual service implemented by Fraunhofer for getting result back.

  /**
   * Get List of results from catalogue for provided {@link Statements}.
   *
   * @param queryLanguage  (required) Language for query the results like openCypher etc.
   * @param statements JSON object to send queries. Use \&quot;application/json\&quot; for openCypher queries.
   *                   A Catalogue may also support the other content types depending on its supported query languages
   *                   but only \&quot;application/json\&quot; is mandatory. (optional)
   * @return List of {@link Results}
   */
  @Override
  public ResponseEntity<Results> query(String queryLanguage, Statements statements) {
    log.debug("query.enter; got queryLanguage:{},got statements:{}", queryLanguage, statements);
    Optional<Results> results = Optional.of(new Results());
    log.debug("query.exit; got results:{}", results);
    return new ResponseEntity<>(results.get(), HttpStatus.OK);
  }
}