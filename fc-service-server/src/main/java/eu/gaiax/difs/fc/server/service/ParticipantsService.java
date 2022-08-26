package eu.gaiax.difs.fc.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.gaiax.difs.fc.api.generated.model.Participant;
import eu.gaiax.difs.fc.api.generated.model.Participants;
import eu.gaiax.difs.fc.api.generated.model.UserProfile;
import eu.gaiax.difs.fc.api.generated.model.UserProfiles;
import eu.gaiax.difs.fc.core.dao.ParticipantDao;
import eu.gaiax.difs.fc.core.exception.NotFoundException;
import eu.gaiax.difs.fc.core.pojo.ContentAccessorDirect;
import eu.gaiax.difs.fc.core.pojo.ParticipantMetaData;
import eu.gaiax.difs.fc.core.pojo.SelfDescriptionMetadata;
import eu.gaiax.difs.fc.core.pojo.VerificationResultParticipant;
import eu.gaiax.difs.fc.core.service.sdstore.SelfDescriptionStore;
import eu.gaiax.difs.fc.core.service.verification.VerificationService;
import eu.gaiax.difs.fc.server.generated.controller.ParticipantsApiDelegate;
import eu.gaiax.difs.fc.server.util.SessionUtils;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ws.rs.ForbiddenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link ParticipantsApiDelegate} interface.
 */
@Slf4j
@Service
public class ParticipantsService implements ParticipantsApiDelegate {
  private static final TypeReference<Map<String, Object>> MAP_TYPE_REF = new TypeReference<>() {};

  @Autowired
  private ParticipantDao partDao;

  @Autowired
  private SelfDescriptionStore selfDescriptionStore;

  @Autowired
  private VerificationService verificationService;

  @Autowired
  private ObjectMapper jsonMapper;

  private final String catalogueAdminRole="ROLE_Ro-MU-CA";

  /**
   * POST /participants : Register a new participant in the catalogue.
   *
   * @param body Participant Self-Description (required)
   * @return Created Participant (status code 201)
   *         or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *         or Forbidden. The user does not have the permission to execute this request. (status code 403)
   *         or May contain hints how to solve the error or indicate what went wrong at the server.
   *         Must not outline any information about the internal structure of the server. (status code 500)
   */
  @Override
  @Transactional
  public ResponseEntity<Participant> addParticipant(String body) {
    log.debug("addParticipant.enter; got participant: {}", body); // it can be JWT?
    ParticipantMetaData part = getParticipantExtWithValidationAndStore(body);
    part = partDao.create(part);
    log.debug("addParticipant.exit; returning: {}", part);
    return ResponseEntity.created(URI.create("/participants/" + part.getId())).body(part);
  }

  /**
   * DELETE /participants/{participantId} : Delete a participant in the catalogue.
   *
   * @param participantId The participant to delete. (required)
   * @return Deleted Participant (status code 200)
   *         or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *         or Forbidden. The user does not have the permission to execute this request. (status code 403)
   *         or The specified resource was not found (status code 404)
   *         or May contain hints how to solve the error or indicate what went wrong at the server.
   *         Must not outline any information about the internal structure of the server. (status code 500)
   */
  @Override
  @Transactional
  public ResponseEntity<Participant> deleteParticipant(String participantId) {
    log.debug("deleteParticipant.enter; got participant: {}", participantId);
    ParticipantMetaData part = partDao.select(participantId)
        .orElseThrow(() -> new NotFoundException("Participant not found: " + participantId));
    if(!SessionUtils.sessionUserHasRole(catalogueAdminRole) && !part.getId().equals(SessionUtils.getSessionParticipantId())) {
      throw new ForbiddenException("User has no permissions to delete participant : " + participantId);
    }
    selfDescriptionStore.deleteSelfDescription(part.getSdHash());
    partDao.delete(participantId)
        .orElseThrow(() -> new NotFoundException("Participant not found: " + participantId));
    log.debug("deleteParticipant.exit; returning: {}", part);
    return ResponseEntity.ok(part);
  }

  /**
   * GET /participants/{participantId} : Get the registered participant.
   *
   * @param participantId The participantId to get. (required)
   * @return The requested participant (status code 200)
   *         or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *         or Forbidden. The user does not have the permission to execute this request. (status code 403)
   *         or The specified resource was not found (status code 404)
   *         or May contain hints how to solve the error or indicate what went wrong at the server.
   *         Must not outline any information about the internal structure of the server. (status code 500)
   */
  @Override
  public ResponseEntity<Participant> getParticipant(String participantId) {
    log.debug("getParticipant.enter; got participant: {}", participantId);
    ParticipantMetaData part = partDao.select(participantId)
        .orElseThrow(() -> new NotFoundException("Participant not found: " + participantId));
    SelfDescriptionMetadata selfDescriptionMetadata = selfDescriptionStore.getByHash(part.getSdHash());
    part.setSelfDescription(selfDescriptionMetadata.getSelfDescription().getContentAsString());
    log.debug("getParticipant.exit; returning: {}", part);
    return ResponseEntity.ok(part);
  }

