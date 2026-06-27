package com.ehrapi.fhir;

import com.ehrapi.dto.SharedPatientRecordDto;
import com.ehrapi.entity.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.ehrapi.fhir.Fhir.*;

/**
 * Translates EHR domain entities to FHIR R4 resources (and back, for creates).
 * Resource mapping:
 * <pre>
 *   Patient        -> Patient
 *   Institution    -> Organization
 *   Problem        -> Condition
 *   Medication     -> MedicationRequest
 *   Allergy        -> AllergyIntolerance
 *   VitalSign      -> Observation (vital-signs panel)
 *   Encounter      -> Encounter
 *   PatientConsent -> Consent
 * </pre>
 */
@Component
public class FhirMapper {

    // ---- Patient -------------------------------------------------------------

    public Map<String, Object> toPatient(Patient p) {
        return obj(
                "resourceType", "Patient",
                "id", str(p.getId()),
                "identifier", list(obj("system", SYS_MRN, "value", p.getMrn())),
                "name", list(obj("family", p.getLastName(), "given", list(p.getFirstName()))),
                "gender", fhirGender(p.getGender()),
                "birthDate", date(p.getDateOfBirth()),
                "telecom", telecom(p.getPhone(), p.getEmail()),
                "address", p.getAddress() == null ? null : list(text(p.getAddress())),
                "managingOrganization", ref("Organization", p.getHomeInstitutionId())
        );
    }

    public Patient fromPatient(Map<String, Object> r) {
        Patient p = new Patient();
        List<Object> ids = asList(r.get("identifier"));
        if (ids != null && !ids.isEmpty()) {
            p.setMrn(str(asMap(ids.get(0)).get("value")));
        }
        List<Object> names = asList(r.get("name"));
        if (names != null && !names.isEmpty()) {
            Map<String, Object> n = asMap(names.get(0));
            p.setLastName(str(n.get("family")));
            List<Object> given = asList(n.get("given"));
            if (given != null && !given.isEmpty()) {
                p.setFirstName(str(given.get(0)));
            }
        }
        p.setGender(domainGender(str(r.get("gender"))));
        p.setDateOfBirth(parseDate(str(r.get("birthDate"))));
        readTelecom(asList(r.get("telecom")), p::setPhone, p::setEmail);
        List<Object> addr = asList(r.get("address"));
        if (addr != null && !addr.isEmpty()) {
            p.setAddress(str(asMap(addr.get(0)).get("text")));
        }
        p.setHomeInstitutionId(refId(r.get("managingOrganization")));
        return p;
    }

    // ---- Organization --------------------------------------------------------

    public Map<String, Object> toOrganization(Institution i) {
        return obj(
                "resourceType", "Organization",
                "id", str(i.getId()),
                "identifier", list(obj("system", SYS_ORG, "value", i.getCode())),
                "active", i.isActive(),
                "type", i.getType() == null ? null : list(text(i.getType())),
                "name", i.getName(),
                "telecom", telecom(i.getPhone(), null),
                "address", i.getAddress() == null ? null : list(text(i.getAddress()))
        );
    }

    // ---- Condition (Problem) -------------------------------------------------

    public Map<String, Object> toCondition(Problem p) {
        return obj(
                "resourceType", "Condition",
                "id", str(p.getId()),
                "clinicalStatus", clinicalStatus(SYS_CONDITION_CLINICAL, p.getStatus()),
                "code", codeable(SYS_ICD10, p.getCode(), p.getDescription(), p.getDescription()),
                "subject", ref("Patient", p.getPatientId()),
                "recorder", ref("Organization", p.getInstitutionId()),
                "onsetDateTime", date(p.getOnsetDate()),
                "recordedDate", date(p.getRecordedDate())
        );
    }

