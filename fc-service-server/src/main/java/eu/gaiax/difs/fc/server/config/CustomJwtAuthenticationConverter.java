package eu.gaiax.difs.fc.server.config;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * Converter provides type conversion for custom jwt claim values.
 */
public class CustomJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
  private final String resourceId;
  private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

  /**
   * Constructs a new Converter with the specified resourceId.
   *
   * @param resourceId Keycloak client id.
   */
  public CustomJwtAuthenticationConverter(String resourceId) {
    this.resourceId = resourceId;
  }

  /**
   * Extract all user authorities.
   *
   * @param jwt User authentication token.
   * @param resourceId Keycloak client id.
   * @return Collection of user authorities.
   */
  private Collection<? extends GrantedAuthority> extractResourceRoles(final Jwt jwt, final String resourceId) {
    Collection<GrantedAuthority> authorities = this.jwtGrantedAuthoritiesConverter.convert(jwt);
    Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
    Map<String, Object> resourceData = (Map<String, Object>) resourceAccess.get(resourceId);
    Collection<String> resourceRoles;
    if (resourceAccess != null && (resourceRoles = (Collection<String>) resourceData.get("roles")) != null) {
      authorities.addAll(resourceRoles.stream()
          .map(x -> new SimpleGrantedAuthority("ROLE_" + x)).collect(Collectors.toSet()));
    }
    return authorities;
  }

  /**
   * Convert user jwt token to JwtAuthenticationToken with all user authorities.
   *
   * @param source User authentication token.
   * @return JwtAuthenticationToken with all user authorities.
   */
  @Override
  public AbstractAuthenticationToken convert(final Jwt source) {
    Collection<GrantedAuthority> authorities =
        Stream.concat(this.jwtGrantedAuthoritiesConverter.convert(source).stream(),
          extractResourceRoles(source, resourceId).stream()).collect(Collectors.toSet());
    return new JwtAuthenticationToken(source, authorities);
  }
}