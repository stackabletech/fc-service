package eu.gaiax.difs.fc.core.service.verification.impl;

import static com.danubetech.verifiablecredentials.jsonld.VerifiableCredentialKeywords.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.apicatalog.rdf.RdfNQuad;
import com.danubetech.verifiablecredentials.CredentialSubject;
import com.danubetech.verifiablecredentials.VerifiablePresentation;

import eu.gaiax.difs.fc.core.pojo.ContentAccessor;
import eu.gaiax.difs.fc.core.pojo.SdClaim;
import eu.gaiax.difs.fc.core.service.verification.ClaimExtractor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DanubeTechClaimExtractor implements ClaimExtractor {

    @Override
    @SuppressWarnings("unchecked")
    public List<SdClaim> extractClaims(ContentAccessor content) throws Exception {
        log.debug("extractClaims.enter; got content: {}", content);
        List<SdClaim> claims = new ArrayList<>();
        VerifiablePresentation vp = VerifiablePresentation.fromJson(content.getContentAsString());
        Map<String, Object> vpm = vp.getJsonObject();
        Map<String, Object> vcm;
        Object obj = vp.getJsonObject().get(JSONLD_TERM_VERIFIABLECREDENTIAL); 
        log.debug("extractClaims; got VC: {}", obj);
        if (obj instanceof Map) {
            vcm = (Map<String, Object>) obj;
        } else if (obj instanceof List) {
            vcm = (Map<String, Object>) ((List) obj).get(0);
        } else {
            // vc is a String ?
            vcm = vpm;
        }
        CredentialSubject cs;
        obj = vcm.get(JSONLD_TERM_CREDENTIALSUBJECT);
        log.debug("extractClaims; got CS: {}", obj);
        if (obj instanceof Map) {
            cs = CredentialSubject.fromMap((Map<String, Object>) obj);
        } else if (obj instanceof List) {
            cs = CredentialSubject.fromMap((Map<String, Object>) ((List) obj).get(0));
        } else {
            // cs is a String ?
            return null; 
        }
        log.debug("extractClaims; CS claims: {}", cs.getClaims());

        for (RdfNQuad nquad: cs.toDataset().toList()) {
          log.debug("extractClaims; got NQuad: {}", nquad);
          SdClaim claim = new SdClaim(rdf2String(nquad.getSubject()), rdf2String(nquad.getPredicate()), rdf2String(nquad.getObject()));
          claims.add(claim);
        }  
        log.debug("extractClaims.exit; returning claims: {}", claims);
        return claims;
    }

     
    
}
