package eu.gaiax.difs.fc.server.model;

/**
 * Validation result interface.
 */
public interface ValidationResult<R> {
  /**
   * Checks if the validation was successful or failed.
   *
   * @return a boolean if the validation succeeded.
   */
  boolean isValid();

  /**
   * Gives information about the validation result.
   *
   * @return the reason why the validation failed.
   *        If the validation succeeded, the method likely returns an empty string.
   */
  String getReason();

  /**
   * Gives the result of the validation. Usually, this is empty.
   *
   * @return the payload object.
   */
  R getPayload();
}
