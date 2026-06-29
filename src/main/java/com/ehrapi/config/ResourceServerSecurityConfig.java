package com.ehrapi.config;

import com.ehrapi.security.EhrAuthoritiesConverter;
import com.ehrapi.security.RoleAuthorityService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * {@code oidc} security mode (the default for higher environments). Every
 * endpoint requires a valid OAuth2/OIDC JWT access token, validated against the
 * configured issuer's JWKS ({@code spring.security.oauth2.resourceserver.jwt.issuer-uri}).
 * A small public allow-list (health, FHIR CapabilityStatement, API docs) stays
 * open so service discovery and probes work without a token.
 *
 * <p>Authorities are derived from the token: a {@code roles} claim (configurable)
 * is expanded into permissions via the admin-editable role definitions, so the
 * same role-based authorization enforced by {@code @PreAuthorize} applies whether
 * the user signs in locally or through the external IdP.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@ConditionalOnProperty(name = "ehr.security.mode", havingValue = "oidc", matchIfMissing = true)
public class ResourceServerSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            @Qualifier("corsConfigurationSource") CorsConfigurationSource cors,
            RoleAuthorityService roleAuthorityService,
            @Value("${ehr.auth.roles-claim:roles}") String rolesClaim,
            @Value("${ehr.auth.authorities-claim:authorities}") String authoritiesClaim) throws Exception {

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(
                new EhrAuthoritiesConverter(authoritiesClaim, rolesClaim, roleAuthorityService));

        http
            .cors(c -> c.configurationSource(cors))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(
                        "/api/billing/webhook",
                        "/fhir/metadata",
                        "/actuator/health", "/actuator/health/**", "/actuator/info",
                        "/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter)));

        return http.build();
    }
}
