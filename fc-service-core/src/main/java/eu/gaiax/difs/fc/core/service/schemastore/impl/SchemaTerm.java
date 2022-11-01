package eu.gaiax.difs.fc.core.service.schemastore.impl;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

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
