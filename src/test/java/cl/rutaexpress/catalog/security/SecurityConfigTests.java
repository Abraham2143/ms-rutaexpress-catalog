package cl.rutaexpress.catalog.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTests {

    private final SecurityConfig config = new SecurityConfig();

    @Test
    void combinaRolesAzureYScopesSinPerderAuthorities() {
        Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "RS256")
                .subject("test-user")
                .claim("roles", List.of("ADMIN", "DISPATCHER", "CLIENT"))
                .claim("scp", "OT.Create OT.Read").build();

        var authentication = config.jwtAuthenticationConverter().convert(jwt);

        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_DISPATCHER", "ROLE_CLIENT",
                        "SCOPE_OT.Create", "SCOPE_OT.Read", "FACTOR_BEARER");
    }

    @Test
    void conservaElClaimScopeEstandarSinRoles() {
        Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "RS256")
                .subject("test-user").claim("scope", "OT.Read").build();

        var authentication = config.jwtAuthenticationConverter().convert(jwt);

        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("SCOPE_OT.Read", "FACTOR_BEARER");
    }

    @Test
    void sinRolesNiScopesConservaSoloElFactorBearer() {
        Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "RS256")
                .subject("test-user").build();

        assertThat(config.jwtAuthenticationConverter().convert(jwt).getAuthorities())
                .extracting(GrantedAuthority::getAuthority).containsExactly("FACTOR_BEARER");
    }
}
