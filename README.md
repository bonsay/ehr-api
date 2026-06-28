# ehr-api

Backend for the **Modular Electronic Health Record (EHR)** platform — Java 21 +
Spring Boot 3.2, persisting to Oracle (with an in-memory H2 profile for local
development). Tech stack and datasource configuration mirror
`content-retrieval-api`.

## What makes it "modular"

Every clinical capability is a **module** in a catalog (`ehr_modules`):
`DEMOGRAPHICS`, `ENCOUNTERS`, `PROBLEMS`, `MEDICATIONS`, `ALLERGIES`, `VITALS`.
Each institution turns on only the modules that suit its workflow
(`institution_modules`). The web client reads this to show/hide navigation and
chart tabs — so two institutions can run completely different feature sets off
the same API.

## Cross-institution sharing with patient consent

A patient is registered at a **home institution** but can grant **consent**
(`patient_consents`) for other institutions to view their record — either the
whole record (`scope = ALL`) or specific modules
(`scope = PROBLEMS,MEDICATIONS`).

`GET /api/sharing/patients/{id}/record?requestingInstitutionId=N` returns the
record **filtered by active consent**:

- Home institution → full access.
- Institution with scoped consent → only the consented modules; the rest are
  listed in `deniedModules` and returned empty.
- Institution with no active consent → `403 Forbidden`.

Consent enforcement lives in `ConsentService` / `SharingService`.

## Running

This is a Gradle project (Spring Boot Gradle plugin). Use the `bootRun` task and
pass the active profile via `--args`.

### Local (H2, no database needed)

```bash
./gradlew bootRun --args='--spring.profiles.active=h2'
```

On **Windows / PowerShell**:

```powershell
.\gradlew.bat bootRun --args='--spring.profiles.active=h2'
# or, avoiding quoting altogether:
$env:SPRING_PROFILES_ACTIVE="h2"; .\gradlew.bat bootRun
```

Seed data (`src/main/resources/data.sql`) loads automatically under H2 and
includes two institutions, a module catalog, two patients, sample clinical data
and one scoped consent. API on <http://localhost:8081>.

- Swagger UI: <http://localhost:8081/swagger-ui.html>
- H2 console: <http://localhost:8081/h2-console>
  (JDBC URL `jdbc:h2:mem:ehrdb`, user `sa`, empty password)

### Oracle (default profile)

```bash
./gradlew bootRun
```

Uses the same Oracle instance as `content-retrieval-api`
(`jdbc:oracle:thin:@therapyapp.internal:1521:XE`). `spring.sql.init.mode` is
`embedded`, so `data.sql` does **not** run against Oracle — seed production data
separately. Hibernate `ddl-auto: update` creates/updates the schema.

## API surface

| Area | Endpoints |
|------|-----------|
| Institutions | `GET/POST/PUT/DELETE /api/institutions` |
| Module catalog | `GET /api/modules` |
| Module enablement | `GET /api/institutions/{id}/modules`, `PUT /api/institutions/{id}/modules/{code}` |
| Patients | `GET/POST/PUT/DELETE /api/patients` (supports `?search=` and `?institutionId=`) |
| Encounters | `GET/POST /api/patients/{id}/encounters`, `PUT/DELETE /api/encounters/{id}` |
| Problems | `GET/POST /api/patients/{id}/problems`, `PUT/DELETE /api/problems/{id}` |
| Medications | `GET/POST /api/patients/{id}/medications`, `PUT/DELETE /api/medications/{id}` |
| Allergies | `GET/POST /api/patients/{id}/allergies`, `PUT/DELETE /api/allergies/{id}` |
| Vitals | `GET/POST /api/patients/{id}/vitals`, `PUT/DELETE /api/vitals/{id}` |
| Consent | `GET/POST /api/patients/{id}/consents`, `POST /api/patients/{id}/consents/{cid}/revoke` |
| Sharing | `GET /api/sharing/patients/{id}/record?requestingInstitutionId=N` |

## FHIR R4 API

The web client (and any third-party system) communicates with the backend using
**FHIR R4** for all patient and clinical data. The `com.ehrapi.fhir` package maps
each domain entity to its canonical FHIR resource and exposes a standards-based
RESTful FHIR interface (`Content-Type: application/fhir+json`). The `/api`
endpoints above remain for platform administration (module catalog, consent
admin).

