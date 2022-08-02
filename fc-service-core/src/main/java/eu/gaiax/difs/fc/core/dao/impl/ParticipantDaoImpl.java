package eu.gaiax.difs.fc.core.dao.impl;

import static eu.gaiax.difs.fc.core.util.KeycloakUtils.getErrorMessage;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.hash.Hashing;

import eu.gaiax.difs.fc.api.generated.model.Participant;
import eu.gaiax.difs.fc.api.generated.model.UserProfile;
import eu.gaiax.difs.fc.core.dao.ParticipantDao;
import eu.gaiax.difs.fc.core.exception.ConflictException;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of the {@link ParticipantDao} interface.
 */
@Slf4j
@Component
public class ParticipantDaoImpl implements ParticipantDao {
  private static final String ATR_NAME = "name";
  private static final String ATR_PUBLIC_KEY = "publicKey";
  private static final String ATR_SD_HASH = "sdHash";

  @Value("${keycloak.realm}")
  private String realm;
  @Autowired
  private Keycloak keycloak;

  /**
   * Create Participant.
   *
   * @param participant Participant entity.
   * @return Created participant.
   */
  @Override
  public Participant create(Participant participant) {

    GroupsResource instance = keycloak.realm(realm).groups();
    GroupRepresentation groupRepo = toGroupRepo(participant);
    Response response = instance.add(groupRepo);
    if (response.getStatus() != HttpStatus.SC_CREATED) {
      String message = getErrorMessage(response);
      log.info("create.error; status {}:{}, {}", response.getStatus(), response.getStatusInfo(), message);
      throw new ConflictException(message);
    }
    return participant;
  }

  /**
   * Get an Optional Participant by id.
   *
   * @param participantId Participant id.
   * @return Optional Participant.
   */
  @Override
  public Optional<Participant> select(String participantId) {

    GroupsResource instance = keycloak.realm(realm).groups();
    List<GroupRepresentation> groups = instance.groups(participantId, 0, 1, false);
    if (groups.size() == 0) {
      return Optional.empty();
    }
    return Optional.of(toParticipant(groups.get(0)));
  }

  /**
   * Get list of users by participant id.
   *
   * @param participantId Participant id.
   * @return Optional list of users.
   */
  @Override
  public Optional<List<UserProfile>> selectUsers(String participantId) {

    GroupsResource instance = keycloak.realm(realm).groups();
    List<GroupRepresentation> groups = instance.groups(participantId, 0, 1, false);
    if (groups.size() == 0) {
      return Optional.empty();
    }
    GroupRepresentation groupRepo = groups.get(0);
    return Optional.of(instance.group(groupRepo.getId())
        .members().stream().map(UserDaoImpl::toUserProfile).collect(Collectors.toList()));
  }

  /**
   * Remove the Participant with the given id.
   *
   * @param participantId Participant id.
   * @return Removed optional participant.
   */
  @Override
  public Optional<Participant> delete(String participantId) {
    GroupsResource instance = keycloak.realm(realm).groups();
    List<GroupRepresentation> groups = instance.groups(participantId, 0, 1, false);
    if (groups.size() == 0) {
      return Optional.empty();
    }
    GroupRepresentation groupRepo = groups.get(0);
    instance.group(groupRepo.getId()).remove();
    return Optional.of(toParticipant(groupRepo));
  }

  /**
   * Update the Participant with the given id.
   *
   * @param participantId Participant id.
   * @param participant Participant model.
   * @return Updated optional participant.
   */
  @Override
  public Optional<Participant> update(String participantId, Participant participant) {
    GroupsResource instance = keycloak.realm(realm).groups();
    List<GroupRepresentation> groups = instance.groups(participantId, 0, 1, true);
    if (groups.size() == 0) {
      return Optional.empty();
    }
    GroupRepresentation groupRepo = groups.get(0);
    GroupRepresentation updated = toGroupRepo(participant);
    instance.group(groupRepo.getId()).update(updated);
    return Optional.of(toParticipant(updated));
  }

  /**
   * Get participants by filtered params.
   *
   * @param offset How many items to skip.
   * @param limit The maximum number of items to return.
   * @return List of filtered participants.
   */
  @Override
  public List<Participant> search(Integer offset, Integer limit) {
    GroupsResource instance = keycloak.realm(realm).groups();
    List<GroupRepresentation> groups = instance.groups(null, offset, limit, false);
    return groups.stream().map(this::toParticipant).collect(Collectors.toList());
  }

  /**
   * Map participant to user group representation model.
   *
   * @param participant Participant model.
   * @return User group representation model.
   */
  public static GroupRepresentation toGroupRepo(Participant participant) {
    GroupRepresentation groupRepo = new GroupRepresentation();
    groupRepo.setName(participant.getId());
    groupRepo.singleAttribute(ATR_NAME, participant.getName());
    groupRepo.singleAttribute(ATR_PUBLIC_KEY, participant.getPublicKey());
    String sha256 = Hashing.sha256().hashString(participant.getSelfDescription(), StandardCharsets.UTF_8).toString();
    groupRepo.singleAttribute(ATR_SD_HASH, sha256);
    return groupRepo;
  }

  /**
   * Map group representation to participant model.
   *
   * @param groupRepo Group representation model.
   * @return Participant model.
   */
  private Participant toParticipant(GroupRepresentation groupRepo) {
    Map<String, List<String>> attributes = groupRepo.getAttributes();
    return new Participant(groupRepo.getName(), attributes.get(ATR_NAME).get(0),
        attributes.get(ATR_PUBLIC_KEY).get(0), attributes.get(ATR_SD_HASH).get(0));
  }
}
