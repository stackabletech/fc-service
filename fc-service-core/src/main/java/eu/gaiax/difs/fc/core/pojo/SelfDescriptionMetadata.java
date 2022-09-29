package eu.gaiax.difs.fc.core.pojo;

import static eu.gaiax.difs.fc.core.util.HashUtils.calculateSha256AsHex;
import static java.time.OffsetDateTime.now;

import com.fasterxml.jackson.annotation.JsonIgnore;

import eu.gaiax.difs.fc.api.generated.model.SelfDescription;
import eu.gaiax.difs.fc.api.generated.model.SelfDescriptionStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Class for handling the metadata of a Self-Description, and optionally a
 * reference to a content accessor.
 */
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
@lombok.EqualsAndHashCode(callSuper = true)
public class SelfDescriptionMetadata extends SelfDescription {

  /**
   * A reference to the self description content.
   */
  @lombok.Getter
  @lombok.Setter
  @JsonIgnore
  private ContentAccessor selfDescription;

  public SelfDescriptionMetadata(ContentAccessorDirect contentAccessor, String id, String issuer, List<Validator> validators) {
    super(calculateSha256AsHex(contentAccessor.getContentAsString()), id, SelfDescriptionStatus.ACTIVE, issuer, validators.stream().map(Validator::getDidURI).collect(Collectors.toList()), now(), now());
    this.selfDescription = contentAccessor;
  }

  public SelfDescriptionMetadata(ContentAccessorDirect contentAccessorDirect, VerificationResultParticipant verificationResult) {
    super(calculateSha256AsHex(contentAccessorDirect.getContentAsString()), verificationResult.getId(), SelfDescriptionStatus.ACTIVE,
         verificationResult.getIssuer(),Collections.<String>emptyList(),verificationResult.getVerificationTimestamp(),
        verificationResult.getVerificationTimestamp());

    this.selfDescription = contentAccessorDirect;
  }
}
