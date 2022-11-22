package eu.gaiax.difs.fc.core.service.verification.impl;

import static eu.gaiax.difs.fc.core.util.TestUtil.getAccessor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import eu.gaiax.difs.fc.core.config.DatabaseConfig;
import eu.gaiax.difs.fc.core.config.FileStoreConfig;
import eu.gaiax.difs.fc.core.exception.ClientException;
import eu.gaiax.difs.fc.core.exception.VerificationException;
import eu.gaiax.difs.fc.core.pojo.ContentAccessor;
import eu.gaiax.difs.fc.core.pojo.SdClaim;
import eu.gaiax.difs.fc.core.pojo.SemanticValidationResult;
import eu.gaiax.difs.fc.core.pojo.VerificationResult;
import eu.gaiax.difs.fc.core.pojo.VerificationResultOffering;
import eu.gaiax.difs.fc.core.pojo.VerificationResultParticipant;
import eu.gaiax.difs.fc.core.service.schemastore.impl.SchemaStoreImpl;
import eu.gaiax.difs.fc.core.service.validatorcache.impl.ValidatorCacheImpl;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("tests-sdstore")
@ContextConfiguration(classes = {VerificationServiceImplTest.TestApplication.class, FileStoreConfig.class,
  VerificationServiceImpl.class, SchemaStoreImpl.class, DatabaseConfig.class, ValidatorCacheImpl.class})
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
public class VerificationServiceImplTest {

  @SpringBootApplication
  public static class TestApplication {

    public static void main(final String[] args) {
      SpringApplication.run(TestApplication.class, args);
    }
  }

  @Autowired
  private VerificationServiceImpl verificationService;

  @Autowired
  private SchemaStoreImpl schemaStore;

  @AfterEach
  public void storageSelfCleaning() throws IOException {
    schemaStore.clear();
  }

  @Test
  void invalidSyntax_MissingQuote() throws Exception {
    log.debug("invalidSyntax_MissingQuote");
    schemaStore.addSchema(getAccessor("Schema-Tests/gax-test-ontology.ttl"));
    String path = "VerificationService/syntax/missingQuote.jsonld";
    ContentAccessor content = getAccessor(path);
    Exception ex = assertThrowsExactly(ClientException.class, ()
        -> verificationService.verifySelfDescription(content));
    assertTrue(ex.getMessage().startsWith("Syntactic error: "));
    assertNotNull(ex.getCause());
  }

  @Test
  void invalidSyntax_NoVCinSD() {
    log.debug("invalidSyntax_NoVCinSD");
    String path = "VerificationService/syntax/smallExample.jsonld";
    schemaStore.addSchema(getAccessor("Schema-Tests/gax-test-ontology.ttl"));
    Exception ex = assertThrowsExactly(VerificationException.class, ()
        -> verificationService.verifySelfDescription(getAccessor(path)));
    assertTrue(ex.getMessage().contains("VerifiablePresentation must contain 'type' property"));
    assertTrue(ex.getMessage().contains("VerifiablePresentation must contain 'verifiableCredential' property"));
  }

  @Test
  void validSyntax_Participant() throws Exception {
    log.debug("validSyntax_Participant");
    schemaStore.addSchema(getAccessor("Schema-Tests/gax-test-ontology.ttl"));
    String path = "VerificationService/syntax/participantSD2.jsonld";
    VerificationResult vr = verificationService.verifySelfDescription(getAccessor(path));
    assertNotNull(vr);
    assertTrue(vr instanceof VerificationResultParticipant);
    VerificationResultParticipant vrp = (VerificationResultParticipant) vr;
    assertEquals("https://www.handelsregister.de/", vrp.getId());
    assertEquals("https://www.handelsregister.de/", vrp.getIssuer());
    assertEquals(Instant.parse("2010-01-01T19:37:24Z"), vrp.getIssuedDateTime());
  }

  @Test
  void validSyntax_ValidSDVP() throws Exception {
    log.debug("validSyntax_ValidSDVP");
    schemaStore.addSchema(getAccessor("Schema-Tests/gax-test-ontology.ttl"));
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
    log.debug("validSyntax_ValidService");
    schemaStore.addSchema(getAccessor("Schema-Tests/gax-test-ontology.ttl"));
    String path = "VerificationService/syntax/service1.jsonld";
    VerificationResult vr = verificationService.verifySelfDescription(getAccessor(path), true, true, false);
    assertNotNull(vr);
    assertTrue(vr instanceof VerificationResult);
    assertFalse(vr instanceof VerificationResultParticipant);
    assertTrue(vr instanceof VerificationResultOffering);
    //VerificationResultOffering vro = (VerificationResultOffering) vr;
    //assertEquals("https://www.handelsregister.de/", vro.getId());
    //assertEquals("https://www.handelsregister.de/", vro.getIssuer());
    //assertEquals(LocalDate.of(2010, 1, 1), vro.getIssuedDate());
  }

