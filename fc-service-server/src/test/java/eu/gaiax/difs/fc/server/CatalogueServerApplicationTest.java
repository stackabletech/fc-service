package eu.gaiax.difs.fc.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

//import eu.gaiax.difs.fc.core.config.EmbeddedNeo4JConfig;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;

@SpringBootTest
@AutoConfigureEmbeddedDatabase(provider = DatabaseProvider.ZONKY)
//@Import(EmbeddedNeo4JConfig.class)
public class CatalogueServerApplicationTest {
    @Test
    void contextLoads() {
    }
}