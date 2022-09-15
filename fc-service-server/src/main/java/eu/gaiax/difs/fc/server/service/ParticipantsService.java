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
import eu.gaiax.difs.fc.core.service.filestore.FileStore;
import eu.gaiax.difs.fc.core.service.sdstore.SelfDescriptionStore;
import eu.gaiax.difs.fc.core.service.verification.VerificationService;
import eu.gaiax.difs.fc.server.generated.controller.ParticipantsApiDelegate;
import eu.gaiax.difs.fc.server.util.SessionUtils;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ws.rs.ForbiddenException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
  @Qualifier("sdFileStore")
  private FileStore fileStore;

  @Autowired
  private SelfDescriptionStore selfDescriptionStore;

  @Autowired
  private VerificationService verificationService;

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
    Pair<VerificationResultParticipant,SelfDescriptionMetadata> pairResult = validateSelfDescription(body);
    VerificationResultParticipant verificationResult = pairResult.getLeft();
    SelfDescriptionMetadata selfDescriptionMetadata = pairResult.getRight();

    selfDescriptionStore.storeSelfDescription(selfDescriptionMetadata, verificationResult);

    ParticipantMetaData participantMetaData = toParticipantMetaData(verificationResult, selfDescriptionMetadata);
    registerRollBackForFileStoreManuallyIfTransactionFail(participantMetaData);

    participantMetaData = partDao.create(participantMetaData);
    return ResponseEntity.created(URI.create("/participants/" + participantMetaData.getId())).body(participantMetaData);
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
    ParticipantMetaData part = checkUserAndRolePermission(participantId);
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

    checkUserAndRolePermission(participantId);

    Pair<VerificationResultParticipant,SelfDescriptionMetadata> pairResult = validateSelfDescription(body);
    VerificationResultParticipant verificationResult = pairResult.getLeft();
    SelfDescriptionMetadata selfDescriptionMetadata = pairResult.getRight();

    selfDescriptionStore.storeSelfDescription(selfDescriptionMetadata,verificationResult);

    ParticipantMetaData participantUpdated = toParticipantMetaData(verificationResult,selfDescriptionMetadata);
    registerRollBackForFileStoreManuallyIfTransactionFail(participantUpdated);

    ParticipantMetaData participantMetaData = partDao.update(participantId, participantUpdated)
        .orElseThrow(() -> new NotFoundException("Participant not found: " + participantId));
    log.debug("updateParticipant.exit; returning: {}", participantMetaData);
    return ResponseEntity.ok(participantMetaData);
  }

  /**
   * Utility method to return {@link ParticipantMetaData}
   * @param verificationResult result of validation
   * @param selfDescriptionMetadata metadata of self-description
   * @return ParticipantMetaData
   */
  private ParticipantMetaData toParticipantMetaData(VerificationResultParticipant verificationResult,
                                                    SelfDescriptionMetadata selfDescriptionMetadata) {
    return new ParticipantMetaData(verificationResult.getId(), verificationResult.getParticipantName(),
        verificationResult.getParticipantPublicKey(), selfDescriptionMetadata.getSelfDescription().getContentAsString(),
        selfDescriptionMetadata.getSdHash());
  }

  /**
   * Catalogue-admin user has All permission and for others Check if session user id has same with the existing
   * participant id.
   * @param participantId id of the participant
   */
  private ParticipantMetaData checkUserAndRolePermission(String participantId) {
    ParticipantMetaData partExisting = partDao.select(participantId)
        .orElseThrow(() -> new NotFoundException("Participant not found: " + participantId));
    log.debug("checkUserAndRolePermission.existing participant got : {}", partExisting);
    if(!SessionUtils.sessionUserHasRole(catalogueAdminRole)  &&
        !partExisting.getId().equals(SessionUtils.getSessionParticipantId())) {
      throw new ForbiddenException("User has no permissions to operate participant : " + participantId);
    }
    return partExisting;
  }

  /**
   * Validate self-Description.
   * @param body self description
   * @return DTO object containing result and metadata of self-description
   */
  private Pair<VerificationResultParticipant,SelfDescriptionMetadata> validateSelfDescription(String body){

    ContentAccessorDirect contentAccessorDirect = new ContentAccessorDirect(body);
    VerificationResultParticipant verificationResultParticipant =
        verificationService.verifyParticipantSelfDescription(contentAccessorDirect);
    log.debug("updateParticipant.verificationResultParticipant.got : {}", verificationResultParticipant);

    SelfDescriptionMetadata selfDescriptionMetadata = new SelfDescriptionMetadata(contentAccessorDirect,
        verificationResultParticipant);
    log.debug("getParticipantExtWithValidationAndStore.got SelfDescriptionMetadata: {}",selfDescriptionMetadata);

    return Pair.of(verificationResultParticipant,selfDescriptionMetadata);
  }

  /**
   * Manually registering rollback for the file system when transactions was rolled-back as spring does not have
   * rollback for file systems storage.
   * @param part participant metadata to be rolled back.
   */
  private void registerRollBackForFileStoreManuallyIfTransactionFail(ParticipantMetaData part) {

    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCompletion(int status) {
        if(TransactionSynchronization.STATUS_ROLLED_BACK == status){
          try {
            fileStore.deleteFile(part.getSdHash());
            log.debug("registerRollBackForFileStoreManuallyIfTransactionFail.Rolling back manually file with hash  : " +
                "{}", part.getSdHash());
            TransactionSynchronizationManager.clearSynchronization();
          } catch(IOException ex) {

          }
        }
      }
    });
  }

}