  @Test
  void validSyntax_ValidPerson() throws Exception {
    log.debug("validSyntax_ValidPerson");
    schemaStore.addSchema(getAccessor("Schema-Tests/gax-test-ontology.ttl"));
    String path = "VerificationService/syntax/legalPerson1.jsonld";
    VerificationResult vr = verificationService.verifySelfDescription(getAccessor(path), true, true, false);
    assertNotNull(vr);
    assertTrue(vr instanceof VerificationResult);
    assertTrue(vr instanceof VerificationResultParticipant);
    assertFalse(vr instanceof VerificationResultOffering);
    //VerificationResultParticipant vrp = (VerificationResultParticipant) vr;
    //assertEquals("https://www.handelsregister.de/", vrp.getId());
    //assertEquals("https://www.handelsregister.de/", vrp.getIssuer());
    //assertEquals(LocalDate.of(2010, 1, 1), vrp.getIssuedDate());
  }

  @Test
  void invalidProof_InvalidSignatureType() throws Exception {
    log.debug("invalidProof_InvalidSignatureType");
    schemaStore.addSchema(getAccessor("Schema-Tests/gax-test-ontology.ttl"));
    String path = "VerificationService/syntax/input.vp.jsonld";
    Exception ex = assertThrowsExactly(VerificationException.class, ()
        -> verificationService.verifySelfDescription(getAccessor(path)));
    assertEquals("Signatures error; This proof type is not yet implemented: Ed25519Signature2018", ex.getMessage());
  }

  @Test
  void invalidProof_MissingProofs() throws IOException {
    log.debug("invalidProof_MissingProofs");
    schemaStore.addSchema(getAccessor("Schema-Tests/gax-test-ontology.ttl"));
    String path = "VerificationService/sign/hasNoSignature1.json";

    Exception ex = assertThrowsExactly(VerificationException.class, ()
        -> verificationService.verifySelfDescription(getAccessor(path), false, true, true));
    assertEquals("Signatures error; No proof found", ex.getMessage());
    assertNull(ex.getCause());
  }

  @Test
  void invalidProof_UnknownVerificationMethod() throws Exception {
    log.debug("invalidProof_UnknownVerificationMethod");
    schemaStore.addSchema(getAccessor("Schema-Tests/gax-test-ontology.ttl"));
    String path = "VerificationService/sign/hasInvalidSignatureType.json";

    Exception ex = assertThrowsExactly(VerificationException.class, ()
        -> verificationService.verifySelfDescription(getAccessor(path)));
    assertEquals("Signatures error; Unknown Verification Method: https://example.edu/issuers/565049#key-1", ex.getMessage());
    assertNull(ex.getCause());
  }

  @Test
  void invalidProof_SignaturesMissing2() throws IOException {
    log.debug("invalidProof_SignaturesMissing2");
    schemaStore.addSchema(getAccessor("Schema-Tests/gax-test-ontology.ttl"));
    String path = "VerificationService/sign/lacksSomeSignatures.json";

    Exception ex = assertThrowsExactly(VerificationException.class, ()
        -> verificationService.verifySelfDescription(getAccessor(path)));
    assertEquals("Signatures error; No proof found", ex.getMessage());
    assertNull(ex.getCause());
  }

  @Test
  void verifySignature_InvalidSignature() throws UnsupportedEncodingException {
    log.debug("verifySignature_InvalidSignature");
    schemaStore.addSchema(getAccessor("Schema-Tests/gax-test-ontology.ttl"));
    String path = "VerificationService/sign/hasInvalidSignature.json";
    Exception ex = assertThrowsExactly(VerificationException.class, ()
        -> verificationService.verifySelfDescription(getAccessor(path)));
    assertEquals("Signatures error; com.danubetech.verifiablecredentials.VerifiableCredential does not match with proof", ex.getMessage());
  }

  @Test
  void validSD() throws UnsupportedEncodingException {
    log.debug("validSD");
    schemaStore.addSchema(getAccessor("Schema-Tests/gax-test-ontology.ttl"));
    String path = "VerificationService/sign/valid_signature.json";
    VerificationResult result = verificationService.verifySelfDescription(getAccessor(path));
    assertEquals(1, result.getValidators().size(), "Incorrect number of validators found");
  }

