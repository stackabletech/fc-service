package eu.gaiax.difs.fc.core.dao;

import java.util.List;
import java.util.Optional;

import eu.gaiax.difs.fc.api.generated.model.Participant;
import eu.gaiax.difs.fc.api.generated.model.UserProfile;

/**
 * Participant repository interface.
 */
public interface ParticipantDao {
  /**
   * Create Participant.
   *
   * @param participant Participant entity.
   * @return Created participant.
   */
  Participant create(Participant participant);

  /**
   * Get an Optional Participant by id.
   *
   * @param participantId Participant id.
   * @return Optional Participant.
   */
  Optional<Participant> select(String participantId);

  /**
   * Get list of users by participant id.
   *
   * @param participantId Participant id.
   * @return Optional list of users.
   */
  Optional<List<UserProfile>> selectUsers(String participantId);

  /**
   * Remove the Participant with the given id.
   *
   * @param participantId Participant id.
   * @return Removed optional participant.
   */
  Optional<Participant> delete(String participantId);

  /**
   * Update the Participant with the given id.
   *
   * @param participantId Participant id.
   * @param participant Participant model.
   * @return Updated optional participant.
   */
  Optional<Participant> update(String participantId, Participant participant);

  /**
   * Get participants by filtered params.
   *
   * @param offset How many items to skip.
   * @param limit The maximum number of items to return.
   * @return List of filtered participants.
   */
  List<Participant> search(Integer offset, Integer limit);
}
