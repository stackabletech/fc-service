package eu.gaiax.difs.fc.core.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;


public class VerificationResult extends eu.gaiax.difs.fc.api.generated.model.VerificationResult {

  private static final long serialVersionUID = 1L;
  
  /**
   * credentialSubject (id) of this SD.
   */
  @JsonIgnore
  private String id;
  /**
   * claims of the SD, to be inserted into the Graph-DB.
   */
  @JsonIgnore
  private List<SdClaim> claims;
  /**
   * validators, that signed parts of the SD.
   */
  @JsonIgnore
  private List<Validator> validators;

  public VerificationResult(Instant verificationTimestamp, String lifecycleStatus, String issuer, Instant issuedDateTime,
          String id, List<SdClaim> claims, List<Validator> validators) {
    super(verificationTimestamp, lifecycleStatus, issuer, issuedDateTime, null);
    this.id = id;
    this.claims = claims;
    this.setValidators(validators);
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public List<SdClaim> getClaims() {
    return claims;
  }

  public void setClaims(List<SdClaim> claims) {
    this.claims = claims;
  }

  public List<Validator> getValidators() {
    return validators;
  }

  public void setValidators(List<Validator> validators) {
    //TODO: Check what parts of the validators should be added to the response
    if (validators == null) {
      super.setValidatorDids(null);  
    } else {
      super.setValidatorDids(validators.stream().map(Validator::getDidURI).collect(Collectors.toList()));
    }
    this.validators = validators;
  }
}
