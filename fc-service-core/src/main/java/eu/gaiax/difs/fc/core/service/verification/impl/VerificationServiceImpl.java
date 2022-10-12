package eu.gaiax.difs.fc.core.service.verification.impl;

import com.apicatalog.rdf.RdfNQuad;
import com.apicatalog.rdf.RdfValue;
import com.danubetech.keyformats.JWK_to_PublicKey;
import com.danubetech.keyformats.crypto.PublicKeyVerifier;
import com.danubetech.keyformats.crypto.PublicKeyVerifierFactory;
import com.danubetech.keyformats.jose.JWK;
import com.danubetech.keyformats.keytypes.KeyTypeName_for_JWK;
import com.danubetech.verifiablecredentials.CredentialSubject;
import com.danubetech.verifiablecredentials.VerifiableCredential;
import com.danubetech.verifiablecredentials.VerifiablePresentation;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import eu.gaiax.difs.fc.api.generated.model.SelfDescriptionStatus;
import eu.gaiax.difs.fc.core.exception.VerificationException;
import eu.gaiax.difs.fc.core.pojo.*;
import eu.gaiax.difs.fc.core.service.schemastore.SchemaStore;
import eu.gaiax.difs.fc.core.service.verification.VerificationService;
import foundation.identity.did.DIDDocument;
import foundation.identity.jsonld.JsonLDException;
import foundation.identity.jsonld.JsonLDObject;
import info.weboftrust.ldsignatures.LdProof;
import info.weboftrust.ldsignatures.verifier.JsonWebSignature2020LdVerifier;
import info.weboftrust.ldsignatures.verifier.LdVerifier;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.topbraid.shacl.validation.ValidationUtil;
import org.topbraid.shacl.vocabulary.SH;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO: 26.07.2022 Awaiting approval and implementation by Fraunhofer.
/**
 * Implementation of the {@link VerificationService} interface.
 */
@Slf4j
@Component
public class VerificationServiceImpl implements VerificationService {
  private static final String sd_format = "JSONLD";
  private static final String shapes_format = "TURTLE";
  private static final String[] TYPE_KEYS = {"type", "types", "@type", "@types"};
  private static final String[] ID_KEYS = {"id", "@id"};
  private static final String PARTICIPANT_TYPE = "LegalPerson";
  private static final String SERVICE_OFFERING_TYPE = "ServiceOfferingExperimental";

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
    Pair<Boolean, Boolean> type = getSDType(presentation);

