package eu.gaiax.difs.fc.server.handler;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import eu.gaiax.difs.fc.api.generated.model.Error;
import eu.gaiax.difs.fc.server.exception.ClientException;
import eu.gaiax.difs.fc.server.exception.NotFoundException;
import eu.gaiax.difs.fc.server.exception.ServerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({ClientException.class})
    protected ResponseEntity<Error> handleBadRequestException(ClientException ex) {
        log.debug("Bad request error: ", ex);
        return new ResponseEntity<>(new Error("client_error", ex.getMessage()), BAD_REQUEST);
    }

    @ExceptionHandler({ServerException.class})
    protected ResponseEntity<Error> handleServerException(ServerException ex) {
        log.debug("Server error: ", ex);
        return new ResponseEntity<>(new Error("not_found_error", ex.getMessage()), INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({NotFoundException.class})
    protected ResponseEntity<Error> handleNotFoundException(NotFoundException ex) {
        log.debug("Not found error: ", ex);
        return new ResponseEntity<>(new Error("server_error", ex.getMessage()), NOT_FOUND);
    }
}