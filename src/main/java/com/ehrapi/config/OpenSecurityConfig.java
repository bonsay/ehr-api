package com.ehrapi.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Open configuration for LOCAL DEVELOPMENT ONLY. Active when
 * {@code ehr.security.enabled=false} (set by the {@code h2} profile). All
 * endpoints are permitted so the app runs without an identity provider.
 *
 * <p>Never enable this in a higher environment.
 */
@Configuration
@EnableWebSecurity
@ConditionalOnProperty(name = "ehr.security.enabled", havingValue = "false")
public class OpenSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            @Qualifier("corsConfigurationSource") CorsConfigurationSource cors) throws Exception {
        http
            .cors(c -> c.configurationSource(cors))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz.anyRequest().permitAll())
            .headers(h -> h.frameOptions(f -> f.disable())); // allow the H2 console

        return http.build();
    }
}
