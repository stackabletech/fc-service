package eu.gaiax.difs.fc.server.handler;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NOT_IMPLEMENTED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;


import eu.gaiax.difs.fc.api.generated.model.Error;
import eu.gaiax.difs.fc.core.exception.ClientException;
import eu.gaiax.difs.fc.core.exception.ConflictException;
import eu.gaiax.difs.fc.core.exception.NotFoundException;
import eu.gaiax.difs.fc.core.exception.ServerException;
import eu.gaiax.difs.fc.core.exception.TimeoutException;
import eu.gaiax.difs.fc.core.exception.VerificationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * RestExceptionHandler translates RestExceptions to error responses according to the status that is set in
 * the application exception. Response content format: {"code" : "ExceptionType", "message" : "some exception message"}
 * Implementation of the {@link ResponseEntityExceptionHandler} exception.
 */
@Slf4j
@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {
  /**
   * Method handles the Client Exception.
   *
   * @param exception Thrown Client Exception.
   * @return The custom Federated Catalogue application error with status code 400.
   */
  @ExceptionHandler({ClientException.class})
  protected ResponseEntity<Error> handleBadRequestException(ClientException exception) {
    log.info("handleBadRequestException; Bad request error: {}", exception.getMessage());
    return new ResponseEntity<>(new Error("client_error", exception.getMessage()), BAD_REQUEST);
  }

  /**
   * Method handles the Conflict Exception.
   *
   * @param exception Thrown Conflict Exception.
   * @return The custom Federated Catalogue application error with status code 409.
   */
  @ExceptionHandler({ConflictException.class})
  protected ResponseEntity<Error> handleConflictException(ConflictException exception) {
    log.info("handleConflictException; Conflict error: {}", exception.getMessage());
    return new ResponseEntity<>(new Error("conflict_error", exception.getMessage()), CONFLICT);
  }

  /**
   * Method handles the Server Exception.
   *
   * @param exception Thrown Server Exception.
   * @return The custom Federated Catalogue application error with status code 500.
   */
  @ExceptionHandler({ServerException.class})
  protected ResponseEntity<Error> handleServerException(ServerException exception) {
    log.info("handleServerException; Server error: {}", exception.getMessage());
    return new ResponseEntity<>(new Error("server_error", exception.getMessage()), INTERNAL_SERVER_ERROR);
  }

  /**
   * Method handles the Not Found Exception.
   *
   * @param exception Thrown Server Exception.
   * @return The custom Federated Catalogue application error with status code 404.
   */
  @ExceptionHandler({NotFoundException.class})
  protected ResponseEntity<Error> handleNotFoundException(NotFoundException exception) {
    log.info("handleNotFoundException; Not found error: {}", exception.getMessage());
    return new ResponseEntity<>(new Error("not_found_error", exception.getMessage()), NOT_FOUND);
  }
  
  /**
   * Method handles the WS RS Not Found Exception.
   *
   * @param exception Thrown Server Exception.
   * @return The custom Federated Catalogue application error with status code 404.
   */
  @ExceptionHandler({javax.ws.rs.NotFoundException.class})
  protected ResponseEntity<Error> handleRsNotFoundException(javax.ws.rs.NotFoundException exception) {
    log.info("handleRsNotFoundException; Not found error: {}", exception.getMessage()); 
    return new ResponseEntity<>(new Error("not_found_error", exception.getMessage()), NOT_FOUND);
  }

  /**
   * Method handles the Verification Exception.
   *
   * @param exception Thrown Server Exception.
   * @return The custom Federated Catalogue application error with status code 422.
   */
  @ExceptionHandler({VerificationException.class})
  protected ResponseEntity<Error> handleVerificationException(VerificationException exception) {
    log.info("handleVerificationException; Verification error: {}", exception.getMessage());
    return new ResponseEntity<>(new Error("verification_error", exception.getMessage()), UNPROCESSABLE_ENTITY);
  }

  /**
   * Method handles the UnsupportedOperation Exception.
   *
   * @param exception Thrown Server Exception.
   * @return The custom Federated Catalogue application error with status code 501.
   */
  @ExceptionHandler({UnsupportedOperationException.class})
  protected ResponseEntity<Error> handleUnsupportedOperationException(UnsupportedOperationException exception) {
    log.info("handleUnsupportedOperationException; handleUnsupportedOperation error: {}", exception.getMessage());
    return new ResponseEntity<>(new Error("processing_error", exception.getMessage()), NOT_IMPLEMENTED);
  }
  
  /**
   * Method handles the Timeout Exception.
   *
   * @param exception Thrown Server Exception.
   * @return The custom Federated Catalogue application error with status code 504.
   */
  @ExceptionHandler({TimeoutException.class})
  protected ResponseEntity<Error> handleTimeoutException(TimeoutException exception) {
    log.info("handleVerificationException; Verification error: {}", exception.getMessage());
    return new ResponseEntity<>(new Error("timeout_error", exception.getMessage()), GATEWAY_TIMEOUT);
  }
  
}