  @Test
  void validComplexSD() throws UnsupportedEncodingException {
    log.debug("validComplexSD");
    schemaStore.addSchema(getAccessor("Schema-Tests/gax-test-ontology.ttl"));
    String path = "VerificationService/sign/valid_complex_signature.json";
    VerificationResult result = verificationService.verifySelfDescription(getAccessor(path));
    assertEquals(1, result.getValidators().size(), "Incorrect number of validators found");
  }

  @Test
  void extractClaims_providerTest() throws Exception {
    log.debug("extractClaims_providerTest");
    schemaStore.addSchema(getAccessor("Schema-Tests/gax-test-ontology.ttl"));
    ContentAccessor content = getAccessor("Claims-Extraction-Tests/providerTest.jsonld");
    VerificationResult result = verificationService.verifySelfDescription(content, true, true, false);
    List<SdClaim> actualClaims = result.getClaims();
    log.debug("extractClaims_providerTest; actual claims: {}", actualClaims);

    Set<SdClaim> expectedClaims = new HashSet<>();
    expectedClaims.add(new SdClaim("<http://example.org/test-issuer>", "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>", "<http://w3id.org/gaia-x/participant#Provider>"));
    expectedClaims.add(new SdClaim("<http://example.org/test-issuer>", "<http://w3id.org/gaia-x/participant#legalAddress>", "_:b0"));
    expectedClaims.add(new SdClaim("<http://example.org/test-issuer>", "<http://w3id.org/gaia-x/participant#legalName>", "\"deltaDAO AG\""));
    expectedClaims.add(new SdClaim("<http://example.org/test-issuer>", "<http://w3id.org/gaia-x/participant#name>", "\"deltaDAO AG\""));
    expectedClaims.add(new SdClaim("_:b0", "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>", "<http://w3id.org/gaia-x/participant#Address>"));
    expectedClaims.add(new SdClaim("_:b0", "<http://w3id.org/gaia-x/participant#country>", "\"DE\""));
    expectedClaims.add(new SdClaim("_:b0", "<http://w3id.org/gaia-x/participant#locality>", "\"Hamburg\""));
    expectedClaims.add(new SdClaim("_:b0", "<http://w3id.org/gaia-x/participant#postal-code>", "\"22303\""));
    expectedClaims.add(new SdClaim("_:b0", "<http://w3id.org/gaia-x/participant#street-address>", "\"Geibelstraße 46b\""));
    assertEquals(expectedClaims.size(), actualClaims.size());
    assertEquals(expectedClaims, new HashSet<>(actualClaims));
  }

