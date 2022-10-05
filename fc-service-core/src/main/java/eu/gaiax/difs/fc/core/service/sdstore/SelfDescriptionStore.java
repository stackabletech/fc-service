package eu.gaiax.difs.fc.core.service.sdstore;

import eu.gaiax.difs.fc.api.generated.model.SelfDescriptionStatus;
import eu.gaiax.difs.fc.core.pojo.ContentAccessor;
import eu.gaiax.difs.fc.core.pojo.PaginatedResults;
import eu.gaiax.difs.fc.core.pojo.SdFilter;
import eu.gaiax.difs.fc.core.pojo.SelfDescriptionMetadata;
import eu.gaiax.difs.fc.core.pojo.VerificationResult;
import java.util.List;

/**
 * A store for storing and retrieving self-description meta data objects.
 *
 * @author hylke
 * @author j_reuter
 */
public interface SelfDescriptionStore {
  /**
   * Fetch a self-description file by its hash value.
   *
   * @param hash The hash value that identifies the self-description meta data.
   * @return The self-description file.
   */
  ContentAccessor getSDFileByHash(String hash);

  /**
   * Fetch a self-description and its meta data by its hash value.
   *
   * @param hash The hash value that identifies the self-description meta data.
   * @return The self-description meta data object with the specified hash value.
   */
  SelfDescriptionMetadata getByHash(String hash);

  /**
   * Fetch all self-descriptions that match the filter parameters.
   *
   * @param filter The filter to match all self-descriptions against.
   * @return List of all self-description meta data objects that match the
   *         specified filter.
   */
  PaginatedResults<SelfDescriptionMetadata> getByFilter(SdFilter filter);

  /**
   * Store the given self-description.
   *
   * @param selfDescription       The self-description to store.
   * @param sdVerificationResults The results of the verification of the
   *                              self-description.
   */
  void storeSelfDescription(SelfDescriptionMetadata selfDescription, VerificationResult sdVerificationResults);

  /**
   * Change the life cycle status of the self-description with the given hash.
   *
   * @param hash         The hash of the self-description to work on.
   * @param targetStatus The new status.
   */
  void changeLifeCycleStatus(String hash, SelfDescriptionStatus targetStatus);

  /**
   * Remove the self-description with the given hash from the store.
   *
   * @param hash The hash of the self-description to work on.
   */
  void deleteSelfDescription(String hash);

  /**
   * Invalidate expired Self-descriptions in the store.
   *
   * @return Number of expired Self-descriptions found.
   */
  int invalidateSelfDescriptions();
}
