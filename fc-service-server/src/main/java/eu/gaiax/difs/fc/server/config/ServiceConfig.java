package eu.gaiax.difs.fc.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import eu.gaiax.difs.fc.core.config.CoreConfig;

/**
 * Federated Catalogue core service configuration.
 */
@Configuration
@Import(value = {CoreConfig.class})
public class ServiceConfig {

}
