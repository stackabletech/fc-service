package eu.gaiax.difs.fc.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import eu.gaiax.difs.fc.api.generated.model.Error;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;

@Configuration
@EnableWebSecurity //(debug = true)
public class SecurityConfig extends KeycloakWebSecurityConfigurerAdapter {
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final Error FORBIDDEN_ERROR = new Error("forbidden_error",
            "The user does not have the permission to execute this request.");
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
    // TODO: 13.07.2022 Need to add access by scopes and by access to the participant.
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);
        http
                .csrf().disable()
                .authorizeRequests(authz -> authz
                        .antMatchers(HttpMethod.GET, "/demo").permitAll()
                        .antMatchers(HttpMethod.GET, "/demo/authorized").authenticated()
                        .antMatchers(HttpMethod.GET, "/demo/admin")
                        .hasAnyAuthority("SCOPE_gaia-x", "ROLE_Ro-MU-CA")

                        // Schema APIs
                        .antMatchers(HttpMethod.POST,"/schemas").hasRole("Ro-MU-CA")
                        .antMatchers(HttpMethod.DELETE,"/schemas/**").hasRole("Ro-MU-CA")
                        .antMatchers(HttpMethod.GET,"/schemas","/schemas/**").hasAnyRole("Ro-MU-CA","Ro-MU-A","Ro-SD-A","Ro-Pa-A")

                        // Self-Description APIs
                        .antMatchers(HttpMethod.GET, "/self-descriptions").authenticated()
                        .antMatchers(HttpMethod.GET, "/self-descriptions/{self_description_hash}").authenticated()
                        .antMatchers(HttpMethod.POST, "/self-descriptions")
                        .hasAnyRole("Ro-MU-CA", "Ro-SD-A", "Ro-MU-A")
                        .antMatchers(HttpMethod.DELETE, "/self-descriptions/{self_description_hash}")
                        .hasAnyRole("Ro-MU-CA", "Ro-SD-A", "Ro-MU-A")
                        .antMatchers(HttpMethod.POST, "/self-descriptions/{self_description_hash}/revoke")
                        .hasAnyRole("Ro-MU-CA", "Ro-SD-A", "Ro-MU-A")

                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt)
                .exceptionHandling()
                .accessDeniedHandler(accessDeniedHandler())
                .and()
                .oauth2ResourceServer()
                .jwt()
                .jwtAuthenticationConverter(new CustomJwtAuthenticationConverter(resourceId));
    }

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
