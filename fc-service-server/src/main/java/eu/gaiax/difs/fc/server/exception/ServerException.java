package eu.gaiax.difs.fc.server.exception;

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
}