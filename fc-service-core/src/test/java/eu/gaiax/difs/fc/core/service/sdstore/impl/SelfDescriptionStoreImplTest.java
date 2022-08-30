package eu.gaiax.difs.fc.core.service.sdstore.impl;

import eu.gaiax.difs.fc.core.config.DatabaseConfig;
import eu.gaiax.difs.fc.api.generated.model.SelfDescriptionStatus;
import eu.gaiax.difs.fc.core.exception.ConflictException;
import eu.gaiax.difs.fc.core.exception.NotFoundException;
import eu.gaiax.difs.fc.core.pojo.ContentAccessor;
import eu.gaiax.difs.fc.core.pojo.ContentAccessorDirect;
import eu.gaiax.difs.fc.core.pojo.SdFilter;
import eu.gaiax.difs.fc.core.pojo.SelfDescriptionMetadata;
import eu.gaiax.difs.fc.core.service.filestore.impl.FileStoreImpl;
import eu.gaiax.difs.fc.core.service.sdstore.SelfDescriptionStore;
import eu.gaiax.difs.fc.core.util.HashUtils;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.MethodName.class)
@SpringBootTest
@ActiveProfiles("tests-sdstore")
@ContextConfiguration(classes = {SelfDescriptionStoreImplTest.TestApplication.class, DatabaseConfig.class, //SelfDescriptionStoreImplTest.class, 
        SelfDescriptionStoreImpl.class, FileStoreImpl.class})
@DirtiesContext
@Transactional
@Slf4j
@AutoConfigureEmbeddedDatabase(provider = DatabaseProvider.ZONKY)
//@Import(EmbeddedPostgresConfig.class)
public class SelfDescriptionStoreImplTest {

  @SpringBootApplication
  public static class TestApplication {

    public static void main(String[] args) {
      SpringApplication.run(TestApplication.class, args);
    }
  }

  @Autowired
  private SelfDescriptionStore sdStore;

  @Autowired
  private FileStoreImpl fileStore;

  @Autowired
  private SessionFactory sessionFactory;

