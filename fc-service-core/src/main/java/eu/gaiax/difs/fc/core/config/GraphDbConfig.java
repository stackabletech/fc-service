package eu.gaiax.difs.fc.core.config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@ConditionalOnProperty(value="federated-catalogue.scope", havingValue="runtime")
public class GraphDbConfig {

	@Value("${graphstore.uri}")
	private String uri;
	@Value("${graphstore.user}")
	private String user;
	@Value("${graphstore.password}")
	private String password;
	
	@Bean(destroyMethod = "close")
	public Driver driver() {
        Driver driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
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
