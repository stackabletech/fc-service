package eu.xfsc.fc.core.service.verification.signature;


import com.danubetech.keyformats.jose.JWK;

import com.danubetech.verifiablecredentials.CredentialSubject;
import eu.xfsc.fc.core.pojo.Validator;
import foundation.identity.jsonld.JsonLDObject;
import info.weboftrust.ldsignatures.LdProof;

import java.util.List;
import java.util.Map;

public interface SignatureVerifier {

	  Validator checkSignature(JsonLDObject payload, LdProof proof);
	  boolean verify(JsonLDObject payload, LdProof proof, JWK jwk, String alg);

	  default boolean isPureCredential(JsonLDObject payload, String type) {
		  Object csObject = payload.getJsonObject().get("credentialSubject");
		  if (csObject instanceof List<?> csObjectList) {
			  // if credential subject is a list, check every element for type
			  List<CredentialSubject> csList = csObjectList.stream()
					  .filter(o -> o instanceof Map<?, ?>)
					  .map(o -> (Map<String, Object>) o)
					  .map(CredentialSubject::fromMap)
					  .toList();
			  boolean allCompliance = !csList.isEmpty();
			  for (CredentialSubject innerCs : csList) {
				  allCompliance &= innerCs.isType(type);
			  }
			  return allCompliance;
		  } else if (csObject instanceof Map<?, ?> csObjectMap) {
			  // if credential subject is not a list, it is a plain object. check type
			  CredentialSubject cs = CredentialSubject.fromMap((Map<String, Object>) csObjectMap);
			  return cs.isType(type);
		  } else {
			  return false;
		  }
	  }
	  default boolean isPureComplianceCredential(JsonLDObject payload) {
		  if (isPureCredential(payload, "gx:compliance")) {
			  return true;
		  } else {
			  Object vcObject = payload.getJsonObject().get("verifiableCredential");
			  if (vcObject instanceof List<?> vcObjectList) {
				  List<CredentialSubject> vcList = vcObjectList.stream()
					  .filter(o -> o instanceof Map<?, ?>)
					  .map(o -> (Map<String, Object>) o)
					  .map(CredentialSubject::fromMap)
					  .toList();
				  boolean allCompliance = !vcList.isEmpty();
				  for (CredentialSubject innerVc : vcList) {
					  allCompliance &= isPureCredential(innerVc, "gx:compliance");
				  }
				  return allCompliance;
			  }
		  }
		  return false;
	  }

	  default boolean isPureRegistrationNumberCredential(JsonLDObject payload) {
		  return isPureCredential(payload, "gx:legalRegistrationNumber");
	  }
}
