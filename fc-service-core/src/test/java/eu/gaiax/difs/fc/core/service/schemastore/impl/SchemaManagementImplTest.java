package eu.gaiax.difs.fc.core.service.schemastore.impl;

import eu.gaiax.difs.fc.core.config.DatabaseConfig;
import eu.gaiax.difs.fc.core.config.FileStoreConfig;
import eu.gaiax.difs.fc.core.exception.ConflictException;
import eu.gaiax.difs.fc.core.pojo.ContentAccessor;
import eu.gaiax.difs.fc.core.pojo.ContentAccessorDirect;
import eu.gaiax.difs.fc.core.service.filestore.FileStore;
import static eu.gaiax.difs.fc.core.service.schemastore.SchemaStore.SchemaType.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.*;

import eu.gaiax.difs.fc.core.service.schemastore.SchemaStore;
import eu.gaiax.difs.fc.core.util.TestUtil;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.neo4j.cypher.internal.expressions.True;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
@ContextConfiguration(classes = {SchemaManagementImplTest.TestApplication.class, FileStoreConfig.class,
  SchemaManagementImplTest.class, SchemaStoreImpl.class, DatabaseConfig.class})
@DirtiesContext
@Transactional
@Slf4j
@AutoConfigureEmbeddedDatabase(provider = DatabaseProvider.ZONKY)
public class SchemaManagementImplTest {

  @SpringBootApplication
  public static class TestApplication {

    public static void main(String[] args) {
      SpringApplication.run(TestApplication.class, args);
    }
  }

  @Autowired
  private SchemaStoreImpl schemaStore;

  @Autowired
  @Qualifier("schemaFileStore")
  private FileStore fileStore;

  @AfterEach
  public void storageSelfCleaning() throws IOException {
    fileStore.clearStorage();
  }