  private SelfDescriptionMetadata createSelfDescriptionMeta(String id, String issuer, OffsetDateTime sdt, OffsetDateTime udt, String content) {
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

  /**
   * Test storing a Self-Description, ensuring it creates exactly one file on
   * disk, retrieving it by hash, and deleting it again.
   */
  @Test
  public void test01StoreSelfDescription() {
    log.info("test01StoreSelfDescription");
    final String content = "Some Test Content";

    SelfDescriptionMetadata sdMeta = createSelfDescriptionMeta("TestSd/1", "TestUser/1", OffsetDateTime.parse("2022-01-01T12:00:00Z"), OffsetDateTime.parse("2022-01-02T12:00:00Z"), content);
    final String hash = sdMeta.getSdHash();
    sdStore.storeSelfDescription(sdMeta, null);

    int count = 0;
    for (File file : fileStore.getFileIterable(SelfDescriptionStore.STORE_NAME)) {
      count++;
    }
    assertEquals(1, count, "Storing one file should result in exactly one file in the store.");

    SelfDescriptionMetadata byHash = sdStore.getByHash(hash);
    assertEquals(sdMeta, byHash);

    ContentAccessor sdfileByHash = sdStore.getSDFileByHash(hash);
    assertEquals(sdfileByHash, sdMeta.getSelfDescription(), "Getting the SD file by hash is equal to the stored SD file");

    sdStore.deleteSelfDescription(hash);
    count = 0;
    for (File file : fileStore.getFileIterable(SelfDescriptionStore.STORE_NAME)) {
      count++;
    }
    assertEquals(0, count, "Deleting the last file should result in exactly 0 files in the store.");

    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash);
    });
  }

  /**
   * Test storing a Self-Description, and deprecating it by storing a second SD
   * with the same subject.
   */
  @Test
  public void test02StoreAndUpdateSelfDescription() {
    log.info("test02StoreAndUpdateSelfDescription");
    final String content1 = "Some Test Content 1";
    final String content2 = "Some Test Content 2";

    final SelfDescriptionMetadata sdMeta1 = createSelfDescriptionMeta("TestSd/1", "TestUser/1", OffsetDateTime.parse("2022-01-01T12:00:00Z"), OffsetDateTime.parse("2022-01-02T12:00:00Z"), content1);
    final String hash1 = sdMeta1.getSdHash();
    sdMeta1.setSelfDescription(new ContentAccessorDirect(content1));
    sdStore.storeSelfDescription(sdMeta1, null);

    int count = 0;
    for (File file : fileStore.getFileIterable(SelfDescriptionStore.STORE_NAME)) {
      count++;
    }
    assertEquals(1, count, "Storing one file should result in exactly one file in the store.");

    final SelfDescriptionMetadata sdMeta2 = createSelfDescriptionMeta("TestSd/1", "TestUser/1", OffsetDateTime.parse("2022-01-01T13:00:00Z"), OffsetDateTime.parse("2022-01-02T13:00:00Z"), content2);
    final String hash2 = sdMeta2.getSdHash();
    sdStore.storeSelfDescription(sdMeta2, null);

    count = 0;
    for (File file : fileStore.getFileIterable(SelfDescriptionStore.STORE_NAME)) {
      count++;
    }
    assertEquals(2, count, "Storing two files should result in exactly two files in the store.");

    SelfDescriptionMetadata byHash1 = sdStore.getByHash(hash1);
    assertEquals(SelfDescriptionStatus.DEPRECATED, byHash1.getStatus(), "First SelfDescription should have been depricated.");
    Assertions.assertTrue(byHash1.getStatusDatetime().isAfter(sdMeta1.getStatusDatetime()));
    SelfDescriptionMetadata byHash2 = sdStore.getByHash(hash2);
    assertEquals(sdMeta2, byHash2);

    sdStore.deleteSelfDescription(hash1);
    sdStore.deleteSelfDescription(hash2);
    count = 0;
    for (File file : fileStore.getFileIterable(SelfDescriptionStore.STORE_NAME)) {
      count++;
    }
    assertEquals(0, count, "Storing all files should result in exactly 0 files in the store.");

    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash1);
    });
    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash2);
    });
  }

  @Test
  public void test03StoreDuplicateSelfDescription() {
    log.info("test03StoreDuplicateSelfDescription");
    final String content1 = "Some Test Content";

    final SelfDescriptionMetadata sdMeta1 = createSelfDescriptionMeta("TestSd/1", "TestUser/1", OffsetDateTime.parse("2022-01-01T12:00:00Z"), OffsetDateTime.parse("2022-01-02T12:00:00Z"), content1);
    final String hash1 = sdMeta1.getSdHash();
    sdStore.storeSelfDescription(sdMeta1, null);

    int count = 0;
    for (File file : fileStore.getFileIterable(SelfDescriptionStore.STORE_NAME)) {
      count++;
    }
    assertEquals(1, count, "Storing one file should result in exactly one file in the store.");

    // For this test we have to "break" the spring transaction by manually committing.
    // Otherwise the error we cause in the next step rolls back our work above.
    final Session currentSession = sessionFactory.getCurrentSession();
    currentSession.getTransaction().commit();
    Transaction transaction = currentSession.beginTransaction();

    final SelfDescriptionMetadata sdMeta2 = createSelfDescriptionMeta("TestSd/1", "TestUser/1", OffsetDateTime.parse("2022-01-01T13:00:00Z"), OffsetDateTime.parse("2022-01-02T13:00:00Z"), content1);
    final String hash2 = sdMeta2.getSdHash();
    Assertions.assertThrows(ConflictException.class, () -> {
      sdStore.storeSelfDescription(sdMeta2, null);
    });

    count = 0;
    for (File file : fileStore.getFileIterable(SelfDescriptionStore.STORE_NAME)) {
      count++;
    }
    assertEquals(1, count, "Second file should not have been stored.");
    // Previous forced error will have rolled back the transaction.
    if (!transaction.isActive()) {
      transaction = currentSession.beginTransaction();
    }

    SelfDescriptionMetadata byHash1 = sdStore.getByHash(hash1);
    assertEquals(SelfDescriptionStatus.ACTIVE, byHash1.getStatus(), "First SelfDescription should not have been depricated.");

    sdStore.deleteSelfDescription(hash1);
    count = 0;
    for (File file : fileStore.getFileIterable(SelfDescriptionStore.STORE_NAME)) {
      count++;
    }
    assertEquals(0, count, "Storing all files should result in exactly 0 files in the store.");

    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash1);
    });
    // We now have to commit, to ensure the test entities are removed from the database again.
    transaction.commit();
    currentSession.beginTransaction();
  }

  /**
   * Test storing a Self-Description, and updating the status.
   */
  @Test
  public void test04ChangeSelfDescriptionStatus() {
    log.info("test04ChangeSelfDescriptionStatus");
    final String content = "Some Test Content";

    SelfDescriptionMetadata sdMeta = createSelfDescriptionMeta("TestSd/1", "TestUser/1", OffsetDateTime.parse("2022-01-01T12:00:00Z"), OffsetDateTime.parse("2022-01-02T12:00:00Z"), content);
    final String hash = sdMeta.getSdHash();
    sdStore.storeSelfDescription(sdMeta, null);

    int count = 0;
    for (File file : fileStore.getFileIterable(SelfDescriptionStore.STORE_NAME)) {
      count++;
    }
    assertEquals(1, count, "Storing one file should result in exactly one file in the store.");

    SelfDescriptionMetadata byHash = sdStore.getByHash(hash);
    assertEquals(sdMeta, byHash);

    sdStore.changeLifeCycleStatus(hash, SelfDescriptionStatus.REVOKED);
    byHash = sdStore.getByHash(hash);
    assertEquals(SelfDescriptionStatus.REVOKED, byHash.getStatus(), "Status should have been changed to 'revoked'");

    Assertions.assertThrows(ConflictException.class, () -> {
      sdStore.changeLifeCycleStatus(hash, SelfDescriptionStatus.ACTIVE);
    });
    byHash = sdStore.getByHash(hash);
    assertEquals(SelfDescriptionStatus.REVOKED, byHash.getStatus(), "Status should not have been changed from 'revoked' to 'active'.");

    sdStore.deleteSelfDescription(hash);
    count = 0;
    for (File file : fileStore.getFileIterable(SelfDescriptionStore.STORE_NAME)) {
      count++;
    }
    assertEquals(0, count, "Deleting the last file should result in exactly 0 files in the store.");

    Assertions.assertThrows(NotFoundException.class, () -> {
      sdStore.getByHash(hash);
    });
  }

  /**
   * Test of getAllSelfDescriptions method, of class SelfDescriptionStore.
   */
  @Test
  @Disabled
  public void testGetAllSelfDescriptions() {
    System.out.println("getAllSelfDescriptions");
    int offset = 0;
    int limit = 0;
    List<SelfDescriptionMetadata> expResult = null;
    List<SelfDescriptionMetadata> result = sdStore.getAllSelfDescriptions(offset, limit);
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of getByFilter method, of class SelfDescriptionStore.
   */
  @Test
  @Disabled
  public void testGetByFilter() {
    System.out.println("getByFilter");
    SdFilter filterParams = null;
    List<SelfDescriptionMetadata> expResult = null;
    List<SelfDescriptionMetadata> result = sdStore.getByFilter(filterParams);
    assertEquals(expResult, result);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

  /**
   * Test of changeLifeCycleStatus method, of class SelfDescriptionStore.
   */
  @Test
  @Disabled
  public void testChangeLifeCycleStatus() {
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
  @Disabled
  public void testDeleteSelfDescription() {
    System.out.println("deleteSelfDescription");
    String hash = "";
    sdStore.deleteSelfDescription(hash);
    // TODO review the generated test code and remove the default call to fail.
    fail("The test case is a prototype.");
  }

}