  @Test
  void extractClaims_participantTest() throws Exception {
    log.debug("extractClaims_participantTest");
    schemaStore.addSchema(getAccessor("Schema-Tests/gax-test-ontology.ttl"));
    ContentAccessor content = getAccessor("Claims-Extraction-Tests/participantSD.jsonld");
    VerificationResult result = verificationService.verifySelfDescription(content, true, false, false);
    List<SdClaim> actualClaims = result.getClaims();
    log.debug("extractClaims_participantTest; actual claims: {}", actualClaims);

    Set<SdClaim> expectedClaims = new HashSet<>();
    expectedClaims.add(new SdClaim("_:b0", "<http://w3id.org/gaia-x/participant#street-address>", "\"Geibelstraße 46b\""));
    expectedClaims.add(new SdClaim("_:b0", "<http://w3id.org/gaia-x/participant#locality>", "\"Hamburg\""));
    expectedClaims.add(new SdClaim("_:b0", "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>", "<http://w3id.org/gaia-x/participant#Address>"));
    expectedClaims.add(new SdClaim("_:b0", "<http://w3id.org/gaia-x/participant#country>", "\"DE\""));
    expectedClaims.add(new SdClaim("_:b0", "<http://w3id.org/gaia-x/participant#postal-code>", "\"22303\""));
    expectedClaims.add(new SdClaim("<did:web:delta-dao.com>", "<http://w3id.org/gaia-x/participant#headquarterAddress>", "_:b0"));
    expectedClaims.add(new SdClaim("_:b1", "<http://w3id.org/gaia-x/participant#street-address>", "\"Geibelstraße 46b\""));
    expectedClaims.add(new SdClaim("_:b1", "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>", "<http://w3id.org/gaia-x/participant#Address>"));
    expectedClaims.add(new SdClaim("_:b1", "<http://w3id.org/gaia-x/participant#postal-code>", "\"22303\""));
    expectedClaims.add(new SdClaim("_:b1", "<http://w3id.org/gaia-x/participant#locality>", "\"Hamburg\""));
    expectedClaims.add(new SdClaim("_:b1", "<http://w3id.org/gaia-x/participant#country>", "\"DE\""));
    expectedClaims.add(new SdClaim("<did:web:delta-dao.com>", "<http://w3id.org/gaia-x/participant#legalAddress>", "_:b1"));
    expectedClaims.add(new SdClaim("_:b2", "<http://w3id.org/gaia-x/service#url>", "\"https://gaia-x.gitlab.io/policy-rules-committee/trust-framework/participant/#legal-person\""));
    expectedClaims.add(new SdClaim("_:b2", "<http://w3id.org/gaia-x/service#hash>", "\"36ba819f30a3c4d4a7f16ee0a77259fc92f2e1ebf739713609f1c11eb41499e7aa2cd3a5d2011e073f9ba9c107493e3e8629cc15cd4fc07f67281d7ea9023db0\""));
    expectedClaims.add(new SdClaim("<did:web:delta-dao.com>", "<http://w3id.org/gaia-x/service#TermsAndConditions>", "_:b2"));
    expectedClaims.add(new SdClaim("<did:web:delta-dao.com>", "<http://w3id.org/gaia-x/participant#legalName>", "\"deltaDAO AG\""));
    expectedClaims.add(new SdClaim("<did:web:delta-dao.com>", "<http://w3id.org/gaia-x/participant#registrationNumber>", "\"DEK1101R.HRB170364\""));
    expectedClaims.add(new SdClaim("<did:web:delta-dao.com>", "<http://w3id.org/gaia-x/participant#leiCode>", "\"391200FJBNU0YW987L26\""));
    expectedClaims.add(new SdClaim("<did:web:delta-dao.com>", "<http://w3id.org/gaia-x/participant#ethereumAddress>", "\"0x4C84a36fCDb7Bc750294A7f3B5ad5CA8F74C4A52\""));
    expectedClaims.add(new SdClaim("<did:web:delta-dao.com>", "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>", "<http://w3id.org/gaia-x/participant#LegalPerson>"));
    assertEquals(expectedClaims.size(), actualClaims.size());
    assertEquals(expectedClaims, new HashSet<>(actualClaims));
  }

  @Test
  void extractClaims_participantTwoVCsTest() throws Exception {
    log.debug("extractClaims_participantTwoVCsTest");
    schemaStore.addSchema(getAccessor("Schema-Tests/gax-test-ontology.ttl"));
    ContentAccessor content = getAccessor("Claims-Extraction-Tests/participantTwoVCs.jsonld");
    VerificationResult result = verificationService.verifySelfDescription(content, true, true, false);
    List<SdClaim> actualClaims = result.getClaims();
    log.debug("extractClaims_participantTest; actual claims: {}", actualClaims);

    Set<SdClaim> expectedClaims = new HashSet<>();

    expectedClaims.add(new SdClaim("_:b0", "<http://www.w3.org/2006/vcard/ns#postal-code>", "\"1234\""));
    expectedClaims.add(new SdClaim("_:b0", "<http://www.w3.org/2006/vcard/ns#country-name>", "\"Country\""));
    expectedClaims.add(new SdClaim("_:b0", "<http://www.w3.org/2006/vcard/ns#locality>", "\"Town Name\""));
    expectedClaims.add(new SdClaim("<http://w3id.org/gaia-x/participant#Provider1>", "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>", "<http://w3id.org/gaia-x/participant#LegalPerson>"));
    expectedClaims.add(new SdClaim("_:b0", "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>", "<http://www.w3.org/2006/vcard/ns#Address>"));
    expectedClaims.add(new SdClaim("_:b0", "<http://www.w3.org/2006/vcard/ns#street-address>", "\"Street Name\""));
    expectedClaims.add(new SdClaim("<http://w3id.org/gaia-x/participant#Provider1>", "<http://w3id.org/gaia-x/participant#headquarterAddress>", "_:b0"));

    assertEquals(expectedClaims.size(), actualClaims.size());
    assertEquals(expectedClaims, new HashSet<>(actualClaims));
  }

