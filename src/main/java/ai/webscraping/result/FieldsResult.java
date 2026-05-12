package ai.webscraping.result;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The {@code /ai/fields} response wraps the extracted fields under a
 * {@code result} key — confirmed against the live API 2026-05-12. Mirrors the
 * same shape every other official SDK exposes.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class FieldsResult {

    private final Map<String, String> result;

    public FieldsResult(@JsonProperty("result") Map<String, String> result) {
        this.result = result == null ? Collections.emptyMap() : Collections.unmodifiableMap(result);
    }

    public Map<String, String> getResult() {
        return result;
    }

    @Override
    public String toString() {
        return "FieldsResult{result=" + result + "}";
    }
}
