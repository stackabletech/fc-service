package eu.gaiax.difs.fc.core.service.verification.impl;

import eu.gaiax.difs.fc.core.exception.VerificationException;
import eu.gaiax.difs.fc.core.pojo.ContentAccessorDirect;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class VerificationServiceImplTest {
    static Path base_path = Paths.get(".").toAbsolutePath().normalize();
    private final VerificationServiceImpl verificationService = new VerificationServiceImpl();
    private String readFile (String relPath) throws IOException {
        // for static access, uses the class name directly
        InputStream is = VerificationServiceImplTest.class.getClassLoader().getResourceAsStream(relPath);

        StringBuilder builder = new StringBuilder();
        int i;

        if (is == null) {
            throw new IllegalArgumentException("file not found!");
        } else {
            while((i = is.read())!=-1) {
                builder.append((char) i);
            }
            return builder.toString();
        }
    }

    @Test
    void verifyJSONLDSyntax_valid1() throws IOException {
        //TODO use ContentAccessorFile
        String path = "JSON-LD-Tests/validSD.jsonld";
        String json = readFile(path);

        assertDoesNotThrow(() -> {
            verificationService.parseSD(new ContentAccessorDirect(json));
        });
    }

    @Test
    void verifyJSONLDSyntax_valid2() throws IOException {
        String path = "JSON-LD-Tests/smallExample.jsonld";
        String json = readFile(path);

        assertDoesNotThrow(() -> {
            verificationService.parseSD(new ContentAccessorDirect(json));
        });
    }

    @Test
    void verifyJSONLDSyntax_MissingQuote() throws IOException {
        String path = "JSON-LD-Tests/missingQuote.jsonld";
        String json = readFile(path);

        Exception ex = assertThrowsExactly(VerificationException.class, () -> verificationService.verifyOfferingSelfDescription(new ContentAccessorDirect(json)));
        assertNotEquals("", ex.getMessage());
    }

    @Test
    @Disabled("The test is disabled because the check to throw the exception is not yet implemented")
    void verifySignature_SignatureDoesNotMatch() throws IOException {
        String path = "Signature-Tests/hasInvalidSignature.jsonld";
        String json = readFile(path);

        Map<String, Object> parsed = verificationService.parseSD (new ContentAccessorDirect(json));

        //TODO: Will throw exception when it is checked cryptographically
        assertThrowsExactly(VerificationException.class, () -> verificationService.validateCryptographic(parsed));
    }

    @Test
    void verifySignature_SignaturesMissing1() throws IOException {
        String path = "Signature-Tests/hasNoSignature1.jsonld";
        String json = readFile(path);

        Map<String, Object> parsed = verificationService.parseSD (new ContentAccessorDirect(json));

        Exception ex = assertThrowsExactly(VerificationException.class, () -> verificationService.validateCryptographic(parsed));
        assertEquals("no proof found", ex.getMessage());
    }

    @Test
    void verifySignature_SignaturesMissing2() throws IOException {
        String path = "Signature-Tests/hasNoSignature2.jsonld";
        String json = readFile(path);

        Map<String, Object> parsed = verificationService.parseSD (new ContentAccessorDirect(json));

        Exception ex = assertThrowsExactly(VerificationException.class, () -> verificationService.validateCryptographic(parsed));
        assertEquals("no proof found", ex.getMessage());
    }

    @Test
    void verifySignature_SignaturesMissing3() throws IOException {
        String path = "Signature-Tests/lacksSomeSignatures.jsonld";
        String json = readFile(path);

        Map<String, Object> parsed = verificationService.parseSD (new ContentAccessorDirect(json));

        Exception ex = assertThrowsExactly(VerificationException.class, () -> verificationService.validateCryptographic(parsed));
        assertEquals("no proof found", ex.getMessage());
    }

    @Test
    void cleanSD_removeProofs() throws IOException {
        String path = "Signature-Tests/hasInvalidSignature.jsonld";
        String json = readFile(path);

        Map<String, Object> parsed = verificationService.parseSD (new ContentAccessorDirect(json));

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
}