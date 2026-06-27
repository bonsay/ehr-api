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

## Package layout

```
com.ehrapi
├── common        ModuleCodes
├── config        SecurityConfig, OpenApiConfig
├── controller    REST controllers (one per area)
├── dto           request/response payloads
├── entity        JPA entities (plain Long FK columns, no lazy relations)
├── exception     ResourceNotFound / ConsentDenied + handler
├── repository    Spring Data JPA repositories
└── service       business logic incl. ModuleService, ConsentService, SharingService
```

> Security note: like the reference project, endpoints are currently open
> (`permitAll`) to keep the demo simple. The "acting as" institution is supplied
> by the client. A production deployment must add authentication and derive the
> requesting institution from the authenticated principal rather than trusting a
> request parameter.
