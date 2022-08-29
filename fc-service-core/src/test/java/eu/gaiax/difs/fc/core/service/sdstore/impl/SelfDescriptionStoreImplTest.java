package eu.gaiax.difs.fc.core.service.sdstore.impl;

import eu.gaiax.difs.fc.core.config.DatabaseConfig;
import eu.gaiax.difs.fc.core.config.EmbeddedNeo4JConfig;
import eu.gaiax.difs.fc.api.generated.model.SelfDescriptionStatus;
import eu.gaiax.difs.fc.core.exception.ConflictException;
import eu.gaiax.difs.fc.core.exception.NotFoundException;
import eu.gaiax.difs.fc.core.pojo.ContentAccessor;
import eu.gaiax.difs.fc.core.pojo.ContentAccessorDirect;
import eu.gaiax.difs.fc.core.pojo.SdClaim;
import eu.gaiax.difs.fc.core.pojo.SdFilter;
import eu.gaiax.difs.fc.core.pojo.SelfDescriptionMetadata;
import eu.gaiax.difs.fc.core.pojo.Signature;
import eu.gaiax.difs.fc.core.pojo.VerificationResult;
import eu.gaiax.difs.fc.core.service.filestore.impl.FileStoreImpl;
import eu.gaiax.difs.fc.core.service.graphdb.impl.Neo4jGraphStore;
import eu.gaiax.difs.fc.core.service.sdstore.SelfDescriptionStore;
import eu.gaiax.difs.fc.core.service.verification.impl.VerificationServiceImpl;
import eu.gaiax.difs.fc.core.util.HashUtils;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import liquibase.repackaged.org.apache.commons.collections4.IterableUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.neo4j.driver.springframework.boot.test.autoconfigure.Neo4jTestHarnessAutoConfiguration;
import org.neo4j.harness.Neo4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.MethodName.class)
//@EnableAutoConfiguration(exclude = {LiquibaseAutoConfiguration.class, DataSourceAutoConfiguration.class, Neo4jTestHarnessAutoConfiguration.class})
@SpringBootTest
@ActiveProfiles("tests-sdstore")
@ContextConfiguration(classes = { SelfDescriptionStoreImplTest.TestApplication.class, DatabaseConfig.class,
    SelfDescriptionStoreImpl.class, FileStoreImpl.class, Neo4jGraphStore.class })
@DirtiesContext
@Transactional
@Slf4j
@AutoConfigureEmbeddedDatabase(provider = DatabaseProvider.ZONKY)
@Import(EmbeddedNeo4JConfig.class)
public class SelfDescriptionStoreImplTest {

  @SpringBootApplication
  public static class TestApplication {

    public static void main(String[] args) {
      SpringApplication.run(TestApplication.class, args);
    }
  }

  /*
  @Autowired
  private VerificationService verificationService;
  */

  private final VerificationServiceImpl verificationService = new VerificationServiceImpl();

  @Autowired
  private SelfDescriptionStore sdStore;

  @Autowired
  private FileStoreImpl fileStore;

  @Autowired
  private Neo4j embeddedDatabaseServer;

  @AfterAll
  void closeNeo4j() {
      embeddedDatabaseServer.close();
  }

  private SelfDescriptionMetadata createSelfDescriptionMeta(String id, String issuer, OffsetDateTime sdt,
      OffsetDateTime udt, String content) {
    final String hash = HashUtils.calculateSha256AsHex(content);
    SelfDescriptionMetadata sdMeta = new SelfDescriptionMetadata();
    sdMeta.setId(id);
    sdMeta.setIssuer(issuer);
    sdMeta.setSdHash(hash);
    sdMeta.setStatus(SelfDescriptionStatus.ACTIVE);
    sdMeta.setStatusDatetime(sdt);
    sdMeta.setUploadDatetime(udt);
    sdMeta.setSelfDescription(new ContentAccessorDirect(content));
    return sdMeta;
  }

  private List<SdClaim> createClaims() {
    final SdClaim claim1 = new SdClaim("https://delta-dao.com/.well-known/serviceMVGPortal.json",
        "gax-service:providedBy", "https://delta-dao.com/.well-known/participant.json");
    final SdClaim claim2 = new SdClaim("https://delta-dao.com/.well-known/serviceMVGPortal.json", "gax-service:name", "EuProGigant Portal");
    final SdClaim claim3 = new SdClaim("https://delta-dao.com/.well-known/serviceMVGPortal.json", "gax-service:description", "EuProGigant Minimal Viable Gaia-X Portal");
    final SdClaim claim4 = new SdClaim("https://delta-dao.com/.well-known/serviceMVGPortal.json", "gax-service:TermsAndConditions", "https://euprogigant.com/en/terms/");
    final SdClaim claim5 = new SdClaim("https://delta-dao.com/.well-known/serviceMVGPortal.json", "gax-service:TermsAndConditions", "contentHash");
    return Arrays.asList(new SdClaim[] {claim1, claim2, claim3, claim4, claim5});
  }

