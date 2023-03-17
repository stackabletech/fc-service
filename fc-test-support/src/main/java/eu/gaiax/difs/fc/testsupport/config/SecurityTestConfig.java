package eu.gaiax.difs.fc.testsupport.config;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

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
        .authorizeHttpRequests(authorize -> authorize 
    			.requestMatchers(antMatcher("/auth/realms/gaia-x/protocol/openid-connect/token")).permitAll()
    			.requestMatchers(antMatcher("/auth/realms/gaia-x/.well-known/openid-configuration")).permitAll()
    			.requestMatchers(antMatcher("/auth/realms/gaia-x/protocol/openid-connect/certs")).permitAll()
    			.requestMatchers(antMatcher("/.well-known/oauth-authorization-server/auth/realms/gaia-x")).permitAll()
    			.requestMatchers(antMatcher("/auth/**")).authenticated()
		);
    return http.build();
  }

}