  public Set<String> getExtractedTermsSet (ContentAccessor extractedTerms) throws IOException {
    Set<String> extractedTermsSet = new HashSet<>();
    try (InputStream resource = extractedTerms.getContentAsStream()) {
      List<String> extractedList = new BufferedReader(new InputStreamReader(resource,
              StandardCharsets.UTF_8)).lines().collect(Collectors.toList());
      extractedTermsSet = new HashSet<>(extractedList);
    }
    return extractedTermsSet;
  }
  @Test
  public void testGaxCoreOntologyGraph() throws IOException {
    String pathTerms = "Schema-Tests/gax-core-ontology-terms.txt";
    String pathGraph = "Schema-Tests/gax-core-ontology.ttl";
    ContentAccessor contentTerms = TestUtil.getAccessor(getClass(), pathTerms);
    ContentAccessor contentGraph = TestUtil.getAccessor(getClass(), pathGraph);
    Set<String> expectedExtractedUrlsSet = getExtractedTermsSet(contentTerms);
    SchemaAnalysisResult result = schemaStore.analyseSchema(contentGraph);
    boolean actual = schemaStore.isSchemaType(contentGraph, ONTOLOGY);
    List<String> actualExtractedUrlsList = result.getExtractedUrls();
    Set<String> actualExtractedUrlsSet = new HashSet<>(actualExtractedUrlsList);
    actualExtractedUrlsSet.removeAll(expectedExtractedUrlsSet);
    assertTrue(actual);
    assertTrue(actualExtractedUrlsSet.isEmpty());
  }
  @Test
  public void testGaxCoreShapeGraph() throws IOException {
    String pathTerms = "Schema-Tests/gax-core-shapes-terms.txt";
    String pathGraph = "Schema-Tests/gax-core-shapes.ttl";
    ContentAccessor contentTerms = TestUtil.getAccessor(getClass(), pathTerms);
    ContentAccessor contentGraph = TestUtil.getAccessor(getClass(), pathGraph);
    Set<String> expectedExtractedUrlsSet = getExtractedTermsSet(contentTerms);
    SchemaAnalysisResult result = schemaStore.analyseSchema(contentGraph);
    boolean actual = schemaStore.isSchemaType(contentGraph, SHAPE);
    List<String> actualExtractedUrlsList = result.getExtractedUrls();
    Set<String> actualExtractedUrlsSet = new HashSet<>(actualExtractedUrlsList);
    assertTrue(actual);
    assertEquals(expectedExtractedUrlsSet.size(), actualExtractedUrlsSet.size());
    assertEquals(expectedExtractedUrlsSet, actualExtractedUrlsSet);

  }
  @Test
  public void testIsValidShape() throws UnsupportedEncodingException {
    Set<String> expectedExtractedUrlsSet = new HashSet<>();
    expectedExtractedUrlsSet.add("http://w3id.org/gaia-x/validation#PhysicalResourceShape");
    String path = "Schema-Tests/valid-schemaShape.ttl";
    ContentAccessor content = TestUtil.getAccessor(getClass(), path);
    SchemaAnalysisResult result = schemaStore.analyseSchema(content);
    boolean actual = schemaStore.isSchemaType(content, SHAPE);
    List<String> actualExtractedUrlsList = result.getExtractedUrls();
    Set<String> actualExtractedUrlsSet = new HashSet<>(actualExtractedUrlsList);
    actualExtractedUrlsSet.removeAll(expectedExtractedUrlsSet);
    assertTrue( actualExtractedUrlsSet.isEmpty());
    assertNull(result.getExtractedId());
    assertTrue(actual);
    assertTrue(schemaStore.verifySchema(content));
  }
  @Test
  public void testValidVocabulary() throws UnsupportedEncodingException {
    Set<String> expectedExtractedUrlsSet = new HashSet<>();
    expectedExtractedUrlsSet.add("http://www.example.com/cat");
    String path = "Schema-Tests/validSkosWith2Urls.ttl";
    ContentAccessor content = TestUtil.getAccessor(getClass(), path);
    SchemaAnalysisResult result = schemaStore.analyseSchema(content);
    boolean actual = schemaStore.isSchemaType(content, VOCABULARY);
    List<String> actualExtractedUrlsList = result.getExtractedUrls();
    Set<String> actualExtractedUrlsSet = new HashSet<>(actualExtractedUrlsList);
    actualExtractedUrlsSet.removeAll(expectedExtractedUrlsSet);
    String extractedIdActual = result.getExtractedId();
    String extractedIdExpected = "http://www.example.com/animals";
    assertTrue(actualExtractedUrlsSet.isEmpty());
    assertEquals(extractedIdExpected,extractedIdActual);
    assertTrue(actual);
    assertTrue(schemaStore.verifySchema(content));
  }
  @Test
  public void testValidOntology() throws UnsupportedEncodingException {
    Set<String> expectedExtractedUrlsSet = new HashSet<>();
    expectedExtractedUrlsSet.add("http://w3id.org/gaia-x/core#providesResourcesFrom");
    expectedExtractedUrlsSet.add("http://w3id.org/gaia-x/core#Interconnection");
    expectedExtractedUrlsSet.add("http://w3id.org/gaia-x/core#Consumer");
    expectedExtractedUrlsSet.add( "http://w3id.org/gaia-x/core#Provider");
    expectedExtractedUrlsSet.add(  "http://w3id.org/gaia-x/core#AssetOwner");
    expectedExtractedUrlsSet.add(  "http://w3id.org/gaia-x/core#ServiceOffering");
    expectedExtractedUrlsSet.add(  "http://w3id.org/gaia-x/core#Contract");
    String path = "Schema-Tests/validOntology.ttl";
    ContentAccessor content = TestUtil.getAccessor(getClass(), path);
    SchemaAnalysisResult result = schemaStore.analyseSchema(content);
    boolean actual = schemaStore.isSchemaType(content, ONTOLOGY);
    List<String> actualExtractedUrlsList = result.getExtractedUrls();
    Set<String> actualExtractedUrlsSet = new HashSet<>(actualExtractedUrlsList);
    actualExtractedUrlsSet.removeAll(expectedExtractedUrlsSet);
    String extractedIdActual = result.getExtractedId();
    String extractedIdExpected = "http://w3id.org/gaia-x/core#";
    assertEquals(extractedIdExpected,extractedIdActual);
    assertTrue( actualExtractedUrlsSet.isEmpty());
    assertTrue(actual);
    assertTrue(schemaStore.verifySchema(content));
  }
  @Test
  public void testNoOntologyIRI() throws UnsupportedEncodingException {
    String path = "Schema-Tests/noOntologyIRI.ttl";
    ContentAccessor content = TestUtil.getAccessor(getClass(), path);
    SchemaAnalysisResult result = schemaStore.analyseSchema(content);
    String actual = result.getErrorMessage();
    String expected = "Schema is not supported";
    assertEquals(expected,actual);
    assertTrue(result.getExtractedUrls().isEmpty());
    assertNull(result.getExtractedId());
  }
  @Test
  public void testInvalidOntologyWith2IRI() throws UnsupportedEncodingException {
    String path = "Schema-Tests/invalidOntologyWithTwoIRIs.ttl";
    ContentAccessor content = TestUtil.getAccessor(getClass(), path);
    SchemaAnalysisResult result = schemaStore.analyseSchema(content);
    String actual = result.getErrorMessage();
    String expected = "Ontology Schema has multiple Ontology IRIs";
    assertEquals(expected,actual);
    assertFalse(result.isValid());
    assertNull(result.getExtractedId());
    assertTrue(result.getExtractedUrls().isEmpty());

  }

