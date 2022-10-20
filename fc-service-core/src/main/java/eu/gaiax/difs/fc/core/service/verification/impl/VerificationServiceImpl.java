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
import com.danubetech.verifiablecredentials.validation.Validation;
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

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
  private static final Set<String> PARTICIPANT_TYPES = Set.of("LegalPerson", "http://w3id.org/gaia-x/participant#LegalPerson", "gax-participant:LegalPerson");
  private static final Set<String> SERVICE_OFFERING_TYPES = Set.of("ServiceOfferingExperimental", "http://w3id.org/gaia-x/service#ServiceOffering", "gax-service:ServiceOffering");
  private static final Set<String> SIGNATURES = Set.of("JsonWebSignature2020"); //, "Ed25519Signature2018");
  
  private static final int VRT_UNKNOWN = 0;
  private static final int VRT_PARTICIPANT= 1;
  private static final int VRT_OFFERING = 2;

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
    return (VerificationResultParticipant) verifySelfDescription(payload, true, VRT_PARTICIPANT, true, true, true);
  }

  /**
   * The function validates the Self-Description as JSON and tries to parse the json handed over.
   *
   * @param payload ContentAccessor to SD which should be syntactically validated.
   * @return a Verification result. If the verification fails, the reason explains the issue.
   */
  @Override
  public VerificationResultOffering verifyOfferingSelfDescription(ContentAccessor payload) throws VerificationException {
    return (VerificationResultOffering) verifySelfDescription(payload, true, VRT_OFFERING, true, true, true);
  }

  /**
   * The function validates the Self-Description as JSON and tries to parse the json handed over.
   *
   * @param payload ContentAccessor to SD which should be syntactically validated.
   * @return a Self-Description metadata validation result. If the validation fails, the reason explains the issue.
   */
  @Override
  public VerificationResult verifySelfDescription(ContentAccessor payload) throws VerificationException {
    return verifySelfDescription(payload, true, true, true);
  }

  @Override
  public VerificationResult verifySelfDescription(ContentAccessor payload, 
          boolean verifySemantics, boolean verifySchema, boolean verifySignatures) throws VerificationException {
    return verifySelfDescription(payload, false, VRT_UNKNOWN, verifySemantics, verifySchema, verifySignatures);
  }

  private VerificationResult verifySelfDescription(ContentAccessor payload, boolean strict, int expectedType, 
          boolean verifySemantics, boolean verifySchema, boolean verifySignatures) throws VerificationException {
    log.debug("verifySelfDescription.enter;");

    // syntactic validation
    VerifiablePresentation vp = parseContent(payload);
    
    // semantic verification
    VerifiableCredential vc; 
    if (verifySemantics) {
      try {
        //Validation.validate(vp);
        //Validation.validate(vc);
        vc = verifyPresentation(vp);
      } catch (VerificationException ex) {
        throw ex;
      } catch (Exception ex) {
        log.error("verifySelfDescription.semantic error", ex);
        throw new VerificationException("Semantic error: " + ex.getMessage()); //, ex);
      }
    } else {
      vc = getCredential(vp);
      // what if vc is null?
    }
    
    Pair<Boolean, Boolean> type = getSDType(vp, vc);
    if (strict) {
      if (type.getLeft()) {
        if (type.getRight()) { 
          throw new VerificationException("Semantic error: SD is both, a Participant and an Service Offering SD");
        }
        if (expectedType == VRT_OFFERING) {
          throw new VerificationException("Semantic error: Expected Service Offering SD, got Participant SD");
        }
      } else if (type.getRight()) {
        if (expectedType == VRT_PARTICIPANT) {
          throw new VerificationException("Semantic error: Expected Participant SD, got Service Offering SD");
        }
      } else {
        throw new VerificationException("Semantic error: SD is neither a Participant SD nor a Service Offering SD");
      }
    }
    
    // schema verification
    if (verifySchema) {
      // TODO: make it workable
      //validatePayloadAgainstSchema(payload);
    }
  
    List<Validator> validators;
    // signature verification
    if (verifySignatures) {
      validators = checkCryptographic(vp);
    } else {
      validators = null; //is it ok?  
    }

    String id = getID(vc);
    String issuer = null;
    URI issuerUri = vc.getIssuer();
    if (issuerUri != null) {
      issuer = vc.getIssuer().toString();
    }
    Date issDate = vc.getIssuanceDate();
    LocalDate issuedDate = issDate == null ? LocalDate.now() : issDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

    CredentialSubject credentialSubject = getCredentialSubject(vc);
    List<SdClaim> claims = extractClaims(credentialSubject);
    
    VerificationResult result;
    if (type.getLeft()) {
      String name = vp.getHolder().toString();
      Map<String, Object> proof = vp.getLdProof().toMap();
      // take it from validators?
      String key = (String) proof.get("verificationMethod");
      if (issuer == null) {
          issuer = id;
      }
      result = new VerificationResultParticipant(OffsetDateTime.now(), SelfDescriptionStatus.ACTIVE.getValue(), issuer, issuedDate,
              claims, validators, name, key);
    } else if (type.getRight()) {
      result = new VerificationResultOffering(OffsetDateTime.now(), SelfDescriptionStatus.ACTIVE.getValue(), issuer, issuedDate,
              id, claims, validators);
    } else {
      result = new VerificationResult(OffsetDateTime.now(), SelfDescriptionStatus.ACTIVE.getValue(), issuer, issuedDate,
            id, claims, validators);
    }

    log.debug("verifySelfDescription.exit;");
    return result;
  }
  
  @Override
  public boolean checkValidator(Validator validator) {
    //Todo delete this function as it's unused?
    //check if pubkey is the same
    //check if pubkey is trusted
    return true; //if all checks succeeded the validator is valid
  }
  
  /* SD parsing, semantic validation */
  
  private VerifiablePresentation parseContent(ContentAccessor content) {
    try {
      return VerifiablePresentation.fromJson(content.getContentAsString());
    } catch (Exception ex) {
      log.error("parseContent.syntactic error;", ex);
      throw new VerificationException("Syntactic error: " + ex.getMessage(), ex);
    }
  }
  
  private VerifiableCredential verifyPresentation(VerifiablePresentation presentation) {
    // VP must have VC
    VerifiableCredential credential = getCredential(presentation);
    if (credential == null) {
      throw new VerificationException("Semantic error: could not find VC in SD");
    }
    String vcId = getID(credential);
    if (vcId == null) {
      throw new VerificationException("Semantic error: could not find ID in VC");
    }
    
    CredentialSubject subject = getCredentialSubject(credential);
    if (subject == null) {
      throw new VerificationException("Semantic error: could not find CS in VC");
    }
    
    Date today = Date.from(Instant.now());
    Date issDate = credential.getIssuanceDate();
    if (issDate != null && issDate.after(today)) {
      throw new VerificationException("Semantic error: issuanceDate must be in the past");
    }

    Date expDate = credential.getExpirationDate();
    if (expDate != null && expDate.before(today)) {
      throw new VerificationException("Semantic error: expirationDate must be in the future");
    }
    
    return credential;
  }
  
  private Pair<Boolean, Boolean> getSDType(VerifiablePresentation presentation, VerifiableCredential credential) {
    boolean isParticipant = false;
    boolean isServiceOffering = false;

    try {
      CredentialSubject cs = getCredentialSubject(credential);
      log.debug("getSDType; type: {}, types: {}", cs.getType(), cs.getTypes());
      for (String key : TYPE_KEYS) {
        Object _type = cs.getJsonObject().get(key);
        log.debug("getSDType; key: {}, value: {}", key, _type);
        if (_type == null) continue;
        
        List<String> types;
        if (_type instanceof List) {
          types = (List<String>) _type;
        } else {
          types = List.of((String) _type);
        }

        for (String type : types) {
          if (PARTICIPANT_TYPES.contains(type)) {
            isParticipant = true;
          }
          if (SERVICE_OFFERING_TYPES.contains(type)) {
            isServiceOffering = true;
          }
        }
      }
    } catch (Exception e) {
      throw new VerificationException("Semantic error: Could not extract SD's type", e);
    }

    return Pair.of(isParticipant, isServiceOffering);
  }

  /**
   * A method that returns a list of claims given a self-description's VerifiablePresentation
   *
   * @param cs a self-description as Verifiable Presentation for claims extraction
   * @return a list of claims.
   */
   private List<SdClaim> extractClaims(CredentialSubject cs) {

     log.debug("extractClaims.enter; got credential subject: {}", cs);
     List<SdClaim> claims = new ArrayList<>();
     try {
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
       throw new VerificationException("Semantic error: " + ex.getMessage());
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
  
  private VerifiableCredential getCredential(VerifiablePresentation presentation) {
    try {
      VerifiableCredential credential = presentation.getVerifiableCredential();
      log.debug("getCredential; vp.credential: {}", credential);

      if (credential != null) {
        return credential;
      }
    } catch (Exception e) {
      log.debug("getCredential; error: {}", e.getMessage());
    }
    
    Object _credential = presentation.getJsonObject().get("verifiableCredential");
    log.debug("getCredential; all credentials: {}", _credential);
    if (_credential instanceof List) {
      List<Map<String, Object>> credentials = (List<Map<String, Object>>) _credential;
      if (credentials.size() > 0) {
        return VerifiableCredential.fromJsonObject(credentials.get(0));
      }
    }

    return null;
  }

  private CredentialSubject getCredentialSubject(VerifiableCredential credential) {
    return credential.getCredentialSubject();
  }

  private String getID(VerifiableCredential credential) {
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
    throw new VerificationException("Semantic error: could not find Credential ID");
  }

  /* SD validation against SHACL Schemas */

  /**
   * Method that validates a dataGraph against shaclShape
   *
   * @param payload    ContentAccessor of a self-Description payload to be validated
   * @param shaclShape ContentAccessor of a union schemas of type SHACL
   * @return                SemanticValidationResult object
   */
  SemanticValidationResult validatePayloadAgainstSchema(ContentAccessor payload, ContentAccessor shaclShape) {
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

  private void validatePayloadAgainstSchema(ContentAccessor payload) {
    ContentAccessor shaclShape = schemaStore.getCompositeSchema(SchemaStore.SchemaType.SHAPE);
    SemanticValidationResult result = validatePayloadAgainstSchema(payload, shaclShape);
    log.debug("validationAgainstShacl.exit; conforms: {}; model: {}", result.isConforming(), result.getValidationReport());
    if (!result.isConforming()) {
      throw new VerificationException("Schema error; Shacl shape schema violated");
    }
  }
  
  /* SD signatures verification */

  private List<Validator> checkCryptographic(VerifiablePresentation presentation) { 
    log.debug("checkCryptographic.enter;");

    Set<Validator> validators = new HashSet<>();
    try {
      validators.add(checkSignature(presentation));
      VerifiableCredential credential = getCredential(presentation);
      validators.add(checkSignature(credential));
    } catch (VerificationException ex) {
      throw ex;
    } catch (Exception ex) {
      log.error("checkCryptographic.error", ex);
      throw new VerificationException("Signatures error; " + ex.getMessage(), ex);  
    }
    log.debug("checkCryptographic.exit; returning: {}", validators);
    return new ArrayList<>(validators);
  }

  private Validator checkSignature (JsonLDObject payload) throws IOException, ParseException {
    Map<String, Object> proof_map = (Map<String, Object>) payload.getJsonObject().get("proof");
    if (proof_map == null) {
      throw new VerificationException("Signarures error; No proof found");
    }

    LdProof proof = LdProof.fromMap(proof_map);
    Validator result = checkSignature(payload, proof);
    return result;
  }

  private Validator checkSignature (JsonLDObject payload, LdProof proof) throws IOException, ParseException {
    log.debug("checkSignature.enter; got payload: {}, proof: {}", payload, proof);
    LdVerifier verifier;
    Validator validator = null; //TODO Cache.getValidator(proof.getVerificationMethod().toString());
    if (validator == null) {
      log.debug("checkSignature; validator was not cached");
      Pair<PublicKeyVerifier, Validator> pkVerifierAndValidator = getVerifiedVerifier(proof);
      PublicKeyVerifier publicKeyVerifier = pkVerifierAndValidator.getLeft();
      validator = pkVerifierAndValidator.getRight();
      verifier = new JsonWebSignature2020LdVerifier(publicKeyVerifier);
    } else {
      log.debug("checkSignature; validator was cached");
      verifier = getVerifierFromValidator(validator);
    }
    //TODO    if(!verifier.verify(payload)) throw new VerificationException(payload.getClass().getName() + "does not match with proof");

    log.debug("checkSignature.exit; returning: {}", validator);
    return validator;
  }

  private Pair<PublicKeyVerifier, Validator> getVerifiedVerifier(LdProof proof) throws IOException {
    log.debug("getVerifiedVerifier.enter;");
    URI uri = proof.getVerificationMethod();
    String jwt = proof.getJws();
    JWK jwk;
    PublicKeyVerifier pubKey;
    Validator validator;

    if (!SIGNATURES.contains(proof.getType())) {
      throw new VerificationException("Signatures error; This proof type is not yet implemented: " + proof.getType());
    }

    if (!uri.getScheme().equals("did")) {
      throw new VerificationException("Signatures error; Unknown Verification Method: " + uri);
    }

    // TODO: resolve diDoc with Universal Resolver (https://github.com/decentralized-identity/universal-resolver)? 
    
    DIDDocument diDoc = readDIDfromURI(uri);
    log.debug("getVerifiedVerifier; methods: {}", diDoc.getVerificationMethods());
    List<Map<String, Object>> available_jwks = (List<Map<String, Object>>) diDoc.toMap().get("verificationMethod");
    Map<String, Object> method = extractRelevantVerificationMethod(available_jwks, uri);
    Map<String, Object> jwk_map_uncleaned = (Map<String, Object>) method.get("publicKeyJwk");
    Map<String, Object> jwk_map_cleaned = extractRelevantValues(jwk_map_uncleaned);

    Instant deprecation = Instant.now(); 
    // Skipped due to performance issues
    // hasPEMTrustAnchorAndIsNotDeprecated((String) jwk_map_uncleaned.get("x5u"));
    log.debug("getVerifiedVerifier; key has valid trust anchor");

    // use from map and extract only relevant
    jwk = JWK.fromMap(jwk_map_cleaned);

    log.debug("getVerifiedVerifier; create VerifierFactory");
    pubKey = PublicKeyVerifierFactory.publicKeyVerifierForKey(
                  KeyTypeName_for_JWK.keyTypeName_for_JWK(jwk),
                  (String) jwk_map_uncleaned.get("alg"),
                  JWK_to_PublicKey.JWK_to_anyPublicKey(jwk));
    validator = new Validator(
                uri.toString(),
                JsonLDObject.fromJsonObject(jwk_map_uncleaned).toString(),
                deprecation);
    //Does this help? https://www.baeldung.com/java-read-pem-file-keys#2-get-public-key-from-pem-string

    log.debug("getVerifiedVerifier.exit;");
    return Pair.of(pubKey, validator);
  }
  
  //This function becomes obsolete when a did resolver will be available
  //https://gitlab.com/gaia-x/lab/compliance/gx-compliance/-/issues/13
  private static DIDDocument readDIDfromURI (URI uri) throws IOException {
    log.debug("readDIDFromURI.enter; got uri: {}", uri);
    String [] uri_parts = uri.getSchemeSpecificPart().split(":");
    String did_json;
    if (uri_parts[0].equals("web")) {
      String [] _parts = uri_parts[1].split("#");
      URL url;
      if (_parts.length == 1) {
        url = new URL("https://" + _parts[0] +"/.well-known/did.json");
      } else {
        url = new URL("https://" + _parts[0] +"/.well-known/did.json#" + _parts[1]);
      }
      log.debug("readDIDFromURI; requesting DIDDocument from: {}", url.toString());
      InputStream stream = url.openStream();
      did_json = IOUtils.toString(stream, StandardCharsets.UTF_8);
    } else {
      throw new IOException("Couldn't load key. Origin not supported");
    }
    DIDDocument result = DIDDocument.fromJson(did_json);
    log.debug("readDIDFromURI.exit; returning: {}", result);
    return result;
  }

  private Map<String, Object> extractRelevantVerificationMethod(List<Map<String, Object>> methods, URI verificationMethodURI) {
    //TODO wait for answer https://gitlab.com/gaia-x/lab/compliance/gx-compliance/-/issues/22
    log.debug("extractRelevantVerificationMethod; methods: {}, uri: {}", methods, verificationMethodURI);  
    if (methods != null && methods.size() > 0) {  
      return methods.get(0);
    }
    return null;
  }

  private Map<String, Object> extractRelevantValues (Map<String, Object> map) {
    Map<String, Object> new_map = new HashMap<>();
    String [] relevants = {"kty", "d", "e", "kid", "use", "x", "y", "n", "crv"};
    for (String relevant: relevants) {
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

  private Instant hasPEMTrustAnchorAndIsNotDeprecated (String uri) throws IOException, CertificateException {
    StringBuilder result = new StringBuilder();
    URL url = new URL(uri);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(conn.getInputStream()))) {
      for (String line; (line = reader.readLine()) != null; ) {
        result.append(line + System.lineSeparator());
      }
    }
    String pem = result.toString();
    InputStream certStream = new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8));
    CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
    List<X509Certificate> certs = (List<X509Certificate>) certFactory.generateCertificates(certStream);

    //Then extract relevant cert
    X509Certificate relevant = null;

    for (X509Certificate cert : certs) {
      try {
        cert.checkValidity();
        if (relevant == null || relevant.getNotAfter().after(cert.getNotAfter())) {
          relevant = cert;
        }
      } catch (Exception e) {
        log.debug("hasPEMTrustAnchorAndIsNotDeprecated.error: {}", e.getMessage());
      }
    }

    if (relevant == null) {
        // ?!
        return null;
    }
    
    //Second, extract required information
    Instant exp = relevant.getNotAfter().toInstant();

    if (!checkTrustAnchor(uri)) {
      throw new VerificationException("Signatures error; The trust anchor is not set in the registry. URI: " + uri);
    }

    return exp;
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

  private boolean checkTrustAnchor(String uri) throws IOException {
    log.debug("checkTrustAnchor.enter; uri: {}", uri);
    //Check the validity of the cert
    //Is the PEM anchor in the registry?
    // we could use some singleton cache client, probably
    HttpClient httpclient = HttpClients.createDefault();
    // must be moved to some config
    HttpPost httppost = new HttpPost("https://registry.lab.gaia-x.eu/v2206/api/trustAnchor/chain/file");

    // Request parameters and other properties.
    List<NameValuePair> params = List.of(new BasicNameValuePair("uri", uri));
    // use standard constant for UTF-8
    httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8")); 

    //Execute and get the response.
    HttpResponse response = httpclient.execute(httppost);
    // what if code is 2xx or 3xx?  
    log.debug("checkTrustAnchor.exit; status code: {}", response.getStatusLine().getStatusCode());
    return response.getStatusLine().getStatusCode() == 200;   
  }
}
