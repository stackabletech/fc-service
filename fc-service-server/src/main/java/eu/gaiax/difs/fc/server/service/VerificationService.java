package eu.gaiax.difs.fc.server.service;

import eu.gaiax.difs.fc.api.generated.model.VerificationResult;
import eu.gaiax.difs.fc.server.generated.controller.VerificationApiDelegate;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Implementation of the {@link eu.gaiax.difs.fc.server.generated.controller.VerificationApiDelegate} interface.
 */
@Slf4j
@Service
public class VerificationService implements VerificationApiDelegate {
  // TODO: 18.07.2022 Need to replace mocked Data with business logic

  /**
    * Service method for POST /verifications/self-descriptions: Send a JSON-LD document to verify with the information
    * from the Catalogue.
    *
    * @param selfDescription JSON-LD document to be verified object to send queries.
    * @return Verification result (status code 200)
    *       or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
    *       or Query Timeout: the query took longer than the configured timeout interval.
    *       The client needs to rewrite the query so it can be processed faster. (status code 408)
    *       or May contain hints how to solve the error or indicate what went wrong at the server.
    *       Must not outline any information about the internal structure of the server. (status code 500)
    */

  public ResponseEntity<VerificationResult> verify(Object selfDescription) {
    log.debug("verify.enter; got self-description: {}", selfDescription);

    VerificationResult result = new VerificationResult();
    result.setVerificationTimestamp("string");
    result.setLifecycleStatus("string");
    result.setIssuedDate("string");
    result.setSignatures(new ArrayList<>());
    log.debug("verify.exit; returning result from time {} with status {} of SD {}",
        result.getVerificationTimestamp(), result.getLifecycleStatus(), selfDescription);
    return new ResponseEntity<>(result, HttpStatus.OK);
  }

  /**
   * Service method for GET /verifications/self-descriptions: Show HTML page to verify (portions of) a signed SD.
   *
   * @return HTML document that contains a query field to verify SD. (status code 200)
   *        or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *        or May contain hints how to solve the error or indicate what went wrong at the server.
   *        Must not outline any information about the internal structure of the server. (status code 500)
   */
  public ResponseEntity<String> verifyPage() {
    log.debug("verifyPage.enter");

    String page = "string";
    HttpHeaders responseHeaders = new HttpHeaders();
    responseHeaders.set("Content-Type", "text/html");

    log.debug("verifyPage.exit; returning page {}", page);
    return ResponseEntity.ok()
        .headers(responseHeaders)
        .body(page);
  }
}
