package eu.gaiax.difs.fc.core.service.verification.impl;

import com.apicatalog.jsonld.loader.DocumentLoader;
import com.apicatalog.jsonld.loader.SchemeRouter;
import com.danubetech.keyformats.JWK_to_PublicKey;
import com.danubetech.keyformats.crypto.PublicKeyVerifier;
import com.danubetech.keyformats.crypto.PublicKeyVerifierFactory;
import com.danubetech.keyformats.jose.JWK;
import com.danubetech.keyformats.keytypes.KeyTypeName_for_JWK;
import com.danubetech.verifiablecredentials.CredentialSubject;
import com.danubetech.verifiablecredentials.VerifiableCredential;
import com.danubetech.verifiablecredentials.VerifiablePresentation;
import eu.gaiax.difs.fc.api.generated.model.SelfDescriptionStatus;
import eu.gaiax.difs.fc.core.exception.ClientException;
import eu.gaiax.difs.fc.core.exception.VerificationException;
import eu.gaiax.difs.fc.core.pojo.*;
import eu.gaiax.difs.fc.core.service.filestore.FileStore;
import eu.gaiax.difs.fc.core.service.schemastore.SchemaStore;
import eu.gaiax.difs.fc.core.service.verification.ClaimExtractor;
import eu.gaiax.difs.fc.core.service.validatorcache.ValidatorCache;
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
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.topbraid.shacl.validation.ValidationUtil;
import org.topbraid.shacl.vocabulary.SH;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.time.Instant;
import java.util.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.system.stream.StreamManager;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Implementation of the {@link VerificationService} interface.
 */
@Slf4j
@Component
public class VerificationServiceImpl implements VerificationService {

  private static final Lang SD_LANG = Lang.JSONLD11;
  private static final Lang SHAPES_LANG = Lang.TURTLE;
  private static final String[] TYPE_KEYS = {"type", "types", "@type", "@types"};
  private static final String[] ID_KEYS = {"id", "@id"};
  // This sniffing is extremely unreliable, since namespace-names are not fixed.
  private static final Set<String> PARTICIPANT_TYPES = Set.of("LegalPerson", "http://w3id.org/gaia-x/participant#LegalPerson", "gax-participant:LegalPerson");
  private static final Set<String> SERVICE_OFFERING_TYPES = Set.of("ServiceOfferingExperimental", "http://w3id.org/gaia-x/service#ServiceOffering", "gax-service:ServiceOffering");
  private static final Set<String> SIGNATURES = Set.of("JsonWebSignature2020"); //, "Ed25519Signature2018");

  private static final ClaimExtractor[] extractors = new ClaimExtractor[]{new TitaniumClaimExtractor(), new DanubeTechClaimExtractor()};

  private static final int VRT_UNKNOWN = 0;
  private static final int VRT_PARTICIPANT = 1;
  private static final int VRT_OFFERING = 2;

  @Autowired
  private SchemaStore schemaStore;

  @Autowired
  private ValidatorCache validatorCache;

  @Autowired
  @Qualifier("contextCacheFileStore")
  private FileStore fileStore;

  private boolean loadersInitialised;
  private StreamManager streamManager;

  public VerificationServiceImpl() {
    Security.addProvider(new BouncyCastleProvider());
  }

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
    List<VerifiableCredential> vcs;
    if (verifySemantics) {
      try {
        vcs = verifyPresentation(vp);
      } catch (VerificationException ex) {
        throw ex;
      } catch (Exception ex) {
        log.error("verifySelfDescription.semantic error", ex);
        throw new VerificationException("Semantic error: " + ex.getMessage()); //, ex);
      }
    } else {
      vcs = getCredentials(vp);
    }

    if (vcs.size() == 0) {
      throw new VerificationException("Semantic Errors: VerifiablePresentation must contain 'verifiableCredential' property");
    }

    VerifiableCredential firstVC = vcs.get(0);
    Pair<Boolean, Boolean> type = getSDTypes(firstVC);
    if (strict) {
      if (type.getLeft()) {
        //if (type.getRight()) {
        //  throw new VerificationException("Semantic error: SD is both, a Participant and a Service Offering SD");
        //}
        if (expectedType == VRT_OFFERING) {
          throw new VerificationException("Semantic error: Expected Service Offering SD, got Participant SD");
        }
      } else if (type.getRight()) {
        if (expectedType == VRT_PARTICIPANT) {
          throw new VerificationException("Semantic error: Expected Participant SD, got Service Offering SD");
        }
      } else {
        throw new VerificationException("Semantic error: SD is neither a Participant nor a Service Offering SD");
      }
    }

