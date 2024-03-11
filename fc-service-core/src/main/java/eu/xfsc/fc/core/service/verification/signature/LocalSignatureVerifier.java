package eu.xfsc.fc.core.service.verification.signature;

import static eu.xfsc.fc.core.util.DidUtils.resolveWebUri;

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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.danubetech.keyformats.crypto.PublicKeyVerifier;
import com.danubetech.keyformats.crypto.PublicKeyVerifierFactory;
import com.danubetech.keyformats.jose.JWK;

import eu.xfsc.fc.core.exception.VerificationException;
import eu.xfsc.fc.core.pojo.Validator;
import foundation.identity.did.DIDDocument;
import foundation.identity.jsonld.JsonLDException;
import foundation.identity.jsonld.JsonLDObject;
import info.weboftrust.ldsignatures.LdProof;
import info.weboftrust.ldsignatures.verifier.JsonWebSignature2020LdVerifier;
import info.weboftrust.ldsignatures.verifier.LdVerifier;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LocalSignatureVerifier implements SignatureVerifier {

    private static final Set<String> SIGNATURES = Set.of("JsonWebSignature2020"); //, "Ed25519Signature2018");
    // take it from properties..
    private static final int HTTP_TIMEOUT = 5*1000; //5sec    
	
    @Value("${federated-catalogue.verification.trust-anchor-url}")
    private String trustAnchorAddr;

    private RestTemplate rest;

    public LocalSignatureVerifier() {
      Security.addProvider(new BouncyCastleProvider());
      rest = restTemplate();
    }

    private RestTemplate restTemplate() {
      HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
      factory.setConnectTimeout(HTTP_TIMEOUT);
      factory.setConnectionRequestTimeout(HTTP_TIMEOUT);
      return new RestTemplate(factory); 
    }
    
	@Override
	public Validator checkSignature(JsonLDObject payload, LdProof proof) {
	  try {
	    log.debug("checkSignature.enter; got payload, proof: {}", proof);
	    Validator validator = getVerifiedValidator(payload, proof);
		log.debug("checkSignature.exit; returning: {}", validator);
	    return validator;
	  } catch (IOException ex) {
	    throw new VerificationException(ex);
	  }
	}

	@SuppressWarnings("unchecked")
	private Validator getVerifiedValidator(JsonLDObject payload, LdProof proof) throws IOException {
	  log.debug("getVerifiedVerifier.enter;");
	    
	  if (!SIGNATURES.contains(proof.getType())) {
	    throw new VerificationException("Signatures error; The proof type is not yet implemented: " + proof.getType());
	  }

	  URI uri = proof.getVerificationMethod();
	  if (!uri.getScheme().equals("did")) {
	    throw new VerificationException("Signatures error; Unknown Verification Method: " + uri);
	  }

	  DIDDocument diDoc = getDIDocFromURI(uri);
	  // better to get methods from doc using DIDDocument API...
	  List<Map<String, Object>> methods = (List<Map<String, Object>>) diDoc.toMap().get("verificationMethod");
	  log.debug("getVerifiedVerifier; methods: {}", methods);

	  Validator result = null;
	  boolean verified = false;
	  for (Map<String, Object> method: methods) {
	    try {	
	      Map<String, Object> jwkMap = getRelevantPublicKey(method, uri);
		  if (jwkMap != null) {
		    String url = (String) jwkMap.get("x5u");
		    Instant expiration = null;
		    if (url != null) {
		      expiration = hasPEMTrustAnchorAndIsNotExpired(url);
		    }
		    // else we'll try it anyway
		    JWK jwk = getPublicKeyJWK(jwkMap);
		    String alg = (String) jwkMap.get("alg");
		    if (verify(payload, proof, jwk, alg)) { 
			  if (jwk.getAlg() == null) {
				jwk.setAlg(alg);
			  }
		      result = new Validator(uri.toString(), jwk.toJson(), expiration);
		      break;
		    }
		    verified = true;
		  }
	    } catch (Exception ex) {
	      log.info("getVerifiedVerifier.error: {}", ex.getMessage());
	    }
	  }
	    
	  if (result == null) {
	    if (verified) {
	      throw new VerificationException("Signatures error; " + payload.getClass().getSimpleName() + " does not match with proof");
		}
    	throw new VerificationException("Signatures error; no proper VerificationMethod found");
	  }
	  log.debug("getVerifiedVerifier.exit; returning validator: {}", result);
	  return result;
	}
	  
	@Override
	public boolean verify(JsonLDObject payload, LdProof proof, JWK jwk, String alg) {
      log.debug("verify; got jwk: {}, alg: {}", jwk, alg);
	  PublicKeyVerifier<?> pkVerifier = PublicKeyVerifierFactory.publicKeyVerifierForJWK(jwk, alg);
	    //.publicKeyVerifierForKey(KeyTypeName_for_JWK.keyTypeName_for_JWK(jwk), alg, JWK_to_PublicKey.JWK_to_anyPublicKey(jwk));
	  LdVerifier<?> verifier = new JsonWebSignature2020LdVerifier(pkVerifier);
	  try {
		return verifier.verify(payload);
	  } catch (IOException | GeneralSecurityException | JsonLDException ex) {
		log.info("verify.error: {}", ex.getMessage());
	  }
	  return false;
	}

	private JWK getPublicKeyJWK(Map<String, Object> jwkMap) throws IOException {
	  // Danubetech JWK supports only known fields, so we clean it..
	  Set<String> relevants = Set.of("kty", "d", "e", "kid", "use", "x", "y", "n", "crv");
	  Map<String, Object> jwkMapCleaned = jwkMap.entrySet().stream()
	   	.filter(e -> relevants.contains(e.getKey()))
	    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
	  return JWK.fromMap(jwkMapCleaned);
	}

	private DIDDocument getDIDocFromURI(URI uri) throws IOException { 
	  log.debug("readDIDFromURI.enter; got uri: {}", uri);
	  DIDDocument diDoc;
	  URI  docUri = resolveWebUri(uri);
	  if (docUri == null) {
	    throw new IOException("Couldn't load key. Method not supported");
	  }
	  diDoc = loadDIDocFromURI(docUri);
	  log.debug("readDIDFromURI.exit; returning: {}", diDoc);
	  return diDoc;
	}
	  
	private DIDDocument loadDIDocFromURI(URI docUri) throws IOException {
	  log.debug("loadDIDFromURL; loading DIDDocument from: {}", docUri.toString());
	  URL url = docUri.toURL();
	  // do this with caching doc-loader..
	  InputStream docStream = url.openStream();
	  //String didDoc = rest.getForObject(url.toString(), String.class);
	  //InputStream docStream = new ByteArrayInputStream(didDoc.getBytes(StandardCharsets.UTF_8));
	  String docJson = IOUtils.toString(docStream, StandardCharsets.UTF_8);
	  return DIDDocument.fromJson(docJson);
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
	  String pem = rest.getForObject(uri, String.class);
	  InputStream certStream = new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8));
	  CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
	  List<X509Certificate> certs = (List<X509Certificate>) certFactory.generateCertificates(certStream);

	  //Then extract relevant cert
	  X509Certificate relevant = null;
	  for (X509Certificate cert: certs) {
	    try {
	      cert.checkValidity();
	      if (relevant == null || relevant.getNotAfter().before(cert.getNotAfter())) { // .after(cert.getNotAfter())) {
	        relevant = cert;
	      }
	    } catch (Exception ex) {
	      log.debug("hasPEMTrustAnchorAndIsNotExpired.error: {}", ex.getMessage());
	    }
	  }

	  //if (relevant == null) {
	  //  throw new VerificationException("Signatures error; PEM file does not contain public key");
	  //}

	  //if (!checkTrustAnchor(uri)) {
	  ResponseEntity<Map> resp = rest.postForEntity(trustAnchorAddr, Map.of("uri", uri), Map.class);
	  if (!resp.getStatusCode().is2xxSuccessful()) {
	    log.info("hasPEMTrustAnchorAndIsNotExpired; Trust anchor is not set in the registry. URI: {}", uri);
	    //throw new VerificationException("Signatures error; Trust anchor is not set in the registry. URI: " + uri);
	  }
	  Instant exp = relevant == null ? null : relevant.getNotAfter().toInstant();
	  log.debug("hasPEMTrustAnchorAndIsNotExpired.exit; returning: {}", exp);
	  return exp;
	}

}
