package com.ehrapi.fhir;

import com.ehrapi.dto.SharedPatientRecordDto;
import com.ehrapi.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.ehrapi.fhir.Fhir.CONTENT_TYPE;

/**
 * FHIR R4 facade. Exposes the EHR over a standards-based RESTful FHIR API so
 * that the web client (and any third-party system) communicates using FHIR
 * resources and Bundles rather than bespoke payloads.
 *
 * Supported interactions:
 * <pre>
 *   GET    /fhir/metadata                         CapabilityStatement
 *   GET    /fhir/Patient                          search (optional ?name=)
 *   GET    /fhir/Patient/{id}                     read
 *   POST   /fhir/Patient                          create
 *   GET    /fhir/Patient/{id}/$everything         consent-gated record Bundle
 *   GET    /fhir/Organization                     search
 *   GET    /fhir/Condition?patient={id}           search   (+ POST / DELETE)
 *   GET    /fhir/MedicationRequest?patient={id}   search   (+ POST / DELETE)
 *   GET    /fhir/AllergyIntolerance?patient={id}  search   (+ POST / DELETE)
 *   GET    /fhir/Observation?patient={id}         search   (+ POST / DELETE)
 *   GET    /fhir/Encounter?patient={id}           search   (+ POST / DELETE)
 *   GET    /fhir/Consent?patient={id}             search
 * </pre>
 */
@RestController
@RequestMapping("/fhir")
@Tag(name = "FHIR", description = "FHIR R4 standards-based API")
public class FhirController {

    private final FhirMapper mapper;
    private final PatientService patientService;
    private final InstitutionService institutionService;
    private final EncounterService encounterService;
    private final ProblemService problemService;
    private final MedicationService medicationService;
    private final AllergyService allergyService;
    private final VitalSignService vitalSignService;
    private final ConsentService consentService;
    private final SharingService sharingService;

    public FhirController(FhirMapper mapper, PatientService patientService,
                          InstitutionService institutionService, EncounterService encounterService,
                          ProblemService problemService, MedicationService medicationService,
                          AllergyService allergyService, VitalSignService vitalSignService,
                          ConsentService consentService, SharingService sharingService) {
        this.mapper = mapper;
        this.patientService = patientService;
        this.institutionService = institutionService;
        this.encounterService = encounterService;
        this.problemService = problemService;
        this.medicationService = medicationService;
        this.allergyService = allergyService;
        this.vitalSignService = vitalSignService;
        this.consentService = consentService;
        this.sharingService = sharingService;
    }

    // ---- Capability statement -------------------------------------------------

    @GetMapping(value = "/metadata", produces = CONTENT_TYPE)
    @Operation(summary = "FHIR CapabilityStatement")
    public Map<String, Object> metadata() {
        List<String> resourceTypes = List.of("Patient", "Organization", "Condition",
                "MedicationRequest", "AllergyIntolerance", "Observation", "Encounter", "Consent");
        List<Object> resources = resourceTypes.stream().map(t -> Fhir.obj(
                "type", t,
                "interaction", Fhir.list(Fhir.obj("code", "read"), Fhir.obj("code", "search-type"))
        )).map(o -> (Object) o).toList();
        return Fhir.obj(
                "resourceType", "CapabilityStatement",
                "status", "active",
                "kind", "instance",
                "fhirVersion", "4.0.1",
                "format", Fhir.list("application/fhir+json"),
                "rest", Fhir.list(Fhir.obj(
                        "mode", "server",
                        "resource", resources,
                        "operation", Fhir.list(Fhir.obj("name", "everything",
                                "definition", "http://hl7.org/fhir/OperationDefinition/Patient-everything")))));
    }

    // ---- Patient --------------------------------------------------------------

    @GetMapping(value = "/Patient", produces = CONTENT_TYPE)
    public Map<String, Object> searchPatients(@RequestParam(required = false) String name,
                                              @RequestParam(required = false) Long institution) {
        var patients = institution != null
                ? patientService.getByInstitution(institution)
                : patientService.search(name);
        return mapper.searchset(patients.stream().map(mapper::toPatient).toList());
    }

    @GetMapping(value = "/Patient/{id}", produces = CONTENT_TYPE)
    public Map<String, Object> readPatient(@PathVariable Long id) {
        return mapper.toPatient(patientService.getById(id));
    }

