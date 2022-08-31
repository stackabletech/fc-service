package eu.gaiax.difs.fc.core.service.graphdb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import java.util.*;

import n10s.graphconfig.GraphConfigProcedures;
import n10s.rdf.load.RDFLoadProcedures;
import org.junit.FixMethodOrder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.runners.MethodSorters;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.configuration.connectors.BoltConnector;
import org.neo4j.configuration.helpers.SocketAddress;

import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.springframework.boot.test.autoconfigure.Neo4jTestHarnessAutoConfiguration;
import org.neo4j.gds.catalog.GraphExistsProc;
import org.neo4j.gds.catalog.GraphListProc;
import org.neo4j.gds.catalog.GraphProjectProc;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
@EnableAutoConfiguration(exclude = {Neo4jTestHarnessAutoConfiguration.class})
public class GraphTest {
  private static Neo4j embeddedDatabaseServer;

  private Driver driver;

  @BeforeAll
  void initializeNeo4j() {
    embeddedDatabaseServer = Neo4jBuilders.newInProcessBuilder()
        .withDisabledServer()
        .withConfig(GraphDatabaseSettings.procedure_allowlist, List.of("gds.*", "n10s.*"))
        .withConfig(BoltConnector.listen_address, new SocketAddress(7687))
        .withConfig(GraphDatabaseSettings.procedure_unrestricted, List.of("gds.*", "n10s.*"))

        // will be user for gds procedure
        .withProcedure(GraphExistsProc.class) // gds.graph.exists procedure
        .withProcedure(GraphListProc.class)
        .withProcedure(GraphProjectProc.class)
        // will be used for neo-semantics
        .withProcedure(GraphConfigProcedures.class) // n10s.graphconfig.*
        .withProcedure(RDFLoadProcedures.class)

        .withFixture("CREATE (SelfDescription1:SelfDescription {sdHash:'test-hash-1', id:'test-sd-1'})")
        .withFixture("CREATE (SelfDescription2:SelfDescription {sdHash:'test-hash-2', id:'test-sd-2'})")
        .withFixture("CREATE (Issuer1:Issuer {id:'test-issuer-1', type: 'Provider'})")
        .withFixture("MATCH (a:SelfDescription), (b:Issuer) WHERE a.id = 'test-sd-1' AND b.id = 'test-issuer-1' CREATE (b)-[r:OWNS]->(a)")
        .withFixture("MATCH (a:SelfDescription), (b:Issuer) WHERE a.id = 'test-sd-2' AND b.id = 'test-issuer-1' CREATE (b)-[r2:OWNS]->(a)")
        .build();
    this.driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI(), Config.builder().withoutEncryption().build());
  }

  @AfterAll
  void closeNeo4j() {
    embeddedDatabaseServer.close();
  }

  @DynamicPropertySource
  static void neo4jProperties(DynamicPropertyRegistry registry) {
    registry.add("org.neo4j.driver.uri", embeddedDatabaseServer::boltURI);
    registry.add("org.neo4j.driver.authentication.password", () -> "");
  }

  @Test
  public void testInitSemanticsGraphConfig() {
    try (Session session = driver.session()) {
      Result results = session.run(
          "CALL n10s.graphconfig.init() yield param, value with param, value where param = 'classLabel' return param, value ");
      assertTrue(results.hasNext());
      assertEquals("Class", results.next().get("value").asString());

      results = session.run(
          "CALL n10s.graphconfig.show() yield param, value with param, value where param = 'classLabel' return param, value ");
      assertTrue(results.hasNext());
      assertEquals("Class", results.next().get("value").asString());
    }
  }

  @Test
  void testIfGraphExists() {
    try (Session session = driver.session()) {
      session.run("CALL gds.graph.project('sds', 'SelfDescription', 'OWNS')");
      assertEquals(1, session.run("CALL gds.graph.list()").list().size());
      assertTrue(session.run("CALL gds.graph.exists('sds')").next().get("exists").asBoolean());
    }
  }

  @Test
  void testCreationOfGraphNodes() {
    try (Session session = driver.session()) {
      assertThat(session.run("MATCH (m:SelfDescription) RETURN m").stream()).hasSize(2);
    }
  }
}