    public Problem fromCondition(Map<String, Object> r) {
        Problem p = new Problem();
        p.setPatientId(refId(r.get("subject")));
        p.setInstitutionId(refId(r.get("recorder")));
        Map<String, Object> code = asMap(r.get("code"));
        if (code != null) {
            p.setDescription(str(code.get("text")));
            String c = firstCodingCode(code);
            if (c != null) { p.setCode(c); }
            if (p.getDescription() == null) { p.setDescription(firstCodingDisplay(code)); }
        }
        String status = clinicalStatusCode(r.get("clinicalStatus"));
        if (status != null) { p.setStatus(status.toUpperCase()); }
        p.setOnsetDate(parseDate(str(r.get("onsetDateTime"))));
        return p;
    }

    // ---- MedicationRequest (Medication) --------------------------------------

    public Map<String, Object> toMedicationRequest(Medication m) {
        String dosageText = join(m.getDosage(), m.getFrequency());
        Map<String, Object> dosage = obj(
                "text", dosageText,
                "timing", m.getFrequency() == null ? null : obj("code", text(m.getFrequency())),
                "route", m.getRoute() == null ? null : text(m.getRoute()));
        return obj(
                "resourceType", "MedicationRequest",
                "id", str(m.getId()),
                "status", fhirMedStatus(m.getStatus()),
                "intent", "order",
                "medicationCodeableConcept", text(m.getName()),
                "subject", ref("Patient", m.getPatientId()),
                "requester", m.getPrescriber() == null ? null : obj("display", m.getPrescriber()),
                "authoredOn", date(m.getStartDate()),
                "dosageInstruction", dosage.isEmpty() ? null : list(dosage),
                "dispenseRequest", obj("validityPeriod",
                        obj("start", date(m.getStartDate()), "end", date(m.getEndDate()))),
                "recorder", ref("Organization", m.getInstitutionId())
        );
    }

    public Medication fromMedicationRequest(Map<String, Object> r) {
        Medication m = new Medication();
        m.setPatientId(refId(r.get("subject")));
        m.setInstitutionId(refId(r.get("recorder")));
        Map<String, Object> med = asMap(r.get("medicationCodeableConcept"));
        if (med != null) { m.setName(str(med.get("text"))); }
        m.setStatus(domainMedStatus(str(r.get("status"))));
        m.setStartDate(parseDate(str(r.get("authoredOn"))));
        Map<String, Object> requester = asMap(r.get("requester"));
        if (requester != null) { m.setPrescriber(str(requester.get("display"))); }
        List<Object> dosages = asList(r.get("dosageInstruction"));
        if (dosages != null && !dosages.isEmpty()) {
            Map<String, Object> d = asMap(dosages.get(0));
            Map<String, Object> route = asMap(d.get("route"));
            if (route != null) { m.setRoute(str(route.get("text"))); }
            Map<String, Object> timing = asMap(d.get("timing"));
            if (timing != null) {
                Map<String, Object> tc = asMap(timing.get("code"));
                if (tc != null) { m.setFrequency(str(tc.get("text"))); }
            }
            if (m.getDosage() == null) { m.setDosage(str(d.get("text"))); }
        }
        return m;
    }

    // ---- AllergyIntolerance (Allergy) ----------------------------------------

    public Map<String, Object> toAllergyIntolerance(Allergy a) {
        Map<String, Object> reaction = obj(
                "manifestation", a.getReaction() == null ? null : list(text(a.getReaction())),
                "severity", fhirAllergySeverity(a.getSeverity()));
        return obj(
                "resourceType", "AllergyIntolerance",
                "id", str(a.getId()),
                "clinicalStatus", clinicalStatus(SYS_ALLERGY_CLINICAL, a.getStatus()),
                "code", text(a.getAllergen()),
                "patient", ref("Patient", a.getPatientId()),
                "recorder", ref("Organization", a.getInstitutionId()),
                "recordedDate", date(a.getRecordedDate()),
                "reaction", reaction.isEmpty() ? null : list(reaction)
        );
    }

