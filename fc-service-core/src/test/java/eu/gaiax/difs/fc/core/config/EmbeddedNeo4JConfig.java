package eu.gaiax.difs.fc.core.config;

import java.util.List;

import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.configuration.connectors.BoltConnector;
import org.neo4j.configuration.helpers.SocketAddress;
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
    
}
