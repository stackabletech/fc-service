package eu.gaiax.difs.fc.demo;

import eu.gaiax.difs.fc.api.generated.model.Error;
import java.net.ConnectException;
import javax.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class ErrorHandler extends ResponseEntityExceptionHandler {
  @ExceptionHandler({ConnectException.class})
  public ResponseEntity<Error> connectionRefused(HttpServletRequest httpServletRequest) {
    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new Error(httpServletRequest.getRequestURI(), "Connection error. Please, try later"));
  }

  @ExceptionHandler({WebClientResponseException.class})
  public ResponseEntity<Object> handleWebClientResponseException(WebClientResponseException ex, WebRequest request) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return handleExceptionInternal(ex, ex.getResponseBodyAsString(), headers, ex.getStatusCode(), request);
  }
}