| Domain | FHIR resource | Endpoints |
|--------|---------------|-----------|
| Patient | `Patient` | `GET /fhir/Patient[?name=&institution=]`, `GET /fhir/Patient/{id}`, `POST /fhir/Patient` |
| Institution | `Organization` | `GET /fhir/Organization`, `GET /fhir/Organization/{id}` |
| Problem | `Condition` | `GET /fhir/Condition?patient={id}`, `POST /fhir/Condition`, `DELETE /fhir/Condition/{id}` |
| Medication | `MedicationRequest` | `GET /fhir/MedicationRequest?patient={id}`, `POST`, `DELETE /fhir/MedicationRequest/{id}` |
| Allergy | `AllergyIntolerance` | `GET /fhir/AllergyIntolerance?patient={id}`, `POST`, `DELETE /fhir/AllergyIntolerance/{id}` |
| Vitals | `Observation` (vital-signs) | `GET /fhir/Observation?patient={id}`, `POST`, `DELETE /fhir/Observation/{id}` |
| Encounter | `Encounter` | `GET /fhir/Encounter?patient={id}`, `POST`, `DELETE /fhir/Encounter/{id}` |
| Consent | `Consent` | `GET /fhir/Consent?patient={id}` |
| — | `CapabilityStatement` | `GET /fhir/metadata` |

Searches return a FHIR `Bundle` of type `searchset`. **Cross-institution sharing
uses the standard `$everything` operation**, consent-enforced:

```
GET /fhir/Patient/{id}/$everything?requestingInstitution=N
```

It returns a `Bundle` containing the `Patient` plus the consented resources;
modules the patient did **not** consent to share are reported as an
informational `OperationOutcome` entry (`code: suppressed`). No active consent →
`403`.

## Authentication & authorization

The API is an **OAuth2/OIDC resource server**. In higher environments every
endpoint requires a valid **JWT access token**, validated against the IdP's JWKS.

Controlled by `ehr.security.enabled`:

| Mode | When | Behaviour |
|------|------|-----------|
| Secured (default) | `ehr.security.enabled=true` | JWT required; small public allow-list (`/fhir/metadata`, `/actuator/health`, API docs) |
| Open (dev) | `ehr.security.enabled=false` (set by the `h2` profile) | All endpoints open — local dev only |

Configuration (env vars):

```bash
EHR_SECURITY_ENABLED=true
OAUTH_ISSUER_URI=https://<your-idp>/realms/ehr   # required when secured
EHR_INSTITUTION_CLAIM=institution_id             # JWT claim carrying the institution id
EHR_CORS_ORIGINS=https://ehr.example.com         # lock to the web app origin(s)
```

**Identity → institution (the trust anchor).** Cross-institution sharing is
gated on the institution from the **verified token claim**, never a request
parameter. `CurrentInstitution` reads `ehr.auth.institution-claim`; the
`$everything` / sharing endpoints ignore the `requestingInstitution` parameter
when a token is present (the parameter is only a fallback in open/dev mode). A
client therefore cannot spoof another institution (covered by `FhirSecurityTest`).

### Configuring an identity provider

Any standards-compliant OIDC provider works — set `OAUTH_ISSUER_URI` to its
issuer and add a mapper/claim that emits the user's institution id under the
configured claim name:

- **Keycloak** — realm `ehr`, issuer `https://host/realms/ehr`; add a user
  attribute `institution_id` + a "User Attribute" protocol mapper (token claim
  `institution_id`).
- **Microsoft Entra ID** — issuer `https://login.microsoftonline.com/<tenant>/v2.0`;
  emit the institution via an app-role or an optional/extension claim.
- **Auth0** — issuer `https://<tenant>.auth0.com/`; add `institution_id` via a
  custom claim (Action/Rule).
- **AWS Cognito** — issuer `https://cognito-idp.<region>.amazonaws.com/<poolId>`;
  add a custom attribute surfaced as a token claim.

## Package layout

```
com.ehrapi
├── common        ModuleCodes
├── config        Cors / ResourceServer / Open security configs, OpenApiConfig
├── controller    REST controllers (one per area)
├── dto           request/response payloads
├── entity        JPA entities (plain Long FK columns, no lazy relations)
├── exception     ResourceNotFound / ConsentDenied + handler
├── fhir          FHIR R4 layer: Fhir (helpers), FhirMapper, FhirController
├── repository    Spring Data JPA repositories
├── security      CurrentInstitution (JWT claim -> institution id)
└── service       business logic incl. ModuleService, ConsentService, SharingService
```
