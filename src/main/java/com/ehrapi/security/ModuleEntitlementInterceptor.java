package com.ehrapi.security;

import com.ehrapi.service.EntitlementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * Enforces module entitlements on annotated endpoints. Before a handler marked
 * with {@link RequiresModule} runs, the institution is resolved from the verified
 * token and checked against {@link EntitlementService}; an unentitled paid module
 * yields HTTP 402 (via {@code ModuleNotEntitledException}).
 *
 * <p>When no institution can be resolved (open/local-dev with no token) the check
 * is skipped, mirroring {@link CurrentInstitution#resolveOrFallback}: enforcement
 * applies wherever a verified institution context exists.
 */
@Component
public class ModuleEntitlementInterceptor implements HandlerInterceptor {

    private final EntitlementService entitlementService;
    private final CurrentInstitution currentInstitution;

    public ModuleEntitlementInterceptor(EntitlementService entitlementService,
                                        CurrentInstitution currentInstitution) {
        this.entitlementService = entitlementService;
        this.currentInstitution = currentInstitution;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        RequiresModule requirement = handlerMethod.getMethodAnnotation(RequiresModule.class);
        if (requirement == null) {
            requirement = handlerMethod.getBeanType().getAnnotation(RequiresModule.class);
        }
        if (requirement == null) {
            return true;
        }
        Optional<Long> institution = currentInstitution.resolve();
        // No verified institution context (open/dev mode): nothing to enforce against.
        if (institution.isPresent()) {
            entitlementService.requireEntitled(institution.get(), requirement.value());
        }
        return true;
    }
}
