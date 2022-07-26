package eu.gaiax.difs.fc.server.exception;

import lombok.Getter;
import lombok.Setter;

/**
 * ParserException is an exception that can be thrown to customize the Self-Description parse error of the Federated
 * Catalogue server application.
 * Implementation of the {@link ServiceException} exception.
 */
@Getter
@Setter
public class ParserException extends ServiceException {
  /**
   * Constructs a new Parser exception with the specified detail message and cause.
   *
   * @param message Detailed message about the thrown exception.
   * @param cause Case of the thrown exception. (A null value is permitted, and indicates that the cause is unknown.)
   */
  public ParserException(String message, Throwable cause) {
    super(message, cause);
  }
}