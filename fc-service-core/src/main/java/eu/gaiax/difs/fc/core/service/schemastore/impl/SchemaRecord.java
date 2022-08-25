package eu.gaiax.difs.fc.core.service.schemastore.impl;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import eu.gaiax.difs.fc.core.service.schemastore.SchemaStore.SchemaType;

/**
 *
 * @author hylke
 */
@Entity
@Table(name = "schemafiles")
@lombok.EqualsAndHashCode
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
@lombok.Getter
@lombok.Setter
public class SchemaRecord implements Serializable {

  /**
   * The identifier of the schema. For schemas without an internal identifier
   * this is the same as the nameHash.
   */
  @Id
  @Column(name = "schemaid", nullable = false, length = 200)
  private String schemaId;

  /**
   * The name of the file that stores the schema.
   */
  @Column(name = "namehash", nullable = false, length = 64)
  private String nameHash;

  /**
   * Type of the schema.
   */
  @Column(name = "type", nullable = false)
  private SchemaType type;

  /**
   * The time stamp when the Schema was uploaded.
   */
  @Column(name = "uploadtime", nullable = false)
  private Instant uploadTime;

  /**
   * The last time stamp the schema changed.
   */
  @Column(name = "updatetime", nullable = false)
  private Instant updateTime;

  /**
   * The terms defined in this schema.
   */
  @OneToMany(mappedBy = "schemaId", fetch = FetchType.LAZY)
  private List<SchemaTerm> terms;

  public SchemaRecord(String schemaId, String nameHash, SchemaType type, List<String> terms) {
    this.schemaId = schemaId;
    this.nameHash = nameHash;
    this.type = type;
    this.terms = terms.stream().map(t -> new SchemaTerm(t, schemaId)).collect(Collectors.toList());
    uploadTime = Instant.now();
    updateTime = Instant.now();
  }

  public void replaceTerms(List<String> terms) {
    this.terms = terms.stream().map(t -> new SchemaTerm(t, schemaId)).collect(Collectors.toList());
  }
}
