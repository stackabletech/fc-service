package eu.gaiax.difs.fc.core.service.verification.impl;

import com.danubetech.verifiablecredentials.VerifiableCredential;
import com.danubetech.verifiablecredentials.VerifiablePresentation;

import eu.gaiax.difs.fc.core.config.FileStoreConfig;
import eu.gaiax.difs.fc.core.exception.VerificationException;
import eu.gaiax.difs.fc.core.pojo.ContentAccessor;
import eu.gaiax.difs.fc.core.pojo.ContentAccessorFile;
import eu.gaiax.difs.fc.core.pojo.SdClaim;
import eu.gaiax.difs.fc.core.pojo.SemanticValidationResult;
import eu.gaiax.difs.fc.core.service.schemastore.impl.SchemaStoreImpl;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("tests-sdstore")
@ContextConfiguration(classes = {VerificationServiceImplTest.TestApplication.class, FileStoreConfig.class, 
        VerificationServiceImpl.class, SchemaStoreImpl.class})
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
public class VerificationServiceImplTest {

    static Path base_path = Paths.get(".").toAbsolutePath().normalize();

    @SpringBootApplication
    public static class TestApplication {

        public static void main(final String[] args) {
            SpringApplication.run(TestApplication.class, args);
        }
    }

    @Autowired
    private VerificationServiceImpl verificationService;    

    private static ContentAccessor getAccessor(String path) throws UnsupportedEncodingException {
        URL url = VerificationServiceImplTest.class.getClassLoader().getResource(path);
        String str = URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8.name());
        return new ContentAccessorFile(new File(str));
    }

    @Test
    void verifyJSONLDSyntax_valid1() throws IOException {
        //TODO use ContentAccessorFile
        String path = "JSON-LD-Tests/validSD.jsonld";

        assertDoesNotThrow(() -> {
            verificationService.parseSD(getAccessor(path));
        });
    }

    @Test
    void verifyJSONLDSyntax_invalid2() throws IOException {
        String path = "JSON-LD-Tests/smallExample.jsonld";

        Exception ex = assertThrowsExactly(VerificationException.class, () -> verificationService.parseSD(getAccessor(path)));
        assertNotEquals("", ex.getMessage());
    }

    @Test
    void verifyJSONLDSyntax_invalid3() throws IOException {
        String path = "Signature-Tests/hasNoSignature2.jsonld";

        Exception ex = assertThrowsExactly(VerificationException.class, () -> verificationService.parseSD (getAccessor(path)));
        assertEquals("invalid VerifiableCredential", ex.getMessage());
    }

    @Test
    void verifyJSONLDSyntax_MissingQuote() throws IOException {
        String path = "JSON-LD-Tests/missingQuote.jsonld";

        Exception ex = assertThrowsExactly(VerificationException.class, () -> verificationService.verifyOfferingSelfDescription(getAccessor(path)));
        assertNotEquals("", ex.getMessage());
    }

    @Test
    @Disabled("The test is disabled because the check to throw the exception is not yet implemented")
    void verifySignature_SignatureDoesNotMatch() throws IOException {
        String path = "Signature-Tests/hasInvalidSignature.jsonld";

        Map<String, Object> parsed = verificationService.parseSD (getAccessor(path)).getJsonObject();

        //TODO: Will throw exception when it is checked cryptographically
        assertThrowsExactly(VerificationException.class, () -> verificationService.validateCryptographic(parsed));
    }

    @Test
    void verifySignature_SignaturesMissing1() throws IOException {
        String path = "Signature-Tests/hasNoSignature1.jsonld";

        Map<String, Object> parsed = verificationService.parseSD (getAccessor(path)).getJsonObject();

        Exception ex = assertThrowsExactly(VerificationException.class, () -> verificationService.validateCryptographic(parsed));
        assertEquals("no proof found", ex.getMessage());
    }

    @Test
    void verifySignature_SignaturesMissing3() throws IOException {
        String path = "Signature-Tests/lacksSomeSignatures.jsonld";

        Map<String, Object> parsed = verificationService.parseSD (getAccessor(path)).getJsonObject();

        Exception ex = assertThrowsExactly(VerificationException.class, () -> verificationService.validateCryptographic(parsed));
        assertEquals("no proof found", ex.getMessage());
    }

    @Test
    void cleanSD_removeProofs() throws IOException {
        String path = "Signature-Tests/hasInvalidSignature.jsonld";

        Map<String, Object> parsed = verificationService.parseSD (getAccessor(path)).getJsonObject();

        //Do proofs exist?
        assertTrue(parsed.containsKey("proof"));
        for (Map<String, Object> credential : (ArrayList<Map<String, Object>>) parsed.get("verifiableCredential")) {
            assertTrue(credential.containsKey("proof"));
        }

        Map<String, Object> cleaned = verificationService.cleanSD (parsed);

        //Are proofs removed?
        assertFalse(cleaned.containsKey("proof"));
        for (Map<String, Object> credential : (ArrayList<Map<String, Object>>) cleaned.get("verifiableCredential")) {
            assertFalse(credential.containsKey("proof"));
        }
    }

    @Test
    void providerClaimsTest() throws Exception {
        String path = "Claims-Extraction-Tests/providerTest.jsonld";
        ContentAccessor content = getAccessor(path);
        VerifiablePresentation presentation = verificationService.parseSD (content);
        List<SdClaim> expectedClaims = new ArrayList<>();
        //expectedClaims.add(new SdClaim("_:b0", "<vcard:country-name>", "\"Country Name 2\"")); 
        //expectedClaims.add(new SdClaim("_:b0", "<vcard:locality>", "\"City Name 2\"")); 
        //expectedClaims.add(new SdClaim("_:b0", "<vcard:postal-code>", "\"99999\"")); 
        //expectedClaims.add(new SdClaim("_:b0", "<vcard:street-address>", "\"Example street 2\""));
        //expectedClaims.add(new SdClaim("<http://example.org/test-issuer>", "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>", "<gax:Provider>")); 
        //expectedClaims.add(new SdClaim("<http://example.org/test-issuer>", "<gax:hasLegallyBindingAddress>", "_:b0"));
        //expectedClaims.add(new SdClaim("<http://example.org/test-issuer>", "<gax:hasLegallyBindingName>", "\"My example provider\""));        
        List<SdClaim> actualClaims = ((VerificationServiceImpl) verificationService).extractClaims(presentation);
        assertTrue(expectedClaims.size() == actualClaims.size() && expectedClaims.containsAll(actualClaims) && actualClaims.containsAll(expectedClaims));
    }
    
    @Test
    void verifyValidationResult() throws IOException {
        String dataPath = "Validation-Tests/DataCenterDataGraph.jsonld";
        String shapePath = "Validation-Tests/physical-resourceShape.ttl";
        SemanticValidationResult validationResult = verificationService.validationAgainstShacl(
                getAccessor(dataPath), getAccessor(shapePath));
        
        if (!validationResult.isConforming()) {
            assertTrue(validationResult.getValidationReport().contains("Property needs to have at least 1 value"));
        } else {
            assertFalse(validationResult.isConforming());
        }
    }    
}
