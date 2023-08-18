package eu.xfsc.fc.core.service.verification;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.jena.riot.system.stream.StreamManager;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

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

import eu.xfsc.fc.api.generated.model.SelfDescriptionStatus;
import eu.xfsc.fc.core.dao.ValidatorCacheDao;
import eu.xfsc.fc.core.exception.ClientException;
import eu.xfsc.fc.core.exception.VerificationException;
import eu.xfsc.fc.core.pojo.ContentAccessor;
import eu.xfsc.fc.core.pojo.SdClaim;
import eu.xfsc.fc.core.pojo.SemanticValidationResult;
import eu.xfsc.fc.core.pojo.Validator;
import eu.xfsc.fc.core.pojo.VerificationResult;
import eu.xfsc.fc.core.pojo.VerificationResultOffering;
import eu.xfsc.fc.core.pojo.VerificationResultParticipant;
import eu.xfsc.fc.core.service.filestore.FileStore;
import eu.xfsc.fc.core.service.schemastore.SchemaStore;
import eu.xfsc.fc.core.util.ClaimValidator;
import foundation.identity.did.DIDDocument;
import foundation.identity.jsonld.JsonLDException;
import foundation.identity.jsonld.JsonLDObject;
import info.weboftrust.ldsignatures.LdProof;
import info.weboftrust.ldsignatures.verifier.JsonWebSignature2020LdVerifier;
import info.weboftrust.ldsignatures.verifier.LdVerifier;
import lombok.extern.slf4j.Slf4j;


/**
 * Implementation of the {@link VerificationService} interface.
 */
@Slf4j
@Component
public class VerificationServiceImpl implements VerificationService {

  private static final Set<String> SIGNATURES = Set.of("JsonWebSignature2020"); //, "Ed25519Signature2018");
  private static final ClaimExtractor[] extractors = new ClaimExtractor[]{new TitaniumClaimExtractor(), new DanubeTechClaimExtractor()};

  @Value("${federated-catalogue.verification.trust-anchor-url}")
  private String trustAnchorAddr;

  @Value("${federated-catalogue.verification.did-resolver-url}")
  private String didResolverAddr;

  private static final int VRT_UNKNOWN = 0;
  private static final int VRT_PARTICIPANT = 1;
  private static final int VRT_OFFERING = 2;
  // take it from properties..
  private static final int HTTP_TIMEOUT = 5*1000; //5sec

  @Value("${federated-catalogue.verification.participant.type}")
  private String participantType; // "http://w3id.org/gaia-x/participant#Participant";
  @Value("${federated-catalogue.verification.service-offering.type}")
  private String serviceOfferingType; //"http://w3id.org/gaia-x/service#ServiceOffering";

  @Autowired
  private SchemaStore schemaStore;

  @Autowired
  private ValidatorCacheDao validatorCache;

  @Autowired
  @Qualifier("contextCacheFileStore")
  private FileStore fileStore;

  @Autowired
  private ObjectMapper objectMapper;

  private boolean loadersInitialised;
  private StreamManager streamManager;
  //@Autowired
  private RestTemplate rest;

  public VerificationServiceImpl() {
    Security.addProvider(new BouncyCastleProvider());
    rest = restTemplate();
  }

