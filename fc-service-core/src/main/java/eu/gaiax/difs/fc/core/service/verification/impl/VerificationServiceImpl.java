package eu.gaiax.difs.fc.core.service.verification.impl;

import com.apicatalog.rdf.RdfNQuad;
import com.apicatalog.rdf.RdfValue;
import com.danubetech.verifiablecredentials.CredentialSubject;
import com.danubetech.verifiablecredentials.VerifiableCredential;
import com.danubetech.verifiablecredentials.VerifiablePresentation;
import eu.gaiax.difs.fc.api.generated.model.SelfDescriptionStatus;
import eu.gaiax.difs.fc.core.exception.ParserException;
import eu.gaiax.difs.fc.core.exception.ServiceException;
import eu.gaiax.difs.fc.core.exception.VerificationException;
import eu.gaiax.difs.fc.core.pojo.*;
import eu.gaiax.difs.fc.core.service.schemastore.SchemaStore;
import eu.gaiax.difs.fc.core.service.verification.VerificationService;
import foundation.identity.jsonld.JsonLDException;

import java.io.Reader;
import java.io.StringReader;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.topbraid.shacl.validation.ValidationUtil;
import org.topbraid.shacl.vocabulary.SH;

// TODO: 26.07.2022 Awaiting approval and implementation by Fraunhofer.
/**
 * Implementation of the {@link VerificationService} interface.
 */
@Slf4j
@Component
public class VerificationServiceImpl implements VerificationService {
  private static final String credentials_key = "verifiableCredential";
  private static final String[] type_keys = {"type", "types", "@type", "@types"};
  private static final String participant_type = "LegalPerson";
  private static final String serviceOfferingType = "ServiceOfferingExperimental";
  private static final String sd_format = "JSONLD";
  private static final String shapes_format = "TURTLE";

  @Autowired
  private SchemaStore schemaStore;
  
  /**
   * The function validates the Self-Description as JSON and tries to parse the json handed over.
   *
   * @param payload ContentAccessor to SD which should be syntactically validated.
   * @return a Participant metadata validation result. If the validation fails, the reason explains the issue.
   */
  @Override
  public VerificationResultParticipant verifyParticipantSelfDescription(ContentAccessor payload) throws VerificationException {
    VerifiablePresentation presentation = parseSD(payload);
    if (!isSDParticipant(presentation)) {
      String msg = "Expected Participant SD, got: ";

      if (isSDServiceOffering(presentation)) msg += "Service Offering SD";
      else msg += "Unknown SD";

      throw new VerificationException(msg);
    }
    
    // TODO: make it workable
    //validationAgainstShacl(payload);
    return verifyParticipantSelfDescription(presentation);
  }

  /**
   * The function validates the Self-Description as JSON and tries to parse the json handed over.
   *
   * @param payload ContentAccessor to SD which should be syntactically validated.
   * @return a Verification result. If the verification fails, the reason explains the issue.
   */
  @Override
  public VerificationResultOffering verifyOfferingSelfDescription(ContentAccessor payload) throws VerificationException {
    VerifiablePresentation presentation = parseSD(payload);
    if (!isSDServiceOffering(presentation)) {
      String msg = "Expected Service Offering SD, got: ";

      if (isSDParticipant(presentation)) msg += "Participant SD";
      else msg += "Unknown SD";

      throw new VerificationException(msg);
    }

    // TODO: make it workable
    //validationAgainstShacl(payload);
    return verifyOfferingSelfDescription(presentation);
  }

  /**
   * The function validates the Self-Description as JSON and tries to parse the json handed over.
   *
   * @param payload ContentAccessor to SD which should be syntactically validated.
   * @return a Self-Description metadata validation result. If the validation fails, the reason explains the issue.
   */
  @Override
  public VerificationResult verifySelfDescription(ContentAccessor payload) throws VerificationException {
    VerifiablePresentation presentation = parseSD(payload);

    // TODO: make it workable
    //validationAgainstShacl(payload);
    Map<String, Boolean> type = getSDType(presentation);

    if(type.get("participant")) {
      return verifyParticipantSelfDescription(presentation);
    } else if (type.get("offering")) {
      return verifyOfferingSelfDescription(presentation);
    } else {
      throw new VerificationException("SD is neither a Participant SD nor a ServiceOffer SD");
    }
  }

