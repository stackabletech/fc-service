package eu.gaiax.difs.fc.core.exception;

import lombok.Getter;
import lombok.Setter;

/**
 * VerificationException is an exception that is thrown when the validation of a SD fails
 * Implementation of the {@link ServiceException} exception.
 */

public class VerificationException extends ServiceException {
  /**
   * Constructs a new Client Exception with the specified detail message.
   *
   * @param message Detailed message about the thrown exception.
   */
  public VerificationException(String message) {
    super(message);
  }
}