    public Allergy fromAllergyIntolerance(Map<String, Object> r) {
        Allergy a = new Allergy();
        a.setPatientId(refId(r.get("patient")));
        a.setInstitutionId(refId(r.get("recorder")));
        Map<String, Object> code = asMap(r.get("code"));
        if (code != null) { a.setAllergen(str(code.get("text"))); }
        String status = clinicalStatusCode(r.get("clinicalStatus"));
        if (status != null) { a.setStatus(status.toUpperCase()); }
        List<Object> reactions = asList(r.get("reaction"));
        if (reactions != null && !reactions.isEmpty()) {
            Map<String, Object> rx = asMap(reactions.get(0));
            a.setSeverity(domainAllergySeverity(str(rx.get("severity"))));
            List<Object> manifest = asList(rx.get("manifestation"));
            if (manifest != null && !manifest.isEmpty()) {
                a.setReaction(str(asMap(manifest.get(0)).get("text")));
            }
        }
        return a;
    }

    // ---- Observation (VitalSign) ---------------------------------------------

    public Map<String, Object> toObservation(VitalSign v) {
        List<Object> components = new ArrayList<>();
        addStringComponent(components, "Blood pressure", v.getBloodPressure());
        addQuantityComponent(components, SYS_LOINC, "8867-4", "Heart rate", v.getHeartRate(), "beats/minute", "/min");
        addQuantityComponent(components, SYS_LOINC, "9279-1", "Respiratory rate", v.getRespiratoryRate(), "breaths/minute", "/min");
        addQuantityComponent(components, SYS_LOINC, "8310-5", "Body temperature", v.getTemperature(), "C", "Cel");
        addQuantityComponent(components, SYS_LOINC, "2708-6", "Oxygen saturation", v.getOxygenSaturation(), "%", "%");
        addQuantityComponent(components, SYS_LOINC, "8302-2", "Body height", v.getHeight(), "cm", "cm");
        addQuantityComponent(components, SYS_LOINC, "29463-7", "Body weight", v.getWeight(), "kg", "kg");
        return obj(
                "resourceType", "Observation",
                "id", str(v.getId()),
                "status", "final",
                "category", list(obj("coding",
                        list(coding(SYS_OBS_CATEGORY, "vital-signs", "Vital Signs")))),
                "code", obj("coding", list(coding(SYS_LOINC, "85353-1", "Vital signs, weight, height, ...")),
                        "text", "Vital signs panel"),
                "subject", ref("Patient", v.getPatientId()),
                "performer", v.getInstitutionId() == null ? null : list(ref("Organization", v.getInstitutionId())),
                "effectiveDateTime", dateTime(v.getRecordedDate()),
                "component", components.isEmpty() ? null : components
        );
    }

    public VitalSign fromObservation(Map<String, Object> r) {
        VitalSign v = new VitalSign();
        v.setPatientId(refId(r.get("subject")));
        List<Object> performers = asList(r.get("performer"));
        if (performers != null && !performers.isEmpty()) {
            v.setInstitutionId(refId(performers.get(0)));
        }
        v.setRecordedDate(parseDateTime(str(r.get("effectiveDateTime"))));
        List<Object> components = asList(r.get("component"));
        if (components != null) {
            for (Object c : components) {
                Map<String, Object> comp = asMap(c);
                String label = componentLabel(comp);
                if (label == null) { continue; }
                Map<String, Object> qty = asMap(comp.get("valueQuantity"));
                Number value = qty == null ? null : (Number) qty.get("value");
                switch (label) {
                    case "Blood pressure" -> v.setBloodPressure(str(comp.get("valueString")));
                    case "Heart rate" -> v.setHeartRate(value == null ? null : value.intValue());
                    case "Respiratory rate" -> v.setRespiratoryRate(value == null ? null : value.intValue());
                    case "Body temperature" -> v.setTemperature(value == null ? null : value.doubleValue());
                    case "Oxygen saturation" -> v.setOxygenSaturation(value == null ? null : value.intValue());
                    case "Body height" -> v.setHeight(value == null ? null : value.doubleValue());
                    case "Body weight" -> v.setWeight(value == null ? null : value.doubleValue());
                    default -> { /* ignore unknown component */ }
                }
            }
        }
        return v;
    }

    // ---- Encounter -----------------------------------------------------------

