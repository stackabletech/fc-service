package eu.gaiax.difs.fc.core.service.schemastore.impl;

import eu.gaiax.difs.fc.core.service.schemastore.SchemaStore.SchemaType;

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
