package cl.rutaexpress.catalog.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.DelegatingJwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String SERVICES = "/api/catalog/services";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        // Permite cualquier consulta GET bajo /api/catalog/** para la comunicación entre microservicios
                        .requestMatchers(HttpMethod.GET, "/api/catalog/**").permitAll()
                        // Permite POST y PUT tanto a ADMIN como a DISPATCHER si se reenvía el token
                        .requestMatchers(HttpMethod.POST, SERVICES).hasAnyRole("ADMIN", "DISPATCHER")
                        .requestMatchers(HttpMethod.PUT, SERVICES + "/**").hasAnyRole("ADMIN", "DISPATCHER")
                        .anyRequest().authenticated()) // Cambiar denyAll() por authenticated() evita bloqueos accidentales
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new DelegatingJwtGrantedAuthoritiesConverter(
                new JwtGrantedAuthoritiesConverter(), authoritiesConverter));
        return converter;
    }
}