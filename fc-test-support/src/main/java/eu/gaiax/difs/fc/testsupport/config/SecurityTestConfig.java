package eu.gaiax.difs.fc.testsupport.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@ConditionalOnProperty(value = "federated-catalogue.scope", havingValue = "test")
public class SecurityTestConfig {

  @Bean
  public SecurityFilterChain authFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf().disable()
        .antMatcher("/auth/**")
        .authorizeRequests(authorization -> authorization
            .antMatchers("/auth/realms/gaia-x/protocol/openid-connect/token").permitAll()
            .antMatchers("/auth/realms/gaia-x/.well-known/openid-configuration").permitAll()
            .antMatchers("/auth/realms/gaia-x/protocol/openid-connect/certs").permitAll()
            .antMatchers("/.well-known/oauth-authorization-server/auth/realms/gaia-x").permitAll()
        )
    ;
    return http.build();
  }
}
