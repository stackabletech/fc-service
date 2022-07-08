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

public class CustomJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final String resourceId;
    private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

    public CustomJwtAuthenticationConverter(String resourceId) {
        this.resourceId = resourceId;
    }

    private Collection<? extends GrantedAuthority> extractResourceRoles(final Jwt jwt, final String resourceId) {
        Collection<GrantedAuthority> authorities = this.extractAuthorities(jwt);
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        Map<String, Object> resource;
        Collection<String> resourceRoles;
        if (resourceAccess != null && (resource = (Map<String, Object>) resourceAccess.get(resourceId)) != null
                && (resourceRoles = (Collection<String>) resource.get("roles")) != null) {
            authorities.addAll(resourceRoles.stream()
                    .map(x -> new SimpleGrantedAuthority("ROLE_" + x))
                    .collect(Collectors.toSet()));
        }
        return authorities;
    }

    protected Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        return this.jwtGrantedAuthoritiesConverter.convert(jwt);
    }

    @Override
    public AbstractAuthenticationToken convert(final Jwt source) {
        Collection<GrantedAuthority> authorities = Stream.concat(this.jwtGrantedAuthoritiesConverter.convert(source)
                                .stream(),
                        extractResourceRoles(source, resourceId).stream())
                .collect(Collectors.toSet());
        return new JwtAuthenticationToken(source, authorities);
    }
}