  @Override
  public boolean checkValidator(Validator validator) {
    //check if pubkey is the same
    //check if pubkey is trusted
    return true; //if all checks succeeded the validator is valid
  }

  /*package private functions*/
  private VerificationResultParticipant verifyParticipantSelfDescription(VerifiablePresentation presentation) throws VerificationException {
    // TODO: use VC.id when we'll fix URI processing  
    //String id = presentation.getVerifiableCredential().getId().toString();
    String id = presentation.getId().toString();
    String name = presentation.getHolder().toString();
    Map<String, Object> proof = presentation.getLdProof().toMap();
    String key = (String) proof.get("verificationMethod");

    //TODO: Verify Cryptographic FIT-WI
    Pair<List<CredentialSubject>, String> claimExtractionResult = extractCredentialSubjects(presentation);
    List<CredentialSubject> credentialSubjects = claimExtractionResult.getLeft();
    List<SdClaim> claims = extractClaims(presentation);

    /*
    Maybe one of these methods can help you
    credentialSubjects.get(0).getClaims();
    credentialSubjects.get(0).getJsonObject();
    */

    //TODO: Extract Claims FIT-DSAI

    //TODO: Verify Schema FIT-DSAI

    return new VerificationResultParticipant(
            name,
            id,
            key,
            OffsetDateTime.now(),
            SelfDescriptionStatus.ACTIVE.getValue(),
            LocalDate.MIN,
            new ArrayList<>(),
            claims
    );
  }

  private VerificationResultOffering verifyOfferingSelfDescription(VerifiablePresentation presentation) throws VerificationException {
    String id = presentation.getVerifiableCredential().getId().toString();
    OffsetDateTime verificationTimestamp = OffsetDateTime.now();
    // TODO: what it is for?
    String participantID = "http://example.org/test-issuer";
    LocalDate issuedDate = null;
    List<Validator> validators = new ArrayList<>();

    //TODO: Verify Cryptographic FIT-WI

    Pair<List<CredentialSubject>, String> claimExtractionResult = extractCredentialSubjects(presentation);
    List<CredentialSubject> credentialSubjects = claimExtractionResult.getLeft();
    List<SdClaim> claims = extractClaims(presentation);

    participantID = claimExtractionResult.getRight();
    // why not: presentation.getVerifiableCredential().getIssuer() ?

    //TODO: Extract Claims FIT-DSAI

    //TODO: Verify Schema FIT-DSAI

    return new VerificationResultOffering(
            id,
            participantID,
            verificationTimestamp,
            SelfDescriptionStatus.ACTIVE.getValue(),
            issuedDate,
            validators,
            claims
    );
  }

  /*package private functions*/
  Pair<List<CredentialSubject>, String> extractCredentialSubjects(VerifiablePresentation presentation) {
    ArrayList<Map<String, Object>> credential_list =
            (ArrayList<Map<String, Object>>) presentation.getJsonObject().get("verifiableCredential");
    List<CredentialSubject> credentialSubjects = new ArrayList<>(credential_list.size());
    String id = null;
    String issuer = null;

    for (Map<String, Object> credential_map : credential_list) {
      VerifiableCredential credential = VerifiableCredential.fromJsonObject(credential_map);
      CredentialSubject credentialSubject = credential.getCredentialSubject();

      for (String key : type_keys) {
        Object _type = credentialSubject.getJsonObject().get(key);
        if (_type == null) continue;

        List<String> types;
        if (_type instanceof List) {
          types = (ArrayList<String>) _type;
        } else {
          types = new ArrayList<>(1);
          types.add((String) _type);
        }

        for (String type : types) {
          if (type.equals(participant_type) || type.equals(serviceOfferingType)) {
            if (id == null) {
              id = (String) credentialSubject.getJsonObject().getOrDefault("id",
                      credentialSubject.getJsonObject().get("@id"));
              if (id == null) continue;
            } else {
              String _id = (String) credentialSubject.getJsonObject().getOrDefault("id",
                      credentialSubject.getJsonObject().get("@id"));
              if (_id == null) continue;

              if (!id.equals(_id)) {
                throw new VerificationException("The SDs credential Subjects affect different IDs");
              }
            }
            issuer = credential.getIssuer().toString();
            credentialSubjects.add(credentialSubject);
          }
        }
      }
    }

    return Pair.of(credentialSubjects, issuer);
  }
  
