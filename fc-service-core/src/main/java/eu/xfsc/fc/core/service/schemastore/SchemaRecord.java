package eu.xfsc.fc.core.service.schemastore;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import eu.xfsc.fc.core.service.schemastore.SchemaStore.SchemaType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

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
   * The content of the schema.
   */
  @Column(name = "content", columnDefinition = "Text", nullable = false)
  private String content;

  /**
   * The terms defined in this schema.
   */
  @OneToMany(mappedBy = "schemaId", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
  private Set<SchemaTerm> terms;

  public SchemaRecord(String schemaId, String nameHash, SchemaType type,String content, Set<String> terms) {
    this.schemaId = schemaId;
    this.nameHash = nameHash;
    this.type = type;
    this.content = content;
    this.terms = terms.stream().map(t -> new SchemaTerm(t, schemaId)).collect(Collectors.toSet());
    uploadTime = Instant.now();
    updateTime = Instant.now();
  }

  public void replaceTerms(List<String> terms) {
    this.terms = terms.stream().map(t -> new SchemaTerm(t, schemaId)).collect(Collectors.toSet());
  }
}
