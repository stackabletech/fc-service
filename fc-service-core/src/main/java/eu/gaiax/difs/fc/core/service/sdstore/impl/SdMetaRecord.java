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
 * Database record for SdMetaData table.
 */
@Entity
@Table(name = "sdfiles")
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Getter
@lombok.Setter
@lombok.EqualsAndHashCode
public class SdMetaRecord implements Serializable {
  private static final long serialVersionUID = -1010712678262829212L;

  @Id
  @Column(name = "sdhash", nullable = false, length = 32)
  private String sdHash;

  /**
   * credentialSubject (subjectId) of this SD.
   */
  @Column(name = "subjectid", nullable = false)
  private String subjectId;

  /**
   * Status of the self-description in the catalogue.
   */
  @Column(name = "status", nullable = false)
  private SelfDescriptionStatus status;

  /**
   * credentialSubject (subjectId) of the participant owning this self-description.
   */
  @Column(name = "issuer", nullable = false)
  private String issuer;

  /**
   * The time stamp (ISO8601) when the self-description was uploaded.
   */
  @Column(name = "uploadtime", nullable = false)
  private Instant uploadTime;

  /**
   * The last time stamp (ISO8601) the status changed (for this catalogue).
   */
  @Column(name = "statustime", nullable = false)
  private Instant statusTime;

  /**
   * The credentialSubjects (subjectIds) of the validators.
   */
  @OneToMany(mappedBy = "sdHash", fetch = FetchType.LAZY)
  private List<ValidatorRecord> validators;

  /**
   * Create a new SdMetaRecord from the specified self-description meta data.
   *
   * @param sdMeta The self-description meta data.
   */
  public SdMetaRecord(final SelfDescriptionMetadata sdMeta) {
    sdHash = sdMeta.getSdHash();
    subjectId = sdMeta.getId();
    status = sdMeta.getStatus();
    issuer = sdMeta.getIssuer();
    uploadTime = sdMeta.getUploadDatetime().toInstant();
    statusTime = sdMeta.getStatusDatetime().toInstant();
    final List<String> validatorsList = sdMeta.getValidators();
    if (validatorsList != null) {
      validators = validatorsList.stream().map(t -> new ValidatorRecord(sdHash, t)).collect(Collectors.toList());
    }
  }

  /**
   * Convert this SdMetaRecord back into a self-description meta data instance
   * that equals the self-description meta data that was used to create this
   * SdMetaRecord instance.
   *
   * @return A self-description meta data instance that equals the
   *         self-description meta data that was used to create this SdMetaRecord
   *         instance.
   */
  public SelfDescriptionMetadata asSelfDescriptionMetadata() {
    return applyTo(new SelfDescriptionMetadata());
  }

  /**
   * Copy underlying meta data from this meta data record onto the specified
   * self-description meta data object.
   *
   * @param sdMeta The self-description meta data object to apply this object's
   *               underlying meta data onto.
   * @return The self-description meta data object that is passed into this method
   *         for use in chained function calls.
   */
  public SelfDescriptionMetadata applyTo(final SelfDescriptionMetadata sdMeta) {
    sdMeta.setSdHash(sdHash);
    sdMeta.setId(subjectId);
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
