package eu.gaiax.difs.fc.core.service.sdstore;

import eu.gaiax.difs.fc.api.generated.model.SelfDescriptionStatus;
import eu.gaiax.difs.fc.core.pojo.SdFilter;
import eu.gaiax.difs.fc.core.pojo.SelfDescriptionMetadata;
import eu.gaiax.difs.fc.core.pojo.VerificationResult;
import java.util.List;

/**
 *
 * @author hylke
 */
public interface SelfDescriptionStore {

  String STORE_NAME = "sdFiles";
    
  /**
   * Get all self descriptions, starting from the given offset, up to limit
   * number of items, consistently ordered.
   *
   * @param offset How many items to skip.
   * @param limit The maximum number of items to return.
   * @return
   */
  List<SelfDescriptionMetadata> getAllSelfDescriptions(int offset, int limit);

  /**
   * Fetch a SelfDescription and its meta data by hash.
   *
   * @param hash
   * @return
   */
  SelfDescriptionMetadata getByHash(String hash);

  /**
   * Fetch all SelfDescriptions that match the filter parameters.
   *
   * @param filterParams
   * @return
   */
  List<SelfDescriptionMetadata> getByFilter(SdFilter filterParams);

  /**
   * Store the given SelfDescription.
   *
   * @param selfDescription The Self-Description to store.
   * @param sdVerificationResults The results of the verification of the
   * Self-Description.
   */
  void storeSelfDescription(SelfDescriptionMetadata selfDescription, VerificationResult sdVerificationResults);

  /**
   * Change the life cycle status of the self description with the given hash.
   *
   * @param hash The hash of the SD to work on.
   * @param targetStatus The new status.
   */
  void changeLifeCycleStatus(String hash, SelfDescriptionStatus targetStatus);

  /**
   * Remove the Self-Description with the given hash from the store.
   *
   * @param hash The hash of the SD to work on.
   */
  void deleteSelfDescription(String hash);
}
