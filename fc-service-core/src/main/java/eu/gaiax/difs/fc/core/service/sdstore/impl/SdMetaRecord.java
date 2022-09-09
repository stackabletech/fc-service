package eu.gaiax.difs.fc.core.service.sdstore.impl;

import eu.gaiax.difs.fc.api.generated.model.SelfDescriptionStatus;
import eu.gaiax.difs.fc.core.pojo.ContentAccessorDirect;
import eu.gaiax.difs.fc.core.pojo.SelfDescriptionMetadata;
import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.NoArgsConstructor;

/**
 * Database record for SdMetaData table.
 */
@Entity
@Access(AccessType.PROPERTY)
@Table(name = "sdfiles")
@NoArgsConstructor
public class SdMetaRecord extends SelfDescriptionMetadata {
  private static final long serialVersionUID = -1010712678262829212L;

  @Id
  @Column(name = "sdhash", nullable = false, length = 32)
  public String getSdHash() {
    return super.getSdHash();
  }

  /**
   * credentialSubject (subjectId) of this SD.
   */
  @Column(name = "subjectid", nullable = false)
  public String getSubjectId() {
    return super.getId();
  }

  public void setSubjectId(String subjectId) {
    super.setId(subjectId);
  }
    /**
     * Status of the self-description in the catalogue.
     */
  @Column(name = "status", nullable = false)
  public SelfDescriptionStatus getStatus() {
    return super.getStatus();
  }

  /**
   * credentialSubject (subjectId) of the participant owning this self-description.
   */
  @Column(name = "issuer", nullable = false)
  public String getIssuer() {
    return super.getIssuer();
  }

  /**
   * The time stamp (ISO8601) when the self-description was uploaded.
   */
  @Column(name = "uploadtime", nullable = false)
  public Instant getUploadTime() {
    return super.getUploadDatetime().toInstant();
  }

  public void setUploadTime(Instant uploadTime) {
    super.setUploadDatetime(uploadTime.atOffset(ZoneOffset.UTC));
  }

  /**
   * The last time stamp (ISO8601) the status changed (for this catalogue).
   */
  @Column(name = "statustime", nullable = false)
  public Instant getStatusTime() {
    return super.getStatusDatetime().toInstant();
  }

  public void setStatusTime(Instant statusTime) {
    super.setStatusDatetime(statusTime.atOffset(ZoneOffset.UTC));
  }

  @Column(columnDefinition = "TEXT", name = "content", nullable = false)
  public String getContent() {
    return super.getSelfDescription().getContentAsString();
  }

  public void setContent(String content) {
    super.setSelfDescription(new ContentAccessorDirect(content));
  }

  /**
   * The credentialSubjects (subjectIds) of the validators.
   */
  @OneToMany(mappedBy = "sdHash", fetch = FetchType.LAZY)
  public List<ValidatorRecord> getValidatorRecords() {
    final List<String> validatorsList = super.getValidators();
    if (validatorsList != null) {
      return validatorsList
          .stream()
          .map(t -> new ValidatorRecord(getSdHash(), t))
          .collect(Collectors.toList());
    }
    return null;
  }

  public void setValidatorRecords(List<ValidatorRecord> validators) {
    if (validators != null) {
      super.setValidators(validators.stream().map(ValidatorRecord::getValidator).collect(Collectors.toList()));
    }
  }

  /**
   * Create a new SdMetaRecord from the specified self-description meta data.
   *
   * @param sdMeta The self-description meta data.
   */
  public SdMetaRecord(final SelfDescriptionMetadata sdMeta) {
    this.setSdHash(sdMeta.getSdHash());
    this.setSubjectId(sdMeta.getId());
    this.setStatus(sdMeta.getStatus());
    this.setIssuer(sdMeta.getIssuer());
    this.setUploadTime(sdMeta.getUploadDatetime().toInstant());
    this.setStatusTime(sdMeta.getStatusDatetime().toInstant());
    this.setContent(sdMeta.getSelfDescription().getContentAsString());
    final List<String> validatorsList = sdMeta.getValidators();
    if (sdMeta.getValidators() != null) {
      this.setValidatorRecords(validatorsList.stream()
          .map(t -> new ValidatorRecord(getSdHash(), t))
          .collect(Collectors.toList()));
    }
  }
}
