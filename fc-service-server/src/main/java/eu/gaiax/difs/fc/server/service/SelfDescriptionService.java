package eu.gaiax.difs.fc.server.service;

import static eu.gaiax.difs.fc.server.util.SelfDescriptionHelper.parseTimeRange;
import static eu.gaiax.difs.fc.server.util.SessionUtils.getSessionParticipantId;

import eu.gaiax.difs.fc.api.generated.model.SelfDescription;
import eu.gaiax.difs.fc.api.generated.model.SelfDescriptionStatus;
import eu.gaiax.difs.fc.api.generated.model.SelfDescriptions;
import eu.gaiax.difs.fc.core.exception.ClientException;
import eu.gaiax.difs.fc.core.pojo.ContentAccessorDirect;
import eu.gaiax.difs.fc.core.pojo.SdFilter;
import eu.gaiax.difs.fc.core.pojo.SelfDescriptionMetadata;
import eu.gaiax.difs.fc.core.pojo.VerificationResultOffering;
import eu.gaiax.difs.fc.core.service.sdstore.SelfDescriptionStore;
import eu.gaiax.difs.fc.core.service.verification.VerificationService;
import eu.gaiax.difs.fc.server.generated.controller.SelfDescriptionsApiDelegate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Implementation of the {@link eu.gaiax.difs.fc.server.generated.controller.SelfDescriptionsApiDelegate} interface.
 */
@Slf4j
@Service
public class SelfDescriptionService implements SelfDescriptionsApiDelegate {
  @Autowired
  private VerificationService verificationService;

  @Autowired
  private SelfDescriptionStore sdStore;

  /**
   * Service method for GET /self-descriptions : Get the list of metadata of SD in the Catalogue.
   *
   * @param uploadTr Filter for the time range when the SD was uploaded to the catalogue.
   *                        The time range has to be specified as start time and end time as ISO8601 timestamp
   *                        separated by a &#x60;/&#x60;. (optional)
   * @param statusTr Filter for the time range when the status of the SD was last changed in the catalogue.
   *                        The time range has to be specified as start time and end time as ISO8601 timestamp
   *                        separated by a &#x60;/&#x60;. (optional)
   * @param issuer          Filter for the issuer of the SD. This is the unique ID of the Participant
   *                        that has prepared the SD. (optional)
   * @param validator       Filter for a validator of the SD. This is the unique ID of the Participant
   *                        that validated (part of) the SD. (optional)
   * @param status          Filter for the status of the SD. (optional, default to active)
   * @param id              Filter for id/credentialSubject of the SD. (optional)
   * @param hash            Filter for a hash of the SD. (optional)
   * @param offset          The number of items to skip before starting to collect the result set.
   *                        (optional, default to 0)
   * @param limit           The number of items to return. (optional, default to 100)
   * @return List of meta-data of available SD. (status code 200)
   *        or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *        or May contain hints how to solve the error or indicate what went wrong at the server.
   *        Must not outline any information about the internal structure of the server. (status code 500)
   */
  public ResponseEntity<SelfDescriptions> readSelfDescriptions(String uploadTr, String statusTr, String issuer,
                                                                    String validator, SelfDescriptionStatus status, String id,
                                                                    String hash, Integer offset, Integer limit) {
    log.debug("readSelfDescriptions.enter; got uploadTimeRange: {}, statusTimeRange: {},"
            + "issuer: {}, validator: {}, status: {}, id: {}, hash: {}, offset: {}, limit: {}",
        uploadTr, statusTr, issuer, validator, status, id, hash, offset, limit);

    List<SelfDescriptionMetadata> selfDescriptions;
    if (isNotNullObjects(id, hash, issuer, validator, uploadTr, statusTr)) {
      SdFilter filter = setupSdFilter(id, hash, limit, offset, status, issuer, validator, uploadTr, statusTr);
      selfDescriptions = sdStore.getByFilter(filter);
    } else {
      selfDescriptions = sdStore.getAllSelfDescriptions(offset, limit);
    }
    log.debug("readSelfDescriptions.exit; returning: {}", selfDescriptions.size());
    // TODO: set total count
    return ResponseEntity.ok(new SelfDescriptions(0, (List) selfDescriptions));
  }

