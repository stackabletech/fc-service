package eu.gaiax.difs.fc.server.service;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.gaiax.difs.fc.api.generated.model.Participant;
import eu.gaiax.difs.fc.api.generated.model.UserProfile;
import eu.gaiax.difs.fc.core.dao.ParticipantDao;
import eu.gaiax.difs.fc.core.exception.ClientException;
import eu.gaiax.difs.fc.core.exception.NotFoundException;
import eu.gaiax.difs.fc.server.generated.controller.ParticipantsApiDelegate;
import lombok.extern.slf4j.Slf4j;

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
  private ObjectMapper jsonMapper;

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
    Participant part = toParticipant(body);
    partDao.create(part);
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
    Participant part = partDao.delete(participantId)
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
    Participant part = partDao.select(participantId)
        .orElseThrow(() -> new NotFoundException("Participant not found: " + participantId));
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
  public ResponseEntity<List<UserProfile>> getParticipantUsers(String participantId) {
    log.debug("getParticipantUsers.enter; got participantId: {}", participantId);
    List<UserProfile> profiles = partDao.selectUsers(participantId)
        .orElseThrow(() -> new NotFoundException("Participant not found: " + participantId));
    log.debug("getParticipantUsers.exit; returning: {}", profiles.size());
    return ResponseEntity.ok(profiles);
  }

  /**
   * GET /participants : Get the registered participants.
   *
   * @param offset The number of items to skip before starting to collect the result set. (optional, default to 0)
   * @param limit The number of items to return. (optional, default to 100)
   * @param orderBy Results will be sorted by this field. (optional)
   * @param asc Ascending/Descending ordering. (optional, default to true)
   * @return List of registered participants (status code 200)
   *         or May contain hints how to solve the error or indicate what was wrong in the request. (status code 400)
   *         or May contain hints how to solve the error or indicate what went wrong at the server.
   *         Must not outline any information about the internal structure of the server. (status code 500)
   */
  @Override
  public ResponseEntity<List<Participant>> getParticipants(Integer offset, Integer limit, String orderBy, Boolean asc) {
    // sorting is not supported yet by keycloak admin API
    log.debug("getParticipants.enter; got offset: {}, limit: {}", offset, limit);
    List<Participant> parts = partDao.search(offset, limit);
    log.debug("getParticipants.exit; returning: {}", parts.size());
    return ResponseEntity.ok(parts);
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
    Participant part = toParticipant(body);
    part = partDao.update(participantId, part)
        .orElseThrow(() -> new NotFoundException("Participant not found: " + participantId));
    log.debug("updateParticipant.exit; returning: {}", part);
    return ResponseEntity.ok(part);
  }

  /**
   * Utility method for converting Participant Self-Description to Participant metadata.
   *
   * @param sd Self-Description.
   * @return Participant metadata.
   */
  private Participant toParticipant(String sd) {
    Map<String, Object> map;
    try {
      map = jsonMapper.readValue(sd, MAP_TYPE_REF);
    } catch (JsonProcessingException ex) {
      throw new ClientException(ex.getMessage());
    }

    String id = (String) map.get("id");
    String name = (String) map.get("holder");
    Map<String, Object> proof = (Map<String, Object>) map.get("proof");
    String key = (String) proof.get("verificationMethod");
    return new Participant(id, name, key, sd);
  }

}
