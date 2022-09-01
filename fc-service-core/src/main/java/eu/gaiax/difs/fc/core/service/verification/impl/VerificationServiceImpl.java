package eu.gaiax.difs.fc.core.service.verification.impl;

import com.github.jsonldjava.utils.JsonUtils;
import eu.gaiax.difs.fc.core.exception.VerificationException;
import eu.gaiax.difs.fc.core.pojo.*;
import eu.gaiax.difs.fc.core.service.verification.VerificationService;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

// TODO: 26.07.2022 Awaiting approval and implementation by Fraunhofer.
/**
 * Implementation of the {@link VerificationService} interface.
 */
@Service
public class VerificationServiceImpl implements VerificationService {
  private static final String credentials_key = "verifiableCredential";

  /**
   * The function validates the Self-Description as JSON and tries to parse the json handed over.
   *
   * @param payload ContentAccessor to SD which should be syntactically validated.
   * @return a Participant metadata validation result. If the validation fails, the reason explains the issue.
   */
  @Override
  public VerificationResultParticipant verifyParticipantSelfDescription(ContentAccessor payload) throws VerificationException {
    Map<String, Object> parsed = parseSD(payload);
    String id = (String) parsed.get("id");
    String name = (String) parsed.get("holder");
    Map<String, Object> proof = (Map<String, Object>) parsed.get("proof");
    String key = (String) proof.get("verificationMethod");
    return new VerificationResultParticipant(
            name,
            id,
            key,
            OffsetDateTime.now(),
            "lifecycle",
            LocalDate.MIN,
            new ArrayList<>(),
            new ArrayList<>()
    );
  }

  /**
   * The function validates the Self-Description as JSON and tries to parse the json handed over.
   *
   * @param payload ContentAccessor to SD which should be syntactically validated.
   * @return a Verification result. If the verification fails, the reason explains the issue.
   */
  @Override
  public VerificationResultOffering verifyOfferingSelfDescription(ContentAccessor payload) throws VerificationException {
    String id = "";
    OffsetDateTime verificationTimestamp = OffsetDateTime.now();
    String lifecycleStatus = "";
    String participantID = "";
    LocalDate issuedDate = null;
    List<Signature> signatures = new ArrayList<>();
    List<SdClaim> claims = new ArrayList<>();

    //Verify Syntax and parse json
    Map<String, Object> parsedSD = parseSD(payload);

    //TODO: Verify Cryptographic FIT-WI
    participantID = getParticipantIDFromSD(parsedSD);
//    signatures = validateCryptographic(parsedSD);

    //TODO: Check if API-User is allowed to submit the self-description FIT-WI

//    parsedSD = cleanSD(parsedSD);

    //TODO: Verify Schema FIT-DSAI

    //TODO: Extract Claims FIT-DSAI


    return new VerificationResultOffering(
            id,
            participantID,
            verificationTimestamp,
            lifecycleStatus,
            issuedDate,
            signatures,
            claims
    );
  }

  /**
   * The function validates the Self-Description as JSON and tries to parse the json handed over.
   *
   * @param payload ContentAccessor to SD which should be syntactically validated.
   * @return a Self-Description metadata validation result. If the validation fails, the reason explains the issue.
   */
  @Override
  public VerificationResult verifySelfDescription(ContentAccessor payload) throws VerificationException {
    return new VerificationResultOffering(
            "id",
            "issuer",
            OffsetDateTime.now(),
            "lifecycle",
            LocalDate.MIN,
            new ArrayList<>(),
            new ArrayList<>()
    );
  }

  /*package private functions*/

  Map<String, Object> parseSD (ContentAccessor payload) throws VerificationException {
    String json = payload.getContentAsString();
    Object parsed;
    try {
      parsed = JsonUtils.fromString(json);
    } catch (IOException e) {
      throw new VerificationException(e.getMessage());
    }

    return (Map<String, Object>) parsed;
  }

  List<Signature> validateCryptographic (Map<String, Object> sd) throws VerificationException {
    List<Signature> signatures = new ArrayList<>();
    //Validate VP's signature
    signatures.add(hasSignature(sd));


    //For Each VC: Validate VC's signature
    List<Map<String, Object>> credentials = (List<Map<String, Object>>) sd.get(credentials_key);
    for (Map<String, Object> credential: credentials) {
      signatures.add(hasSignature(credential));
    }

    return signatures;
  }

  Map<String, Object> cleanSD (Map<String, Object> sd) {

    //TODO remove proofs
    sd.remove("proof");
    ArrayList<Map<String, Object>> credentials = (ArrayList<Map<String, Object>>) sd.get(credentials_key);
    for (Map<String, Object> credential : credentials) {
      credential.remove("proof");
    }

    return sd;
  }

  String getParticipantIDFromSD (Map<String, Object> sd)  {
    //TODO: check if participant ID is matching and extract it from SD
    return null;
  }

  Signature hasSignature (Map<String, Object> cred) {
    if (cred == null || cred.isEmpty()) {
      throw new VerificationException("the credential is empty");
    }

    if(! cred.containsKey("proof")) {
      throw new VerificationException("no proof found");
    }

    Map<String,Object> proofLHM = (Map<String,Object> ) cred.get("proof");
    String type = (String) proofLHM.get("type");
    if(type == null || !type.equals("JsonWebSignature2020")) {
      throw new VerificationException("wrong type of proof, type is: " + type);
    }

    String created = (String) proofLHM.get("created");
    if(created == null) {
      throw new VerificationException("created timestamp not found");
    }
    if (created.isEmpty() || created.isBlank()) {
      throw new VerificationException("created timestamp is empty");
    }
    try {
      ZonedDateTime time = ZonedDateTime.parse(created);
      if (time.isAfter(ZonedDateTime.now())){
        throw new VerificationException("Signature was created in the future");
      }
    } catch (DateTimeParseException e) {
      throw new VerificationException("cannot parse timestamp");
    }

    String verificationMethod = (String) proofLHM.get("verificationMethod");
    if(verificationMethod == null) {
      throw new VerificationException("verificationMethod not found");
    }
    if (verificationMethod.isEmpty() || verificationMethod.isBlank()) {
      throw new VerificationException("verificationMethod is empty");
    }

    String proofPurpose = (String) proofLHM.get("proofPurpose");
    if(proofPurpose == null) {
      throw new VerificationException("proofPurpose not found");
    }
    if (proofPurpose.isEmpty() || proofPurpose.isBlank()) {
      throw new VerificationException("proofPurpose is empty");
    }

    String jws = (String) proofLHM.get("jws");
    if(jws == null) {
      throw new VerificationException("jws not found");
    }
    if (jws.isEmpty() || jws.isBlank()) {
      throw new VerificationException("jws is empty");
    }

    return new Signature(
            //TODO
    );
  }
}
