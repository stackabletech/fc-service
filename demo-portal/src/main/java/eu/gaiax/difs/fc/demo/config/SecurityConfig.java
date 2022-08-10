package eu.gaiax.difs.fc.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;


/**
 * Specifies the application's security configuration.
 */
@EnableWebSecurity //(debug = true)
public class SecurityConfig { 

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        
        http
          .csrf()
          .disable()
          .authorizeRequests()
          .anyRequest()
          .authenticated()
          .and()
          .oauth2Login(oauth2Login ->
              oauth2Login
                  .loginPage("/oauth2/authorization/fc-client-oidc")
              )
          .oauth2Client(Customizer.withDefaults())
          ;
        return http.build();
    }
       
}