    public Map<String, Object> toEncounter(Encounter e) {
        Map<String, Object> resource = obj(
                "resourceType", "Encounter",
                "id", str(e.getId()),
                "status", fhirEncounterStatus(e.getStatus()),
                "class", coding("http://terminology.hl7.org/CodeSystem/v3-ActCode", "AMB", "ambulatory"),
                "type", e.getType() == null ? null : list(text(e.getType())),
                "subject", ref("Patient", e.getPatientId()),
                "period", obj("start", dateTime(e.getEncounterDate())),
                "reasonCode", e.getReason() == null ? null : list(text(e.getReason())),
                "participant", e.getProviderName() == null ? null
                        : list(obj("individual", obj("display", e.getProviderName()))),
                "serviceProvider", ref("Organization", e.getInstitutionId())
        );
        if (e.getNotes() != null) {
            resource.put("extension", list(obj("url", EXT_ENCOUNTER_NOTES, "valueString", e.getNotes())));
        }
        return resource;
    }

    public Encounter fromEncounter(Map<String, Object> r) {
        Encounter e = new Encounter();
        e.setPatientId(refId(r.get("subject")));
        e.setInstitutionId(refId(r.get("serviceProvider")));
        e.setStatus(domainEncounterStatus(str(r.get("status"))));
        List<Object> types = asList(r.get("type"));
        if (types != null && !types.isEmpty()) {
            e.setType(str(asMap(types.get(0)).get("text")));
        }
        Map<String, Object> period = asMap(r.get("period"));
        if (period != null) {
            LocalDateTime start = parseDateTime(str(period.get("start")));
            if (start != null) { e.setEncounterDate(start); }
        }
        List<Object> reasons = asList(r.get("reasonCode"));
        if (reasons != null && !reasons.isEmpty()) {
            e.setReason(str(asMap(reasons.get(0)).get("text")));
        }
        List<Object> participants = asList(r.get("participant"));
        if (participants != null && !participants.isEmpty()) {
            Map<String, Object> ind = asMap(asMap(participants.get(0)).get("individual"));
            if (ind != null) { e.setProviderName(str(ind.get("display"))); }
        }
        List<Object> extensions = asList(r.get("extension"));
        if (extensions != null) {
            for (Object ext : extensions) {
                Map<String, Object> ex = asMap(ext);
                if (ex != null && EXT_ENCOUNTER_NOTES.equals(str(ex.get("url")))) {
                    e.setNotes(str(ex.get("valueString")));
                }
            }
        }
        return e;
    }

    // ---- Consent -------------------------------------------------------------

    public Map<String, Object> toConsent(PatientConsent c) {
        List<Object> data = new ArrayList<>();
        if (c.getScope() != null && !"ALL".equalsIgnoreCase(c.getScope())) {
            for (String module : c.getScope().split(",")) {
                data.add(obj("meaning", "instance",
                        "reference", obj("display", module.trim())));
            }
        }
        Map<String, Object> provision = obj(
                "type", "permit",
                "period", c.getExpiryDate() == null ? null : obj("end", dateTime(c.getExpiryDate())),
                "actor", list(obj(
                        "role", codeable("http://terminology.hl7.org/CodeSystem/v3-RoleClass", "PROV", "healthcare provider", null),
                        "reference", ref("Organization", c.getGrantedToInstitutionId()))),
                "data", data.isEmpty() ? null : data);
        return obj(
                "resourceType", "Consent",
                "id", str(c.getId()),
                "status", fhirConsentStatus(c.getStatus()),
                "scope", codeable("http://terminology.hl7.org/CodeSystem/consentscope",
                        "patient-privacy", "Privacy Consent", null),
                "category", list(codeable("http://loinc.org", "59284-0", "Patient Consent", null)),
                "patient", ref("Patient", c.getPatientId()),
                "dateTime", dateTime(c.getGrantedDate()),
                "organization", list(ref("Organization", c.getGrantedToInstitutionId())),
                "provision", provision
        );
    }

    // ---- Bundles -------------------------------------------------------------

