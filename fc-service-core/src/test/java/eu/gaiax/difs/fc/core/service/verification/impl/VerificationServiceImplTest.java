package eu.gaiax.difs.fc.core.service.verification.impl;

import eu.gaiax.difs.fc.core.config.FileStoreConfig;
import eu.gaiax.difs.fc.core.exception.VerificationException;
import eu.gaiax.difs.fc.core.pojo.*;
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

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
    void invalidSyntax_MissingQuote() {
        String path = "VerificationService/syntax/missingQuote.jsonld";

        Exception ex = assertThrowsExactly(VerificationException.class, () ->
                verificationService.verifySelfDescription(getAccessor(path)));
        assertTrue(ex.getMessage().startsWith("Syntactic error: "));
        assertNotNull(ex.getCause());
    }

    @Test
    void invalidSyntax_NoVCinSD() {
        String path = "VerificationService/syntax/smallExample.jsonld";

        Exception ex = assertThrowsExactly(VerificationException.class, () ->
                verificationService.verifySelfDescription(getAccessor(path)));
        assertEquals("Semantic error: could not find VC in SD", ex.getMessage());
    }

    @Test
    void validSyntax_Participant() throws Exception {
        String path = "VerificationService/syntax/participantSD2.jsonld";
        VerificationResult vr = verificationService.verifySelfDescription(getAccessor(path));
        assertNotNull(vr);
        assertTrue(vr instanceof VerificationResultParticipant);
        VerificationResultParticipant vrp = (VerificationResultParticipant) vr;
        assertEquals("https://www.handelsregister.de/", vrp.getId());
        assertEquals("https://www.handelsregister.de/", vrp.getIssuer());
        assertEquals(LocalDate.of(2010, 1, 1), vrp.getIssuedDate());
    }

    @Test
    void validSyntax_ValidSDVP() throws Exception {
        String path = "VerificationService/syntax/input.vp.jsonld";
        VerificationResult vr = verificationService.verifySelfDescription(getAccessor(path), true, true, false);
        assertNotNull(vr);
        //assertTrue(vr instanceof VerificationResultParticipant);
        //VerificationResultParticipant vrp = (VerificationResultParticipant) vr;
        //assertEquals("https://www.handelsregister.de/", vrp.getId());
        //assertEquals("https://www.handelsregister.de/", vrp.getIssuer());
        //assertEquals(LocalDate.of(2010, 1, 1), vrp.getIssuedDate());
    }

    @Test
    void validSyntax_ValidService() throws Exception {
        String path = "VerificationService/syntax/service1.jsonld";
        VerificationResult vr = verificationService.verifySelfDescription(getAccessor(path), true, true, false);
        assertNotNull(vr);
        assertTrue(vr instanceof VerificationResult);
        assertFalse(vr instanceof VerificationResultParticipant);
        assertFalse(vr instanceof VerificationResultOffering);
        //VerificationResultOffering vro = (VerificationResultOffering) vr;
        //assertEquals("https://www.handelsregister.de/", vro.getId());
        //assertEquals("https://www.handelsregister.de/", vro.getIssuer());
        //assertEquals(LocalDate.of(2010, 1, 1), vro.getIssuedDate());
    }

    @Test
    void validSyntax_ValidService2() throws Exception {
        String path = "VerificationService/syntax/service2.jsonld";
        VerificationResult vr = verificationService.verifySelfDescription(getAccessor(path), true, true, false);
        assertNotNull(vr);
        assertTrue(vr instanceof VerificationResult);
        //VerificationResultOffering vro = (VerificationResultOffering) vr;
        //assertEquals("https://www.handelsregister.de/", vro.getId());
        //assertEquals("https://www.handelsregister.de/", vro.getIssuer());
        //assertEquals(LocalDate.of(2010, 1, 1), vro.getIssuedDate());
    }
    
    @Test
    void validSyntax_ValidPerson() throws Exception {
        String path = "VerificationService/syntax/legalPerson1.jsonld";
        VerificationResult vr = verificationService.verifySelfDescription(getAccessor(path), true, true, false);
        assertNotNull(vr);
        assertTrue(vr instanceof VerificationResult);
        assertFalse(vr instanceof VerificationResultParticipant);
        assertFalse(vr instanceof VerificationResultOffering);
        //VerificationResultParticipant vrp = (VerificationResultParticipant) vr;
        //assertEquals("https://www.handelsregister.de/", vrp.getId());
        //assertEquals("https://www.handelsregister.de/", vrp.getIssuer());
        //assertEquals(LocalDate.of(2010, 1, 1), vrp.getIssuedDate());
    }
    
    @Test
    void invalidProof_InvalidSignatureType() throws Exception {
        String path = "VerificationService/syntax/input.vp.jsonld";
        Exception ex = assertThrowsExactly(VerificationException.class, () ->
                verificationService.verifySelfDescription(getAccessor(path)));
        assertEquals("Signatures error; This proof type is not yet implemented: Ed25519Signature2018", ex.getMessage());
    }
    
    @Test
    void invalidProof_MissingProofs() throws IOException {
        String path = "VerificationService/sign/hasNoSignature1.jsonld";

        Exception ex = assertThrowsExactly(VerificationException.class, () ->
                verificationService.verifySelfDescription(getAccessor(path)));
        System.out.println(ex.getMessage());
        assertEquals("Signarures error; No proof found", ex.getMessage()); 
        assertNull(ex.getCause());
    }

    @Test
    void invalidProof_UnknownVerificationMethod () throws Exception {
        String path = "VerificationService/sign/hasInvalidSignatureType.jsonld";

        Exception ex = assertThrowsExactly(VerificationException.class, () ->
                verificationService.verifySelfDescription(getAccessor(path)));
        assertEquals("Signatures error; Unknown Verification Method: https://example.edu/issuers/565049#key-1", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    @Disabled("We need an SD with valid proofs")
    void invalidProof_SignaturesMissing2() throws IOException {
        String path = "VerificationService/sign/lacksSomeSignatures.jsonld";

        Exception ex = assertThrowsExactly(VerificationException.class, () ->
                verificationService.verifySelfDescription(getAccessor(path)));
        System.out.println(ex.getMessage());
        assertEquals("No proof found", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    @Disabled() //TODO
    void verifySignature_InvalidSignature () throws UnsupportedEncodingException {
        String path = "VerificationService/sign/hasInvalidSignature.jsonld";

        Exception ex = assertThrowsExactly(VerificationException.class, () ->
                verificationService.verifySelfDescription(getAccessor(path)));
        System.out.println(ex.getMessage());
        assertTrue(ex.getMessage().contains("does not match with proof"));
    }

    @Test
    @Disabled("This test wont work like this anymore since some functions are private now")
    void providerClaimsTest() throws Exception {
        String path = "Claims-Extraction-Tests/providerTest.jsonld";

        VerificationResult result = verificationService.verifySelfDescription(getAccessor(path));
        List<SdClaim> actualClaims = result.getClaims();

        List<SdClaim> expectedClaims = new ArrayList<>();
        expectedClaims.add(new SdClaim("_:b0", "<vcard:country-name>", "\"Country Name 2\""));
        expectedClaims.add(new SdClaim("_:b0", "<vcard:locality>", "\"City Name 2\""));
        expectedClaims.add(new SdClaim("_:b0", "<vcard:postal-code>", "\"99999\""));
        expectedClaims.add(new SdClaim("_:b0", "<vcard:street-address>", "\"Example street 2\""));
        expectedClaims.add(new SdClaim("<http://example.org/test-issuer>", "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>", "<gax:Provider>"));
        expectedClaims.add(new SdClaim("<http://example.org/test-issuer>", "<gax:hasLegallyBindingAddress>", "_:b0"));
        expectedClaims.add(new SdClaim("<http://example.org/test-issuer>", "<gax:hasLegallyBindingName>", "\"My example provider\""));

        assertTrue(expectedClaims.size() == actualClaims.size());
        assertTrue(expectedClaims.containsAll(actualClaims));
        assertTrue(actualClaims.containsAll(expectedClaims));
    }

    @Test
    void verifyValidationResult() throws IOException {
        String dataPath = "Validation-Tests/DataCenterDataGraph.jsonld";
        String shapePath = "Validation-Tests/physical-resourceShape.ttl";
        SemanticValidationResult validationResult = verificationService.validatePayloadAgainstSchema(
                getAccessor(dataPath), getAccessor(shapePath));

        if (!validationResult.isConforming()) {
            assertTrue(validationResult.getValidationReport().contains("Property needs to have at least 1 value"));
        } else {
            assertFalse(validationResult.isConforming());
        }
    }
}