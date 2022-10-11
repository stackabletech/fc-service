package eu.gaiax.difs.fc.server.config;

import java.util.Collections;

import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

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

  /**
   * Defines firewall settings.
   */
  @Bean
  public HttpFirewall configureFirewall() {
    StrictHttpFirewall strictHttpFirewall = new StrictHttpFirewall();
    //strictHttpFirewall.setAllowedHttpMethods(Collections.emptyList());
    strictHttpFirewall.setAllowUrlEncodedDoubleSlash(true);
    return strictHttpFirewall;
  }

  /**
   * Defines RegistrationsHandlerMapping settings.
   */
  @Bean
  public WebMvcRegistrations webMvcRegistrationsHandlerMapping() {
    return new WebMvcRegistrations() {
        @Override
        public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
            RequestMappingHandlerMapping mapper = new RequestMappingHandlerMapping();
            mapper.setUrlDecode(true);
            return mapper;
        }
    };
  }
    
}