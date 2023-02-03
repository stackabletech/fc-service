package eu.gaiax.difs.fc.core.service.verification.impl;

import com.apicatalog.rdf.RdfTriple;
import com.danubetech.verifiablecredentials.CredentialSubject;
import com.danubetech.verifiablecredentials.VerifiablePresentation;
import eu.gaiax.difs.fc.core.pojo.ContentAccessor;
import eu.gaiax.difs.fc.core.pojo.SdClaim;
import eu.gaiax.difs.fc.core.service.verification.TripleExtractor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.danubetech.verifiablecredentials.jsonld.VerifiableCredentialKeywords.JSONLD_TERM_CREDENTIALSUBJECT;
import static com.danubetech.verifiablecredentials.jsonld.VerifiableCredentialKeywords.JSONLD_TERM_VERIFIABLECREDENTIAL;

@Slf4j
public class DanubeTechClaimExtractorNQuad implements TripleExtractor {


    @Override
    public List<RdfTriple> extractClaimsRDF(ContentAccessor content) throws Exception {
        List <RdfTriple> returnedRDFList = new ArrayList<>();
        log.debug("extractClaims.enter; got content: {}", content);
        List<SdClaim> claims = new ArrayList<>();
        VerifiablePresentation vp = VerifiablePresentation.fromJson(content.getContentAsString());
        Map<String, Object> vpm = vp.getJsonObject();
        List<Map<String, Object>> vcms;
        Object obj = vp.getJsonObject().get(JSONLD_TERM_VERIFIABLECREDENTIAL); 
        log.debug("extractClaims; got VC: {}", obj);
        if (obj instanceof Map) {
            vcms = List.of((Map<String, Object>) obj);
        } else if (obj instanceof List) {
            vcms = (List<Map<String, Object>>) obj;
        } else {
            // vc is a String ?
            vcms = List.of(vpm);
        }
        CredentialSubject cs;
        List<Map<String, Object>> csms;
        for (Map<String, Object> vcm: vcms) {
            obj = vcm.get(JSONLD_TERM_CREDENTIALSUBJECT);
            log.debug("extractClaims; got CS: {}", obj);
            if (obj instanceof Map) {
                csms = List.of((Map<String, Object>) obj);
            } else if (obj instanceof List) {
                csms = (List<Map<String, Object>>) obj;
            } else {
                // cs is a String ?
                continue;
            }
            
            for (Map<String, Object> csm: csms) {
                cs = CredentialSubject.fromMap(csm);
                log.debug("extractClaims; CS claims: {}", cs.getClaims());
                for (RdfTriple nquad: cs.toDataset().toList()) {
                    returnedRDFList.add(nquad);

                }
            }
        }
        log.debug("extractClaims.exit; returning claims: {}", returnedRDFList);
        return returnedRDFList;
    }

     
    
}