  /**
   * Service method for GET /self-descriptions/{self_description_hash} : Read a SD by its hash. Returns the content
   * of the single SD.
   *
   * @param selfDescriptionHash Hash of the self-description (required)
   * @return The requested Self-Description (status code 200)
   *         or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *         or Self-Description not found (status code 404)
   *         or May contain hints how to solve the error or indicate what went wrong at the server.
   *         Must not outline any information about the internal structure of the server. (status code 500)
   */
  @Override
  public ResponseEntity<String> readSelfDescriptionByHash(String selfDescriptionHash) {
    log.debug("readSelfDescriptionByHash.enter; got hash: {}", selfDescriptionHash);
    SelfDescriptionMetadata sdMetadata = sdStore.getByHash(selfDescriptionHash);

    HttpHeaders responseHeaders = new HttpHeaders();
    //responseHeaders.set("Content-Type", "application/ld+json");

    log.debug("readSelfDescriptionByHash.exit; returning self-description by hash: {}", selfDescriptionHash);
    return ResponseEntity.ok()
        .headers(responseHeaders)
        .body(sdMetadata.getSelfDescription().getContentAsString());
  }

  /**
   * Service method for DELETE /self-descriptions/{self_description_hash} : Completely delete a SD.
   *
   * @param selfDescriptionHash Hash of the SD (required)
   * @return OK (status code 200)
   *         or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *         or Forbidden. The user does not have the permission to execute this request. (status code 403)
   *         or May contain hints how to solve the error or indicate what went wrong at the server.
   *         Must not outline any information about the internal structure of the server. (status code 500)
   */
  @Override
  public ResponseEntity<Void> deleteSelfDescription(String selfDescriptionHash) {
    log.debug("deleteSelfDescription.enter; got hash: {}", selfDescriptionHash);
    // TODO: 27.07.2022 The method is not described in the documentation.

    SelfDescriptionMetadata sdMetadata = sdStore.getByHash(selfDescriptionHash);

    checkParticipantAccess(sdMetadata.getIssuer());

    sdStore.deleteSelfDescription(selfDescriptionHash);
    log.debug("deleteSelfDescription.exit; deleted self-description by hash: {}", sdMetadata.getSdHash());
    return new ResponseEntity<>(HttpStatus.OK);
  }

  /**
   * Service method for POST /self-descriptions : Add a new SD to the catalogue.
   *
   * @param selfDescription The new SD (required)
   * @return Created (status code 201)
   *         or The request was accepted but the validation is not finished yet. (status code 202)
   *         or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *         or Forbidden. The user does not have the permission to execute this request. (status code 403)
   *         or May contain hints how to solve the error or indicate what went wrong at the server.
   *         Must not outline any information about the internal structure of the server. (status code 500)
   */
  @Override
  public ResponseEntity<SelfDescription> addSelfDescription(String selfDescription) {
    log.debug("addSelfDescription.enter; got selfDescription: {}", selfDescription);

    try {
     // TODO: 27.07.2022 Need to change the description and the order of actions in the documentation.
     //  The FH scheme is different from the real process.
      ContentAccessorDirect contentAccessor = new ContentAccessorDirect(selfDescription);

      VerificationResultOffering verificationResult = verificationService.verifyOfferingSelfDescription(contentAccessor);

      // TODO: 24.08.2022 Need to set a validator
      SelfDescriptionMetadata sdMetadata = new SelfDescriptionMetadata(contentAccessor,
          verificationResult.getId(), verificationResult.getIssuer(), new ArrayList<>());

      checkParticipantAccess(sdMetadata.getIssuer());

      // TODO: 23.08.2022 The docs states that metadata is returned from this method when SD added.
      sdStore.storeSelfDescription(sdMetadata, verificationResult);

      log.debug("addSelfDescription.exit; returning self-description by hash: {}", sdMetadata.getSdHash());
      // TODO: with CREATED we must properly set Location header
      return new ResponseEntity<>(sdMetadata, HttpStatus.CREATED);
    } catch (ValidationException exception) {
      log.debug("Self-description isn't parsed due to: " + exception.getMessage(), exception);
      throw new ClientException("Self-description isn't parsed due to: " + exception.getMessage());
    }
  }

