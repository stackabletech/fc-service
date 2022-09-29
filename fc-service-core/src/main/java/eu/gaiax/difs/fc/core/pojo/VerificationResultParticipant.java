package eu.gaiax.difs.fc.core.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * POJO Class for holding validation results.
 */
@lombok.Getter
@lombok.Setter
public class VerificationResultParticipant extends VerificationResult {

  /**
   * The Name of the Participant.
   */
  @JsonIgnore
  private String participantName;
  /**
   * The public key of the participant.
   */
  @JsonIgnore
  private String participantPublicKey;

  /**
   * Constructor for the VerificationResultParticipant
   *
   * @param participantName Name of participant
   * @param id id of SD
   * @param participantPublicKey public key of participant
   * @param verificationTimestamp time stamp of verification
   * @param lifecycleStatus status according to GAIA-X lifecycle
   * @param issuedDate issuing date of the SD
   * @param validators Validators, signing parts of the SD
   * @param claims List of claims in the SD
   */
  public VerificationResultParticipant(
          String participantName,
          String id,
          String participantPublicKey,
          OffsetDateTime verificationTimestamp,
          String lifecycleStatus,
          LocalDate issuedDate,
          List<Validator> validators,
          List<SdClaim> claims) {
    super(id, claims, validators, verificationTimestamp, lifecycleStatus, id, issuedDate);
    this.participantName = participantName;
    this.participantPublicKey = participantPublicKey;
  }

}
