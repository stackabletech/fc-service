package eu.gaiax.difs.fc.server.service;

import com.github.jsonldjava.utils.JsonUtils;
import eu.gaiax.difs.fc.api.generated.model.VerificationResult;
import eu.gaiax.difs.fc.core.exception.ServerException;
import eu.gaiax.difs.fc.core.pojo.ContentAccessorDirect;
import eu.gaiax.difs.fc.server.generated.controller.VerificationApiDelegate;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;
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
 * Implementation of the {@link eu.gaiax.difs.fc.server.generated.controller.VerificationApiDelegate} interface.
 */
@Slf4j
@Service
public class VerificationService implements VerificationApiDelegate {
  // TODO: 18.07.2022 Need to replace mocked Data with business logic

  @Autowired
  private eu.gaiax.difs.fc.core.service.verification.VerificationService verificationService;

  @Autowired
  private ResourceLoader resourceLoader;


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
  @Override
  public ResponseEntity<VerificationResult> verify(String jsonSelfDescription) {
    log.debug("verify.enter; got body: {}", jsonSelfDescription);
    VerificationResult verificationResult = verificationService.verifySelfDescription(new ContentAccessorDirect(jsonSelfDescription));
    log.debug("verify.exit; returning result {} ", verificationResult);
    return new ResponseEntity<>(verificationResult, HttpStatus.OK);

  }

  /**
   * Service method for GET /verifications/self-descriptions: Show HTML page to verify (portions of) a signed SD.
   *
   * @return HTML document that contains a query field to verify SD. (status code 200)
   *        or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *        or May contain hints how to solve the error or indicate what went wrong at the server.
   *        Must not outline any information about the internal structure of the server. (status code 500)
   */
  @Override
  public ResponseEntity<String> verifyPage() {
    log.debug("verifyPage.enter");

    final Resource resource = resourceLoader.getResource("classpath:static/verification.html");
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
    log.debug("verifyPage.exit; returning page");
    return ResponseEntity.ok()
        .headers(responseHeaders)
        .body(page);

  }

}
