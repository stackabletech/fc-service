package eu.gaiax.difs.fc.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
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
          .authorizeRequests()
          .antMatchers("/","/index.html","/css/styles.css","/js/scripts.js","js/library/**").permitAll()
          .anyRequest()
          .authenticated()
          .and()
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