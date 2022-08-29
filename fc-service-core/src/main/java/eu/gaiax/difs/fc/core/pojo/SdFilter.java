package eu.gaiax.difs.fc.core.pojo;

import eu.gaiax.difs.fc.api.generated.model.SelfDescriptionStatus;
import java.time.Instant;

/**
 * Filter parameters for searching Self-Description meta data. If multiple items
 * are present, they are combined with an 'AND' semantic.
 */
@lombok.Getter
@lombok.Setter
public class SdFilter {

  /**
   * Start time for the time range filter when the Self-Description was uploaded
   * to the catalogue.
   */
  private Instant uploadTimeStart;

  /**
   * End time for the time range filter when the Self-Description was uploaded
   * to the catalogue.
   */
  private Instant uploadTimeEnd;

  /**
   * Start time for the time range filter when the status of the
   * Self-Description was last changed in the catalogue.
   */
  private Instant statusTimeStart;

  /**
   * End time for the time range filter when the status of the Self-Description
   * was last changed in the catalogue.
   */
  private Instant statusTimeEnd;

  /**
   * Filter for the issuer of the Self-Description. This is the unique ID
   * (credentialSubject) of the Participant that has prepared the
   * Self-Description.
   */
  private String issuer;

  /**
   * Filter for a validator of the Self-Description. This is the unique ID
   * (credentialSubject) of the Participant that validated (part of) the
   * Self-Description.
   */
  private String validator;

  /**
   * Filter for the status of the Self-Description.
   */
  private SelfDescriptionStatus status;

  /**
   * Filter for a id/credentialSubject of the Self-Description.
   */
  private String id;

  /**
   * Filter for a hash of the Self-Description.
   */
  private String hash;

  /**
   * Filter for the issuer of the Self-Description. This is the unique ID
   * (credentialSubject) of the Participant that has prepared the
   * Self-Description.
   */
  private Integer offset;

  /**
   * Filter for the issuer of the Self-Description. This is the unique ID
   * (credentialSubject) of the Participant that has prepared the
   * Self-Description.
   */
  private Integer limit;

}