  @Test
  void extractClaims_participantTwoCSsTest() throws Exception {
    log.debug("extractClaims_participantTwoCSsTest");
    schemaStore.addSchema(getAccessor("Schema-Tests/gax-test-ontology.ttl"));
    ContentAccessor content = getAccessor("Claims-Extraction-Tests/participantTwoCSs.jsonld");
    VerificationResult result = verificationService.verifySelfDescription(content, true, true, false);
    List<SdClaim> actualClaims = result.getClaims();
    log.debug("extractClaims_participantTest; actual claims: {}", actualClaims);

    Set<SdClaim> expectedClaims = new HashSet<>();

    expectedClaims.add(new SdClaim("_:b0", "<http://www.w3.org/2006/vcard/ns#postal-code>", "\"1234\""));
    expectedClaims.add(new SdClaim("_:b0", "<http://www.w3.org/2006/vcard/ns#country-name>", "\"Country\""));
    expectedClaims.add(new SdClaim("_:b1", "<http://www.w3.org/2006/vcard/ns#street-address>", "\"Street Name\""));
    expectedClaims.add(new SdClaim("_:b1", "<http://www.w3.org/2006/vcard/ns#country-name>", "\"Country\""));
    expectedClaims.add(new SdClaim("_:b0", "<http://www.w3.org/2006/vcard/ns#street-address>", "\"Street Name\""));
    expectedClaims.add(new SdClaim("_:b1", "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>", "<http://www.w3.org/2006/vcard/ns#Address>"));
    expectedClaims.add(new SdClaim("_:b0", "<http://www.w3.org/2006/vcard/ns#locality>", "\"Town Name\""));
    expectedClaims.add(new SdClaim("<http://w3id.org/gaia-x/participant#Provider1>", "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>", "<http://w3id.org/gaia-x/participant#LegalPerson>"));
    expectedClaims.add(new SdClaim("_:b0", "<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>", "<http://www.w3.org/2006/vcard/ns#Address>"));
    expectedClaims.add(new SdClaim("_:b0", "<http://w3id.org/gaia-x/participant#legalAddress>", "_:b1"));
    expectedClaims.add(new SdClaim("_:b1", "<http://www.w3.org/2006/vcard/ns#postal-code>", "\"1234\""));
    expectedClaims.add(new SdClaim("_:b1", "<http://www.w3.org/2006/vcard/ns#locality>", "\"Town Name\""));
    expectedClaims.add(new SdClaim("<http://w3id.org/gaia-x/participant#Provider1>", "<http://w3id.org/gaia-x/participant#headquarterAddress>", "_:b0"));

    assertEquals(expectedClaims.size(), actualClaims.size());
    assertEquals(expectedClaims, new HashSet<>(actualClaims));
  }

  @Test
  void verifyValidationResult() throws IOException {
    log.debug("verifyValidationResult");
    SemanticValidationResult validationResult = verificationService.validatePayloadAgainstSchema(
        getAccessor("Validation-Tests/DataCenterDataGraph.jsonld"), getAccessor("Validation-Tests/physical-resourceShape.ttl"));

    if (!validationResult.isConforming()) {
      assertTrue(validationResult.getValidationReport().contains("Property needs to have at least 1 value"));
    } else {
      assertFalse(validationResult.isConforming());
    }
  }

  @Test
  void verifyInvalidSDValidation_Result_Against_CompositeSchema() throws IOException {
    log.debug("verifyInvalidSDValidation_Result_Against_CompositeSchema");
    schemaStore.addSchema(getAccessor("Schema-Tests/FirstValidSchemaShape.ttl"));
    schemaStore.addSchema(getAccessor("Schema-Tests/SecondValidSchemaShape.ttl"));
    SemanticValidationResult result = verificationService.getSemanticValidationResults(getAccessor("Validation-Tests/DataCenterDataGraph.jsonld"));
    assertFalse(result.isConforming(), "Validation should have failed.");
    assertTrue(result.getValidationReport().contains("Property needs to have at least 1 value"));
  }

  @Test
  void verifyValidVP_SDValidationCompositeSchema() throws IOException {
    log.debug("verifyValidVP_SDValidationCompositeSchema");
    schemaStore.addSchema(getAccessor("Validation-Tests/legal-personShape.ttl"));
    schemaStore.addSchema(getAccessor("Schema-Tests/FirstValidSchemaShape.ttl"));
    SemanticValidationResult validationResult = verificationService.getSemanticValidationResults(
        getAccessor("Claims-Extraction-Tests/providerTest.jsonld"));
    assertTrue(validationResult.isConforming());
  }
}