  /**
   * Service method for POST /self-descriptions/{self_description_hash}/revoke :
   * Change the lifecycle state of a SD to revoke.
   *
   * @param selfDescriptionHash Hash of the self-description (required)
   * @return Revoked (status code 200)
   *         or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *         or Forbidden. The user does not have the permission to execute this request. (status code 403)
   *         or May contain hints how to solve the error or indicate what went wrong at the server.
   *         Must not outline any information about the internal structure of the server. (status code 500)
   */
  @Override
  public ResponseEntity<SelfDescription> updateSelfDescription(String selfDescriptionHash) {
    log.debug("updateSelfDescription.enter; got hash: {}", selfDescriptionHash);
    // TODO: 27.07.2022 Need to change the description and the order of actions in the documentation
    //  (The documentation specifies a search by credential object, not by hash.)

    SelfDescriptionMetadata sdMetadata = sdStore.getByHash(selfDescriptionHash);

    checkParticipantAccess(sdMetadata.getIssuer());

    if (sdMetadata.getStatus().equals(SelfDescriptionStatus.ACTIVE)) {
      sdStore.changeLifeCycleStatus(sdMetadata.getSdHash(), SelfDescriptionStatus.DEPRECATED);
    }

    // TODO: 23.07.2022 Add to the interface / a new interface for working with files
    //  and include a method for adding a self-description file there + Awaiting GraphDd repository interface.

    log.debug("updateSelfDescription.exit; update self-description by hash: {}", selfDescriptionHash);
    return new ResponseEntity<>(sdMetadata, HttpStatus.OK);
  }

  /**
   * Internal service method for checking user access to a particular Participant.
   *
   * @param sdIssuer The Participant issuer of SD (required).
   */
  // TODO: 24.08.2022 It is required to rewrite the logic when the SD parsing methods are ready.
  private void checkParticipantAccess(String sdIssuer) {
    String sessionParticipantId = getSessionParticipantId();
    if (Objects.isNull(sdIssuer) || Objects.isNull(sessionParticipantId) || !sdIssuer.equals(sessionParticipantId)) {
      log.debug("checkParticipantAccess; The user does not have access to the specified participant."
          + " User participant id = {}, self-description participant id = {}.", sessionParticipantId, sdIssuer);
      throw new AccessDeniedException("The user does not have access to the specified participant.");
    }
  }

  private boolean isNotNullObjects(Object... objs) {
    return Arrays.stream(objs).anyMatch(x-> !Objects.isNull(x));
  }

  private SdFilter setupSdFilter(String id, String hash, Integer limit, Integer offset, SelfDescriptionStatus status, String issuer,
                                 String validator, String uploadTr, String statusTr) {
    SdFilter filterParams = new SdFilter();
    filterParams.setId(id);
    filterParams.setHash(hash);
    filterParams.setLimit(limit);
    filterParams.setOffset(offset);
    filterParams.setStatus(status);
    filterParams.setIssuer(issuer);
    filterParams.setValidator(validator);
    if (uploadTr != null) {
      String[] timeRanges = parseTimeRange(uploadTr);
      filterParams.setUploadTimeStart(Instant.parse(timeRanges[0]));
      filterParams.setUploadTimeEnd(Instant.parse(timeRanges[1]));
    }
    if (statusTr != null) {
      String[] timeRanges = parseTimeRange(statusTr);
      filterParams.setStatusTimeStart(Instant.parse(timeRanges[0]));
      filterParams.setStatusTimeEnd(Instant.parse(timeRanges[1]));
    }
    return filterParams;
  }
}