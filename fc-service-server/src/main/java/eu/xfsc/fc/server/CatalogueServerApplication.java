package eu.xfsc.fc.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * The Application class.
 */
@EnableScheduling
@SpringBootApplication(exclude = HibernateJpaAutoConfiguration.class)
@EnableFeignClients(basePackages = {"eu.xfsc.fc.core.service.catalogue"})
@EnableNeo4jRepositories(basePackages = {"eu.xfsc.fc.core.dao.catalogue.node.repository"})
// HibernateJpaAutoConfiguration breaks the Participant API implementation
public class CatalogueServerApplication {
    /**
     * The main Method.
     *
     * @param args the args for the main method
     */
    public static void main(String[] args) {
        System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true");
        SpringApplication.run(CatalogueServerApplication.class, args);
    }
}