  private RestTemplate restTemplate() {
    HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
    factory.setConnectTimeout(HTTP_TIMEOUT);
    factory.setConnectionRequestTimeout(HTTP_TIMEOUT);
    return new RestTemplate(factory); 
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
  public VerificationResult verifySelfDescription(ContentAccessor payload, boolean verifySemantics, boolean verifySchema, 
		  boolean verifySignatures) throws VerificationException {
    return verifySelfDescription(payload, false, VRT_UNKNOWN, verifySemantics, verifySchema, verifySignatures);
  }

  private VerificationResult verifySelfDescription(ContentAccessor payload, boolean strict, int expectedType, boolean verifySemantics, 
		  boolean verifySchema, boolean verifySignatures) throws VerificationException {
    log.debug("verifySelfDescription.enter; strict: {}, expectedType: {}, verifySemantics: {}, verifySchema: {}, verifySignatures: {}",
            strict, expectedType, verifySemantics, verifySchema, verifySignatures);
    long stamp = System.currentTimeMillis();

    // syntactic validation
    VerifiablePresentation vp = parseContent(payload);
    log.debug("verifySelfDescription; content parsed, time taken: {}", System.currentTimeMillis() - stamp);

    // semantic verification
    long stamp2 = System.currentTimeMillis();
    TypedCredentials tcs;
    if (verifySemantics) {
      try {
        tcs = verifyPresentation(vp);
      } catch (VerificationException ex) {
        throw ex;
      } catch (Exception ex) {
        log.error("verifySelfDescription.semantic error", ex);
        throw new VerificationException("Semantic error: " + ex.getMessage());
      }
    } else {
      tcs = getCredentials(vp);
    }
    log.debug("verifySelfDescription; credentials processed, time taken: {}", System.currentTimeMillis() - stamp2);

    if (tcs.isEmpty()) {
      throw new VerificationException("Semantic Error: no proper CredentialSubject found");
    }

    if (strict) {
      if (tcs.isParticipant()) {
        if (tcs.isOffering()) {
          throw new VerificationException("Semantic error: SD is both, Participant and Service Offering SD");
        }
        if (expectedType == VRT_OFFERING) {
          throw new VerificationException("Semantic error: Expected Service Offering SD, got Participant SD");
        }
      } else if (tcs.isOffering()) {
        if (expectedType == VRT_PARTICIPANT) {
          throw new VerificationException("Semantic error: Expected Participant SD, got Service Offering SD");
        }
      } else {
        throw new VerificationException("Semantic error: SD is neither Participant nor Service Offering SD");
      }
    }

    stamp2 = System.currentTimeMillis();
    List<SdClaim> claims = extractClaims(payload);
    log.debug("verifySelfDescription; claims extracted: {}, time taken: {}", (claims == null ? "null" : claims.size()),
    		System.currentTimeMillis() - stamp2);

    if (verifySemantics) {
      Set<String> subjects = new HashSet<>();
      Set<String> objects = new HashSet<>();
      if (claims != null && !claims.isEmpty()) {
        for (SdClaim claim : claims) {
          subjects.add(claim.getSubjectString());
          objects.add(claim.getObjectString());
        }
      }
      subjects.removeAll(objects);

      if (subjects.size() > 1) {
        String sep = System.lineSeparator();
        StringBuilder sb = new StringBuilder("Semantic Errors: There are different subject ids in credential subjects: ").append(sep);
        for (String s : subjects) {
          sb.append(s).append(sep);
        }
        throw new VerificationException(sb.toString());
      } else if (subjects.isEmpty()) {
        throw new VerificationException("Semantic Errors: There is no uniquely identified credential subject");
      }
    }

    // schema verification
    if (verifySchema) {
      SemanticValidationResult result = verifyClaimsAgainstCompositeSchema(claims);
      if (result == null || !result.isConforming()) {
        throw new VerificationException("Schema error: " + (result == null ? "unknown" : result.getValidationReport()));
      }
    }

    // signature verification
    List<Validator> validators;
    if (verifySignatures) {
      validators = checkCryptography(tcs);
    } else {
      validators = null; //is it ok?
    }

    String id = tcs.getID();
    String issuer = tcs.getIssuer();
    Instant issuedDate = tcs.getIssuanceDate();

    VerificationResult result;
    if (tcs.isParticipant()) {
      if (issuer == null) {
        issuer = id;
      }
      String method = tcs.getProofMethod();
      String holder = tcs.getHolder();
      String name = holder == null ? issuer : holder;
      result = new VerificationResultParticipant(Instant.now(), SelfDescriptionStatus.ACTIVE.getValue(), issuer, issuedDate,
              claims, validators, name, method);
    } else if (tcs.isOffering()) {
      result = new VerificationResultOffering(Instant.now(), SelfDescriptionStatus.ACTIVE.getValue(), issuer, issuedDate,
              id, claims, validators);
    } else {
      result = new VerificationResult(Instant.now(), SelfDescriptionStatus.ACTIVE.getValue(), issuer, issuedDate,
              id, claims, validators);
    }

    stamp = System.currentTimeMillis() - stamp;
    log.debug("verifySelfDescription.exit; returning: {}; time taken: {}", result, stamp);
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

  private TypedCredentials verifyPresentation(VerifiablePresentation presentation) {
    log.debug("verifyPresentation.enter; got presentation with id: {}", presentation.getId());
    StringBuilder sb = new StringBuilder();
    String sep = System.lineSeparator();
    if (checkAbsence(presentation, "@context")) {
      sb.append(" - VerifiablePresentation must contain '@context' property").append(sep);
    }
    if (checkAbsence(presentation, "type", "@type")) {
      sb.append(" - VerifiablePresentation must contain 'type' property").append(sep);
    }
    if (checkAbsence(presentation, "verifiableCredential")) {
      sb.append(" - VerifiablePresentation must contain 'verifiableCredential' property").append(sep);
    }
    TypedCredentials tcreds = getCredentials(presentation);
    List<VerifiableCredential> credentials = tcreds.getCredentials();
    for (int i = 0; i < credentials.size(); i++) {
      VerifiableCredential credential = credentials.get(i);
      if (credential != null) {
        if (checkAbsence(credential, "@context")) {
          sb.append(" - VerifiableCredential[").append(i).append("] must contain '@context' property").append(sep);
        }
        if (checkAbsence(credential, "type", "@type")) {
          sb.append(" - VerifiableCredential[").append(i).append("] must contain 'type' property").append(sep);
        }
        if (checkAbsence(credential, "credentialSubject")) {
          sb.append(" - VerifiableCredential[").append(i).append("] must contain 'credentialSubject' property").append(sep);
        }
        if (checkAbsence(credential, "issuer")) {
          sb.append(" - VerifiableCredential[").append(i).append("] must contain 'issuer' property").append(sep);
        }
        if (checkAbsence(credential, "issuanceDate")) {
          sb.append(" - VerifiableCredential[").append(i).append("] must contain 'issuanceDate' property").append(sep);
        }

        Date today = Date.from(Instant.now());
        Date issDate = credential.getIssuanceDate();
        if (issDate != null && issDate.after(today)) {
          sb.append(" - 'issuanceDate' of VerifiableCredential[").append(i).append("] must be in the past").append(sep);
        }
        Date expDate = credential.getExpirationDate();
        if (expDate != null && expDate.before(today)) {
          sb.append(" - 'expirationDate' of VerifiableCredential[").append(i).append("] must be in the future").append(sep);
        }
      }
    }

    if (sb.length() > 0) {
      sb.insert(0, "Semantic Errors:").insert(16, sep);
      throw new VerificationException(sb.toString());
    }

    log.debug("verifyPresentation.exit; returning {} VCs", credentials.size());
    return tcreds;
  }

  private boolean checkAbsence(JsonLDObject container, String... keys) {
    for (String key : keys) {
      if (container.getJsonObject().containsKey(key)) {
        return false;
      }
    }
    return true;
  }

  private TypedCredentials getCredentials(VerifiablePresentation vp) {
    log.trace("getCredentials.enter; got VP: {}", vp);
    TypedCredentials tcs = new TypedCredentials(vp);
    log.trace("getCredentials.exit; returning: {}", tcs);
    return tcs;
  }

  /**
   * A method that returns a list of claims given a self-description's VerifiablePresentation
   *
   * @param payload a self-description as Verifiable Presentation for claims extraction
   * @return a list of claims.
   */
  @Override
  public List<SdClaim> extractClaims(ContentAccessor payload) {
    // Make sure our interceptors are in place.
    initLoaders();
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

  private void initLoaders() {
    if (!loadersInitialised) {
      log.debug("initLoaders; Setting up Caching com.apicatalog.jsonld DocumentLoader");
      DocumentLoader cachingLoader = new CachingHttpLoader(fileStore);
      SchemeRouter loader = (SchemeRouter) SchemeRouter.defaultInstance();
      loader.set("http", cachingLoader);
      loader.set("https", cachingLoader);
      loadersInitialised = true;
    }
  }

  private StreamManager getStreamManager() {
    if (streamManager == null) {
      // Make sure Caching com.​apicatalog.​jsonld DocumentLoader is set up.
      initLoaders();
      log.debug("getStreamManager; Setting up Jena caching Locator");
      StreamManager clone = StreamManager.get().clone();
      clone.clearLocators();
      clone.addLocator(new LocatorCaching(fileStore));
      streamManager = clone;
    }
    return streamManager;
  }

  public void setTypes(String partType, String soType) {
    this.participantType = partType;
    this.serviceOfferingType = soType;
  }


  /* SD validation against SHACL Schemas */
  @Override
  public SemanticValidationResult verifySelfDescriptionAgainstCompositeSchema(ContentAccessor payload) {
    return verifySelfDescriptionAgainstSchema(payload, null);
  }

  @Override
  public SemanticValidationResult verifySelfDescriptionAgainstSchema(ContentAccessor payload, ContentAccessor schema) {
    log.debug("verifySelfDescriptionAgainstSchema.enter;");
    SemanticValidationResult result = null;
    try {
      if (schema == null) { 	
        schema = schemaStore.getCompositeSchema(SchemaStore.SchemaType.SHAPE);
      }
      List<SdClaim> claims = extractClaims(payload);
      result = verifyClaimsAgainstSchema(claims, schema);
    } catch (Exception exc) {
      log.info("verifySelfDescriptionAgainstSchema.error: {}", exc.getMessage());
    }
    log.debug("verifySelfDescriptionAgainstSchema.exit; conforms: {}", result.isConforming());
    return result;
  }
  
  private SemanticValidationResult verifyClaimsAgainstCompositeSchema(List<SdClaim> claims) {
	log.debug("verifyClaimsAgainstCompositeSchema.enter;");
	SemanticValidationResult result = null;
	try {
	  ContentAccessor shaclShape = schemaStore.getCompositeSchema(SchemaStore.SchemaType.SHAPE);
	  result = verifyClaimsAgainstSchema(claims, shaclShape);
	} catch (Exception exc) {
	  log.info("verifyClaimsAgainstCompositeSchema.error: {}", exc.getMessage());
	}
	log.debug("verifyClaimsAgainstCompositeSchema.exit; conforms: {}", result.isConforming());
	return result;
  }

  /**
   * Method that validates a dataGraph against shaclShape
   *
   * @param payload    ContentAccessor of a self-Description payload to be validated
   * @param shaclShape ContentAccessor of a union schemas of type SHACL
   * @return SemanticValidationResult object
   */
  private SemanticValidationResult verifyClaimsAgainstSchema(List<SdClaim> claims, ContentAccessor schema) {
	String report = ClaimValidator.validateClaimsBySchema(claims, schema, getStreamManager());
	return new SemanticValidationResult(report == null, report);
  }

  
  /* SD signatures verification */
  private List<Validator> checkCryptography(TypedCredentials tcs) {
    log.debug("checkCryptography.enter;");
    long timestamp = System.currentTimeMillis();

    Set<Validator> validators = new HashSet<>();
    try {
      validators.add(checkSignature(tcs.getPresentation()));
      for (VerifiableCredential credential : tcs.getCredentials()) {
        validators.add(checkSignature(credential));
      }
    } catch (VerificationException ex) {
      throw ex;
    } catch (Exception ex) {
      log.error("checkCryptography.error", ex);
      throw new VerificationException("Signatures error; " + ex.getMessage(), ex);
    }
    timestamp = System.currentTimeMillis() - timestamp;
    log.debug("checkCryptography.exit; returning: {}; time taken: {}", validators, timestamp);
    return new ArrayList<>(validators);
  }

  @SuppressWarnings("unchecked")
  private Validator checkSignature(JsonLDObject payload) throws IOException, GeneralSecurityException, JsonLDException {
    Map<String, Object> proofMap = (Map<String, Object>) payload.getJsonObject().get("proof");
    if (proofMap == null) {
      throw new VerificationException("Signatures error; No proof found");
    }

    LdProof proof = LdProof.fromMap(proofMap);
    if (proof.getType() == null) {
      throw new VerificationException("Signatures error; Proof must have 'type' property");
    }
    Validator result = checkSignature(payload, proof);
    return result;
  }

  private Validator checkSignature(JsonLDObject payload, LdProof proof) throws IOException, GeneralSecurityException, JsonLDException { 
    log.debug("checkSignature.enter; got payload, proof: {}", proof);
    PublicKeyVerifier<?> pkVerifier;
    Validator validator = validatorCache.getFromCache(proof.getVerificationMethod().toString());
    if (validator == null) {
      log.debug("checkSignature; validator was not cached");
      Pair<PublicKeyVerifier<?>, Validator> pkVerifierAndValidator = getVerifiedVerifier(proof);
      validator = pkVerifierAndValidator.getRight();
      validatorCache.addToCache(validator);
      pkVerifier = pkVerifierAndValidator.getLeft();
    } else {
      log.debug("checkSignature; validator was cached");
      Map<String, Object> jwkMap = JsonLDObject.fromJson(validator.getPublicKey()).getJsonObject();
      pkVerifier = getVerifier(jwkMap);
    }

    LdVerifier<?> verifier = new JsonWebSignature2020LdVerifier(pkVerifier);
    if (!verifier.verify(payload)) {
      throw new VerificationException("Signatures error; " + payload.getClass().getName() + " does not match with proof");
    }

    log.debug("checkSignature.exit; returning: {}", validator);
    return validator;
  }

  @SuppressWarnings("unchecked")
  private Pair<PublicKeyVerifier<?>, Validator> getVerifiedVerifier(LdProof proof) throws IOException, CertificateException {
    log.debug("getVerifiedVerifier.enter;");
    URI uri = proof.getVerificationMethod();
    Pair<PublicKeyVerifier<?>, Validator> result = null;
    
    if (!SIGNATURES.contains(proof.getType())) {
      throw new VerificationException("Signatures error; The proof type is not yet implemented: " + proof.getType());
    }

    if (!uri.getScheme().equals("did")) {
      throw new VerificationException("Signatures error; Unknown Verification Method: " + uri);
    }

    DIDDocument diDoc = readDIDfromURI(uri);
    // better to get methods from doc using DIDDocument API...
    List<Map<String, Object>> methods = (List<Map<String, Object>>) diDoc.toMap().get("verificationMethod");
    log.debug("getVerifiedVerifier; methods: {}", methods);

    for (Map<String, Object> method: methods) { 
      Map<String, Object> jwkMap = getRelevantPublicKey(method, uri);
      if (jwkMap != null) {
        String url = (String) jwkMap.get("x5u");
        Instant expiration = null;
        if (url == null) {
          // not sure what to do in this case..	
          log.info("getVerifiedVerifier; no verification URI provided, method is: {}", jwkMap);
        } else {
          expiration =	hasPEMTrustAnchorAndIsNotExpired(url);
          log.debug("getVerifiedVerifier; key has valid trust anchor, expires: {}", expiration);
          PublicKeyVerifier<?> pubKey = getVerifier(jwkMap);
          String jwkString = objectMapper.writeValueAsString(jwkMap);
          Validator validator = new Validator(uri.toString(), jwkString, expiration);
          result = Pair.of(pubKey, validator);
          break;
        }
      }
    }

    if (result == null) {
      throw new VerificationException("Signatures error; no proper VerificationMethod found");
    }
    log.debug("getVerifiedVerifier.exit; returning pair: {}", result);
    return result;
  }

  private PublicKeyVerifier<?> getVerifier(Map<String, Object> jwkMap) throws IOException {
	  // Danubetech JWK supports only known fields, so we clean it..
	  Set<String> relevants = Set.of("kty", "d", "e", "kid", "use", "x", "y", "n", "crv");
      Map<String, Object> jwkMapCleaned = jwkMap.entrySet().stream()
    	.filter(e -> relevants.contains(e.getKey()))
	    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
      JWK jwk = JWK.fromMap(jwkMapCleaned);

      String alg = (String) jwkMap.get("alg");
      //if (alg == null) {
      //  alg = (String) jwkMap.get("crv");	
      //}
      log.debug("getVerifier; creating Verifier, alg: {}", alg);
      return PublicKeyVerifierFactory.publicKeyVerifierForKey(KeyTypeName_for_JWK.keyTypeName_for_JWK(jwk),
              alg, JWK_to_PublicKey.JWK_to_anyPublicKey(jwk));
  }

  private DIDDocument readDIDfromURI(URI uri) throws IOException {
    log.debug("readDIDFromURI.enter; got uri: {}", uri);
    DIDDocument didDoc;
    // let's try universal resolver
    URL url = new URL(didResolverAddr + uri.toString());
    try {
      didDoc = loadDIDfromURL(url);
    } catch (Exception ex) {
  	  log.info("readDIDfromURI; error loading URI: {}", ex.getMessage());
  	  url = resolveWebUrl(uri);
  	  if (url == null) {
        throw new IOException("Couldn't load key. Method not supported");
  	  }
      didDoc = loadDIDfromURL(url);
    }
    log.debug("readDIDFromURI.exit; returning: {}", didDoc);
    return didDoc;
  }
  
  private DIDDocument loadDIDfromURL(URL url) throws IOException {
    log.debug("loadDIDFromURL; loading DIDDocument from: {}", url.toString());
	InputStream stream = url.openStream();
    String docJson = IOUtils.toString(stream, StandardCharsets.UTF_8);
    return DIDDocument.fromJson(docJson);
  }
  
  public static URL resolveWebUrl(URI uri) throws IOException {
	String[] uri_parts = uri.getSchemeSpecificPart().split(":");
	if (uri_parts.length >= 2 && "web".equals(uri_parts[0])) {
	  String url = "https://";
      url += uri_parts[1];
	  if (uri_parts.length == 2) {
	    url += "/.well-known";
	  } else {
	    int idx;
	    try {
	      Integer.parseInt(uri_parts[2]);
	      url += ":" + uri_parts[2];
	      idx = 3;
	    } catch (NumberFormatException e) {
		  idx = 2;  
	    }
	    for (int i=idx; i < uri_parts.length; i++) {
		  url += "/" + uri_parts[i];
	    }
	  }
	  url += "/did.json";
	  if (uri.getFragment() != null) {
	    url += "#" + uri.getFragment();
	  }
	  return new URL(url);
    }
    return null;  
  }  

  @SuppressWarnings("unchecked")
  private Map<String, Object> getRelevantPublicKey(Map<String, Object> method, URI verificationMethodURI) {
	// TODO: how to check method conforms to uri? 
	// publicKeyMultibase can be used also..  
    return (Map<String, Object>) method.get("publicKeyJwk");
  }

  @SuppressWarnings("unchecked")
  private Instant hasPEMTrustAnchorAndIsNotExpired(String uri) throws IOException, CertificateException {
    log.debug("hasPEMTrustAnchorAndIsNotExpired.enter; got uri: {}", uri);
    //StringBuilder result = new StringBuilder();
    //URL url = new URL(uri);
    //HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    //conn.setRequestMethod("GET");
    //try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
    //  for (String line; (line = reader.readLine()) != null;) {
    //    result.append(line).append(System.lineSeparator());
    //  }
    //}
    //String pem = result.toString();
    
    String pem = rest.getForObject(uri, String.class);
    InputStream certStream = new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8));
    CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
    List<X509Certificate> certs = (List<X509Certificate>) certFactory.generateCertificates(certStream);

    //Then extract relevant cert
    X509Certificate relevant = null;
    for (X509Certificate cert: certs) {
      try {
        cert.checkValidity();
        if (relevant == null || relevant.getNotAfter().after(cert.getNotAfter())) {
          relevant = cert;
        }
      } catch (Exception e) {
        log.debug("hasPEMTrustAnchorAndIsNotExpired.error: {}", e.getMessage());
      }
    }

    if (relevant == null) {
      throw new VerificationException("Signatures error; PEM file does not contain public key");
    }

    //if (!checkTrustAnchor(uri)) {
    ResponseEntity<Map> resp = rest.postForEntity(trustAnchorAddr, Map.of("uri", uri), Map.class);
	if (!resp.getStatusCode().is2xxSuccessful()) {
      throw new VerificationException("Signatures error; Trust anchor is not set in the registry. URI: " + uri);
    }
    Instant exp = relevant.getNotAfter().toInstant();
    log.debug("hasPEMTrustAnchorAndIsNotExpired.exit; returning: {}", exp);
    return exp;
  }

  private class TypedCredentials {

    private Boolean isParticipant;
    private Boolean isOffering;
    private VerifiablePresentation presentation;
    private List<VerifiableCredential> credentials;

    TypedCredentials(VerifiablePresentation presentation) {
      this.presentation = presentation;
      initCredentials();
    }

    @SuppressWarnings("unchecked")
    private void initCredentials() {
      Object obj = presentation.getJsonObject().get("verifiableCredential");
      List<VerifiableCredential> creds;
      if (obj == null) {
        creds = Collections.emptyList();
      } else if (obj instanceof List) {
        List<Map<String, Object>> l = (List<Map<String, Object>>) obj;
        creds = new ArrayList<>(l.size());
        for (Map<String, Object> _vc : l) {
          VerifiableCredential vc = VerifiableCredential.fromMap(_vc);
          Pair<Boolean, Boolean> p = getSDTypes(vc);
          if (Objects.equals(p.getLeft(), p.getRight())) {
            continue;
          }
          creds.add(vc);
          // TODO: dont't think the next two lines are correct..
          // not sure we should override existing values
          isParticipant = p.getLeft();
          isOffering = p.getRight();
        }
      } else {
        VerifiableCredential vc = VerifiableCredential.fromMap((Map<String, Object>) obj);
        Pair<Boolean, Boolean> p = getSDTypes(vc);
        if (Objects.equals(p.getLeft(), p.getRight())) {
          creds = Collections.emptyList();
        } else {
          creds = List.of(vc);
          isParticipant = p.getLeft();
          isOffering = p.getRight();
        }
      }
      this.credentials = creds;
    }

    private VerifiableCredential getFirstVC() {
      return credentials.isEmpty() ? null : credentials.get(0);
    }

    List<VerifiableCredential> getCredentials() {
      return credentials;
    }

    String getHolder() {
      URI holder = presentation.getHolder();
      if (holder == null) {
        return null;
      }
      return holder.toString();
    }

    String getID() {
      VerifiableCredential first = getFirstVC();
      if (first == null) {
        return null;
      }

      List<CredentialSubject> subjects = getSubjects(first);
      if (subjects.isEmpty()) {
        return getID(first.getJsonObject());
      }
      return getID(subjects.get(0).getJsonObject());
    }

    String getIssuer() {
      VerifiableCredential first = getFirstVC();
      if (first == null) {
        return null;
      }
      URI issuer = first.getIssuer();
      if (issuer == null) {
        return null;
      }
      return issuer.toString();
    }

    Instant getIssuanceDate() {
      VerifiableCredential first = getFirstVC();
      if (first == null) {
        return null;
      }
      Date issDate = first.getIssuanceDate();
      if (issDate == null) {
        return Instant.now();
      }
      return issDate.toInstant();
    }

    VerifiablePresentation getPresentation() {
      return presentation;
    }

    String getProofMethod() {
      LdProof proof = presentation.getLdProof();
      URI method = proof == null ? null : proof.getVerificationMethod();
      return method == null ? null : method.toString();
    }

    boolean isEmpty() {
      return credentials.isEmpty();
    }

    boolean isParticipant() {
      return isParticipant != null && isParticipant;
    }

    boolean isOffering() {
      return isOffering != null && isOffering;
    }

    private Pair<Boolean, Boolean> getSDTypes(VerifiableCredential credential) {
      ContentAccessor gaxOntology = schemaStore.getCompositeSchema(SchemaStore.SchemaType.ONTOLOGY);
      Boolean result = ClaimValidator.getSubjectType(gaxOntology, getStreamManager(), credential.toJson(), participantType, serviceOfferingType);
      log.debug("getSDTypes; got type result: {}", result);
      if (result == null) {
        return Pair.of(false, false);
      }
      return result ? Pair.of(true, false) : Pair.of(false, true);
    }

    @SuppressWarnings("unchecked")
    private List<CredentialSubject> getSubjects(VerifiableCredential credential) {
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

    private String getID(Map<String, Object> map) {
      Object id = map.get("id");
      if (id != null) {
        return id.toString();
      }
      id = map.get("@id");
      if (id != null) {
        return id.toString();
      }
      return null;
    }

  }

}
