package eu.gaiax.difs.fc.core.service.schemastore.impl;

import eu.gaiax.difs.fc.core.config.DatabaseConfig;
import eu.gaiax.difs.fc.core.config.FileStoreConfig;
import eu.gaiax.difs.fc.core.exception.ConflictException;
import eu.gaiax.difs.fc.core.pojo.ContentAccessor;
import eu.gaiax.difs.fc.core.pojo.ContentAccessorDirect;
import eu.gaiax.difs.fc.core.service.schemastore.SchemaStore;
import eu.gaiax.difs.fc.core.service.schemastore.SchemaStore.SchemaType;
import static eu.gaiax.difs.fc.core.service.schemastore.SchemaStore.SchemaType.ONTOLOGY;
import static eu.gaiax.difs.fc.core.service.schemastore.SchemaStore.SchemaType.SHAPE;
import static eu.gaiax.difs.fc.core.service.schemastore.SchemaStore.SchemaType.VOCABULARY;
import eu.gaiax.difs.fc.core.util.TestUtil;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
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
@ContextConfiguration(classes = {SchemaStoreImplTest.TestApplication.class, FileStoreConfig.class,
  SchemaStoreImplTest.class, SchemaStoreImpl.class, DatabaseConfig.class})
@DirtiesContext
@Transactional
@Slf4j
@AutoConfigureEmbeddedDatabase(provider = DatabaseProvider.ZONKY)
public class SchemaStoreImplTest {

  @SpringBootApplication
  public static class TestApplication {

    public static void main(String[] args) {
      SpringApplication.run(TestApplication.class, args);
    }
  }

  @Autowired
  private SchemaStoreImpl schemaStore;

  @Autowired
  private SessionFactory sessionFactory;

  //@Autowired
  //@Qualifier("schemaFileStore")
  //private FileStore fileStore;

  @AfterEach
  public void storageSelfCleaning() throws IOException {
    //fileStore.clearStorage();
  }

