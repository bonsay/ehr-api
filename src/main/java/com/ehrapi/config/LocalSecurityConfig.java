package com.ehrapi.config;

import com.ehrapi.security.EhrAuthoritiesConverter;
import com.ehrapi.security.RoleAuthorityService;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
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
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * {@code local} security mode: the API is its own identity provider. It issues
 * HS256 JWTs from {@code POST /api/auth/login} (see {@code LocalTokenService})
 * and validates them as a resource server using the same shared secret. This
 * makes the full authentication + role-based authorization story runnable with
 * no external IdP — ideal for development and demos.
 *
 * <p>Method security is enabled here so {@code @PreAuthorize} on write endpoints
 * is enforced. The login endpoint stays public; everything else requires a valid
 * token.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@ConditionalOnProperty(name = "ehr.security.mode", havingValue = "local")
public class LocalSecurityConfig {

    private final SecretKey secretKey;

    public LocalSecurityConfig(@Value("${ehr.security.local.jwt-secret:dev-local-secret-change-me-please-32b+}") String secret) {
        // HS256 requires a >= 256-bit key; pad short dev secrets deterministically.
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            bytes = Arrays.copyOf(bytes, 32);
        }
        this.secretKey = new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            @Qualifier("corsConfigurationSource") CorsConfigurationSource cors,
            RoleAuthorityService roleAuthorityService) throws Exception {

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(
                new EhrAuthoritiesConverter("authorities", "roles", roleAuthorityService));

        http
            .cors(c -> c.configurationSource(cors))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(
                        "/api/auth/login",
                        "/api/onboarding/register",
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