  /**
   * A method that returns a list of claims given a self-description's VerifiablePresentation
   *
   * @param sd map represents a self-description for claims extraction
   * @return a list of claims.
   */
   List<SdClaim> extractClaims(VerifiablePresentation presentation) {

     log.debug("extractClaims.enter; got Presentation: {}", presentation);
     List<SdClaim> claims = new ArrayList<>();
     try {
       CredentialSubject cs = presentation.getVerifiableCredential().getCredentialSubject();
       for (RdfNQuad nquad: cs.toDataset().toList()) {
         log.debug("extractClaims; got NQuad: {}", nquad);
         if (nquad.getSubject().isIRI()) {
           String sub = rdf2String(nquad.getSubject());
           if (sub != null) {
             String pre = rdf2String(nquad.getPredicate());
             if (pre != null) {
               String obj = rdf2String(nquad.getObject());
               if (obj != null) {
                 SdClaim claim = new SdClaim(sub, pre, obj);
                 claims.add(claim);
               }
             }
           }  
         }
       }
     } catch (JsonLDException ex) {
       throw new VerificationException("error extracting claims: " + ex.getMessage());
     }
     log.debug("extractClaims.exit; returning claims: {}", claims);
     return claims;
  }
  
  private String rdf2String(RdfValue rdf) {
     if (rdf.isBlankNode()) return rdf.getValue();
     if (rdf.isLiteral()) return "\"" + rdf.getValue() + "\"";
     // this is IRI, Neo4J does not process namespaced IRIs yet 
     if (rdf.getValue().startsWith("http://") || rdf.getValue().startsWith("https://")) return "<" + rdf.getValue() + ">";
     return null;
  }

  VerifiablePresentation parseSD(ContentAccessor accessor) {
    try {
      VerifiablePresentation vp = VerifiablePresentation.fromJson(accessor.getContentAsString()
              .replaceAll("JsonWebKey2020", "JsonWebSignature2020"));
      //This has to be done to handle current examples. In the final code the replacement becomes obsolete
      //TODO remove replace
      
      if (vp == null) {
        throw new VerificationException("invalid VerifiablePresentation");
      }
      VerifiableCredential vc = vp.getVerifiableCredential();
      if (vc == null) {
        throw new VerificationException("invalid VerifiableCredential");
      }
      CredentialSubject cs = vc.getCredentialSubject();
      if (cs == null) {
        throw new VerificationException("invalid CredentialSubject");
      }
      return vp;
    } catch (ServiceException e) {
      throw e;
    } catch (RuntimeException e) {
      throw new VerificationException(e);
    }
  }

  Map<String, Boolean> getSDType (VerifiablePresentation presentation) {
    boolean isParticipant = false;
    boolean isServiceOffering = false;

    List<Map<String, Object>> vcs = (List<Map<String, Object>>) presentation.getJsonObject().get("verifiableCredential");

    for (Map<String, Object> vc : vcs) {
      VerifiableCredential credential = VerifiableCredential.fromJsonObject(vc);

      for (String key : type_keys) {
        Object _type = credential.getCredentialSubject().getJsonObject().get(key);
        if (_type == null) continue;

        List<String> types;
        if (_type instanceof List) {
          types = (ArrayList<String>) _type;
        } else {
          types = new ArrayList<>(1);
          types.add((String) _type);
        }

        for (String type : types) {
          if (type.contains(participant_type)) {
            isParticipant = true;
          }
          if (type.contains(serviceOfferingType)) {
            isServiceOffering = true;
          }
        }
      }
    }

    if (isParticipant && isServiceOffering) {
      throw new VerificationException("SD is both, a participant and an offering SD");
    }

    boolean finalIsParticipant = isParticipant;
    boolean finalIsServiceOffering = isServiceOffering;

    return new HashMap<>() {{
      put("participant", finalIsParticipant);
      put("offering", finalIsServiceOffering);
    }};
  }

