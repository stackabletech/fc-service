package eu.gaiax.difs.fc.core.service.schemastore.impl;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "schematerms")
@lombok.EqualsAndHashCode
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.Getter
@lombok.Setter
@lombok.ToString
public class SchemaTerm implements Serializable {

  @Id
  @Column(name = "term", nullable = false, length = 256)
  private String term;

  @Column(name = "schemaid", nullable = false, length = 200)
  private String schemaId;

}
