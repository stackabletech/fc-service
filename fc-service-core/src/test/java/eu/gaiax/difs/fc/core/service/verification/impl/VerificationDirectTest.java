package eu.gaiax.difs.fc.core.service.verification.impl;

import static eu.gaiax.difs.fc.core.util.TestUtil.getAccessor;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.danubetech.verifiablecredentials.VerifiableCredential;
import com.danubetech.verifiablecredentials.VerifiablePresentation;
import com.danubetech.verifiablecredentials.validation.Validation;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;

import eu.gaiax.difs.fc.core.pojo.ContentAccessor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VerificationDirectTest {
    
    // TODO: this test is to see how Neo4j works only, will be removed at some point later on
    
    @Test
    void parseJSONLDDirectly() throws Exception {
        //String path = "Claims-Extraction-Tests/participantTest.jsonld";
        String path = "Claims-Extraction-Tests/neo4jTest.jsonld";
        //String path = "Claims-Extraction-Tests/providerTest.jsonld";
        ContentAccessor content = getAccessor(VerificationDirectTest.class, path);

        // Read the file into an Object (The type of this object will be a List, Map, String, Boolean,
        // Number or null depending on the root object in the file).
        Object jsonObject = JsonUtils.fromInputStream(content.getContentAsStream());
        // Create a context JSON map containing prefixes and definitions
        Map context = new HashMap();
        // Customise context...
        // Create an instance of JsonLdOptions with the standard JSON-LD options
        JsonLdOptions options = new JsonLdOptions();
        options.setProcessingMode(JsonLdOptions.JSON_LD_1_1);
        // Customise options...
        // Call whichever JSONLD function you want! (e.g. compact)
        //Object compact = JsonLdProcessor.compact(jsonObject, context, options);
        Object rdf = JsonLdProcessor.toRDF(jsonObject);
        // Print out the result (or don't, it's your call!)
        log.debug("RDF: {}; {}", rdf, JsonUtils.toPrettyString(rdf));
    }
    
    @Test
    @Disabled()
    void parseJSONLDDirectly3() throws Exception {
        String path = "Claims-Extraction-Tests/providerTest.jsonld";
        ContentAccessor content = getAccessor(VerificationDirectTest.class, path);
        Object jsonObject = JsonUtils.fromInputStream(content.getContentAsStream());
        Object rdf = JsonLdProcessor.toRDF(jsonObject, new LoggingTripleCallback());
        log.debug("RDF: {}; {}", rdf, JsonUtils.toPrettyString(rdf));
    }
    
    @Test
    void extractClaimsDirectly() throws Exception {
        ContentAccessor content = getAccessor(VerificationDirectTest.class, "Claims-Extraction-Tests/providerTest.jsonld");
        VerifiablePresentation vp = VerifiablePresentation.fromJson(content.getContentAsString());
        Map<String, Object> claims = vp.getVerifiableCredential().getCredentialSubject().getClaims();
        log.debug("provider claims: {}", claims);
        log.debug("provider RDF: {}", vp.getVerifiableCredential().getCredentialSubject().toDataset().toList());

        content = getAccessor(VerificationDirectTest.class, "Claims-Extraction-Tests/participantTest.jsonld");
        vp = VerifiablePresentation.fromJson(content.getContentAsString());
        claims = vp.getVerifiableCredential().getCredentialSubject().getClaims();
        log.debug("participant claims: {}", claims);
        log.debug("participant RDF: {}", vp.getVerifiableCredential().getCredentialSubject().toDataset().toList());

        //content = getAccessor("Claims-Extraction-Tests/participantSD.jsonld");
        //vp = VerifiablePresentation.fromJson(content.getContentAsString());
        //claims = vp.getVerifiableCredential().getCredentialSubject().getClaims();
        //log.debug("big participant claims: {}", claims);
        //log.debug("big participant RDF: {}", vp.getVerifiableCredential().getCredentialSubject().toDataset().toList());
    }
    
    @Test
    void validateVP() throws Exception {
        //validate(VerifiableCredential verifiableCredential)
        ContentAccessor content = getAccessor(VerificationDirectTest.class, "VerificationService/jsonld/input.vp.jsonld");
        VerifiablePresentation vp = VerifiablePresentation.fromJson(content.getContentAsString());
        try {
            Validation.validate(vp);
            Assertions.assertNotNull(vp.getVerifiableCredential());
            Validation.validate(vp.getVerifiableCredential());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
        
}
 