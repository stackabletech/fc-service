package eu.gaiax.difs.fc.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import eu.gaiax.difs.fc.api.generated.model.Error;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

/**
 * Note: WebSecurity adapter is deprecated in spring security 5.7;
 * so we are using SecurityFilterChain for configuration security without extending deprecated adapter.
 */
@Configuration
@EnableWebSecurity //(debug = true)
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {
  private static final ObjectMapper mapper = new ObjectMapper();

  private static final Error FORBIDDEN_ERROR = new Error("forbidden_error",
      "User does not have permission to execute this request.");
  
  @Value("${keycloak.resource}")
  private String resourceId;

  /**
   * Define security constraints for the application resources.
   */
  // TODO: 13.07.2022 Need to add access by scopes and by access to the participant.
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
      .csrf().disable()
      .authorizeRequests(authorization -> authorization
        .antMatchers(HttpMethod.GET, "/api/**", "/swagger-ui/**", "/actuator", "/actuator/**")
          .permitAll()

          // Schema APIs
          .antMatchers(HttpMethod.POST, "/schemas").hasRole("Ro-MU-CA")
          .antMatchers(HttpMethod.DELETE, "/schemas/**").hasRole("Ro-MU-CA")
          .antMatchers(HttpMethod.GET, "/schemas", "/schemas/**")
          .hasAnyRole("Ro-MU-CA", "Ro-MU-A", "Ro-SD-A", "Ro-Pa-A")

          // Query APIs
          .antMatchers("/query").permitAll()

          // Verification APIs
          .antMatchers("/verification").permitAll()
          
          // Self-Description APIs
          .antMatchers(HttpMethod.GET, "/self-descriptions").authenticated()
          .antMatchers(HttpMethod.GET, "/self-descriptions/{self_description_hash}")
          .authenticated()
          .antMatchers(HttpMethod.POST, "/self-descriptions")
          .hasAnyRole("Ro-MU-CA", "Ro-SD-A", "Ro-MU-A")
          .antMatchers(HttpMethod.DELETE, "/self-descriptions/{self_description_hash}")
          .hasAnyRole("Ro-MU-CA", "Ro-SD-A", "Ro-MU-A")
          .antMatchers(HttpMethod.POST, "/self-descriptions/{self_description_hash}/revoke")
          .hasAnyRole("Ro-MU-CA", "Ro-SD-A", "Ro-MU-A")

          // Participants API
          .antMatchers(HttpMethod.POST, "/participants").hasRole("Ro-MU-CA")
          .antMatchers(HttpMethod.GET, "/participants").hasAnyRole("Ro-MU-CA", "Ro-MU-A", "Ro-SD-A", "Ro-PA-A")
          .antMatchers("/participants/*").hasAnyRole("Ro-MU-CA", "Ro-MU-A")
          .antMatchers(HttpMethod.GET, "/participants/*/users").hasAnyRole("Ro-MU-CA", "Ro-MU-A", "Ro-PA-A")

          // User APIs
          .antMatchers("/users", "users/**").hasAnyRole("Ro-MU-CA", "Ro-MU-A", "Ro-PA-A")

          // Roles APIs
          .antMatchers("/roles").authenticated()

          // Session APIs
          .antMatchers("/session").authenticated()
          
          .anyRequest().authenticated()
        )
        .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt)
        .exceptionHandling()
        .accessDeniedHandler(accessDeniedHandler())
        .and()
        .oauth2ResourceServer()
        .jwt()
        .jwtAuthenticationConverter(new CustomJwtAuthenticationConverter(resourceId));

    return http.build();
  }

  /**
   * Customize Access Denied application exception.
   */
  private static AccessDeniedHandler accessDeniedHandler() {
    return (HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) -> {
      response.setStatus(HttpStatus.FORBIDDEN.value());
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
      response.getWriter().write(ow.writeValueAsString(FORBIDDEN_ERROR));
    };
  }
}
