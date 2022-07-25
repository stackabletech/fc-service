package eu.gaiax.difs.fc.server.service;

import eu.gaiax.difs.fc.api.generated.model.Results;
import eu.gaiax.difs.fc.api.generated.model.Statements;
import eu.gaiax.difs.fc.server.generated.controller.QueryApiDelegate;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class QueryService implements QueryApiDelegate {
  // TODO : We will add actual service implemented by Fraunhofer for getting result back.

  @Override
  public ResponseEntity<Results> query(String queryLanguage, Statements statements) {
    log.debug("query.enter; got queryLanguage:{},got statements:{}", queryLanguage, statements);
    Optional<Results> results = Optional.of(new Results());
    log.debug("query.exit; got results:{}", results);
    return new ResponseEntity<>(results.get(), HttpStatus.OK);
  }
}