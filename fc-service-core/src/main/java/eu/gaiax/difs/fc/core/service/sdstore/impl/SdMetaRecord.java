package eu.gaiax.difs.fc.core.service.sdstore.impl;

import eu.gaiax.difs.fc.api.generated.model.SelfDescriptionStatus;
import eu.gaiax.difs.fc.core.pojo.SelfDescriptionMetadata;
import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * Database record for SdMetaData table
 */
@Entity
@Table(name = "sdfiles")
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Getter
@lombok.Setter
@lombok.EqualsAndHashCode
public class SdMetaRecord implements Serializable {

  @Id
  @Column(name = "sdhash", nullable = false, length = 32)
  private String sdHash;

  /**
   * credentialSubject (subject) of this SD.
   */
  @Column(name = "subject", nullable = false)
  private String subject;
  /**
   * Status of the SelfDescription in the catalogue.
   */
  @Column(name = "status", nullable = false)
  private SelfDescriptionStatus status;

  /**
   * credentialSubject (subject) of the participant owning this SD.
   */
  @Column(name = "issuer", nullable = false)
  private String issuer;

  /**
   * The time stamp (ISO8601) when the SD was uploaded.
   */
  @Column(name = "uploadtime", nullable = false)
  private Instant uploadTime;

  /**
   * The last time stamp (ISO8601) the status changed (for this Catalogue).
   */
  @Column(name = "statustime", nullable = false)
  private Instant statusTime;

  /**
   * The credentialSubjects (ids) of the validators.
   */
  @OneToMany(mappedBy = "sdHash", fetch = FetchType.LAZY)
  private List<ValidatorRecord> validators;

  public SdMetaRecord(SelfDescriptionMetadata sdMeta) {
    sdHash = sdMeta.getSdHash();
    subject = sdMeta.getId();
    status = sdMeta.getStatus();
    issuer = sdMeta.getIssuer();
    uploadTime = sdMeta.getUploadDatetime().toInstant();
    statusTime = sdMeta.getStatusDatetime().toInstant();
    List<String> validatorsList = sdMeta.getValidators();
    if (validatorsList != null) {
      validators = validatorsList.stream().map(t -> new ValidatorRecord(sdHash, t)).collect(Collectors.toList());
    }
  }

  public SelfDescriptionMetadata asSelfDescriptionMetadata() {
    return applyTo(new SelfDescriptionMetadata());
  }

  public SelfDescriptionMetadata applyTo(SelfDescriptionMetadata sdMeta) {
    sdMeta.setSdHash(sdHash);
    sdMeta.setId(subject);
    sdMeta.setStatus(status);
    sdMeta.setIssuer(issuer);
    sdMeta.setUploadDatetime(uploadTime.atOffset(ZoneOffset.UTC));
    sdMeta.setStatusDatetime(statusTime.atOffset(ZoneOffset.UTC));
    if (validators != null) {
      sdMeta.setValidators(validators.stream().map(ValidatorRecord::getValidator).collect(Collectors.toList()));
    }
    return sdMeta;
  }
}
