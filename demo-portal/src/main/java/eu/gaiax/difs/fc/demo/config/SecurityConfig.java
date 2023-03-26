package eu.gaiax.difs.fc.demo.config;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;


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
          .authorizeHttpRequests(authorization -> authorization
        		  .requestMatchers(antMatcher("/")).permitAll()
        		  .requestMatchers(antMatcher("/index.html")).permitAll()
        		  .requestMatchers(antMatcher("/css/**")).permitAll()
        		  .requestMatchers(antMatcher("/js/**")).permitAll()
        		  .requestMatchers(antMatcher("/")).permitAll()
        		  .anyRequest().authenticated()
        	)
          //.and()
          .oauth2Login(oauth2Login ->
              oauth2Login
                  .loginPage("/oauth2/authorization/fc-client-oidc")
              )
          .oauth2Client(Customizer.withDefaults())
            .logout()
            .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
            .logoutSuccessUrl("/index.html")
            .clearAuthentication(true)
            .invalidateHttpSession(true)
            .deleteCookies("JSESSIONID","XSRF-TOKEN").permitAll()
          ;
        return http.build();
    }
       
}