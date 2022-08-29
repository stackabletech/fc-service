package eu.gaiax.difs.fc.core.service.schemastore.impl;

import eu.gaiax.difs.fc.core.config.DatabaseConfig;
import eu.gaiax.difs.fc.core.exception.ConflictException;
import eu.gaiax.difs.fc.core.pojo.ContentAccessorDirect;
import eu.gaiax.difs.fc.core.service.filestore.impl.FileStoreImpl;
import eu.gaiax.difs.fc.core.service.schemastore.SchemaStore.SchemaType;
import eu.gaiax.difs.fc.core.service.schemastore.impl.SchemaStoreImpl;
import eu.gaiax.difs.fc.core.service.sdstore.impl.SelfDescriptionStoreImplTest;
import eu.gaiax.difs.fc.core.util.HashUtils;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.MethodName.class)
@SpringBootTest
@ActiveProfiles("tests-sdstore")
@ContextConfiguration(classes = {SelfDescriptionStoreImplTest.TestApplication.class, SchemaManagementImplTest.class, SchemaStoreImpl.class, FileStoreImpl.class, DatabaseConfig.class})
@DirtiesContext
@Transactional
@Slf4j
@AutoConfigureEmbeddedDatabase(provider = DatabaseProvider.ZONKY)
public class SchemaManagementImplTest {

  @Autowired
  private SchemaStoreImpl schemaStore;

  @Autowired
  private FileStoreImpl fileStore;

  /**
   * Test of verifySchema method, of class SchemaManagementImpl.
   */
  @Disabled
  @Test
  public void testVerifySchema() {
    // TODO: FIT
  }

  /**
   * Test of addSchema method, of class SchemaManagementImpl.
   */
  @Test
  public void testAddSchema() {
    log.info("testAddSchema");
    String schema1 = "Some Schema Content";

    String schemaId1 = schemaStore.addSchema(new ContentAccessorDirect(schema1));

    Map<SchemaType, List<String>> expected = new HashMap<>();
    expected.computeIfAbsent(SchemaType.SHAPE, t -> new ArrayList<>()).add(schemaId1);
    Map<SchemaType, List<String>> schemaList = schemaStore.getSchemaList();
    assertEquals(expected, schemaList);

    int count = 0;
    for (File file : fileStore.getFileIterable(SchemaStoreImpl.STORE_NAME)) {
      count++;
    }
    assertEquals(1, count, "Storing one schama should result in exactly one file in the store.");

    schemaStore.deleteSchema(schemaId1);
    count = 0;
    for (File file : fileStore.getFileIterable(SchemaStoreImpl.STORE_NAME)) {
      count++;
    }
    assertEquals(0, count, "Deleting the only file should result in exactly 0 files in the store.");
  }

  /**
   * Test of addSchema method, of class SchemaManagementImpl.
   * Adding the schema twice
   */
  @Test
  public void testAddDuplicateSchema() throws IOException {
    log.info("testAddDuplicateSchema");
    String schema1 = "Some Schema Content";
    String schema2 = "Some Schema Content";

    schemaStore.addSchema(new ContentAccessorDirect(schema1));
    assertThrowsExactly(ConflictException.class, () -> schemaStore.addSchema(new ContentAccessorDirect(schema2)));

    fileStore.deleteFile(SchemaStoreImpl.STORE_NAME, HashUtils.calculateSha256AsHex(schema2));
  }

  /**
   * Test of updateSchema method, of class SchemaManagementImpl.
   */
  @Test
  public void testUpdateSchema() throws IOException {
    log.info("UpdateSchema");
    String schema1 = "Some Schema Content";
    String schema2 = "Some Schema Content2";

    String schemaId = schemaStore.addSchema(new ContentAccessorDirect(schema1));
    schemaStore.updateSchema(schemaId, new ContentAccessorDirect(schema2));

    assertEquals(schema2, fileStore.readFile(SchemaStoreImpl.STORE_NAME, schemaId).getContentAsString(), "The content of the updated schema is stored in the schema file.");

    schemaStore.deleteSchema(schemaId);
    int count = 0;
    for (File file : fileStore.getFileIterable(SchemaStoreImpl.STORE_NAME)) {
      count++;
    }
    assertEquals(0, count, "Deleting the only file should result in exactly 0 files in the store.");

    //TODO: check if terms are updated correctly
  }

  /**
   * Test of deleteSchema method, of class SchemaManagementImpl.
   */
  @Test
  @Disabled
  public void testDeleteSchema() {
  }

  /**
   * Test of getSchemaList method, of class SchemaManagementImpl.
   */
  @Test
  @Disabled
  public void testGetSchemaList() {
  }

  /**
   * Test of getSchema method, of class SchemaManagementImpl.
   */
  @Test
  @Disabled
  public void testGetSchema() {
  }

  /**
   * Test of getSchemasForType method, of class SchemaManagementImpl.
   */
  @Test
  @Disabled
  public void testGetSchemasForType() {
  }

  /**
   * Test of getCompositeSchema method, of class SchemaManagementImpl.
   */
  @Test
  @Disabled
  public void testGetCompositeSchema() {
  }
}
