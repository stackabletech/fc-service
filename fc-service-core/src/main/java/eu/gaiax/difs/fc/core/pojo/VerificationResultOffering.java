package eu.gaiax.difs.fc.core.pojo;

import java.time.Instant;
import java.util.List;

/**
 * POJO Class for holding validation results.
 */
public class VerificationResultOffering extends VerificationResult {

  /**
   * Constructor for the VerificationResultParticipant
   *
   * @param id id of SD
   * @param claims List of claims in the SD
   * @param validators Validators, signing parts of the SD
   * @param verificationTimestamp time stamp of verification
   * @param lifecycleStatus status according to GAIA-X lifecycle
   * @param issuer Issuer of the offering
   * @param issuedDate issuing date of the SD
   */
  public VerificationResultOffering(Instant verificationTimestamp, String lifecycleStatus, String issuer, Instant issuedDateTime,
          String id, List<SdClaim> claims, List<Validator> validators) {
    super(verificationTimestamp, lifecycleStatus, issuer, issuedDateTime, id, claims, validators);
  }

}
