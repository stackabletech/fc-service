package eu.gaiax.difs.fc.core.dao.impl;

import eu.gaiax.difs.fc.api.generated.model.SelfDescription;
import eu.gaiax.difs.fc.core.dao.SelfDescriptionDao;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * Implementation of the {@link SelfDescriptionDao} interface.
 */
@Repository
public class SelfDescriptionDaoImpl implements SelfDescriptionDao {
  // TODO: 27.07.2022 Awaiting implementation by Fraunhofer
  /**
   * Fetch a Self-Description and its metadata by hash.
   *
   * @param hash Filter for a hash of the SD.
   * @return Self-Description metadata.
   */
  @Override
  public SelfDescription getByHash(String hash) {
    return getDefaultSdMetadata();
  }

  /**
   * Store the given Self-Description.
   *
   * @param selfDescription Self-Description metadata.
   */
  @Override
  public void storeSelfDescription(SelfDescription selfDescription) {

  }

  /**
   * Change the life cycle status of the Self-Description with the given hash.
   *
   * @param hash Filter for a hash of the SD.
   * @param targetStatus Self-Description status.
   */
  @Override
  public void changeLifeCycleStatus(String hash, SelfDescription.StatusEnum targetStatus) {

  }

  /**
   * Remove the Self-Description with the given hash from the store.
   *
   * @param hash Filter for a hash of the SD.
   */
  @Override
  public void deleteSelfDescription(String hash) {

  }

  /**
   * Get all self descriptions, starting from the given offset, up to limit number
   * of items, consistently ordered.
   *
   * @param offset How many items to skip.
   * @param limit  The maximum number of items to return.
   * @return List of meta-data of available SD.
   */
  @Override
  public List<SelfDescription> getAllSelfDescriptions(int offset, int limit) {
    List<SelfDescription> selfDescriptions = new ArrayList<>();
    selfDescriptions.add(getDefaultSdMetadata());
    selfDescriptions.add(getDefaultSdMetadata());
    return selfDescriptions;
  }

  private SelfDescription getDefaultSdMetadata() {
    SelfDescription sdMetadata = new SelfDescription();
    sdMetadata.setId("string");
    sdMetadata.setSdHash("string");
    sdMetadata.setIssuer("http://example.org/test-provider");
    sdMetadata.setStatus(SelfDescription.StatusEnum.ACTIVE);
    List<String> validators = new ArrayList<>();
    validators.add("string");
    sdMetadata.setValidators(validators);
    sdMetadata.setStatusTime("2022-05-11T15:30:00Z");
    sdMetadata.setUploadTime("2022-03-01T13:00:00Z");
    return sdMetadata;
  }
}
