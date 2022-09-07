package eu.gaiax.difs.fc.core.exception;

import eu.gaiax.difs.fc.core.exception.ServiceException;
import lombok.Getter;
import lombok.Setter;

/**
 * ServerException is an exception that can be thrown to customize the Internal server error of the Federated
 * Catalogue server application.
 * Implementation of the {@link ServiceException} exception.
 */
@Getter
@Setter
public class ServerException extends ServiceException {
  /**
   * Constructs a new Server Exception with the specified detail message.
   *
   * @param message Detailed message about the thrown exception.
   */
  public ServerException(String message) {
    super(message);
  }

  /**
   * Constructs a new Server exception with the specified detail message and cause.
   *
   * @param message Detailed message about the thrown exception.
   * @param cause Case of the thrown exception. (A null value is permitted, and indicates that the cause is unknown.)
   */
  public ServerException(String message, Throwable cause) {
    super(message, cause);
  }
}