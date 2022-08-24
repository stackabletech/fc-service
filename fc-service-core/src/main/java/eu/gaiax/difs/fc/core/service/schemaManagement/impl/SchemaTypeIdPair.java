package eu.gaiax.difs.fc.core.service.schemaManagement.impl;

import eu.gaiax.difs.fc.core.service.schemaManagement.SchemaStore.SchemaType;

/**
 *
 * @author hylke
 */
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
@lombok.Getter
@lombok.Setter
public class SchemaTypeIdPair {

  private SchemaType type;
  private String schemaId;

}