  @Test
  public void testIsInvalidVocabulary() throws UnsupportedEncodingException {
    String path = "Schema-Tests/skosConceptInvalid.ttl";
    ContentAccessor content = TestUtil.getAccessor(getClass(), path);
    SchemaAnalysisResult result = schemaStore.analyseSchema(content);
    String expected = "Vocabulary contains multiple concept schemes";
    assertFalse(result.isValid());
    assertNull(result.getExtractedId());
    assertTrue(result.getExtractedUrls().isEmpty());
    assertEquals(expected,result.getErrorMessage());
  }
  @Test
  public void testIsInvalidSchema() throws UnsupportedEncodingException {
    String path = "Schema-Tests/invalidSchema.ttl";
    ContentAccessor content = TestUtil.getAccessor(getClass(), path);
    SchemaAnalysisResult result = schemaStore.analyseSchema(content);
    String actual = result.getErrorMessage();
    String expected = "Schema is not supported";
    assertNull(result.getExtractedId());
    assertTrue(result.getExtractedUrls().isEmpty());
    assertFalse(result.isValid());
    assertEquals(expected,actual);
  }


  /**
   * Test of addSchema method, of class SchemaManagementImpl.
   */
  @Test
  public void testAddSchema() throws UnsupportedEncodingException {
    log.info("testAddSchema");
    String path = "Schema-Tests/valid-schemaShapeCopy.ttl";
    ContentAccessor content = TestUtil.getAccessor(getClass(), path);
    // String schema1 = "Some Schema Content";

    String schemaId1 = schemaStore.addSchema(content);

    Map<SchemaStore.SchemaType, List<String>> expected = new HashMap<>();
    expected.computeIfAbsent(SHAPE, t -> new ArrayList<>()).add(schemaId1);
    Map<SchemaStore.SchemaType, List<String>> schemaList = schemaStore.getSchemaList();
    assertEquals(expected, schemaList);

    int count = 0;
    for (File file : fileStore.getFileIterable()) {
      count++;
    }
    assertEquals(1, count, "Storing one schama should result in exactly one file in the store.");

    schemaStore.deleteSchema(schemaId1);
    count = 0;
    for (File file : fileStore.getFileIterable()) {
      count++;
    }
    assertEquals(0, count, "Deleting the only file should result in exactly 0 files in the store.");
  }

  /**
   * Test of addSchema method with content storing checking, of class
   * SchemaManagementImpl.
   *
   */
  @Test
  public void testAddSchemaWithLongContent() throws IOException {
    log.info("testAddSchemaWithLongContent");

    String path = "Schema-Tests/schema.ttl";

    String schema1 = TestUtil.getAccessor(getClass(), path).getContentAsString();

    String schemaId1 = schemaStore.addSchema(new ContentAccessorDirect(schema1));

    ContentAccessor ContentAccessor = schemaStore.getSchema(schemaId1);

    assertEquals(schema1, ContentAccessor.getContentAsString(), "Checking schema content stored properly "
        + "and retrieved properly");

    schemaStore.deleteSchema(schemaId1);
  }

