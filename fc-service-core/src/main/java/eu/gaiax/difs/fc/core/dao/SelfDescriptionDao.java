package eu.gaiax.difs.fc.core.dao;

import eu.gaiax.difs.fc.api.generated.model.SelfDescription;
import java.util.List;

// TODO: 27.07.2022 The interface describes the logic for metadata only. No work with regular data.
//  (Interface taken from Fraunhofer documentation)
/**
 * Self-Description repository interface.
 */
public interface SelfDescriptionDao {
  /**
   * Fetch a Self-Description and its metadata by hash.
   *
   * @param hash Filter for a hash of the SD.
   * @return Self-Description metadata.
   */
  SelfDescription getByHash(String hash);

  /**
   * Store the given Self-Description.
   *
   * @param selfDescription Self-Description metadata.
   */
  void storeSelfDescription(SelfDescription selfDescription);

  /**
   * Change the life cycle status of the Self-Description with the given hash.
   *
   * @param hash Filter for a hash of the SD.
   * @param targetStatus Self-Description status.
   */
  void changeLifeCycleStatus(String hash, SelfDescription.StatusEnum targetStatus);

  /**
   * Remove the Self-Description with the given hash from the store.
   *
   * @param hash Filter for a hash of the SD.
   */
  void deleteSelfDescription(String hash);

  /**
   * Get all self-descriptions, starting from the given offset, up to limit number
   * of items, consistently ordered.
   *
   * @param offset How many items to skip.
   * @param limit  The maximum number of items to return.
   * @return List of meta-data of available SD.
   */
  List<SelfDescription> getAllSelfDescriptions(int offset, int limit);
}