  /**
   * GET /participants/{participantId}/users : Get all users of the registered participant.
   *
   * @param participantId The participant Id (required)
   * @return Users of the participant (status code 200)
   *         or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *         or Forbidden. The user does not have the permission to execute this request. (status code 403)
   *         or The specified resource was not found (status code 404)
   *         or May contain hints how to solve the error or indicate what went wrong at the server.
   *         Must not outline any information about the internal structure of the server. (status code 500)
   */
  @Override
  public ResponseEntity<UserProfiles> getParticipantUsers(String participantId, Integer offset, Integer limit) {
    log.debug("getParticipantUsers.enter; got participantId: {}", participantId);
    List<UserProfile> profiles = partDao.selectUsers(participantId)
        .orElseThrow(() -> new NotFoundException("Participant not found: " + participantId));
    log.debug("getParticipantUsers.exit; returning: {}", profiles.size());
    // TODO: set total count
    return ResponseEntity.ok(new UserProfiles(0, profiles));
  }

  /**
   * GET /participants : Get the registered participants.
   *
   * @param offset The number of items to skip before starting to collect the result set. (optional, default to 0)
   * @param limit The number of items to return. (optional, default to 100)
   * @return List of registered participants (status code 200)
   *         or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *         or May contain hints how to solve the error or indicate what went wrong at the server.
   *         Must not outline any information about the internal structure of the server. (status code 500)
   */
  @Override
  public ResponseEntity<Participants> getParticipants(Integer offset, Integer limit) { //, String orderBy, Boolean asc) {
    // sorting is not supported yet by keycloak admin API
    log.debug("getParticipants.enter; got offset: {}, limit: {}", offset, limit);
    List<ParticipantMetaData> partsExt = partDao.search(offset, limit);
    //Adding actual SD from sd-store for each sd-hash present in keycloak
    List<Participant> parts =
        partsExt.stream().map(participant -> {
          participant.setSelfDescription(
              selfDescriptionStore.getByHash(participant.getSdHash()).getSelfDescription()
                  .getContentAsString());
          return participant;
        }).collect(
            Collectors.toList());
    log.debug("getParticipants.exit; returning size:{}, parts: {}", parts.size(),parts);
    return ResponseEntity.ok(new Participants(partsExt.size(), parts));
  }

  /**
   * PUT /participants/{participantId} : Update a participant in the catalogue.
   *
   * @param participantId The participant to update. (required)
   * @param body Participant Self-Description (required)
   * @return Updated Participant (status code 200)
   *         or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *         or Forbidden. The user does not have the permission to execute this request. (status code 403)
   *         or The specified resource was not found (status code 404)
   *         or May contain hints how to solve the error or indicate what went wrong at the server.
   *         Must not outline any information about the internal structure of the server. (status code 500)
   */
  @Override
  @Transactional
  public ResponseEntity<Participant> updateParticipant(String participantId, String body) {
    log.debug("updateParticipant.enter; got participant: {}", participantId);
    ParticipantMetaData partExisting = partDao.select(participantId)
        .orElseThrow(() -> new NotFoundException("Participant not found: " + participantId));

    ContentAccessorDirect contentAccessorDirect = new ContentAccessorDirect(body);
    VerificationResultParticipant verificationResultParticipant =
        verificationService.verifyParticipantSelfDescription(contentAccessorDirect);

    log.debug("updateParticipant.existing participant got : {}", partExisting);
    log.debug("updateParticipant.verificationResultParticipant.got : {}", verificationResultParticipant);
    if(!SessionUtils.sessionUserHasRole(catalogueAdminRole)  && !partExisting.getId().equals(SessionUtils.getSessionParticipantId())) {
      throw new ForbiddenException("User has no permissions to update participant : " + participantId);
    }
    ParticipantMetaData participantUpdated = getParticipantExtWithValidationAndStore(contentAccessorDirect.getContentAsString());

    ParticipantMetaData participantMetaData = partDao.update(participantId, participantUpdated)
        .orElseThrow(() -> new NotFoundException("Participant not found: " + participantId));
    log.debug("updateParticipant.exit; returning: {}", participantMetaData);
    return ResponseEntity.ok(participantMetaData);
  }

  /**
   * Utility method for converting Participant Self-Description to ParticipantExt metadata.
   *
   * @param body Self-Description.
   * @return ParticipantExt ParticipantExt.
   */
  private ParticipantMetaData getParticipantExtWithValidationAndStore(String body) {
    ContentAccessorDirect contentAccessorDirect = new ContentAccessorDirect(body);
    VerificationResultParticipant verificationResult = verificationService.verifyParticipantSelfDescription(contentAccessorDirect);
    log.debug("getParticipantExtWithValidationAndStore.got VerificationResultParticipant: {}",verificationResult);
    SelfDescriptionMetadata selfDescriptionMetadata = new SelfDescriptionMetadata(contentAccessorDirect, verificationResult);
    log.debug("getParticipantExtWithValidationAndStore.got SelfDescriptionMetadata: {}",selfDescriptionMetadata);
    selfDescriptionStore.storeSelfDescription(selfDescriptionMetadata, verificationResult);
    return new ParticipantMetaData(verificationResult.getId(), verificationResult.getParticipantName(),
            verificationResult.getParticipantPublicKey(), selfDescriptionMetadata.getSelfDescription().getContentAsString(),
            selfDescriptionMetadata.getSdHash());
  }
}