  boolean isSDServiceOffering (VerifiablePresentation presentation) {
    return getSDType(presentation).get("offering").booleanValue();
  }

  boolean isSDParticipant (VerifiablePresentation presentation) {
    return getSDType(presentation).get("participant").booleanValue();
  }

  void validateCryptographic (Map<String, Object> sd) throws VerificationException {
    hasSignature(sd);

    //For Each VC: Validate VC's signature
    List<Map<String, Object>> credentials = (List<Map<String, Object>>) sd.get(credentials_key);
    for (Map<String, Object> credential: credentials) {
      hasSignature(credential);
    }
  }

  Map<String, Object> cleanSD (Map<String, Object> sd) {
    sd.remove("proof");
    ArrayList<Map<String, Object>> credentials = (ArrayList<Map<String, Object>>) sd.get(credentials_key);
    for (Map<String, Object> credential : credentials) {
      credential.remove("proof");
    }

    return sd;
  }

  String getParticipantIDFromSD (Map<String, Object> sd)  {
    // TODO: 05.09.2022 Test implementation for passing tests.
    //  It is required to replace the method when the logic from FH is ready
    try {
      if (!sd.isEmpty() && sd.containsKey("verifiableCredential")) {
        for (Map<String, Object> v : (ArrayList<Map<String, Object>>) sd.get("verifiableCredential")) {
          if (v.containsKey("credentialSubject")) {
            Map<String, Object> credentialSubjectNode = (Map<String, Object>) v.get("credentialSubject");
            if (credentialSubjectNode.containsKey("@type")
                && Arrays.asList("gax:Provider", "gax:Consumer", "gax:FederationService", "gax:ServiceOffering")
                .contains(credentialSubjectNode.get("@type").toString())) {
              String participantId = credentialSubjectNode.get("@id").toString();
              log.debug("getParticipantIDFromSD.exit; returning participantId {}.", participantId);
              return participantId;
            }
          }
        }
      }
    } catch (Exception exception) {
      log.error("Self-description doesn't contain information about participant.", exception);
      throw new ParserException("Self-description doesn't contain information about participant.", exception);
    }
    return null;
  }

  void hasSignature (Map<String, Object> cred) {
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
  }
  
  /**
   * Method that validates a dataGraph against shaclShape
   *
   * @param payload    ContentAccessor of a self-Description payload to be validated
   * @param shaclShape ContentAccessor of a union schemas of type SHACL
   * @return                SemanticValidationResult object
   */
  SemanticValidationResult validationAgainstShacl(ContentAccessor payload, ContentAccessor shaclShape) {
      Reader dataGraphReader = new StringReader(payload.getContentAsString());
      Reader shaclShapeReader = new StringReader(shaclShape.getContentAsString());
      Model data = ModelFactory.createDefaultModel();
      data.read(dataGraphReader, null, sd_format);
      Model shape = ModelFactory.createDefaultModel();
      shape.read(shaclShapeReader, null, shapes_format);
      Resource reportResource = ValidationUtil.validateModel(data, shape, true);
      boolean conforms = reportResource.getProperty(SH.conforms).getBoolean();
      String report = null;
      if (!conforms) {
          report = reportResource.getModel().toString();
      }
      return new SemanticValidationResult(conforms, report);
  }
  
  private void validationAgainstShacl(ContentAccessor payload) {
    ContentAccessor shaclShape = schemaStore.getCompositeSchema(SchemaStore.SchemaType.SHAPE);
    SemanticValidationResult result = validationAgainstShacl(payload, shaclShape);
    log.debug("validationAgainstShacl.exit; conforms: {}; model: {}", result.isConforming(), result.getValidationReport());
    if (!result.isConforming()) {
      throw new VerificationException("shacl shape schema violated");  
    }
  }
  
  
}

