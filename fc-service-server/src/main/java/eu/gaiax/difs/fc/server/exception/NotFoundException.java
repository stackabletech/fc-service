package eu.gaiax.difs.fc.server.exception;

import lombok.Getter;
import lombok.Setter;

/**
 * NotFoundException is an exception that can be thrown to customize the Not Found Error of the Federated
 * Catalogue server application.
 * Implementation of the {@link ServiceException} exception.
 */
@Getter
@Setter
public class NotFoundException extends ServiceException {
  /**
   * Constructs a new Not Found Exception with the specified detail message.
   *
   * @param message Detailed message about the thrown exception.
   */
  public NotFoundException(String message) {
    super(message);
  }
}