    // schema verification
    if (verifySchema) {
      SemanticValidationResult result = verifySelfDescriptionAgainstCompositeSchema(payload);
      if (result == null || !result.isConforming()) {
        throw new VerificationException("Schema error: " + (result == null ? "unknown" : result.getValidationReport()));
      }

    }

    List<Validator> validators;
    // signature verification
    if (verifySignatures) {
      validators = checkCryptographic(vp);
    } else {
      validators = null; //is it ok?
    }

    String id = getID(firstVC); //claims.get(0).getSubject();
    String issuer = null;
    URI issuerUri = firstVC.getIssuer();
    if (issuerUri != null) {
      issuer = issuerUri.toString();
    }
    Date issDate = firstVC.getIssuanceDate();
    Instant issuedDate = issDate == null ? Instant.now() : issDate.toInstant();

    List<SdClaim> claims = extractClaims(payload);
    Set<String> ids = new HashSet<>();

    if (claims != null && !claims.isEmpty()) {

      for (SdClaim claim : claims) {
        if (claim.getSubject().startsWith("_:")) {
          continue; //Ignore blank nodes
        }

        ids.add(claim.getSubject());
      }
    }

    if (ids.size() > 1) {
      String sep = System.lineSeparator();
      StringBuilder sb = new StringBuilder("Semantic Errors: There are different subjects ids in the credential subjects:").append(sep);
      for (String s : ids) {
        sb.append(s).append(sep);
      }
      throw new VerificationException(sb.toString());
    }

    VerificationResult result;
    if (type.getLeft()) {
      LdProof proof = vp.getLdProof();
      URI method = proof == null ? null : proof.getVerificationMethod();
      String key = method == null ? null : method.toString();
      if (issuer == null) {
        issuer = id;
      }
      URI holder = vp.getHolder();
      String name = holder == null ? issuer : holder.toString();
      result = new VerificationResultParticipant(Instant.now(), SelfDescriptionStatus.ACTIVE.getValue(), issuer, issuedDate,
          claims, validators, name, key);
    } else if (type.getRight()) {
      result = new VerificationResultOffering(Instant.now(), SelfDescriptionStatus.ACTIVE.getValue(), issuer, issuedDate,
          id, claims, validators);
    } else {
      result = new VerificationResult(Instant.now(), SelfDescriptionStatus.ACTIVE.getValue(), issuer, issuedDate,
          id, claims, validators);
    }