    @PostMapping(value = "/Patient", produces = CONTENT_TYPE)
    public ResponseEntity<Map<String, Object>> createPatient(@RequestBody Map<String, Object> body) {
        var created = patientService.create(mapper.fromPatient(body));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toPatient(created));
    }

    @GetMapping(value = "/Patient/{id}/$everything", produces = CONTENT_TYPE)
    @Operation(summary = "Consent-gated patient record Bundle",
            description = "Returns the patient's record as a FHIR Bundle, filtered by the patient's "
                    + "active consent for the requesting institution. 403 when no consent exists.")
    public Map<String, Object> everything(@PathVariable Long id,
                                          @RequestParam Long requestingInstitution) {
        SharedPatientRecordDto record = sharingService.getSharedRecord(id, requestingInstitution);
        return mapper.everythingBundle(record);
    }

    // ---- Organization ---------------------------------------------------------

    @GetMapping(value = "/Organization", produces = CONTENT_TYPE)
    public Map<String, Object> searchOrganizations() {
        return mapper.searchset(institutionService.getAll().stream().map(mapper::toOrganization).toList());
    }

    @GetMapping(value = "/Organization/{id}", produces = CONTENT_TYPE)
    public Map<String, Object> readOrganization(@PathVariable Long id) {
        return mapper.toOrganization(institutionService.getById(id));
    }

    // ---- Condition (Problem) --------------------------------------------------

    @GetMapping(value = "/Condition", produces = CONTENT_TYPE)
    public Map<String, Object> searchConditions(@RequestParam Long patient) {
        return mapper.searchset(problemService.getForPatient(patient).stream()
                .map(mapper::toCondition).toList());
    }

    @PostMapping(value = "/Condition", produces = CONTENT_TYPE)
    public ResponseEntity<Map<String, Object>> createCondition(@RequestBody Map<String, Object> body) {
        var problem = mapper.fromCondition(body);
        var created = problemService.create(problem.getPatientId(), problem);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toCondition(created));
    }

    @DeleteMapping("/Condition/{id}")
    public ResponseEntity<Void> deleteCondition(@PathVariable Long id) {
        problemService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---- MedicationRequest (Medication) ---------------------------------------

    @GetMapping(value = "/MedicationRequest", produces = CONTENT_TYPE)
    public Map<String, Object> searchMedications(@RequestParam Long patient) {
        return mapper.searchset(medicationService.getForPatient(patient).stream()
                .map(mapper::toMedicationRequest).toList());
    }

    @PostMapping(value = "/MedicationRequest", produces = CONTENT_TYPE)
    public ResponseEntity<Map<String, Object>> createMedication(@RequestBody Map<String, Object> body) {
        var medication = mapper.fromMedicationRequest(body);
        var created = medicationService.create(medication.getPatientId(), medication);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toMedicationRequest(created));
    }

    @DeleteMapping("/MedicationRequest/{id}")
    public ResponseEntity<Void> deleteMedication(@PathVariable Long id) {
        medicationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---- AllergyIntolerance (Allergy) -----------------------------------------

    @GetMapping(value = "/AllergyIntolerance", produces = CONTENT_TYPE)
    public Map<String, Object> searchAllergies(@RequestParam Long patient) {
        return mapper.searchset(allergyService.getForPatient(patient).stream()
                .map(mapper::toAllergyIntolerance).toList());
    }

    @PostMapping(value = "/AllergyIntolerance", produces = CONTENT_TYPE)
    public ResponseEntity<Map<String, Object>> createAllergy(@RequestBody Map<String, Object> body) {
        var allergy = mapper.fromAllergyIntolerance(body);
        var created = allergyService.create(allergy.getPatientId(), allergy);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toAllergyIntolerance(created));
    }

    @DeleteMapping("/AllergyIntolerance/{id}")
    public ResponseEntity<Void> deleteAllergy(@PathVariable Long id) {
        allergyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Observation (VitalSign) ----------------------------------------------

    @GetMapping(value = "/Observation", produces = CONTENT_TYPE)
    public Map<String, Object> searchObservations(@RequestParam Long patient) {
        return mapper.searchset(vitalSignService.getForPatient(patient).stream()
                .map(mapper::toObservation).toList());
    }

    @PostMapping(value = "/Observation", produces = CONTENT_TYPE)
    public ResponseEntity<Map<String, Object>> createObservation(@RequestBody Map<String, Object> body) {
        var vital = mapper.fromObservation(body);
        var created = vitalSignService.create(vital.getPatientId(), vital);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toObservation(created));
    }

    @DeleteMapping("/Observation/{id}")
    public ResponseEntity<Void> deleteObservation(@PathVariable Long id) {
        vitalSignService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Encounter ------------------------------------------------------------

    @GetMapping(value = "/Encounter", produces = CONTENT_TYPE)
    public Map<String, Object> searchEncounters(@RequestParam Long patient) {
        return mapper.searchset(encounterService.getForPatient(patient).stream()
                .map(mapper::toEncounter).toList());
    }

    @PostMapping(value = "/Encounter", produces = CONTENT_TYPE)
    public ResponseEntity<Map<String, Object>> createEncounter(@RequestBody Map<String, Object> body) {
        var encounter = mapper.fromEncounter(body);
        var created = encounterService.create(encounter.getPatientId(), encounter);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toEncounter(created));
    }

    @DeleteMapping("/Encounter/{id}")
    public ResponseEntity<Void> deleteEncounter(@PathVariable Long id) {
        encounterService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Consent --------------------------------------------------------------

    @GetMapping(value = "/Consent", produces = CONTENT_TYPE)
    public Map<String, Object> searchConsents(@RequestParam Long patient) {
        return mapper.searchset(consentService.getConsentsForPatient(patient).stream()
                .map(mapper::toConsent).toList());
    }
}
