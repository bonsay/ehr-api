package com.ehrapi.config;

import com.ehrapi.security.ModuleEntitlementInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the module-entitlement interceptor across the application/FHIR API
 * surface. The interceptor itself is a no-op except on endpoints annotated with
 * {@code @RequiresModule}, so the broad path patterns are safe.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ModuleEntitlementInterceptor moduleEntitlementInterceptor;

    public WebMvcConfig(ModuleEntitlementInterceptor moduleEntitlementInterceptor) {
        this.moduleEntitlementInterceptor = moduleEntitlementInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(moduleEntitlementInterceptor)
                .addPathPatterns("/api/**", "/fhir/**");
    }
}
