package com.ehrapi.fhir;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the OAuth2 resource-server behaviour and that cross-institution
 * sharing is driven by the JWT's institution claim (not a request parameter).
 *
 * Runs the secured OIDC config ({@code ehr.security.mode=oidc}) over the H2 seed
 * data. A mocked {@link JwtDecoder} satisfies the resource server without
 * needing a real identity provider; the {@code jwt()} post-processor injects a
 * pre-authenticated token with the desired claim.
 */
@SpringBootTest
@ActiveProfiles("h2")
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "ehr.security.mode=oidc",
        "ehr.security.seed-demo-users=false",
        "ehr.auth.institution-claim=institution_id"
})
class FhirSecurityTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mvc.perform(get("/fhir/Patient/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void capabilityStatementIsPublic() throws Exception {
        mvc.perform(get("/fhir/metadata"))
                .andExpect(status().isOk());
    }

    @Test
    void authenticatedReadIsAllowed() throws Exception {
        mvc.perform(get("/fhir/Patient/1").with(jwt().jwt(j -> j.claim("institution_id", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceType").value("Patient"));
    }

    @Test
    void everythingUsesHomeInstitutionFromToken() throws Exception {
        // Patient 1's home institution is 1 -> full record.
        mvc.perform(get("/fhir/Patient/1/$everything").with(jwt().jwt(j -> j.claim("institution_id", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceType").value("Bundle"));
    }

    @Test
    void everythingHonoursScopedConsentFromToken() throws Exception {
        // Institution 2 has a scoped consent (PROBLEMS, MEDICATIONS) for patient 1.
        mvc.perform(get("/fhir/Patient/1/$everything").with(jwt().jwt(j -> j.claim("institution_id", 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceType").value("Bundle"));
    }

    @Test
    void everythingForbiddenWhenTokenInstitutionHasNoConsent() throws Exception {
        // Institution 999 has no consent and is not the home institution.
        mvc.perform(get("/fhir/Patient/1/$everything").with(jwt().jwt(j -> j.claim("institution_id", 999))))
                .andExpect(status().isForbidden());
    }

    @Test
    void parameterCannotSpoofInstitutionWhenAuthenticated() throws Exception {
        // Token says 999 (no consent); the spoofed parameter must be ignored -> 403.
        mvc.perform(get("/fhir/Patient/1/$everything")
                        .param("requestingInstitution", "1")
                        .with(jwt().jwt(j -> j.claim("institution_id", 999))))
                .andExpect(status().isForbidden());
    }
}