    log.debug("verifySelfDescription.exit;");
    return result;
  }

  /* SD parsing, semantic validation */
  private VerifiablePresentation parseContent(ContentAccessor content) {
    try {
      return VerifiablePresentation.fromJson(content.getContentAsString());
    } catch (Exception ex) {
      log.error("parseContent.syntactic error;", ex);
      throw new ClientException("Syntactic error: " + ex.getMessage(), ex);
    }
  }

  private List<VerifiableCredential> verifyPresentation(VerifiablePresentation presentation) {
    StringBuilder sb = new StringBuilder();
    String sep = System.getProperty("line.separator");
    if (checkAbsence(presentation, "@context")) {
      sb.append(" - VerifiablePresentation must contain '@context' property").append(sep);
    }
    if (checkAbsence(presentation, "type", "@type")) {
      sb.append(" - VerifiablePresentation must contain 'type' property").append(sep);
    }
    if (checkAbsence(presentation, "verifiableCredential")) {
      sb.append(" - VerifiablePresentation must contain 'verifiableCredential' property").append(sep);
    }
    List<VerifiableCredential> credentials = getCredentials(presentation);
    for (int i = 0; i < credentials.size(); i++) {
      VerifiableCredential credential = credentials.get(i);
      if (credential != null) {
        if (checkAbsence(credential, "@context")) {
          sb.append(" - VerifiableCredential [").append(i).append("] must contain '@context' property").append(sep);
        }
        if (checkAbsence(credential, "type", "@type")) {
          sb.append(" - VerifiableCredential [").append(i).append("] must contain 'type' property").append(sep);
        }
        if (checkAbsence(credential, "credentialSubject")) {
          sb.append(" - VerifiableCredential [").append(i).append("] must contain 'credentialSubject' property").append(sep);
        }
        if (checkAbsence(credential, "issuer")) {
          sb.append(" - VerifiableCredential [").append(i).append("] must contain 'issuer' property").append(sep);
        }
        if (checkAbsence(credential, "issuanceDate")) {
          sb.append(" - VerifiableCredential [").append(i).append("] must contain 'issuanceDate' property").append(sep);
        }

        Date today = Date.from(Instant.now());
        Date issDate = credential.getIssuanceDate();
        if (issDate != null && issDate.after(today)) {
          sb.append(" - 'issuanceDate' of VerifiableCredential [").append(i).append("] must be in the past").append(sep);
        }
        Date expDate = credential.getExpirationDate();
        if (expDate != null && expDate.before(today)) {
          sb.append(" - 'expirationDate' of VerifiableCredential [").append(i).append("] must be in the future").append(sep);
        }
      }
    }

    if (sb.length() > 0) {
      sb.insert(0, "Semantic Errors:").insert(16, sep);
      throw new VerificationException(sb.toString());
    }

    return credentials;
  }

  private boolean checkAbsence(JsonLDObject container, String... keys) {
    boolean found = false;
    for (String key : keys) {
      if (container.getJsonObject().containsKey(key)) {
        found = true;
        break;
      }
    }
    return !found;
  }

  private Pair<Boolean, Boolean> getSDTypes(VerifiableCredential credential) {
    Boolean result = getSDType(credential);
    if (result == null) {
      List<CredentialSubject> subjects = getCredentialSubject(credential);
      for (CredentialSubject subject : subjects) {
        result = getSDType(subject);
        if (result != null) {
          break;
        }
      }
    }

    if (result == null) {
      return Pair.of(false, false);
    }
    return result ? Pair.of(true, false) : Pair.of(false, true);
  }

  private Boolean getSDType(JsonLDObject container) {
    try {
      for (String key : TYPE_KEYS) {
        Object _type = container.getJsonObject().get(key);
        log.debug("getSDType; key: {}, value: {}", key, _type);
        if (_type == null) {
          continue;
        }

        List<String> types;
        if (_type instanceof List) {
          types = (List<String>) _type;
        } else {
          types = List.of((String) _type);
        }

        for (String type : types) {
          // This should properly expand the namespace before matching.
          if (PARTICIPANT_TYPES.contains(type)) {
            return Boolean.TRUE;
          }
          if (SERVICE_OFFERING_TYPES.contains(type)) {
            return Boolean.FALSE;
          }
        }
      }
    } catch (Exception e) {
      log.debug("getSDType.error: {}", e.getMessage());
    }
    return null;
  }


  /*package private functions*/
  /**
   * A method that returns a list of claims given a self-description's VerifiablePresentation
   *
   * @param payload a self-description as Verifiable Presentation for claims extraction
   * @return a list of claims.
   */
  private List<SdClaim> extractClaims(ContentAccessor payload) {
    // Make sure our interceptors are in place.
    initLoaders();
    //TODO does it work with an Array of VCs
    List<SdClaim> claims = null;
    for (ClaimExtractor extra : extractors) {
      try {
        claims = extra.extractClaims(payload);
        if (claims != null) {
          break;
        }
      } catch (Exception ex) {
        log.error("extractClaims.error using {}: {}", extra.getClass().getName(), ex.getMessage());
      }
    }
    return claims;
  }

  private List<CredentialSubject> getCredentialSubject(VerifiableCredential credential) {
    Object obj = credential.getJsonObject().get("credentialSubject");

    if (obj == null) {
      return Collections.emptyList();
    } else if (obj instanceof List) {
      List<Map<String, Object>> l = (List<Map<String, Object>>) obj;
      List<CredentialSubject> result = new ArrayList<>(l.size());

      for (Map<String, Object> _cs : l) {
        CredentialSubject cs = CredentialSubject.fromMap(_cs);

        result.add(cs);
      }

      return result;
    } else if (obj instanceof Map) {
      CredentialSubject vc = CredentialSubject.fromMap((Map<String, Object>) obj);

      return List.of(vc);
    } else {
      return Collections.emptyList();
    }
  }

  private String getID(VerifiableCredential credential) {
    String id;
    List<CredentialSubject> subjects = getCredentialSubject(credential);

    if (!subjects.isEmpty()) {
      id = getID(subjects.get(0).getJsonObject());
    } else {
      id = getID(credential.getJsonObject());
    }
    return id;
  }

  private String getID(Map<String, Object> map) {
    for (String key : ID_KEYS) {
      Object id = map.get(key);
      if (id != null) {
        if (id instanceof String) {
          return (String) id;
        }
        if (id instanceof URI) {
          URI uri = (URI) id;
          return uri.toString();
        }
      }
    }
    return null;
  }

  private void initLoaders() {
    if (!loadersInitialised) {
      log.debug("Setting up Caching com.apicatalog.jsonld DocumentLoader");
      DocumentLoader cachingLoader = new CachingHttpLoader(fileStore);
      SchemeRouter loader = (SchemeRouter) SchemeRouter.defaultInstance();
      loader.set("http", cachingLoader);
      loader.set("https", cachingLoader);
      loadersInitialised = true;
    }
  }

  public StreamManager getStreamManager() {
    if (streamManager == null) {
      // Make sure Caching com.​apicatalog.​jsonld DocumentLoader is set up.
      initLoaders();
      log.debug("Setting up Jena caching Locator");
      StreamManager clone = StreamManager.get().clone();
      clone.clearLocators();
      clone.addLocator(new LocatorCaching(fileStore));
      streamManager = clone;
    }
    return streamManager;
  }


  /* SD validation against SHACL Schemas */
  /**
   * Method that validates a dataGraph against shaclShape
   *
   * @param payload ContentAccessor of a self-Description payload to be validated
   * @param shaclShape ContentAccessor of a union schemas of type SHACL
   * @return SemanticValidationResult object
   */
  SemanticValidationResult validatePayloadAgainstSchema(ContentAccessor payload, ContentAccessor shaclShape) {
    Model data = ModelFactory.createDefaultModel();
    RDFParser.create()
        .streamManager(getStreamManager())
        .source(payload.getContentAsStream())
        .lang(SD_LANG)
        .parse(data);

    Model shape = ModelFactory.createDefaultModel();
    RDFParser.create()
        .streamManager(getStreamManager())
        .source(shaclShape.getContentAsStream())
        .lang(SHAPES_LANG)
        .parse(shape);

    Resource reportResource = ValidationUtil.validateModel(data, shape, true);

    data.close();
    shape.close();

    boolean conforms = reportResource.getProperty(SH.conforms).getBoolean();
    String report = null;
    if (!conforms) {
      report = reportResource.getModel().toString();
    }
    return new SemanticValidationResult(conforms, report);
  }

  @Override
  public SemanticValidationResult verifySelfDescriptionAgainstCompositeSchema(ContentAccessor payload) {
    SemanticValidationResult result = null;
    try {
      ContentAccessor shaclShape = schemaStore.getCompositeSchema(SchemaStore.SchemaType.SHAPE);
      result = validatePayloadAgainstSchema(payload, shaclShape);
      log.debug("validationAgainstShacl.exit; conforms: {}; model: {}", result.isConforming(), result.getValidationReport());
    } catch (Exception exc) {
      log.debug("Failed: ", exc);
    }
    return result;
  }

  public SemanticValidationResult getSemanticValidationResults(ContentAccessor payload) {
    return verifySelfDescriptionAgainstCompositeSchema(payload);
  }

  /* SD signatures verification */
  private List<Validator> checkCryptographic(VerifiablePresentation presentation) {
    log.debug("checkCryptographic.enter;");

    Set<Validator> validators = new HashSet<>();
    try {
      validators.add(checkSignature(presentation));
      List<VerifiableCredential> credentials = getCredentials(presentation);
      for (VerifiableCredential credential : credentials) {
        validators.add(checkSignature(credential));
      }
    } catch (VerificationException ex) {
      throw ex;
    } catch (Exception ex) {
      log.error("checkCryptographic.error", ex);
      throw new VerificationException("Signatures error; " + ex.getMessage(), ex);
    }
    log.debug("checkCryptographic.exit; returning: {}", validators);
    return new ArrayList<>(validators);
  }

  private Validator checkSignature(JsonLDObject payload) throws IOException, ParseException {
    Map<String, Object> proof_map = (Map<String, Object>) payload.getJsonObject().get("proof");
    if (proof_map == null) {
      throw new VerificationException("Signatures error; No proof found");
    }
    if (proof_map.get("type") == null) {
      throw new VerificationException("Signatures error; Proof must have 'type' property");
    }

    LdProof proof = LdProof.fromMap(proof_map);
    Validator result = checkSignature(payload, proof);
    return result;
  }

  private Validator checkSignature(JsonLDObject payload, LdProof proof) throws IOException, ParseException {
    log.debug("checkSignature.enter; got payload: {}, proof: {}", payload, proof);
    LdVerifier verifier;
    Validator validator = validatorCache.getFromCache(proof.getVerificationMethod().toString());
    if (validator == null) {
      log.debug("checkSignature; validator was not cached");
      Pair<PublicKeyVerifier, Validator> pkVerifierAndValidator = null;
      try {
        pkVerifierAndValidator = getVerifiedVerifier(proof);
      } catch (CertificateException e) {
        throw new VerificationException("Signatures error; " + e.getMessage(), e);
      }
      PublicKeyVerifier publicKeyVerifier = pkVerifierAndValidator.getLeft();
      validator = pkVerifierAndValidator.getRight();
      verifier = new JsonWebSignature2020LdVerifier(publicKeyVerifier);
      validatorCache.addToCache(validator);
    } else {
      log.debug("checkSignature; validator was cached");
      verifier = getVerifierFromValidator(validator);
    }

    try {
      if (!verifier.verify(payload)) {
        throw new VerificationException("Signatures error; " + payload.getClass().getName() + " does not match with proof");
      }
    } catch (JsonLDException | GeneralSecurityException e) {
      throw new VerificationException("Signatures error; " + e.getMessage(), e);
    } catch (VerificationException e) {
      throw e;
    }

    log.debug("checkSignature.exit; returning: {}", validator);
    return validator;
  }

  private Pair<PublicKeyVerifier, Validator> getVerifiedVerifier(LdProof proof) throws IOException, CertificateException {
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

    DIDDocument diDoc = readDIDfromURI(uri);
    log.debug("getVerifiedVerifier; methods: {}", diDoc.getVerificationMethods());
    List<Map<String, Object>> available_jwks = (List<Map<String, Object>>) diDoc.toMap().get("verificationMethod");
    Map<String, Object> method = extractRelevantVerificationMethod(available_jwks, uri);
    Map<String, Object> jwk_map_uncleaned = (Map<String, Object>) method.get("publicKeyJwk");
    Map<String, Object> jwk_map_cleaned = extractRelevantValues(jwk_map_uncleaned);

    Instant deprecation = hasPEMTrustAnchorAndIsNotDeprecated((String) jwk_map_uncleaned.get("x5u"));
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

    log.debug("getVerifiedVerifier.exit;");
    return Pair.of(pubKey, validator);
  }

  //This function becomes obsolete when a did resolver will be available
  //https://gitlab.com/gaia-x/lab/compliance/gx-compliance/-/issues/13
  //Resolve DID-Doc with Universal Resolver (https://github.com/decentralized-identity/universal-resolver)?
  private static DIDDocument readDIDfromURI(URI uri) throws IOException {
    log.debug("readDIDFromURI.enter; got uri: {}", uri);
    String[] uri_parts = uri.getSchemeSpecificPart().split(":");
    String did_json;
    if (uri_parts[0].equals("web")) {
      String[] _parts = uri_parts[1].split("#");
      URL url;
      if (_parts.length == 1) {
        url = new URL("https://" + _parts[0] + "/.well-known/did.json");
      } else {
        url = new URL("https://" + _parts[0] + "/.well-known/did.json#" + _parts[1]);
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
    if (methods != null && !methods.isEmpty()) {
      return methods.get(0);
    }
    return null;
  }

  private Map<String, Object> extractRelevantValues(Map<String, Object> map) {
    Map<String, Object> new_map = new HashMap<>();
    String[] relevants = {"kty", "d", "e", "kid", "use", "x", "y", "n", "crv"};
    for (String relevant : relevants) {
      if (map.containsKey(relevant)) {
        new_map.put(relevant, map.get(relevant));
      }
    }
    return new_map;
  }

  private Instant hasPEMTrustAnchorAndIsNotDeprecated(String uri) throws IOException, CertificateException {
    StringBuilder result = new StringBuilder();
    URL url = new URL(uri);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    try ( BufferedReader reader = new BufferedReader(
        new InputStreamReader(conn.getInputStream()))) {
      for (String line; (line = reader.readLine()) != null;) {
        result.append(line).append(System.lineSeparator());
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
      throw new VerificationException("Signatures error; PEM file does not contain a public key");
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
        (String) jwk_map_uncleaned.get("alg"),
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

  private List<VerifiableCredential> getCredentials(VerifiablePresentation vp) {
    Object obj = vp.getJsonObject().get("verifiableCredential");

    if (obj == null) {
      return Collections.emptyList();
    } else if (obj instanceof List) {
      List<Map<String, Object>> l = (List<Map<String, Object>>) obj;
      List<VerifiableCredential> result = new ArrayList<>(l.size());

      for (Map<String, Object> _vc : l) {
        VerifiableCredential vc = VerifiableCredential.fromMap(_vc);

        Pair<Boolean, Boolean> p = getSDTypes(vc);
        if (Objects.equals(p.getLeft(), p.getRight())) {
          continue;
        }

        result.add(vc);
      }

      return result;
    } else {
      VerifiableCredential vc = VerifiableCredential.fromMap((Map<String, Object>) obj);

      Pair<Boolean, Boolean> p = getSDTypes(vc);
      if (Objects.equals(p.getLeft(), p.getRight())) {
        return Collections.emptyList();
      }

      return List.of(vc);
    }
  }
}