  public Set<String> getExtractedTermsSet(ContentAccessor extractedTerms) throws IOException {
    Set<String> extractedTermsSet = new HashSet<>();
    try ( InputStream resource = extractedTerms.getContentAsStream()) {
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
    SchemaAnalysisResult result = schemaStore.analyzeSchema(contentGraph);
    boolean actual = schemaStore.isSchemaType(contentGraph, ONTOLOGY);
    Set<String> actualExtractedUrlsSet = result.getExtractedUrls();
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
    SchemaAnalysisResult result = schemaStore.analyzeSchema(contentGraph);
    boolean actual = schemaStore.isSchemaType(contentGraph, SHAPE);
    Set<String> actualExtractedUrlsSet = result.getExtractedUrls();
    assertTrue(result.isValid());
    assertTrue(actual);
    assertEquals(expectedExtractedUrlsSet.size(), actualExtractedUrlsSet.size());
    assertEquals(expectedExtractedUrlsSet, actualExtractedUrlsSet);

  }

  @Test
  public void testIsValidShape() {
    Set<String> expectedExtractedUrlsSet = new HashSet<>();
    expectedExtractedUrlsSet.add("http://w3id.org/gaia-x/validation#PhysicalResourceShape");
    expectedExtractedUrlsSet.add("http://w3id.org/gaia-x/validation#MeasureShape");
    String path = "Schema-Tests/valid-schemaShape.ttl";
    ContentAccessor content = TestUtil.getAccessor(getClass(), path);
    SchemaAnalysisResult result = schemaStore.analyzeSchema(content);
    boolean actual = schemaStore.isSchemaType(content, SHAPE);
    Set<String> actualExtractedUrlsSet = result.getExtractedUrls();
    assertTrue(result.isValid());
    assertEquals(expectedExtractedUrlsSet.size(), actualExtractedUrlsSet.size());
    assertEquals(expectedExtractedUrlsSet, actualExtractedUrlsSet);
    assertNull(result.getExtractedId());
    assertTrue(actual);
    assertTrue(schemaStore.verifySchema(content));
  }

  @Test
  public void testValidVocabulary() {
    Set<String> expectedExtractedUrlsSet = new HashSet<>();
    expectedExtractedUrlsSet.add("http://www.example.com/cat");
    String path = "Schema-Tests/validSkosWith2Urls.ttl";
    ContentAccessor content = TestUtil.getAccessor(getClass(), path);
    SchemaAnalysisResult result = schemaStore.analyzeSchema(content);
    boolean actual = schemaStore.isSchemaType(content, VOCABULARY);
    Set<String> actualExtractedUrlsSet = result.getExtractedUrls();
    String extractedIdActual = result.getExtractedId();
    String extractedIdExpected = "http://www.example.com/animals";
    assertTrue(result.isValid());
    assertEquals(expectedExtractedUrlsSet.size(), actualExtractedUrlsSet.size());
    assertEquals(expectedExtractedUrlsSet, actualExtractedUrlsSet);
    assertEquals(extractedIdExpected, extractedIdActual);
    assertTrue(actual);
    assertTrue(schemaStore.verifySchema(content));
  }

  @Test
  public void testValidOntology() {
    Set<String> expectedExtractedUrlsSet = new HashSet<>();
    expectedExtractedUrlsSet.add("http://w3id.org/gaia-x/core#providesResourcesFrom");
    expectedExtractedUrlsSet.add("http://w3id.org/gaia-x/core#Interconnection");
    expectedExtractedUrlsSet.add("http://w3id.org/gaia-x/core#Consumer");
    expectedExtractedUrlsSet.add("http://w3id.org/gaia-x/core#Provider");
    expectedExtractedUrlsSet.add("http://w3id.org/gaia-x/core#AssetOwner");
    expectedExtractedUrlsSet.add("http://w3id.org/gaia-x/core#ServiceOffering");
    expectedExtractedUrlsSet.add("http://w3id.org/gaia-x/core#Contract");
    String path = "Schema-Tests/validOntology.ttl";
    ContentAccessor content = TestUtil.getAccessor(getClass(), path);
    SchemaAnalysisResult result = schemaStore.analyzeSchema(content);
    boolean actual = schemaStore.isSchemaType(content, ONTOLOGY);
    Set<String> actualExtractedUrlsSet = result.getExtractedUrls();
    String extractedIdActual = result.getExtractedId();
    String extractedIdExpected = "http://w3id.org/gaia-x/core#";
    assertTrue(result.isValid());
    assertEquals(extractedIdExpected, extractedIdActual);
    assertEquals(expectedExtractedUrlsSet.size(), actualExtractedUrlsSet.size());
    assertEquals(expectedExtractedUrlsSet, actualExtractedUrlsSet);
    assertTrue(actual);
    assertTrue(schemaStore.verifySchema(content));
  }

  @Test
  public void testNoOntologyIRI() {
    String path = "Schema-Tests/noOntologyIRI.ttl";
    ContentAccessor content = TestUtil.getAccessor(getClass(), path);
    SchemaAnalysisResult result = schemaStore.analyzeSchema(content);
    String actual = result.getErrorMessage();
    String expected = "Schema is not supported";
    assertFalse(result.isValid());
    assertEquals(expected, actual);
    assertTrue(result.getExtractedUrls().isEmpty());
    assertNull(result.getExtractedId());
  }

  @Test
  public void testInvalidOntologyWith2IRI() {
    String path = "Schema-Tests/invalidOntologyWithTwoIRIs.ttl";
    ContentAccessor content = TestUtil.getAccessor(getClass(), path);
    SchemaAnalysisResult result = schemaStore.analyzeSchema(content);
    String actual = result.getErrorMessage();
    String expected = "Ontology Schema has multiple Ontology IRIs";
    assertEquals(expected, actual);
    assertFalse(result.isValid());
    assertNull(result.getExtractedId());
    assertTrue(result.getExtractedUrls().isEmpty());

  }

  @Test
  public void testIsInvalidVocabulary() {
    String path = "Schema-Tests/skosConceptInvalid.ttl";
    ContentAccessor content = TestUtil.getAccessor(getClass(), path);
    SchemaAnalysisResult result = schemaStore.analyzeSchema(content);
    String expected = "Vocabulary contains multiple concept schemes";
    assertFalse(result.isValid());
    assertNull(result.getExtractedId());
    assertTrue(result.getExtractedUrls().isEmpty());
    assertEquals(expected, result.getErrorMessage());
  }

  @Test
  public void testIsInvalidSchema() {
    String path = "Schema-Tests/invalidSchema.ttl";
    ContentAccessor content = TestUtil.getAccessor(getClass(), path);
    SchemaAnalysisResult result = schemaStore.analyzeSchema(content);
    String actual = result.getErrorMessage();
    String expected = "Schema is not supported";
    assertNull(result.getExtractedId());
    assertTrue(result.getExtractedUrls().isEmpty());
    assertFalse(result.isValid());
    assertEquals(expected, actual);
  }
  @Test
  public void testValidJSONlD() throws UnsupportedEncodingException {
    Set<String> expectedExtractedUrlsSet = new HashSet<>();
    expectedExtractedUrlsSet.add("http://w3id.org/gaia-x/validation#PhysicalResourceShape");
    String path = "Schema-Tests/validShacl.jsonld";
    ContentAccessor content = TestUtil.getAccessor(getClass(), path);
    SchemaAnalysisResult result = schemaStore.analyzeSchema(content);
    Set<String> actualExtractedUrlsSet = result.getExtractedUrls();
    assertTrue(result.isValid());
    assertNull(result.getExtractedId());
    assertTrue(schemaStore.verifySchema(content));
    assertTrue(schemaStore.isSchemaType(content, SHAPE));
    assertEquals(expectedExtractedUrlsSet.size(), actualExtractedUrlsSet.size());
    assertEquals(expectedExtractedUrlsSet, actualExtractedUrlsSet);
  }
  @Test
  public void testValidRDFXML() throws UnsupportedEncodingException {
    Set<String> expectedExtractedUrlsSet = new HashSet<>();
    expectedExtractedUrlsSet.add("http://w3id.org/gaia-x/validation#PhysicalResourceShape");
    String path = "Schema-Tests/validShacl.rdfxml";
    ContentAccessor content = TestUtil.getAccessor(getClass(), path);
    SchemaAnalysisResult result = schemaStore.analyzeSchema(content);
    Set<String> actualExtractedUrlsSet = result.getExtractedUrls();
    assertTrue(result.isValid());
    assertNull(result.getExtractedId());
    assertTrue(schemaStore.verifySchema(content));
    assertTrue(schemaStore.isSchemaType(content, SHAPE));
    assertEquals(expectedExtractedUrlsSet.size(), actualExtractedUrlsSet.size());
    assertEquals(expectedExtractedUrlsSet, actualExtractedUrlsSet);
  }

  /**
   * Test of addSchema method, of class SchemaManagementImpl.
   */
  @Test
  public void testAddSchema() {
    log.info("testAddSchema");
    String path = "Schema-Tests/valid-schemaShape.ttl";
    ContentAccessor content = TestUtil.getAccessor(path);

    String schemaId1 = schemaStore.addSchema(content);

    Map<SchemaStore.SchemaType, List<String>> expected = new HashMap<>();
    expected.computeIfAbsent(SHAPE, t -> new ArrayList<>()).add(schemaId1);
    Map<SchemaStore.SchemaType, List<String>> schemaList = schemaStore.getSchemaList();
    assertEquals(expected, schemaList);

    assertTermsEquals(
        "http://w3id.org/gaia-x/validation#PhysicalResourceShape",
        "http://w3id.org/gaia-x/validation#MeasureShape");

    schemaStore.deleteSchema(schemaId1);
  }

  private void assertTermsEquals(String... expectedTerms) {
    List<String> foundTermsList = sessionFactory.getCurrentSession()
        .createNativeQuery("select term from schematerms")
        .getResultList();
    Set<String> foundTermsSet = new HashSet<>(foundTermsList);
    Set<String> expectedTermsSet = new HashSet<>(Arrays.asList(expectedTerms));
    assertEquals(expectedTermsSet, foundTermsSet, "Incorrect set of terms found in database.");
  }

  private void assertTermCountEquals(int count) {
    Object termCount = sessionFactory.getCurrentSession()
        .createNativeQuery("select count(*) from schematerms")
        .getSingleResult();
    assertEquals(Integer.toString(count), termCount.toString(), "incorrect number of terms found in database");
  }

  /**
   * Test of addSchema method with content storing checking, of class SchemaManagementImpl.
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
   * Test of addSchema method, of class SchemaManagementImpl. Adding the schema twice
   */
  @Test
  public void testAddDuplicateSchema() throws IOException {
    log.info("testAddDuplicateSchema");
    String path = "Schema-Tests/valid-schemaShape.ttl";
    schemaStore.addSchema(TestUtil.getAccessor(getClass(), path));
    assertThrowsExactly(ConflictException.class, () -> schemaStore.addSchema(TestUtil.getAccessor(getClass(), path)));

  }

  @Test
  public void testAddSchemaConflictingTerm() throws IOException {
    log.info("testAddSchemaConflictingTerm");
    String pathOne = "Schema-Tests/shapeCpu.ttl";
    String idOne = schemaStore.addSchema(TestUtil.getAccessor(pathOne));
    Map<SchemaType, List<String>> schemaListOne = schemaStore.getSchemaList();
    assertEquals(1, schemaListOne.get(SchemaType.SHAPE).size(), "Incorrect number of shape schemas found.");

    String pathTwo = "Schema-Tests/shapeGpu.ttl";
    assertThrowsExactly(ConflictException.class, () -> schemaStore.addSchema(TestUtil.getAccessor(pathTwo)));
    Map<SchemaType, List<String>> schemaListTwo = schemaStore.getSchemaList();
    assertEquals(schemaListOne, schemaListTwo, "schema list should not have changed.");
    assertEquals(1, schemaListTwo.get(SchemaType.SHAPE).size(), "Incorrect number of shape schemas found.");
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
    assertTermCountEquals(1);
    assertTermsEquals(
        "http://w3id.org/gaia-x/validation#PhysicalResourceShape");

    schemaStore.updateSchema(schemaId, TestUtil.getAccessor(getClass(), path2));
    assertTermCountEquals(2);
    assertTermsEquals(
        "http://w3id.org/gaia-x/validation#PhysicalResourceShape",
        "http://w3id.org/gaia-x/validation#MeasureShape");
    assertEquals(TestUtil.getAccessor(getClass(), path2).getContentAsString(), schemaStore.getSchema(schemaId).getContentAsString(), 
            "The content of the updated schema should be stored in the schema DB.");

    schemaStore.updateSchema(schemaId, TestUtil.getAccessor(getClass(), path1));
    assertTermCountEquals(1);
    assertTermsEquals(
        "http://w3id.org/gaia-x/validation#PhysicalResourceShape");
    assertEquals(TestUtil.getAccessor(getClass(), path1).getContentAsString(), schemaStore.getSchema(schemaId).getContentAsString(), 
            "The content of the updated schema should be stored in the schema DB.");

    schemaStore.deleteSchema(schemaId);
  }

  @Test
  void addDeleteDefaultSchemas() {
    int initialized = schemaStore.initializeDefaultSchemas();
    assertEquals(3, initialized, "Expected different number of schemas initialized.");
    //int count = TestUtil.countFilesInStore(fileStore);
    //assertEquals(3, count, "Expected different number of files in the store.");
    Map<SchemaType, List<String>> schemaList = schemaStore.getSchemaList();
    assertEquals(2, schemaList.get(SchemaType.ONTOLOGY).size());
    assertEquals(1, schemaList.get(SchemaType.SHAPE).size());
    assertTrue(schemaList.get(SchemaType.ONTOLOGY).contains("https://w3id.org/gaia-x/gax-trust-framework#"), "Ontology identifier not found in schema list.");
    assertTrue(schemaList.get(SchemaType.ONTOLOGY).contains("https://w3id.org/gaia-x/core#"), "Ontology identifier not found in schema list.");
    schemaStore.deleteSchema("https://w3id.org/gaia-x/gax-trust-framework#");
    Map<SchemaType, List<String>> schemaListDelete = schemaStore.getSchemaList();
    assertFalse(schemaListDelete.get(SchemaType.ONTOLOGY).contains("https://w3id.org/gaia-x/gax-trust-framework#"), "Ontology identifier not found in schema list.");
    assertEquals(1, schemaListDelete.get(SchemaType.ONTOLOGY).size());
  }

  /**
   * Test of getCompositeSchema method, of class SchemaManagementImpl.
   */
  @Test
  public void testGetCompositeSchema() throws IOException {
    Model modelActual = ModelFactory.createDefaultModel();
    String sub01 = "http://w3id.org/gaia-x/validation#PhysicalResourceShape";
    String pre01 = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    String obj01 = "http://www.w3.org/ns/shacl#NodeShape";

    String sub02 = "http://w3id.org/gaia-x/validation#DataConnectorShape";
    String pre02 = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    String obj02 = "http://www.w3.org/ns/shacl#NodeShape";

    String schemaPath1 = "Schema-Tests/FirstValidSchemaShape.ttl";
    String schemaPath2 = "Schema-Tests/SecondValidSchemaShape.ttl";

    ContentAccessor schema01Content = TestUtil.getAccessor(getClass(), schemaPath1);

    storageSelfCleaning();

    schemaStore.addSchema(TestUtil.getAccessor(getClass(), schemaPath1));

    SchemaAnalysisResult schemaResult = schemaStore.analyzeSchema(schema01Content);
    assertTrue(schemaResult.isValid());

    ContentAccessor compositeSchemaActual = schemaStore.getCompositeSchema(SHAPE);
    log.info(compositeSchemaActual.getContentAsString());

    StringReader schemaContentReaderComposite = new StringReader(compositeSchemaActual.getContentAsString());
    modelActual.read(schemaContentReaderComposite, "", "TURTLE");
    assertTrue(isExistTriple(modelActual, sub01, pre01, obj01));
    assertFalse(isExistTriple(modelActual, sub02, pre02, obj02));

    ContentAccessor schema02Content = TestUtil.getAccessor(getClass(), schemaPath2);

    schemaStore.addSchema(TestUtil.getAccessor(getClass(), schemaPath2));

    schemaResult = schemaStore.analyzeSchema(schema02Content);

    compositeSchemaActual = schemaStore.getCompositeSchema(SHAPE);

    log.info(compositeSchemaActual.getContentAsString());

    schemaContentReaderComposite = new StringReader(compositeSchemaActual.getContentAsString());

    modelActual.read(schemaContentReaderComposite, "", "TURTLE");
    assertTrue(isExistTriple(modelActual, sub01, pre01, obj01));
    assertTrue(isExistTriple(modelActual, sub02, pre02, obj02));
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
