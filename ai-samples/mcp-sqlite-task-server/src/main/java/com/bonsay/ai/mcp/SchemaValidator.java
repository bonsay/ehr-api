package com.bonsay.ai.mcp;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Iterator;
import java.util.Map;

/**
 * Validates tool arguments against the small JSON Schema subset our tools use: object
 * properties, required, type (string/integer/number/boolean), enum, minLength/maxLength and
 * minimum/maximum, plus additionalProperties=false. Never trust the model's arguments.
 */
final class SchemaValidator {
    private SchemaValidator() {}

    static void validate(JsonNode schema, JsonNode args) {
        if (!args.isObject()) throw new IllegalArgumentException("arguments must be an object");
        JsonNode props = schema.path("properties");
        for (JsonNode req : schema.path("required")) {
            if (!args.has(req.asText()) || args.get(req.asText()).isNull()) {
                throw new IllegalArgumentException("missing required argument '" + req.asText() + "'");
            }
        }
        Iterator<Map.Entry<String, JsonNode>> it = args.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            JsonNode prop = props.get(e.getKey());
            if (prop == null) {
                if (schema.path("additionalProperties").isBoolean() && !schema.get("additionalProperties").asBoolean()) {
                    throw new IllegalArgumentException("unexpected argument '" + e.getKey() + "'");
                }
                continue;
            }
            checkValue(e.getKey(), prop, e.getValue());
        }
    }

    private static void checkValue(String name, JsonNode prop, JsonNode value) {
        String type = prop.path("type").asText("");
        boolean ok = switch (type) {
            case "string" -> value.isTextual();
            case "integer" -> value.isIntegralNumber();
            case "number" -> value.isNumber();
            case "boolean" -> value.isBoolean();
            default -> true;
        };
        if (!ok) throw new IllegalArgumentException("'" + name + "' must be of type " + type);

        if (prop.has("enum")) {
            boolean found = false;
            for (JsonNode allowed : prop.get("enum")) found |= allowed.equals(value);
            if (!found) throw new IllegalArgumentException("'" + name + "' must be one of " + prop.get("enum"));
        }
        if (value.isTextual()) {
            int len = value.asText().length();
            if (prop.has("minLength") && len < prop.get("minLength").asInt())
                throw new IllegalArgumentException("'" + name + "' is too short");
            if (prop.has("maxLength") && len > prop.get("maxLength").asInt())
                throw new IllegalArgumentException("'" + name + "' is too long");
        }
        if (value.isNumber()) {
            if (prop.has("minimum") && value.asDouble() < prop.get("minimum").asDouble())
                throw new IllegalArgumentException("'" + name + "' must be >= " + prop.get("minimum"));
            if (prop.has("maximum") && value.asDouble() > prop.get("maximum").asDouble())
                throw new IllegalArgumentException("'" + name + "' must be <= " + prop.get("maximum"));
        }
    }
}
