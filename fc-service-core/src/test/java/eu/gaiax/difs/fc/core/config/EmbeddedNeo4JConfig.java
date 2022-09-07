package eu.gaiax.difs.fc.core.config;

import java.util.List;

import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.configuration.connectors.BoltConnector;
import org.neo4j.configuration.helpers.SocketAddress;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.gds.catalog.GraphExistsProc;
import org.neo4j.gds.catalog.GraphListProc;
import org.neo4j.gds.catalog.GraphProjectProc;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import lombok.extern.slf4j.Slf4j;
import n10s.graphconfig.GraphConfigProcedures;
import n10s.rdf.load.RDFLoadProcedures;

@Slf4j
@TestConfiguration
public class EmbeddedNeo4JConfig {

    @Bean
    public Neo4j embeddedDatabaseServer() {
        log.info("starting Embedded Neo4J DB");
        Neo4j embeddedDatabaseServer = Neo4jBuilders.newInProcessBuilder()
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
                .build();
        log.info("started Embedded Neo4J DB: {}", embeddedDatabaseServer);
        return embeddedDatabaseServer;
    }
    
    @Bean(destroyMethod = "close")
    public Driver driver(Neo4j embeddedDatabaseServer) {
        Driver driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI());
        Session session = driver.session();
        Result result = session.run("CALL gds.graph.exists('neo4j');");
        if (result.hasNext()) {
            org.neo4j.driver.Record record = result.next();
            org.neo4j.driver.Value value = record.get("exists");
            if (value != null && value.asBoolean()) {
                log.info("Graph already loaded");
                return driver;
            }
        }
        session.run("CALL n10s.graphconfig.init();"); /// run only when creating a new graph
        session.run("CREATE CONSTRAINT n10s_unique_uri IF NOT EXISTS ON (r:Resource) ASSERT r.uri IS UNIQUE");
        log.info("n10 procedure and Constraints are loaded successfully");
        return driver;
    }
    
}