    /** A FHIR searchset Bundle wrapping the given resources. */
    public Map<String, Object> searchset(List<Map<String, Object>> resources) {
        List<Object> entries = new ArrayList<>();
        for (Map<String, Object> res : resources) {
            entries.add(obj(
                    "fullUrl", res.get("resourceType") + "/" + res.get("id"),
                    "resource", res));
        }
        return obj(
                "resourceType", "Bundle",
                "type", "searchset",
                "total", resources.size(),
                "entry", entries);
    }

    /**
     * The consent-filtered Patient/$everything Bundle. Withheld modules are
     * reported as an informational OperationOutcome entry (idiomatic FHIR for
     * suppressed data).
     */
    public Map<String, Object> everythingBundle(SharedPatientRecordDto record) {
        List<Map<String, Object>> resources = new ArrayList<>();
        resources.add(toPatient(record.getPatient()));
        record.getEncounters().forEach(e -> resources.add(toEncounter(e)));
        record.getProblems().forEach(p -> resources.add(toCondition(p)));
        record.getMedications().forEach(m -> resources.add(toMedicationRequest(m)));
        record.getAllergies().forEach(a -> resources.add(toAllergyIntolerance(a)));
        record.getVitalSigns().forEach(v -> resources.add(toObservation(v)));

        Map<String, Object> bundle = searchset(resources);
        if (!record.getDeniedModules().isEmpty()) {
            List<Object> issues = new ArrayList<>();
            for (String module : record.getDeniedModules()) {
                issues.add(obj(
                        "severity", "information",
                        "code", "suppressed",
                        "diagnostics", "Module " + module + " withheld: not covered by patient consent"));
            }
            Map<String, Object> outcome = obj("resourceType", "OperationOutcome", "issue", issues);
            @SuppressWarnings("unchecked")
            List<Object> entries = (List<Object>) bundle.get("entry");
            entries.add(obj("resource", outcome));
            bundle.put("total", resources.size());
        }
        return bundle;
    }

    // ---- value-set translations ----------------------------------------------

    private static String fhirGender(String g) {
        if (g == null) { return "unknown"; }
        return switch (g.toUpperCase()) {
            case "MALE" -> "male";
            case "FEMALE" -> "female";
            case "OTHER" -> "other";
            default -> "unknown";
        };
    }

    private static String domainGender(String g) {
        if (g == null) { return null; }
        return switch (g.toLowerCase()) {
            case "male" -> "MALE";
            case "female" -> "FEMALE";
            case "other" -> "OTHER";
            default -> "UNKNOWN";
        };
    }

    private static String fhirMedStatus(String s) {
        if (s == null) { return "unknown"; }
        return switch (s.toUpperCase()) {
            case "ACTIVE" -> "active";
            case "STOPPED" -> "stopped";
            case "COMPLETED" -> "completed";
            default -> "unknown";
        };
    }

    private static String domainMedStatus(String s) {
        return s == null ? "ACTIVE" : s.toUpperCase();
    }

    private static String fhirAllergySeverity(String s) {
        if (s == null) { return null; }
        return switch (s.toUpperCase()) {
            case "MILD" -> "mild";
            case "MODERATE" -> "moderate";
            case "SEVERE" -> "severe";
            default -> null;
        };
    }

    private static String domainAllergySeverity(String s) {
        return s == null ? null : s.toUpperCase();
    }

    private static String fhirEncounterStatus(String s) {
        if (s == null) { return "finished"; }
        return switch (s.toUpperCase()) {
            case "COMPLETED", "FINISHED" -> "finished";
            case "IN_PROGRESS", "IN-PROGRESS" -> "in-progress";
            case "CANCELLED" -> "cancelled";
            case "PLANNED" -> "planned";
            default -> "finished";
        };
    }

    private static String domainEncounterStatus(String s) {
        if (s == null) { return "COMPLETED"; }
        return switch (s.toLowerCase()) {
            case "finished" -> "COMPLETED";
            case "in-progress" -> "IN_PROGRESS";
            case "cancelled" -> "CANCELLED";
            case "planned" -> "PLANNED";
            default -> s.toUpperCase();
        };
    }

