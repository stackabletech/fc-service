package eu.gaiax.difs.fc.server.config;

import org.keycloak.adapters.springsecurity.authentication.KeycloakAuthenticationProvider;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.keycloak.adapters.springsecurity.filter.KeycloakAuthenticatedActionsFilter;
import org.keycloak.adapters.springsecurity.filter.KeycloakAuthenticationProcessingFilter;
import org.keycloak.adapters.springsecurity.filter.KeycloakPreAuthActionsFilter;
import org.keycloak.adapters.springsecurity.filter.KeycloakSecurityContextRequestFilter;
import org.keycloak.adapters.springsecurity.management.HttpSessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;

@Configuration
@EnableWebSecurity //(debug = true)
public class SecurityConfig extends KeycloakWebSecurityConfigurerAdapter {
    @Value("${keycloak.resource}")
    private String resourceId;


    /**
     * Defines bean for the Keycloak Authentication Processing Filter.
     */
    @Bean
    public FilterRegistrationBean keycloakAuthenticationProcessingFilter(
            KeycloakAuthenticationProcessingFilter filter) {
        FilterRegistrationBean registrationBean = new FilterRegistrationBean(filter);
        registrationBean.setEnabled(false);
        return registrationBean;
    }

    /**
     * Defines bean for the Keycloak Pre Auth Actions Filter.
     */
    @Bean
    public FilterRegistrationBean keycloakPreAuthActionsFilter(KeycloakPreAuthActionsFilter filter) {
        FilterRegistrationBean registrationBean = new FilterRegistrationBean(filter);
        registrationBean.setEnabled(false);
        return registrationBean;
    }

    /**
     * Defines bean for the Keycloak Authenticated Actions Filter.
     */
    @Bean
    public FilterRegistrationBean keycloakAuthenticatedActionsFilter(KeycloakAuthenticatedActionsFilter filter) {
        FilterRegistrationBean registrationBean = new FilterRegistrationBean(filter);
        registrationBean.setEnabled(false);
        return registrationBean;
    }

    /**
     * Defines bean for the Keycloak Security Context Request Filter.
     */
    @Bean
    public FilterRegistrationBean keycloakSecurityContextRequestFilter(KeycloakSecurityContextRequestFilter filter) {
        FilterRegistrationBean registrationBean = new FilterRegistrationBean(filter);
        registrationBean.setEnabled(false);
        return registrationBean;
    }

    /**
     * <p>
     * Registers the KeycloakAuthenticationProvider with the authentication manager.
     * </p>
     * <p>
     * Since Spring Security requires that role names start with "ROLE_",
     * a SimpleAuthorityMapper is used to instruct the KeycloakAuthenticationProvider
     * to insert the "ROLE_" prefix.
     * </p>
     * <p>
     * e.g. Librarian -> ROLE_Librarian
     * </p>
     * <p>
     * Should you prefer to have the role all in uppercase, you can instruct
     * the SimpleAuthorityMapper to convert it by calling:
     * {@code grantedAuthorityMapper.setConvertToUpperCase(true); }.
     * The result will be: Librarian -> ROLE_LIBRARIAN.
     * </p>
     */
    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) {
        KeycloakAuthenticationProvider keycloakAuthenticationProvider = keycloakAuthenticationProvider();
        SimpleAuthorityMapper grantedAuthorityMapper = new SimpleAuthorityMapper();
        keycloakAuthenticationProvider.setGrantedAuthoritiesMapper(grantedAuthorityMapper);
        auth.authenticationProvider(keycloakAuthenticationProvider);
    }

    /**
     * <p>
     * Defines the session authentication strategy.
     * </p>
     * <p>
     * RegisterSessionAuthenticationStrategy is used because this is a public application from the Keycloak point
     * of view.
     * </p>
     */
    @Bean
    @Override
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
    }

    /**
     * <p>
     * Define an HttpSessionManager bean only if missing.
     * </p>
     * <p>
     * This is necessary because since Spring Boot 2.1.0, spring.main.allow-bean-definition-overriding is disabled
     * by default.
     * </p>
     */
    @Bean
    @Override
    @ConditionalOnMissingBean(HttpSessionManager.class)
    protected HttpSessionManager httpSessionManager() {
        return new HttpSessionManager();
    }

    /**
     * Define security constraints for the application resources.
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);
        http
                .authorizeRequests(authz -> authz
                        .antMatchers(HttpMethod.GET, "/demo").permitAll()
                        .antMatchers(HttpMethod.GET, "/demo/authorized").authenticated()
                        .antMatchers(HttpMethod.GET, "/demo/admin")
                        .access("hasAuthority('SCOPE_gaia-x') and hasRole('Ro-MU-CA')"))
                .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt).oauth2ResourceServer()
                .jwt()
                .jwtAuthenticationConverter(new CustomJwtAuthenticationConverter(resourceId));
    }
}
