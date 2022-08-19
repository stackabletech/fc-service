package eu.gaiax.difs.fc.core.service.sdstore.impl;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "validators")
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
@lombok.EqualsAndHashCode
@lombok.Getter
@lombok.Setter
public class ValidatorRecord implements Serializable {

  @Id
  @Column(name = "sdhash")
  private String sdHash;

  @Id
  @Column(name = "validator")
  private String validator;

}
