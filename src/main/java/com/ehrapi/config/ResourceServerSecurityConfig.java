package com.ehrapi.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Secured configuration used in higher environments (the default).
 *
 * <p>Active when {@code ehr.security.enabled} is true (or unset). Every endpoint
 * requires a valid OAuth2/OIDC JWT access token, validated against the configured
 * issuer's JWKS ({@code spring.security.oauth2.resourceserver.jwt.issuer-uri}).
 * A small public allow-list (health, FHIR CapabilityStatement, API docs) stays
 * open so service discovery and probes work without a token.
 */
@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "ehr.security.enabled", havingValue = "true", matchIfMissing = true)
public class ResourceServerSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            @Qualifier("corsConfigurationSource") CorsConfigurationSource cors) throws Exception {
        http
            .cors(c -> c.configurationSource(cors))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(
                        "/fhir/metadata",
                        "/actuator/health", "/actuator/health/**", "/actuator/info",
                        "/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }
}
