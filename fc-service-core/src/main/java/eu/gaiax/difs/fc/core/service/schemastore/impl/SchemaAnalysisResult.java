package eu.gaiax.difs.fc.core.service.schemastore.impl;

import java.util.List;
import java.util.Set;

import eu.gaiax.difs.fc.core.service.schemastore.SchemaStore.SchemaType;

/**
 *
 * @author hylke
 */
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
@lombok.Getter
@lombok.Setter
public class SchemaAnalysisResult {

  /**
   * Flag indicating the schema is a valid schema.
   */
  private boolean valid;
  /**
   * The detected type of the schema.
   */
  private SchemaType schemaType;
  /**
   * The identifier of the schema if it can be extracted from the schema. Null
   * otherwise.
   */
  private String extractedId;
  /**
   * The URLs of the entities that are defined in this schema.
   */
  private Set<String> extractedUrls;
  /**
   * The error message if validation failed.
   */
  private String errorMessage;

}
