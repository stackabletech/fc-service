package eu.gaiax.difs.fc.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Defines callback methods to customize the Java-based configuration for Spring MVC.
 * Custom implementation of the {@link WebMvcConfigurer}.
 */
@Configuration
public class WebConfiguration implements WebMvcConfigurer {
  /**
   * Defines cross-origin resource sharing.
   */
  @Bean
  public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**").allowedOrigins("*")
          .allowedMethods("GET", "PUT", "POST", "PATCH", "DELETE", "OPTIONS");
      }
    };
  }
}