package com.ehrapi.fhir;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Small helpers for building FHIR R4 JSON structures as ordered maps. Jackson
 * serialises these directly, keeping the FHIR layer dependency-free while still
 * producing spec-shaped resources.
 */
public final class Fhir {

    /** Code systems / identifier systems used by this server. */
    public static final String CONTENT_TYPE = "application/fhir+json";
    public static final String SYS_MRN = "urn:ehr:mrn";
    public static final String SYS_ORG = "urn:ehr:organization";
    public static final String SYS_ICD10 = "http://hl7.org/fhir/sid/icd-10";
    public static final String SYS_LOINC = "http://loinc.org";
    public static final String SYS_CONDITION_CLINICAL =
            "http://terminology.hl7.org/CodeSystem/condition-clinical";
    public static final String SYS_ALLERGY_CLINICAL =
            "http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical";
    public static final String SYS_OBS_CATEGORY =
            "http://terminology.hl7.org/CodeSystem/observation-category";
    public static final String EXT_ENCOUNTER_NOTES = "urn:ehr:encounter-notes";

    private Fhir() {}

    /** Build an ordered map from alternating key/value arguments. Nulls skipped. */
    public static Map<String, Object> obj(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            Object value = kv[i + 1];
            if (value != null) {
                map.put((String) kv[i], value);
            }
        }
        return map;
    }

    public static List<Object> list(Object... items) {
        List<Object> out = new ArrayList<>();
        for (Object item : items) {
            if (item != null) {
                out.add(item);
            }
        }
        return out;
    }

    /** A FHIR Reference, e.g. {"reference":"Patient/1"}. */
    public static Map<String, Object> ref(String type, Object id) {
        if (id == null) {
            return null;
        }
        return obj("reference", type + "/" + id);
    }

    public static Map<String, Object> coding(String system, String code, String display) {
        return obj("system", system, "code", code, "display", display);
    }

    /** A CodeableConcept with a single coding and/or text. */
    public static Map<String, Object> codeable(String system, String code, String display, String text) {
        Map<String, Object> cc = new LinkedHashMap<>();
        if (code != null) {
            cc.put("coding", list(coding(system, code, display)));
        }
        if (text != null) {
            cc.put("text", text);
        }
        return cc.isEmpty() ? null : cc;
    }

    public static Map<String, Object> text(String value) {
        return value == null ? null : obj("text", value);
    }

    public static String date(LocalDate d) {
        return d == null ? null : d.toString();
    }

    public static String dateTime(LocalDateTime d) {
        return d == null ? null : d.toString();
    }
}