    if (type.getLeft()) {
      return verifyParticipantSelfDescription(presentation);
    } else if (type.getRight()) {
      return verifyOfferingSelfDescription(presentation);
    } else {
      throw new VerificationException("SD is neither a Participant SD nor a ServiceOffer SD");
    }
  }

  @Override
  public boolean checkValidator(Validator validator) {
    //Todo delete this function as it's unused?
    //check if pubkey is the same
    //check if pubkey is trusted
    return true; //if all checks succeeded the validator is valid
  }

  /*package private functions*/
  private VerificationResultParticipant verifyParticipantSelfDescription(VerifiablePresentation presentation) throws VerificationException {
    try {
      checkCryptographic(presentation);
    } catch (JsonLDException | GeneralSecurityException | IOException | ParseException e) {
      throw new VerificationException(e);
    }

    VerifiableCredential credential = getCredential(presentation);
    String id = getID(credential);
    String name = presentation.getHolder().toString();
    Map<String, Object> proof = presentation.getLdProof().toMap();
    String key = (String) proof.get("verificationMethod");

    CredentialSubject credentialSubjects = getCredentialSubjects(credential);
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
    try {
      checkCryptographic(presentation);
    } catch (JsonLDException | GeneralSecurityException | IOException | ParseException e) {
      throw new VerificationException(e);
    }

    VerifiableCredential credential = getCredential(presentation);
    String id = getID(credential);
    OffsetDateTime verificationTimestamp = OffsetDateTime.now();
    // TODO: what it is for?
    String participantID = "http://example.org/test-issuer";
    LocalDate issuedDate = null;
    List<Validator> validators = new ArrayList<>();

    CredentialSubject credentialSubjects = getCredentialSubjects(credential);
    List<SdClaim> claims = extractClaims(presentation);

    URI issuer = credential.getIssuer();
    if (issuer == null) {
      participantID = null;
    } else {
      participantID = credential.getIssuer().toString();
    }

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
  /**
   * A method that returns a list of claims given a self-description's VerifiablePresentation
   *
   * @param presentation a self-description as Verifiable Presentation for claims extraction
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

  private VerifiablePresentation parseSD(ContentAccessor accessor) {
    try {
      VerifiablePresentation vp = VerifiablePresentation.fromJson(accessor.getContentAsString());

      if (vp == null) {
        throw new VerificationException("invalid VerifiablePresentation");
      }

      VerifiableCredential vc = getCredential(vp);
      if (vc == null) {
        throw new VerificationException("invalid VerifiableCredential");
      }
      CredentialSubject cs = vc.getCredentialSubject();
      if (cs == null) {
        throw new VerificationException("invalid CredentialSubject");
      }
      return vp;
    } catch (VerificationException e) {
      throw e;
    } catch (Exception e) {
      throw new VerificationException("Parsing of SD failed", e);
    }
  }

  private Pair<Boolean, Boolean> getSDType (VerifiablePresentation presentation) {
    boolean isParticipant = false;
    boolean isServiceOffering = false;

    try {
      VerifiableCredential credential = getCredential(presentation);
      CredentialSubject cs = getCredentialSubjects(credential);
      for (String key : TYPE_KEYS) {
        Object _type = cs.getJsonObject().get(key);
        if (_type == null) continue;

        List<String> types;
        if (_type instanceof List) {
          types = (List<String>) _type;
        } else {
          types = new ArrayList<>(1);
          types.add((String) _type);
        }

        for (String type : types) {
          if (type.contains(PARTICIPANT_TYPE)) {
            isParticipant = true;
          }
          if (type.contains(SERVICE_OFFERING_TYPE)) {
            isServiceOffering = true;
          }
        }
      }
    } catch (Exception e) {
      throw new VerificationException("Could not extract SD's type", e);
    }

    if (isParticipant && isServiceOffering) {
      throw new VerificationException("SD is both, a participant and an offering SD");
    }

    return Pair.of(isParticipant, isServiceOffering);
  }

  boolean isSDServiceOffering (VerifiablePresentation presentation) {
    return getSDType(presentation).getRight();
  }

  boolean isSDParticipant (VerifiablePresentation presentation) {
    return getSDType(presentation).getLeft();
  }

  //This function becomes obsolete when a did resolver will be available
  //https://gitlab.com/gaia-x/lab/compliance/gx-compliance/-/issues/13
  private static DIDDocument readDIDfromURI (URI uri) throws IOException {
    String [] uri_parts = uri.getSchemeSpecificPart().split(":");
    String did_json;
    if(uri_parts[0].equals("web")) {
      String [] _parts = uri_parts[1].split("#");
      URL url;
      if (_parts.length == 1) {
        url = new URL("https://" + _parts[0] +"/.well-known/did.json");
      } else {
        url = new URL("https://" + _parts[0] +"/.well-known/did.json" + _parts[1]);
      }
      InputStream stream = url.openStream();
      did_json = IOUtils.toString(stream, StandardCharsets.UTF_8);
    } else {
      throw new VerificationException("Couldn't load key. Origin not supported");
    }
    return DIDDocument.fromJson(did_json);
  }

  private Map<String, Object> extractRelevantVerificationMethod (List<Map<String, Object>> methods, URI verificationMethodURI) {
    return methods.get(0);
    //TODO wait for answer https://gitlab.com/gaia-x/lab/compliance/gx-compliance/-/issues/22
  }

  private Map<String, Object> extractRelevantValues (Map<String, Object> map) {
    Map<String, Object> new_map = new HashMap<>();
    String [] relevants = {"kty", "d", "e", "kid", "use", "x", "y", "n", "crv"};
    for (String relevant:relevants) {
      if (map.containsKey(relevant)) {
        new_map.put(relevant, map.get(relevant));
      }
    }
    return new_map;
  }

  private String getAlgorithmFromJWT(String s) throws ParseException {
    JWT jwt = JWTParser.parse(s);
    return jwt.getHeader().getAlgorithm().getName();
  }

  private Instant hasPEMTrustAnchorAndIsNotDeprecated (String uri) throws IOException {
    //Is the PEM anchor in the registry?
    HttpClient httpclient = HttpClients.createDefault();
    HttpPost httppost = new HttpPost("https://registry.lab.gaia-x.eu/v2206/api/trustAnchor/chain/file");

    // Request parameters and other properties.
    List<NameValuePair> params = new ArrayList<NameValuePair>(1);
    params.add(new BasicNameValuePair("uri", uri));
    httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

    //Execute and get the response.
    HttpResponse response = httpclient.execute(httppost);
    if(response.getStatusLine().getStatusCode() != 200) {
      throw new VerificationException("The trust anchor is not set in the registry. URI: " + uri);
    }

    //TODO deprecation time from pem from URI
    return Instant.now(); // The registry is down, thus this code fails
  }

  private Pair<PublicKeyVerifier, Validator> getVerifiedVerifier(LdProof proof) throws IOException {
    URI uri = proof.getVerificationMethod();
    String jwt = proof.getJws();
    JWK jwk;
    PublicKeyVerifier pubKey;
    Validator validator;

    if (!proof.getType().equals("JsonWebSignature2020")) throw new UnsupportedOperationException("This proof type is not yet implemented");

    if (uri.getScheme().equals("did")) {
      DIDDocument did = readDIDfromURI(uri);

      List<Map<String, Object>> available_jwks = (List<Map<String, Object>>) did.toMap().get("verificationMethod");
      Map<String, Object> jwk_map_uncleaned = (Map<String, Object>) extractRelevantVerificationMethod(available_jwks, uri).get("publicKeyJwk");
      Map<String, Object> jwk_map_cleaned = extractRelevantValues(jwk_map_uncleaned);

      Instant deprecation = hasPEMTrustAnchorAndIsNotDeprecated((String) jwk_map_uncleaned.get("x5u"));

      // use from map and extract only relevant
      jwk = JWK.fromMap(jwk_map_cleaned);

      try {
        pubKey = PublicKeyVerifierFactory.publicKeyVerifierForKey(
                KeyTypeName_for_JWK.keyTypeName_for_JWK(jwk),
                (String) jwk_map_uncleaned.get("alg"),
                JWK_to_PublicKey.JWK_to_anyPublicKey(jwk));
      } catch (IllegalArgumentException ex) {
        throw new VerificationException(ex);
      }
      validator = new Validator(
              uri.toString(),
              JsonLDObject.fromJsonObject(jwk_map_uncleaned).toString(),
              deprecation);
      //Does this help? https://www.baeldung.com/java-read-pem-file-keys#2-get-public-key-from-pem-string
    } else throw new VerificationException("Unknown Verification Method: " + uri);

    return Pair.of(pubKey, validator);
  }

  private Validator checkSignature (JsonLDObject payload) throws JsonLDException, GeneralSecurityException, IOException, ParseException {
    Map<String, Object> proof_map = (Map<String, Object>) payload.getJsonObject().get("proof");
    if (proof_map == null) throw new VerificationException("No proof found");
    LdProof proof = LdProof.fromMap(proof_map);

    try {
      return checkSignature(payload, proof);
    } catch (GeneralSecurityException e) {
      throw new VerificationException("Could not verify signature", e);
    }
  }

  private Validator checkSignature (JsonLDObject payload, LdProof proof) throws JsonLDException, GeneralSecurityException, IOException, ParseException {
    LdVerifier verifier;
    Validator validator = null; //TODO Cache.getValidator(proof.getVerificationMethod().toString());
    if (validator == null) {
      Pair<PublicKeyVerifier, Validator> pkVerifierAndValidator = getVerifiedVerifier(proof);
      PublicKeyVerifier publicKeyVerifier = pkVerifierAndValidator.getLeft();
      validator = pkVerifierAndValidator.getRight();
      verifier = new JsonWebSignature2020LdVerifier(publicKeyVerifier);
    } else {
      verifier = getVerifierFromValidator(validator);
    }
    //TODO    if(!verifier.verify(payload)) throw new VerificationException(payload.getClass().getName() + "does not match with proof");

    return validator;
  }

  private LdVerifier getVerifierFromValidator(Validator validator) throws IOException, ParseException {
    Map<String, Object> jwk_map_uncleaned = JsonLDObject.fromJson(validator.getPublicKey()).getJsonObject();
    Map<String, Object> jwk_map_cleaned = extractRelevantValues(jwk_map_uncleaned);

    // use from map and extract only relevant
    JWK jwk = JWK.fromMap(jwk_map_cleaned);

    PublicKeyVerifier pubKey = PublicKeyVerifierFactory.publicKeyVerifierForKey(
            KeyTypeName_for_JWK.keyTypeName_for_JWK(jwk),
            getAlgorithmFromJWT((String) jwk_map_uncleaned.get("alg")),
            JWK_to_PublicKey.JWK_to_anyPublicKey(jwk));
    return new JsonWebSignature2020LdVerifier(pubKey);
  }

  private List<Validator> checkCryptographic (VerifiablePresentation presentation) throws JsonLDException, GeneralSecurityException, IOException, ParseException {
    List<Validator> validators = new ArrayList<>();

    validators.add(checkSignature(presentation));

    VerifiableCredential credential = getCredential(presentation);
    validators.add(checkSignature(credential));

    //If this point was reached without an exception, the signatures are valid
    return validators;
  }

  private String getParticipantID(VerifiablePresentation presentation) {
    //TODO compare to validators
    VerifiableCredential credential = getCredential(presentation);
    String id = getID(credential);
    return id;
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

  private VerifiableCredential getCredential (VerifiablePresentation presentation) {
    try {
      VerifiableCredential credential = presentation.getVerifiableCredential();

      if (credential != null) {
        return credential;
      }
    } catch (Exception e) {
      //do nothing
    }
    Object _credential = presentation.getJsonObject().get("verifiableCredential");

    if (_credential != null) {
      if (_credential instanceof List) {
        List<Map<String, Object>> credentials = (List<Map<String, Object>>) _credential;
        if (credentials.size() > 0)
          return VerifiableCredential.fromJsonObject(credentials.get(0));
      }
    }

    throw new VerificationException("Could not find a VC in SD");
  }

  private CredentialSubject getCredentialSubjects (VerifiableCredential credential) {
    return credential.getCredentialSubject();
  }

  private String getID (VerifiablePresentation presentation) {
    return getID(presentation.getJsonObject());
  }

  private String getID (VerifiableCredential credential) {
    return getID(credential.getJsonObject());
  }

  private String getID (Map<String, Object> map) {
    for (String key : ID_KEYS) {
      Object id = map.get(key);
      if (id != null) {
        if (id instanceof String) return (String) id;
        if (id instanceof URI) {
          URI uri = (URI) id;
          return uri.toString();
        }
      }
    }
    throw new VerificationException("Could not find String");
  }
}