  private void assertStoredSdFiles(final int expected) {
    final MutableInt count = new MutableInt(0);
    fileStore.getFileIterable(SelfDescriptionStore.STORE_NAME).forEach(file -> count.increment());
    final String message = String.format("Storing %d file(s) should result in exactly %d file(s) in the store.",
        expected, expected);
    assertEquals(expected, count.intValue(), message);
  }

  private void assertAllSdFilesDeleted() {
    final MutableInt count = new MutableInt(0);
    fileStore.getFileIterable(SelfDescriptionStore.STORE_NAME).forEach(file -> count.increment());
    assertEquals(0, count.intValue(), "Deleting the last file should result in exactly 0 files in the store.");
  }

  /**
   * Test storing a self-description, ensuring it creates exactly one file on
   * disk, retrieving it by hash, and deleting it again.
   */
  @Test
  void test01StoreSelfDescription() {
    log.info("test01StoreSelfDescription");
    final String content = "Some Test Content";

    final SelfDescriptionMetadata sdMeta = createSelfDescriptionMeta("TestSd/1", "TestUser/1",
        OffsetDateTime.parse("2022-01-01T12:00:00Z"), OffsetDateTime.parse("2022-01-02T12:00:00Z"), content);
    final String hash = sdMeta.getSdHash();
    final VerificationResult vr = new VerificationResult("vhash", createClaims(), new ArrayList<Signature>(),
        OffsetDateTime.now(), "lifecyclestatus", "issuer", LocalDate.now());
    sdStore.storeSelfDescription(sdMeta, vr);
    assertStoredSdFiles(1);

    final SelfDescriptionMetadata byHash = sdStore.getByHash(hash);
    assertEquals(sdMeta, byHash);

    final ContentAccessor sdfileByHash = sdStore.getSDFileByHash(hash);
    assertEquals(sdfileByHash, sdMeta.getSelfDescription(),
        "Getting the SD file by hash is equal to the stored SD file");

    sdStore.deleteSelfDescription(hash);
    assertAllSdFilesDeleted();

    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash);
    });
  }

  /**
   * Test storing a self-description, and deprecating it by storing a second SD
   * with the same subjectId.
   */
  @Test
  void test02StoreAndUpdateSelfDescription() {
    log.info("test02StoreAndUpdateSelfDescription");
    final String content1 = "Some Test Content 1";
    final String content2 = "Some Test Content 2";

    final SelfDescriptionMetadata sdMeta1 = createSelfDescriptionMeta("TestSd/1", "TestUser/1",
        OffsetDateTime.parse("2022-01-01T12:00:00Z"), OffsetDateTime.parse("2022-01-02T12:00:00Z"), content1);
    final String hash1 = sdMeta1.getSdHash();
    sdMeta1.setSelfDescription(new ContentAccessorDirect(content1));
    final VerificationResult vr1 = new VerificationResult("vhash1", createClaims(), new ArrayList<Signature>(),
        OffsetDateTime.now(), "lifecyclestatus1", "issuer1", LocalDate.now());
    sdStore.storeSelfDescription(sdMeta1, vr1);
    assertStoredSdFiles(1);

    final SelfDescriptionMetadata sdMeta2 = createSelfDescriptionMeta("TestSd/1", "TestUser/1",
        OffsetDateTime.parse("2022-01-01T13:00:00Z"), OffsetDateTime.parse("2022-01-02T13:00:00Z"), content2);
    final String hash2 = sdMeta2.getSdHash();
    final VerificationResult vr2 = new VerificationResult("vhash2", createClaims(), new ArrayList<Signature>(),
        OffsetDateTime.now(), "lifecyclestatus2", "issuer2", LocalDate.now());
    sdStore.storeSelfDescription(sdMeta2, vr2);
    assertStoredSdFiles(2);

    final SelfDescriptionMetadata byHash1 = sdStore.getByHash(hash1);
    assertEquals(SelfDescriptionStatus.DEPRECATED, byHash1.getStatus(),
        "First self-description should have been depricated.");
    Assertions.assertTrue(byHash1.getStatusDatetime().isAfter(sdMeta1.getStatusDatetime()));
    final SelfDescriptionMetadata byHash2 = sdStore.getByHash(hash2);
    assertEquals(sdMeta2, byHash2);

    sdStore.deleteSelfDescription(hash1);
    sdStore.deleteSelfDescription(hash2);
    assertAllSdFilesDeleted();

    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash1);
    });
    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash2);
    });
  }

  @Disabled("TODO Check why self-description is deprecated and if this is intended behavior.")
  @Test
  void test03StoreDuplicateSelfDescription() {
    log.info("test03StoreDuplicateSelfDescription");
    final String content1 = "Some Test Content";

    final SelfDescriptionMetadata sdMeta1 = createSelfDescriptionMeta("TestSd/1", "TestUser/1",
        OffsetDateTime.parse("2022-01-01T12:00:00Z"), OffsetDateTime.parse("2022-01-02T12:00:00Z"), content1);
    final String hash1 = sdMeta1.getSdHash();
    final VerificationResult vr1 = new VerificationResult("vhash1", createClaims(), new ArrayList<Signature>(),
        OffsetDateTime.now(), "lifecyclestatus1", "issuer1", LocalDate.now());
    sdStore.storeSelfDescription(sdMeta1, vr1);
    assertStoredSdFiles(1);

    final SelfDescriptionMetadata sdMeta2 = createSelfDescriptionMeta("TestSd/1", "TestUser/1",
        OffsetDateTime.parse("2022-01-01T13:00:00Z"), OffsetDateTime.parse("2022-01-02T13:00:00Z"), content1);
    Assertions.assertThrows(ConflictException.class, () -> {
      final VerificationResult vr2 = new VerificationResult("vhash2", createClaims(), new ArrayList<Signature>(),
          OffsetDateTime.now(), "lifecyclestatus2", "issuer2", LocalDate.now());
      sdStore.storeSelfDescription(sdMeta2, vr2);
    });

    final int count = IterableUtils.size(fileStore.getFileIterable(SelfDescriptionStore.STORE_NAME));
    assertEquals(1, count, "Second file should not have been stored.");

    final SelfDescriptionMetadata byHash1 = sdStore.getByHash(hash1);
    final SelfDescriptionStatus status1 = byHash1.getStatus();
    assertEquals(SelfDescriptionStatus.ACTIVE, status1, "First self-description should stay active.");

    sdStore.deleteSelfDescription(hash1);
    assertAllSdFilesDeleted();

    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash1);
    });
  }

  /**
   * Test storing a self-description, and updating the status.
   */
  @Test
  void test04ChangeSelfDescriptionStatus() {
    log.info("test04ChangeSelfDescriptionStatus");
    final String content = "Some Test Content";

    final SelfDescriptionMetadata sdMeta = createSelfDescriptionMeta("TestSd/1", "TestUser/1",
        OffsetDateTime.parse("2022-01-01T12:00:00Z"), OffsetDateTime.parse("2022-01-02T12:00:00Z"), content);
    final String hash = sdMeta.getSdHash();
    final VerificationResult vr = new VerificationResult("vhash", createClaims(), new ArrayList<Signature>(),
        OffsetDateTime.now(), "lifecyclestatus", "issuer", LocalDate.now());
    sdStore.storeSelfDescription(sdMeta, vr);
    assertStoredSdFiles(1);

    SelfDescriptionMetadata byHash = sdStore.getByHash(hash);
    assertEquals(sdMeta, byHash);

    sdStore.changeLifeCycleStatus(hash, SelfDescriptionStatus.REVOKED);
    byHash = sdStore.getByHash(hash);
    assertEquals(SelfDescriptionStatus.REVOKED, byHash.getStatus(), "Status should have been changed to 'revoked'");

    Assertions.assertThrows(ConflictException.class, () -> {
      sdStore.changeLifeCycleStatus(hash, SelfDescriptionStatus.ACTIVE);
    });
    byHash = sdStore.getByHash(hash);
    assertEquals(SelfDescriptionStatus.REVOKED, byHash.getStatus(),
        "Status should not have been changed from 'revoked' to 'active'.");

    sdStore.deleteSelfDescription(hash);
    assertAllSdFilesDeleted();

    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash);
    });
  }

  /**
   * Test applying an SD filter on matching issuer.
   */
  @Test
  void test05FilterMatchingIssuer() {
    log.info("test05FilterMatchingIssuer");
    final String id = "TestSd/1";
    final String issuer = "TestUser/1";
    final String content = "Test: Fetch SD Meta Data via SD Filter, test for matching issuer";
    final OffsetDateTime statusTime = OffsetDateTime.parse("2022-01-01T12:00:00Z");
    final OffsetDateTime uploadTime = OffsetDateTime.parse("2022-01-02T12:00:00Z");
    final SelfDescriptionMetadata sdMeta = createSelfDescriptionMeta(id, issuer, statusTime, uploadTime, content);
    final String hash = sdMeta.getSdHash();
    final VerificationResult vr = new VerificationResult("vhash", createClaims(), new ArrayList<Signature>(),
        OffsetDateTime.now(), "lifecyclestatus", "issuer", LocalDate.now());
    sdStore.storeSelfDescription(sdMeta, vr);
    assertStoredSdFiles(1);

    final SdFilter filterParams = new SdFilter();
    filterParams.setIssuer(issuer);
    final List<SelfDescriptionMetadata> byFilter = sdStore.getByFilter(filterParams);
    final int matchCount = byFilter.size();
    log.info("filter returned {} match(es)", matchCount);
    assertEquals(1, matchCount, "expected 1 filter match, but got " + matchCount);
    assertEquals(sdMeta.getId(), byFilter.get(0).getId());

    sdStore.deleteSelfDescription(hash);
    assertAllSdFilesDeleted();

    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash);
    });
    log.info("#### Test 05 succeeded.");
  }

  /**
   * Test applying an SD filter on non-matching issuer.
   */
  @Test
  void test06FilterNonMatchingIssuer() {
    log.info("test06FilterNonMatchingIssuer");
    final String id = "TestSd/1";
    final String issuer = "TestUser/1";
    final String otherIssuer = "TestUser/2";
    final String content = "Test: Fetch SD Meta Data via SD Filter, test for non-matching issuer";
    final OffsetDateTime statusTime = OffsetDateTime.parse("2022-01-01T12:00:00Z");
    final OffsetDateTime uploadTime = OffsetDateTime.parse("2022-01-02T12:00:00Z");
    final SelfDescriptionMetadata sdMeta = createSelfDescriptionMeta(id, issuer, statusTime, uploadTime, content);
    final String hash = sdMeta.getSdHash();
    final VerificationResult vr = new VerificationResult("vhash", createClaims(), new ArrayList<Signature>(),
        OffsetDateTime.now(), "lifecyclestatus", "issuer", LocalDate.now());
    sdStore.storeSelfDescription(sdMeta, vr);
    assertStoredSdFiles(1);

    final SdFilter filterParams = new SdFilter();
    filterParams.setIssuer(otherIssuer);
    final List<SelfDescriptionMetadata> byFilter = sdStore.getByFilter(filterParams);
    final int matchCount = byFilter.size();
    log.info("filter returned {} match(es)", matchCount);
    assertEquals(0, matchCount, "expected 0 filter matches, but got " + matchCount);

    sdStore.deleteSelfDescription(hash);
    assertAllSdFilesDeleted();

    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash);
    });

    log.info("#### Test 06 succeeded.");
  }

  /**
   * Test applying an SD filter on matching status start time.
   */
  @Test
  void test07FilterMatchingStatusTimeStart() {
    log.info("test07FilterMatchingStatusTimeStart");
    final String id = "TestSd/1";
    final String issuer = "TestUser/1";
    final String content = "Test: Fetch SD Meta Data via SD Filter, test for matching status time start";
    final OffsetDateTime statusTime = OffsetDateTime.parse("2022-01-01T12:00:00Z");
    final OffsetDateTime statusTimeStart = OffsetDateTime.parse("2021-01-01T12:00:00Z");
    final OffsetDateTime statusTimeEnd = OffsetDateTime.parse("2022-01-01T12:00:00Z");
    final OffsetDateTime uploadTime = OffsetDateTime.parse("2022-01-02T12:00:00Z");
    final SelfDescriptionMetadata sdMeta = createSelfDescriptionMeta(id, issuer, statusTime, uploadTime, content);
    final String hash = sdMeta.getSdHash();
    final VerificationResult vr = new VerificationResult("vhash", createClaims(), new ArrayList<Signature>(),
        OffsetDateTime.now(), "lifecyclestatus", "issuer", LocalDate.now());
    sdStore.storeSelfDescription(sdMeta, vr);
    assertStoredSdFiles(1);

    final SdFilter filterParams = new SdFilter();
    filterParams.setStatusTimeRange(statusTimeStart.toInstant(), statusTimeEnd.toInstant());
    final List<SelfDescriptionMetadata> byFilter = sdStore.getByFilter(filterParams);
    final int matchCount = byFilter.size();
    log.info("filter returned {} match(es)", matchCount);
    assertEquals(1, matchCount, "expected 1 filter match, but got " + matchCount);
    assertEquals(sdMeta.getId(), byFilter.get(0).getId());

    sdStore.deleteSelfDescription(hash);
    assertAllSdFilesDeleted();

    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash);
    });
    log.info("#### Test 07 succeeded.");
  }

  /**
   * Test applying an SD filter on non-matching issuer.
   */
  @Test
  void test08FilterNonMatchingStatusTimeStart() {
    log.info("test08FilterNonMatchingStatusTimeStart");
    final String id = "TestSd/1";
    final String issuer = "TestUser/1";
    final String content = "Test: Fetch SD Meta Data via SD Filter, test for non-matching issuer";
    final OffsetDateTime statusTime = OffsetDateTime.parse("2022-01-01T12:00:00Z");
    final OffsetDateTime statusTimeStart = OffsetDateTime.parse("2022-02-01T12:00:00Z");
    final OffsetDateTime statusTimeEnd = OffsetDateTime.parse("2023-02-01T12:00:00Z");
    final OffsetDateTime uploadTime = OffsetDateTime.parse("2022-01-02T12:00:00Z");
    final SelfDescriptionMetadata sdMeta = createSelfDescriptionMeta(id, issuer, statusTime, uploadTime, content);
    final String hash = sdMeta.getSdHash();
    final VerificationResult vr = new VerificationResult("vhash", createClaims(), new ArrayList<Signature>(),
        OffsetDateTime.now(), "lifecyclestatus", "issuer", LocalDate.now());
    sdStore.storeSelfDescription(sdMeta, vr);
    assertStoredSdFiles(1);

    final SdFilter filterParams = new SdFilter();
    filterParams.setStatusTimeRange(statusTimeStart.toInstant(), statusTimeEnd.toInstant());
    final List<SelfDescriptionMetadata> byFilter = sdStore.getByFilter(filterParams);
    final int matchCount = byFilter.size();
    log.info("filter returned {} match(es)", matchCount);
    assertEquals(0, matchCount, "expected 0 filter matches, but got " + matchCount);

    sdStore.deleteSelfDescription(hash);
    assertAllSdFilesDeleted();
    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash);
    });

    log.info("#### Test 08 succeeded.");
  }

  /**
   * Test applying an SD filter that matches multiple records.
   */
  @Test
  void test09FilterMatchingMultipleRecords() {
    log.info("test09FilterMatchingMultipleRecords");
    final String id1 = "TestSd/1";
    final String id2 = "TestSd/2";
    final String id3 = "TestSd/3";
    final String issuer1 = "TestUser/1";
    final String issuer2 = "TestUser/2";
    final String issuer3 = "TestUser/3";
    final String content1 = "Test: Fetch SD Meta Data via SD Filter, test for matching status time start (1/3)";
    final String content2 = "Test: Fetch SD Meta Data via SD Filter, test for matching status time start (2/3)";
    final String content3 = "Test: Fetch SD Meta Data via SD Filter, test for matching status time start (3/3)";
    final OffsetDateTime statusTime1 = OffsetDateTime.parse("2022-01-01T12:00:00Z");
    final OffsetDateTime statusTime2 = OffsetDateTime.parse("2022-01-02T12:00:00Z");
    final OffsetDateTime statusTime3 = OffsetDateTime.parse("2022-01-03T12:00:00Z");
    final OffsetDateTime statusTimeStart = OffsetDateTime.parse("2022-01-01T12:00:00Z");
    final OffsetDateTime statusTimeEnd = OffsetDateTime.parse("2022-01-02T12:00:00Z");
    final OffsetDateTime uploadTime1 = OffsetDateTime.parse("2022-02-01T12:00:00Z");
    final OffsetDateTime uploadTime2 = OffsetDateTime.parse("2022-02-01T12:00:00Z");
    final OffsetDateTime uploadTime3 = OffsetDateTime.parse("2022-02-01T12:00:00Z");
    final SelfDescriptionMetadata sdMeta1 = createSelfDescriptionMeta(id1, issuer1, statusTime1, uploadTime1, content1);
    final SelfDescriptionMetadata sdMeta2 = createSelfDescriptionMeta(id2, issuer2, statusTime2, uploadTime2, content2);
    final SelfDescriptionMetadata sdMeta3 = createSelfDescriptionMeta(id3, issuer3, statusTime3, uploadTime3, content3);
    final String hash1 = sdMeta1.getSdHash();
    final String hash2 = sdMeta2.getSdHash();
    final String hash3 = sdMeta3.getSdHash();
    final VerificationResult vr1 = new VerificationResult("vhash1", createClaims(), new ArrayList<Signature>(),
        OffsetDateTime.now(), "lifecyclestatus1", "issuer1", LocalDate.now());
    final VerificationResult vr2 = new VerificationResult("vhash2", createClaims(), new ArrayList<Signature>(),
        OffsetDateTime.now(), "lifecyclestatus2", "issuer2", LocalDate.now());
    final VerificationResult vr3 = new VerificationResult("vhash3", createClaims(), new ArrayList<Signature>(),
        OffsetDateTime.now(), "lifecyclestatus3", "issuer3", LocalDate.now());
    sdStore.storeSelfDescription(sdMeta1, vr1);
    sdStore.storeSelfDescription(sdMeta2, vr2);
    sdStore.storeSelfDescription(sdMeta3, vr3);
    assertStoredSdFiles(3);

    final SdFilter filterParams = new SdFilter();
    filterParams.setStatusTimeRange(statusTimeStart.toInstant(), statusTimeEnd.toInstant());
    final List<SelfDescriptionMetadata> byFilter = sdStore.getByFilter(filterParams);
    final int matchCount = byFilter.size();
    log.info("filter returned {} match(es)", matchCount);
    assertEquals(2, matchCount, "expected 2 filter match, but got " + matchCount);
    final SelfDescriptionMetadata filterSdMeta1 = byFilter.get(0);
    final SelfDescriptionMetadata filterSdMeta2 = byFilter.get(1);
    assertEquals(true, sdMeta1.getId().equals(filterSdMeta1.getId()) || sdMeta1.getId().equals(filterSdMeta2.getId()),
        "expected filter match sdMeta1 missing in results");
    assertEquals(true, sdMeta2.getId().equals(filterSdMeta1.getId()) || sdMeta2.getId().equals(filterSdMeta2.getId()),
        "expected filter match sdMeta2 missing in results");

    sdStore.deleteSelfDescription(hash1);
    sdStore.deleteSelfDescription(hash2);
    sdStore.deleteSelfDescription(hash3);
    assertAllSdFilesDeleted();

    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash1);
    });
    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash2);
    });
    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash3);
    });

    log.info("#### Test 09 succeeded.");
  }

  /**
   * Test applying an empty SD filter for matching all records.
   */
  @Test
  void test10EmptyFilterMatchingMultipleRecords() {
    log.info("test10EmptyFilterMatchingAllRecords");
    final String id1 = "TestSd/1";
    final String id2 = "TestSd/2";
    final String id3 = "TestSd/3";
    final String issuer1 = "TestUser/1";
    final String issuer2 = "TestUser/2";
    final String issuer3 = "TestUser/3";
    final String content1 = "Test: Fetch SD Meta Data via SD Filter, test for matching empty filter (1/3)";
    final String content2 = "Test: Fetch SD Meta Data via SD Filter, test for matching empty filter (2/3)";
    final String content3 = "Test: Fetch SD Meta Data via SD Filter, test for matching empty filter (3/3)";
    final OffsetDateTime statusTime1 = OffsetDateTime.parse("2022-01-01T12:00:00Z");
    final OffsetDateTime statusTime2 = OffsetDateTime.parse("2022-01-02T12:00:00Z");
    final OffsetDateTime statusTime3 = OffsetDateTime.parse("2022-01-03T12:00:00Z");
    final OffsetDateTime uploadTime1 = OffsetDateTime.parse("2022-02-01T12:00:00Z");
    final OffsetDateTime uploadTime2 = OffsetDateTime.parse("2022-02-01T12:00:00Z");
    final OffsetDateTime uploadTime3 = OffsetDateTime.parse("2022-02-01T12:00:00Z");
    final SelfDescriptionMetadata sdMeta1 = createSelfDescriptionMeta(id1, issuer1, statusTime1, uploadTime1, content1);
    final SelfDescriptionMetadata sdMeta2 = createSelfDescriptionMeta(id2, issuer2, statusTime2, uploadTime2, content2);
    final SelfDescriptionMetadata sdMeta3 = createSelfDescriptionMeta(id3, issuer3, statusTime3, uploadTime3, content3);
    final String hash1 = sdMeta1.getSdHash();
    final String hash2 = sdMeta2.getSdHash();
    final String hash3 = sdMeta3.getSdHash();
    final VerificationResult vr1 = new VerificationResult("vhash1", createClaims(), new ArrayList<Signature>(),
        OffsetDateTime.now(), "lifecyclestatus1", "issuer1", LocalDate.now());
    final VerificationResult vr2 = new VerificationResult("vhash2", createClaims(), new ArrayList<Signature>(),
        OffsetDateTime.now(), "lifecyclestatus2", "issuer2", LocalDate.now());
    final VerificationResult vr3 = new VerificationResult("vhash3", createClaims(), new ArrayList<Signature>(),
        OffsetDateTime.now(), "lifecyclestatus3", "issuer3", LocalDate.now());
    sdStore.storeSelfDescription(sdMeta1, vr1);
    sdStore.storeSelfDescription(sdMeta2, vr2);
    sdStore.storeSelfDescription(sdMeta3, vr3);
    assertStoredSdFiles(3);

    final SdFilter filterParams = new SdFilter();
    final List<SelfDescriptionMetadata> byFilter = sdStore.getByFilter(filterParams);
    final int matchCount = byFilter.size();
    log.info("filter returned {} match(es)", matchCount);
    assertEquals(3, matchCount, "expected 3 filter match, but got " + matchCount);
    final SelfDescriptionMetadata filterSdMeta1 = byFilter.get(0);
    final SelfDescriptionMetadata filterSdMeta2 = byFilter.get(1);
    final SelfDescriptionMetadata filterSdMeta3 = byFilter.get(2);
    assertEquals(true, sdMeta1.getId().equals(filterSdMeta1.getId()) || sdMeta1.getId().equals(filterSdMeta2.getId())
        || sdMeta1.getId().equals(filterSdMeta3.getId()), "expected filter match sdMeta1 missing in results");
    assertEquals(true, sdMeta2.getId().equals(filterSdMeta1.getId()) || sdMeta2.getId().equals(filterSdMeta2.getId())
        || sdMeta2.getId().equals(filterSdMeta3.getId()), "expected filter match sdMeta2 missing in results");
    assertEquals(true, sdMeta3.getId().equals(filterSdMeta1.getId()) || sdMeta3.getId().equals(filterSdMeta2.getId())
        || sdMeta3.getId().equals(filterSdMeta3.getId()), "expected filter match sdMeta3 missing in results");
    sdStore.deleteSelfDescription(hash1);
    sdStore.deleteSelfDescription(hash2);
    sdStore.deleteSelfDescription(hash3);
    assertAllSdFilesDeleted();

    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash1);
    });
    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash2);
    });
    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash3);
    });

    log.info("#### Test 10 succeeded.");
  }

  /**
   * Test applying an SD filter on non-matching validator.
   */
  @Test
  void test11FilterNonMatchingValidator() {
    log.info("test11FilterNonMatchingValidator");
    final String id = "TestSd/1";
    final String validatorId = "TestSd/0815";
    final String issuer = "TestUser/1";
    final String content = "Test: Fetch SD Meta Data via SD Filter, test for non-matching validator";
    final OffsetDateTime statusTime = OffsetDateTime.parse("2022-01-01T12:00:00Z");
    final OffsetDateTime uploadTime = OffsetDateTime.parse("2022-01-02T12:00:00Z");
    final SelfDescriptionMetadata sdMeta = createSelfDescriptionMeta(id, issuer, statusTime, uploadTime, content);
    final String hash = sdMeta.getSdHash();
    final VerificationResult vr = new VerificationResult("vhash", createClaims(), new ArrayList<Signature>(),
        OffsetDateTime.now(), "lifecyclestatus", "issuer", LocalDate.now());
    sdStore.storeSelfDescription(sdMeta, vr);
    assertStoredSdFiles(1);

    final SdFilter filterParams = new SdFilter();
    filterParams.setValidator(validatorId);
    final List<SelfDescriptionMetadata> byFilter = sdStore.getByFilter(filterParams);
    final int matchCount = byFilter.size();
    log.info("filter returned {} match(es)", matchCount);
    assertEquals(0, matchCount, "expected 0 filter matches, but got " + matchCount);

    sdStore.deleteSelfDescription(hash);
    assertAllSdFilesDeleted();

    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash);
    });

    log.info("#### Test 11 succeeded.");
  }

  /**
   * Test applying an SD filter with limited number of results.
   */
  @Test
  void test12FilterLimit() {
    log.info("test12FilterLimit");
    final String id1 = "TestSd/1";
    final String id2 = "TestSd/2";
    final String id3 = "TestSd/3";
    final String issuer1 = "TestUser/1";
    final String issuer2 = "TestUser/2";
    final String issuer3 = "TestUser/3";
    final String content1 = "Test: Fetch SD Meta Data via SD Filter, test for matching empty filter (1/3)";
    final String content2 = "Test: Fetch SD Meta Data via SD Filter, test for matching empty filter (2/3)";
    final String content3 = "Test: Fetch SD Meta Data via SD Filter, test for matching empty filter (3/3)";
    final OffsetDateTime statusTime1 = OffsetDateTime.parse("2022-01-01T12:00:00Z");
    final OffsetDateTime statusTime2 = OffsetDateTime.parse("2022-01-02T12:00:00Z");
    final OffsetDateTime statusTime3 = OffsetDateTime.parse("2022-01-03T12:00:00Z");
    final OffsetDateTime uploadTime1 = OffsetDateTime.parse("2022-02-01T12:00:00Z");
    final OffsetDateTime uploadTime2 = OffsetDateTime.parse("2022-02-01T12:00:00Z");
    final OffsetDateTime uploadTime3 = OffsetDateTime.parse("2022-02-01T12:00:00Z");
    final SelfDescriptionMetadata sdMeta1 = createSelfDescriptionMeta(id1, issuer1, statusTime1, uploadTime1, content1);
    final SelfDescriptionMetadata sdMeta2 = createSelfDescriptionMeta(id2, issuer2, statusTime2, uploadTime2, content2);
    final SelfDescriptionMetadata sdMeta3 = createSelfDescriptionMeta(id3, issuer3, statusTime3, uploadTime3, content3);
    final String hash1 = sdMeta1.getSdHash();
    final String hash2 = sdMeta2.getSdHash();
    final String hash3 = sdMeta3.getSdHash();
    final VerificationResult vr1 = new VerificationResult("vhash1", createClaims(), new ArrayList<Signature>(),
        OffsetDateTime.now(), "lifecyclestatus1", "issuer1", LocalDate.now());
    final VerificationResult vr2 = new VerificationResult("vhash2", createClaims(), new ArrayList<Signature>(),
        OffsetDateTime.now(), "lifecyclestatus2", "issuer2", LocalDate.now());
    final VerificationResult vr3 = new VerificationResult("vhash3", createClaims(), new ArrayList<Signature>(),
        OffsetDateTime.now(), "lifecyclestatus3", "issuer3", LocalDate.now());
    sdStore.storeSelfDescription(sdMeta1, vr1);
    sdStore.storeSelfDescription(sdMeta2, vr2);
    sdStore.storeSelfDescription(sdMeta3, vr3);
    assertStoredSdFiles(3);

    final SdFilter filterParams = new SdFilter();
    filterParams.setLimit(2);
    final List<SelfDescriptionMetadata> byFilter = sdStore.getByFilter(filterParams);
    final int matchCount = byFilter.size();
    log.info("filter returned {} match(es)", matchCount);
    assertEquals(2, matchCount, "expected 2 filter match, but got " + matchCount);
    sdStore.deleteSelfDescription(hash1);
    sdStore.deleteSelfDescription(hash2);
    sdStore.deleteSelfDescription(hash3);
    assertAllSdFilesDeleted();

    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash1);
    });
    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash2);
    });
    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash3);
    });
    log.info("#### Test 12 succeeded.");
  }

  /**
   * Test of changeLifeCycleStatus method, of class SelfDescriptionStore.
   */
  @Test
  @Disabled("TODO review the generated test code and remove the default call to fail.")
  void testChangeLifeCycleStatus() {
    System.out.println("changeLifeCycleStatus");
    String hash = "";
    SelfDescriptionStatus targetStatus = null;
    sdStore.changeLifeCycleStatus(hash, targetStatus);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of deleteSelfDescription method, of class SelfDescriptionStore.
   */
  @Test
  @Disabled("TODO review the generated test code and remove the default call to fail.")
  void testDeleteSelfDescription() {
    System.out.println("deleteSelfDescription");
    String hash = "";
    sdStore.deleteSelfDescription(hash);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }
}
