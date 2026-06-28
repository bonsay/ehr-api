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
| Auth | `POST /api/auth/login` (local mode), `GET /api/auth/me` |
| Admin · Roles | `GET /api/admin/roles`, `GET /api/admin/roles/permission-catalog`, `PUT /api/admin/roles/{code}/permissions` (`ADMIN:ROLES`) |
| Admin · Users | `GET/POST /api/admin/users`, `PUT/DELETE /api/admin/users/{id}` (`ADMIN:USERS`) |

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

Authentication is a **JWT bearer** model with **role-based authorization**
enforced by Spring method security. The mode is selected by
`ehr.security.mode`:

| Mode | When | Behaviour |
|------|------|-----------|
| `oidc` (default) | higher environments | OAuth2/OIDC resource server — JWTs validated against the external IdP's JWKS. Roles come from the token. |
| `local` | dev / demo (set by the `h2` profile) | The API authenticates users itself (`POST /api/auth/login`) and issues its own HS256 JWTs. Self-contained — no external IdP. |
| `open` | pure local dev | No authentication; method security is off, so every action is permitted. |

A small public allow-list (`/fhir/metadata`, `/actuator/health`, API docs, and —
in local mode — `/api/auth/login`) stays open in the secured modes.

### Roles, permissions and the access model

Authorization is driven by **permissions** of the form `MODULE:ACTION`
(`VITALS:WRITE`, `MEDICATIONS:WRITE`, …) plus administrative permissions
(`ADMIN:USERS`, `ADMIN:ROLES`, `ADMIN:MODULES`). **Reads** require only an
authenticated user; **writes** require the matching `MODULE:WRITE` permission;
the admin surface requires the matching `ADMIN:*` permission.

Permissions are bundled into **roles**, stored in the database and editable at
runtime by an administrator (`PUT /api/admin/roles/{code}/permissions`). The
built-in roles seed as:

| Role | Highlights |
|------|------------|
| `ADMINISTRATOR` | `ADMIN:USERS`, `ADMIN:ROLES`, `ADMIN:MODULES` + read across all modules |
| `PHYSICIAN` | read + write on every clinical module, **including prescribing** (`MEDICATIONS:WRITE`) |
| `NURSE` | read all; write vitals, encounters, allergies — **cannot prescribe** medications |
| `RECEPTIONIST` | register/update patient demographics; read encounters |

This is the worked example from the brief: a **nurse can enter vitals but not
prescribe medication**, while a **physician can** — enforced server-side by
`@PreAuthorize` on the FHIR and `/api` write endpoints, and mirrored in the web
UI. An administrator manages all of this through `/api/admin/*`.

### Demo users (local mode)

With the `h2` profile (`ehr.security.seed-demo-users=true`) these accounts are
seeded (password = `<username>123`):

| Username | Password | Role | Institution |
|----------|----------|------|-------------|
| `admin` | `admin123` | ADMINISTRATOR | General Hospital |
| `physician` | `physician123` | PHYSICIAN | General Hospital |
| `nurse` | `nurse123` | NURSE | General Hospital |
| `reception` | `reception123` | RECEPTIONIST | Downtown Clinic |

```bash
# Log in and call a protected endpoint
TOKEN=$(curl -s localhost:8081/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"physician","password":"physician123"}' | jq -r .accessToken)
curl -s localhost:8081/api/auth/me -H "Authorization: Bearer $TOKEN"
```

### Configuration (env vars)

```bash
EHR_SECURITY_MODE=oidc                           # oidc | local | open
OAUTH_ISSUER_URI=https://<your-idp>/realms/ehr   # required for oidc mode
EHR_INSTITUTION_CLAIM=institution_id             # JWT claim carrying the institution id
EHR_ROLES_CLAIM=roles                            # JWT claim carrying role codes (oidc)
EHR_LOCAL_JWT_SECRET=<32+ byte secret>           # HS256 signing key for local mode
EHR_SEED_DEMO_USERS=false                        # seed the demo accounts above
EHR_CORS_ORIGINS=https://ehr.example.com         # lock to the web app origin(s)
```

In **oidc** mode the token's `roles` claim is expanded into permissions using the
same admin-editable role definitions, so the role model applies identically
whether a user signs in locally or through the external IdP.

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
├── config        Cors / Local / ResourceServer (oidc) / Open security configs, PasswordConfig, OpenApiConfig
├── controller    REST controllers incl. AuthController, RoleAdminController, UserAdminController
├── dto           request/response payloads (login, current user, roles, users…)
├── entity        JPA entities incl. Role, AppUser (plain Long FK columns)
├── exception     ResourceNotFound / ConsentDenied / auth + access-denied handlers
├── fhir          FHIR R4 layer: Fhir (helpers), FhirMapper, FhirController (@PreAuthorize on writes)
├── repository    Spring Data JPA repositories incl. RoleRepository, AppUserRepository
├── security      Permissions catalog, CurrentInstitution, CurrentUser, RoleAuthorityService,
│                  EhrAuthoritiesConverter, LocalTokenService, SecuritySeeder
└── service       business logic incl. LoginService, RoleAdminService, UserAdminService
```
