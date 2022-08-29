package eu.gaiax.difs.fc.core.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Core Service configs.
 */
@Configuration
@ComponentScan(basePackages = {"eu.gaiax.difs.fc.core"})
public class CoreConfig {    
    
    //@Bean
    //public FileStoreImpl fileStore() {
    //    return new FileStoreImpl();
    //}
}
