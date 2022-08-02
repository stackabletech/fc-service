package eu.gaiax.difs.fc.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configurations for working with JSON.
 */
@Configuration
public class JacksonConfig {
  /**
   * Provides functionality for reading and writing JSON, either to and from basic POJOs, or to and from
   * a general-purpose JSON Tree Model (JsonNode), as well as related functionality for performing conversions.
   *
   * @return ObjectMapper
   */
  @Bean
  @Primary
  public ObjectMapper objectMapper() {
    return new ObjectMapper()
        .registerModule(new ParameterNamesModule())
        .registerModule(new JavaTimeModule());
  }
}