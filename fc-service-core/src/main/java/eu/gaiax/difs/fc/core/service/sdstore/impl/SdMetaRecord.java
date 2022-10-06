package eu.gaiax.difs.fc.core.service.sdstore.impl;

import com.vladmihalcea.hibernate.type.array.StringArrayType;
import eu.gaiax.difs.fc.api.generated.model.SelfDescriptionStatus;
import eu.gaiax.difs.fc.core.pojo.ContentAccessorDirect;
import eu.gaiax.difs.fc.core.pojo.SelfDescriptionMetadata;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

/**
 * Database record for SdMetaData table.
 */
@Entity
@Access(AccessType.PROPERTY)
@Table(name = "sdfiles")
@NoArgsConstructor
@TypeDefs({
  @TypeDef(
      name = "string-array",
      typeClass = StringArrayType.class
  )
})
public class SdMetaRecord extends SelfDescriptionMetadata {

  private static final long serialVersionUID = -1010712678262829212L;

  @Id
  @Column(name = "sdhash", nullable = false, length = 32)
  @Override
  public String getSdHash() {
    return super.getSdHash();
  }

  /**
   * @return credentialSubject (subjectId) of this SD.
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
  @Override
  public SelfDescriptionStatus getStatus() {
    return super.getStatus();
  }

  /**
   * credentialSubject (subjectId) of the participant owning this
   * self-description.
   */
  @Column(name = "issuer", nullable = false)
  @Override
  public String getIssuer() {
    return super.getIssuer();
  }

  /**
   * The time stamp (ISO8601) when the self-description was uploaded.
   *
   * @return The upload time Instant
   */
  @Column(name = "uploadtime", nullable = false)
  public Instant getUploadTime() {
    return super.getUploadDatetime().toInstant();
  }

  /**
   * Set the uploadTime from an Instant.
   *
   * @param uploadTime the upload time as Instant.
   */
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

  /**
   * Set the statucTime from an Instant.
   *
   * @param statusTime the upload time as Instant.
   */
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
   * @return The validators' DIDs as an array for Hibernate.
   */
  @Type(type = "string-array")
  @Column(
      name = "validators",
      columnDefinition = "text[]"
  )
  public String[] getValidatorDidArray() {
    final List<String> validatorDids = getValidatorDids();
    if (validatorDids == null) {
      return null;
    }
    return validatorDids.toArray(String[]::new);
  }

  public void setValidatorDidArray(String[] validatorDids) {
    if (validatorDids == null) {
      super.setValidatorDids(null);
      return;
    }
    super.setValidatorDids(Arrays.asList(validatorDids));
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
    this.setValidatorDids(sdMeta.getValidatorDids());
  }
}
