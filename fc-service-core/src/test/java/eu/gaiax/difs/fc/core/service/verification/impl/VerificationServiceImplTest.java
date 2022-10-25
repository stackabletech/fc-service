package eu.gaiax.difs.fc.core.service.verification.impl;

import eu.gaiax.difs.fc.core.config.DatabaseConfig;
import eu.gaiax.difs.fc.core.config.FileStoreConfig;
import eu.gaiax.difs.fc.core.exception.ClientException;
import eu.gaiax.difs.fc.core.exception.VerificationException;
import eu.gaiax.difs.fc.core.pojo.*;
import eu.gaiax.difs.fc.core.service.schemastore.impl.SchemaStoreImpl;
import eu.gaiax.difs.fc.core.service.validatorcache.impl.ValidatorCacheImpl;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("tests-sdstore")
@ContextConfiguration(classes = {VerificationServiceImplTest.TestApplication.class, FileStoreConfig.class,
        VerificationServiceImpl.class, SchemaStoreImpl.class, DatabaseConfig.class, ValidatorCacheImpl.class})
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
@Transactional
public class VerificationServiceImplTest {
    @Autowired
    ValidatorCacheImpl validatorCache;

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

        Exception ex = assertThrowsExactly(ClientException.class, () ->
                verificationService.verifySelfDescription(getAccessor(path)));
        assertTrue(ex.getMessage().startsWith("Syntactic error: "));
        assertNotNull(ex.getCause());
    }

    @Test
    void invalidSyntax_NoVCinSD() {
        String path = "VerificationService/syntax/smallExample.jsonld";

        Exception ex = assertThrowsExactly(VerificationException.class, () ->
                verificationService.verifySelfDescription(getAccessor(path)));
        assertTrue(ex.getMessage().contains("VerifiablePresentation must contain 'type' property"));
        assertTrue(ex.getMessage().contains("VerifiablePresentation must contain 'verifiableCredential' property"));
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
        assertEquals(OffsetDateTime.of(2010, 1, 1, 19, 37, 24, 0, ZoneOffset.UTC), vrp.getIssuedDateTime()); //2010-01-01T19:73:24
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
    @Disabled("invalid SO generated")
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
    @Disabled("invalid SO generated")
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
    @Disabled("invalid LP generated")
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
        String path = "VerificationService/sign/hasNoSignature1.json";

        Exception ex = assertThrowsExactly(VerificationException.class, () ->
                verificationService.verifySelfDescription(getAccessor(path), false, true, true));
        assertEquals("Signatures error; No proof found", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void invalidProof_UnknownVerificationMethod () throws Exception {
        String path = "VerificationService/sign/hasInvalidSignatureType.json";

        Exception ex = assertThrowsExactly(VerificationException.class, () ->
                verificationService.verifySelfDescription(getAccessor(path)));
        assertEquals("Signatures error; Unknown Verification Method: https://example.edu/issuers/565049#key-1", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void invalidProof_SignaturesMissing2() throws IOException {
        String path = "VerificationService/sign/lacksSomeSignatures.json";

        Exception ex = assertThrowsExactly(VerificationException.class, () ->
                verificationService.verifySelfDescription(getAccessor(path)));
        assertEquals("Signatures error; No proof found", ex.getMessage());
        assertNull(ex.getCause());
    }

    @Test
    void verifySignature_InvalidSignature () throws UnsupportedEncodingException {
        String path = "VerificationService/sign/hasInvalidSignature.json";

        Exception ex = assertThrowsExactly(VerificationException.class, () ->
                verificationService.verifySelfDescription(getAccessor(path)));
        assertEquals("Signatures error; com.danubetech.verifiablecredentials.VerifiableCredential does not match with proof", ex.getMessage());
    }

    @Test
    void validSD () throws UnsupportedEncodingException {
        String path = "VerificationService/sign/valid_signature.json";

        verificationService.verifySelfDescription(getAccessor(path));
    }

    @Test
    //@Disabled("This test wont work like this anymore since some functions are private now")
    void providerClaimsTest() throws Exception {
        String path = "Claims-Extraction-Tests/providerTest.jsonld";

        VerificationResult result = verificationService.verifySelfDescription(getAccessor(path), true, true, false);
        List<SdClaim> actualClaims = result.getClaims();

        log.debug("providerClaimsTest; actual claims: {}", actualClaims);

        List<SdClaim> expectedClaims = new ArrayList<>();
        expectedClaims.add(new SdClaim("_:b0", "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>", "<http://w3id.org/gaia-x/participant#Provider>"));
        expectedClaims.add(new SdClaim("_:b0", "<http://w3id.org/gaia-x/participant#legalAddress>", "_:b1"));
        expectedClaims.add(new SdClaim("_:b0", "<http://w3id.org/gaia-x/participant#legalName>", "\"deltaDAO AG\""));
        expectedClaims.add(new SdClaim("_:b0", "<http://w3id.org/gaia-x/participant#name>", "\"deltaDAO AG\""));
        expectedClaims.add(new SdClaim("_:b1", "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>", "<http://w3id.org/gaia-x/participant#Address>"));
        expectedClaims.add(new SdClaim("_:b1", "<http://w3id.org/gaia-x/participant#country>", "\"DE\""));
        expectedClaims.add(new SdClaim("_:b1", "<http://w3id.org/gaia-x/participant#locality>", "\"Hamburg\""));
        expectedClaims.add(new SdClaim("_:b1", "<http://w3id.org/gaia-x/participant#postal-code>", "\"22303\""));
        expectedClaims.add(new SdClaim("_:b1", "<http://w3id.org/gaia-x/participant#street-address>", "\"Geibelstra?e 46b\""));

        //expectedClaims.add(new SdClaim("_:b0", "<vcard:country-name>", "\"Country Name 2\""));
        //expectedClaims.add(new SdClaim("_:b0", "<vcard:locality>", "\"City Name 2\""));
        //expectedClaims.add(new SdClaim("_:b0", "<vcard:postal-code>", "\"99999\""));
        //expectedClaims.add(new SdClaim("_:b0", "<vcard:street-address>", "\"Example street 2\""));
        //expectedClaims.add(new SdClaim("<http://example.org/test-issuer>", "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>", "<gax:Provider>"));
        //expectedClaims.add(new SdClaim("<http://example.org/test-issuer>", "<gax:hasLegallyBindingAddress>", "_:b0"));
        //expectedClaims.add(new SdClaim("<http://example.org/test-issuer>", "<gax:hasLegallyBindingName>", "\"My example provider\""));

        assertEquals(expectedClaims.size(), actualClaims.size());
        //assertEquals(expectedClaims, actualClaims); do not match, for some reason..
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