  /**
   * Test of addSchema method, of class SchemaManagementImpl. Adding the schema
   * twice
   */
  @Test
  public void testAddDuplicateSchema() throws IOException {
    log.info("testAddDuplicateSchema");
    String path = "Schema-Tests/valid-schemaShape.ttl";
    schemaStore.addSchema(TestUtil.getAccessor(getClass(), path));
    assertThrowsExactly(ConflictException.class, () -> schemaStore.addSchema(TestUtil.getAccessor(getClass(), path)));

    fileStore.clearStorage();
  }

  /**
   * Test of updateSchema method, of class SchemaManagementImpl.
   */
  @Test
  public void testUpdateSchema() throws IOException {
    log.info("UpdateSchema");
    String path1 = "Schema-Tests/valid-schemaShapeReduced.ttl";
    String path2 = "Schema-Tests/valid-schemaShape.ttl";

    String schemaId = schemaStore.addSchema(TestUtil.getAccessor(getClass(), path1));
    schemaStore.updateSchema(schemaId, TestUtil.getAccessor(getClass(), path2));

    assertEquals(TestUtil.getAccessor(getClass(), path2).getContentAsString(), fileStore.readFile(schemaId).getContentAsString(), "The content of the updated schema should be stored in the schema file.");

    schemaStore.deleteSchema(schemaId);
    int count = 0;
    for (File file : fileStore.getFileIterable()) {
      count++;
    }
    assertEquals(0, count, "Deleting the only file should result in exactly 0 files in the store.");

    //TODO: check if terms are updated correctly
  }

  @Test
  void addDefaultSchemas() {
    schemaStore.initializeDefaultSchemas();
    int count = 0;
    for (File file : fileStore.getFileIterable()) {
      count++;
    }
    assertEquals(5, count, "Expected a different number of files in the store.");
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
   * Test of getCompositeSchema method, of class SchemaManagementImpl.
   */
  @Test
  public void testGetCompositeSchema() throws IOException {
    Model modelActual = ModelFactory.createDefaultModel();
    String sub = "http://w3id.org/gaia-x/validation#EndpointShape";
    String pre = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    String obj = "http://www.w3.org/ns/shacl#NodeShape";

    String schemaPath1 = "Schema-Tests/FirstValidSchemaShape.ttl";
    String schemaPath2 = "Schema-Tests/SecondValidSchemaShape.ttl";

    ContentAccessor schema01Content = TestUtil.getAccessor(getClass(), schemaPath1);
    ContentAccessor schema02Content = TestUtil.getAccessor(getClass(), schemaPath2);

    storageSelfCleaning();

    schemaStore.addSchema(TestUtil.getAccessor(getClass(), schemaPath1));
    schemaStore.addSchema(TestUtil.getAccessor(getClass(), schemaPath2));

    SchemaAnalysisResult schemaResult = schemaStore.analyseSchema(schema01Content);

    ContentAccessor compositeSchemaActual = schemaStore.getCompositeSchema(SHAPE);

    log.info(compositeSchemaActual.getContentAsString());

    StringReader schemaContentReaderComposite = new StringReader(compositeSchemaActual.getContentAsString());

    modelActual.read(schemaContentReaderComposite, "", "TURTLE");

    assertTrue(isExistTriple(modelActual, sub, pre, obj));
  }

  private static boolean isExistTriple(Model model, String sub, String pre, String obj) {
    StmtIterator iterActual = model.listStatements();
    while (iterActual.hasNext()) {
      Statement stmt = iterActual.nextStatement();
      if (sub.equals(stmt.getSubject().toString()) && pre.equals(stmt.getPredicate().toString()) && obj.equals(stmt.getObject().toString())) {
        return true;
      }
    }
    return false;
  }

}
