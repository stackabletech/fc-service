package eu.gaiax.difs.fc.core.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Class for handling the metadata of a Self-Description, and optionally a
 * reference to a content accessor.
 *
 */
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
@lombok.EqualsAndHashCode(callSuper = true)
public class SelfDescriptionMetadata extends eu.gaiax.difs.fc.api.generated.model.SelfDescription {

  /**
   * A reference to the self description content.
   */
  @lombok.Getter
  @lombok.Setter
  @JsonIgnore
  private ContentAccessor selfDescription;

}