    private static String fhirConsentStatus(String s) {
        if (s == null) { return "active"; }
        return "ACTIVE".equalsIgnoreCase(s) ? "active" : "inactive";
    }

    // ---- low-level helpers ---------------------------------------------------

    private static Map<String, Object> clinicalStatus(String system, String domainStatus) {
        if (domainStatus == null) { return null; }
        return obj("coding", list(coding(system, domainStatus.toLowerCase(), null)));
    }

    private static String clinicalStatusCode(Object node) {
        Map<String, Object> cs = asMap(node);
        return cs == null ? null : firstCodingCode(cs);
    }

    private static List<Object> telecom(String phone, String email) {
        List<Object> out = new ArrayList<>();
        if (phone != null) { out.add(obj("system", "phone", "value", phone)); }
        if (email != null) { out.add(obj("system", "email", "value", email)); }
        return out.isEmpty() ? null : out;
    }

    private static void readTelecom(List<Object> telecom, java.util.function.Consumer<String> phone,
                                    java.util.function.Consumer<String> email) {
        if (telecom == null) { return; }
        for (Object t : telecom) {
            Map<String, Object> entry = asMap(t);
            if (entry == null) { continue; }
            String system = str(entry.get("system"));
            String value = str(entry.get("value"));
            if ("phone".equals(system)) { phone.accept(value); }
            else if ("email".equals(system)) { email.accept(value); }
        }
    }

    private static void addStringComponent(List<Object> components, String label, String value) {
        if (value == null) { return; }
        components.add(obj("code", text(label), "valueString", value));
    }

    private static void addQuantityComponent(List<Object> components, String system, String code,
                                             String label, Number value, String unit, String ucum) {
        if (value == null) { return; }
        components.add(obj(
                "code", obj("coding", list(coding(system, code, label)), "text", label),
                "valueQuantity", obj("value", value, "unit", unit, "system", "http://unitsofmeasure.org", "code", ucum)));
    }

    private static String componentLabel(Map<String, Object> component) {
        Map<String, Object> code = asMap(component.get("code"));
        if (code == null) { return null; }
        String text = str(code.get("text"));
        if (text != null) { return text; }
        return firstCodingDisplay(code);
    }

    private static String firstCodingCode(Map<String, Object> codeable) {
        List<Object> coding = asList(codeable.get("coding"));
        if (coding == null || coding.isEmpty()) { return null; }
        return str(asMap(coding.get(0)).get("code"));
    }

    private static String firstCodingDisplay(Map<String, Object> codeable) {
        List<Object> coding = asList(codeable.get("coding"));
        if (coding == null || coding.isEmpty()) { return null; }
        return str(asMap(coding.get(0)).get("display"));
    }

    /** Extract the numeric id from a Reference like {"reference":"Patient/1"}. */
    private static Long refId(Object referenceNode) {
        Map<String, Object> ref = asMap(referenceNode);
        if (ref == null) { return null; }
        String reference = str(ref.get("reference"));
        if (reference == null || !reference.contains("/")) { return null; }
        try {
            return Long.valueOf(reference.substring(reference.lastIndexOf('/') + 1));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String join(String a, String b) {
        if (a == null && b == null) { return null; }
        if (a == null) { return b; }
        if (b == null) { return a; }
        return a + " " + b;
    }

    private static LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) { return null; }
        try {
            return LocalDate.parse(s.length() > 10 ? s.substring(0, 10) : s);
        } catch (Exception ex) {
            return null;
        }
    }

    private static LocalDateTime parseDateTime(String s) {
        if (s == null || s.isBlank()) { return null; }
        try {
            return LocalDateTime.parse(s);
        } catch (Exception ex) {
            LocalDate d = parseDate(s);
            return d == null ? null : d.atStartOfDay();
        }
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : null;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asList(Object o) {
        return o instanceof List ? (List<Object>) o : null;
    }
}
